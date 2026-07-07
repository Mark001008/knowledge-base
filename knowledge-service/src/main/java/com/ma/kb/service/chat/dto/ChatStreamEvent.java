package com.ma.kb.service.chat.dto;

/**
 * SSE 问答流事件。
 */
public record ChatStreamEvent(
        String type,
        String content,
        ChatMessageResponse message
) {
}
