package com.ma.kb.service.system.dto;

/**
 * 创建角色请求
 */
public record CreateRoleRequest(
        String roleCode,
        String roleName,
        String description
) {
}
