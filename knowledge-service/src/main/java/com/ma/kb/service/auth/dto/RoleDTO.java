package com.ma.kb.service.auth.dto;

/**
 * 角色 DTO
 */
public record RoleDTO(
        Long id,
        String roleCode,
        String roleName
) {
}
