package com.ma.kb.service.chat.dto;

import jakarta.validation.constraints.Size;

/**
 * 创建会话请求
 */
public record ChatSessionCreateRequest(
        @Size(max = 200, message = "会话标题长度不能超过200字符") String title
) {
}
