package com.ma.kb.core.chat;

import com.ma.kb.integration.vector.SearchResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RAG Prompt 构造器
 */
@Component
public class PromptBuilder {

    private static final String SYSTEM_PROMPT = """
            你是企业知识库助手。请只根据提供的上下文回答用户问题。
            如果上下文中没有答案，请回答"当前知识库中未找到相关信息"。
            不要编造事实，不要编造引用来源。
            回答中的关键事实必须能被上下文支持；每个段落尽量标注对应引用编号，例如 [引用1]。
            如果只能找到弱相关材料，请明确说明"以下仅为弱命中参考，需要核验原文"。
            回答应简洁、准确、结构清晰。
            """;

    /**
     * 构造系统提示词
     */
    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    /**
     * 构造带上下文的用户消息（支持多轮对话历史）
     *
     * @param question 用户问题
     * @param results  检索结果
     * @param history  历史消息列表（可为null，表示无历史）
     * @return 包含上下文的用户消息
     */
    public String buildUserMessageWith(String question, List<SearchResult> results, 
                                        List<ChatMessage> history) {
        StringBuilder sb = new StringBuilder();
        
        // 添加对话历史（如果有）
        if (history != null && !history.isEmpty()) {
            sb.append("对话历史：\n");
            for (ChatMessage msg : history) {
                String roleLabel = "user".equals(msg.getRole()) ? "用户" : "助手";
                sb.append(roleLabel).append(": ").append(msg.getContent()).append("\n");
            }
            sb.append("\n");
        }
        
        // 添加检索上下文
        if (results != null && !results.isEmpty()) {
            sb.append("上下文：\n\n");
            for (int i = 0; i < results.size(); i++) {
                SearchResult result = results.get(i);
                sb.append(String.format("[引用%d] 文档：%s", i + 1, result.getDocumentName()));
                if (result.getPageNumber() != null && result.getPageNumber() > 0) {
                    sb.append(String.format("，页码：%d", result.getPageNumber()));
                }
                if (result.getChunkIndex() != null) {
                    sb.append(String.format("，分片：%d", result.getChunkIndex()));
                }
                sb.append("\n");
                sb.append(result.getContent());
                sb.append("\n\n");
            }
        }
        
        // 添加当前问题
        sb.append("用户问题：\n").append(question);
        
        return sb.toString();
    }

    /**
     * 兼容旧接口：无历史消息的单轮问答
     */
    public String buildUserMessageWith(String question, List<SearchResult> results) {
        return buildUserMessageWith(question, results, null);
    }

    /**
     * 生成无上下文时的固定回答
     */
    public String buildNoContextAnswer() {
        return "当前知识库中未找到相关信息";
    }
    
    /**
     * 内部类：用于历史消息传递
     */
    public record ChatMessage(String role, String content) {}
}
