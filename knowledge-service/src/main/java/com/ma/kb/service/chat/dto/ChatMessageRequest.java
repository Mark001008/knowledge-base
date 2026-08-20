package com.ma.kb.service.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发送消息请求
 */
public record ChatMessageRequest(
        @NotBlank(message = "问题不能为空") @Size(max = 5000, message = "问题长度不能超过5000字符") String question
) {
}
