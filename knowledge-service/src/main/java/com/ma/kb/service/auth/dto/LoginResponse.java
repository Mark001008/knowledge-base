package com.ma.kb.service.auth.dto;

/**
 * 登录响应
 */
public record LoginResponse(
        String accessToken,
        long expiresIn,
        UserInfoDTO user
) {
}
