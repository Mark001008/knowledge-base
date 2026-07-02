package com.ma.kb.service.space.dto;

/**
 * 添加知识库成员请求
 */
public record SpaceMemberRequest(
        Long userId,
        String role
) {
}
