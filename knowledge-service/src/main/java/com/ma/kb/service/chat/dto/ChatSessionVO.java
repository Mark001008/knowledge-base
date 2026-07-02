package com.ma.kb.service.chat.dto;

import java.time.LocalDateTime;

/**
 * 会话视图对象
 */
public record ChatSessionVO(
        Long id,
        Long spaceId,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
