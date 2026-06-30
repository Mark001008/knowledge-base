package com.ma.agent.agent;

import com.ma.agent.agent.dto.KnowledgeQAResponse;
import com.ma.agent.entity.ConversationEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 知识库问答服务接口
 * <p>基于 RAG 检索增强生成，结合知识库文档内容回答用户问题</p>
 */
public interface KnowledgeQAService {

    /**
     * 知识库问答（同步）
     *
     * @param question       用户问题
     * @param kbId           知识库ID
     * @param conversationId 会话ID（为空时创建新会话）
     * @return 问答响应（包含答案和引用来源）
     */
    KnowledgeQAResponse ask(String question, String kbId, String conversationId);

    /**
     * 知识库问答（SSE 流式）
     *
     * @param question       用户问题
     * @param kbId           知识库ID
     * @param conversationId 会话ID
     * @return SSE 发射器
     */
    SseEmitter askStream(String question, String kbId, String conversationId);

    /**
     * 获取对话历史
     */
    List<ConversationEntity> getConversationHistory(String conversationId);
}
