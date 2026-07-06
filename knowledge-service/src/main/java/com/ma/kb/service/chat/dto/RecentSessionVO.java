package com.ma.kb.service.chat.dto;

import java.time.LocalDateTime;

/**
 * 最近会话视图对象
 */
public record RecentSessionVO(
        Long sessionId,
        Long spaceId,
        String spaceName,
        String title,
        LocalDateTime updatedAt
) {
}
