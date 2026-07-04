package com.ma.kb.service.system.dto;

/**
 * 更新角色请求
 */
public record UpdateRoleRequest(
        String roleName,
        String description,
        String status
) {
}
