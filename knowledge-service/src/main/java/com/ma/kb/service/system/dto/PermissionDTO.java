package com.ma.kb.service.system.dto;

import java.time.LocalDateTime;

/**
 * 权限 DTO
 */
public record PermissionDTO(
        Long id,
        String permissionCode,
        String permissionName,
        String module,
        String description,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
