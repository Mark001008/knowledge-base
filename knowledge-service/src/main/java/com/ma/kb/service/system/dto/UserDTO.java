package com.ma.kb.service.system.dto;

import com.ma.kb.service.auth.dto.RoleDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户 DTO
 */
public record UserDTO(
        Long id,
        String username,
        String displayName,
        String email,
        String status,
        List<RoleDTO> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
