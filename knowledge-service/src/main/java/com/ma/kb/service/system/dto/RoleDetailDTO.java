package com.ma.kb.service.system.dto;

import java.time.LocalDateTime;

/**
 * 角色详情 DTO
 */
public record RoleDetailDTO(
        Long id,
        String roleCode,
        String roleName,
        String description,
        Integer builtin,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
