package com.ma.kb.service.system.dto;

/**
 * 更新用户请求
 */
public record UpdateUserRequest(
        String displayName,
        String email,
        String status
) {
}
