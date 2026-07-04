package com.ma.kb.service.system.dto;

/**
 * 创建权限请求
 */
public record CreatePermissionRequest(
        String permissionCode,
        String permissionName,
        String module,
        String description
) {
}
