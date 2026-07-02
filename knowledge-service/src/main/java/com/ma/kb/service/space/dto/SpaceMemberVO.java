package com.ma.kb.service.space.dto;

import java.time.LocalDateTime;

/**
 * 知识库成员视图对象
 */
public record SpaceMemberVO(
        Long userId,
        String username,
        String displayName,
        String role,
        LocalDateTime createdAt
) {
}
