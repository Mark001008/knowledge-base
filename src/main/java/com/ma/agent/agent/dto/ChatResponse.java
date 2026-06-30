package com.ma.agent.agent.dto;

import java.time.Instant;

/**
 * 普通对话响应 DTO。
 *
 * @param conversationId 会话ID
 * @param message        模型回复消息文本
 * @param model          使用的模型名称
 * @param createdAt      消息创建时间
 */
public record ChatResponse(
        String conversationId,
        String message,
        String model,
        Instant createdAt
) {
}
