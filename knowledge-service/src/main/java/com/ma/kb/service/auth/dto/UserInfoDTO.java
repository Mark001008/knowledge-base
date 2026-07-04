package com.ma.kb.service.auth.dto;

import java.util.List;

/**
 * 用户信息 DTO
 */
public record UserInfoDTO(
        Long id,
        String username,
        String displayName,
        List<RoleDTO> roles
) {
}
