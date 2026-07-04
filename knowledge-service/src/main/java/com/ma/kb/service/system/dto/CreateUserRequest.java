package com.ma.kb.service.system.dto;

/**
 * 创建用户请求
 */
public record CreateUserRequest(
        String username,
        String password,
        String displayName,
        String email
) {
}
