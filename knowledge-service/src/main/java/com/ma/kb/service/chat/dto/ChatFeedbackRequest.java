package com.ma.kb.service.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 问答反馈请求。
 */
public record ChatFeedbackRequest(
        @NotNull(message = "消息ID不能为空") Long messageId,
        @NotBlank(message = "评分不能为空") @Size(max = 20, message = "评分长度不能超过20") String rating,
        @Size(max = 500, message = "反馈原因长度不能超过500字符") String reason
) {
}
