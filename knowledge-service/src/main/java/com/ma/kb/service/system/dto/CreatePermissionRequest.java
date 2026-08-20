package com.ma.kb.service.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建权限请求
 */
public record CreatePermissionRequest(
        @NotBlank(message = "权限编码不能为空") @Size(max = 100, message = "权限编码长度不能超过100") String permissionCode,
        @NotBlank(message = "权限名称不能为空") @Size(max = 100, message = "权限名称长度不能超过100") String permissionName,
        @NotBlank(message = "模块不能为空") @Size(max = 50, message = "模块长度不能超过50") String module,
        @Size(max = 500, message = "描述长度不能超过500") String description
) {
}
