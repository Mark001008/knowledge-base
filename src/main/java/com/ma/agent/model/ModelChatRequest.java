package com.ma.agent.model;

/**
 * 模型网关层对话请求 DTO，各 ModelGateway 实现统一的入参。
 *
 * @param conversationId 会话ID
 * @param message        用户消息文本
 */
public record ModelChatRequest(
        String conversationId,
        String message
) {
}
