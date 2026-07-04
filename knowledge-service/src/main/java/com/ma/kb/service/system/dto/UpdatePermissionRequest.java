package com.ma.kb.service.system.dto;

/**
 * 更新权限请求
 */
public record UpdatePermissionRequest(
        String permissionName,
        String module,
        String description,
        String status
) {
}
