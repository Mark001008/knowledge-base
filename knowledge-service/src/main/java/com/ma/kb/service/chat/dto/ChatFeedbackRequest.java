package com.ma.kb.service.chat.dto;

/**
 * 问答反馈请求。
 */
public record ChatFeedbackRequest(
        Long messageId,
        String rating,
        String reason
) {
}
