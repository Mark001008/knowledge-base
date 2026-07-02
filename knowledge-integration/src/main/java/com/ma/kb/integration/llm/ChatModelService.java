package com.ma.kb.integration.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * LLM 聊天模型服务
 * MVP 阶段使用 Mock 实现，后续接入 OpenAI 兼容接口
 */
@Service
public class ChatModelService {

    private static final Logger log = LoggerFactory.getLogger(ChatModelService.class);

    /**
     * 调用 LLM 生成回答
     *
     * @param systemPrompt 系统提示词
     * @param userMessage  用户消息
     * @return 模型回答
     */
    public ChatResponse chat(String systemPrompt, String userMessage) {
        // MVP Mock: 返回固定回答
        log.info("LLM 调用 (mock): systemPromptLength={}, userMessageLength={}",
                systemPrompt.length(), userMessage.length());

        String answer = "这是 Mock 回答。当前系统未配置真实的 LLM 模型服务。\n\n"
                + "您的问题是：" + userMessage + "\n\n"
                + "请配置 AI_BASE_URL 和 AI_API_KEY 环境变量以启用真实的 AI 问答功能。";

        return new ChatResponse(answer, "mock-model", 100, 50);
    }

    /**
     * LLM 响应
     */
    public record ChatResponse(
            String answer,
            String modelName,
            int promptTokens,
            int completionTokens
    ) {
    }
}
