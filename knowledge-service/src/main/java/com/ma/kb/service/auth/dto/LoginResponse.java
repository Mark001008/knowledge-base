package com.ma.kb.service.auth.dto;

import java.util.List;

/**
 * 登录响应
 */
public record LoginResponse(
        String accessToken,
        long expiresIn,
        UserInfoDTO user,
        List<String> permissions,
        List<MenuDTO> menus
) {
}
