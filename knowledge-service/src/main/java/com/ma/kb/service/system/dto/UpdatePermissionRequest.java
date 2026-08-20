package com.ma.kb.service.system.dto;

import jakarta.validation.constraints.Size;

/**
 * 更新权限请求
 */
public record UpdatePermissionRequest(
        @Size(max = 100, message = "权限名称长度不能超过100") String permissionName,
        @Size(max = 50, message = "模块长度不能超过50") String module,
        @Size(max = 500, message = "描述长度不能超过500") String description,
        @Size(max = 20, message = "状态值长度不能超过20") String status
) {
}
