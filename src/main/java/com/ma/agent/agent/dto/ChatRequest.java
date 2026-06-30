package com.ma.agent.agent.dto;

/**
 * 普通对话请求 DTO。
 *
 * @param conversationId 会话ID，为空时创建新会话
 * @param message        用户发送的消息文本
 */
public record ChatRequest(
        String conversationId,
        String message
) {
}
