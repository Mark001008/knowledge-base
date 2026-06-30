package com.ma.agent.agent.dto;

/**
 * 流式对话 SSE 事件块 DTO。
 *
 * @param conversationId 会话ID
 * @param delta          增量文本内容
 * @param done           是否已结束
 */
public record ChatStreamChunk(
        String conversationId,
        String delta,
        boolean done
) {
}
