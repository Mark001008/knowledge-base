package com.ma.kb.service.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建角色请求
 */
public record CreateRoleRequest(
        @NotBlank(message = "角色编码不能为空") @Size(max = 50, message = "角色编码长度不能超过50") String roleCode,
        @NotBlank(message = "角色名称不能为空") @Size(max = 100, message = "角色名称长度不能超过100") String roleName,
        @Size(max = 500, message = "描述长度不能超过500") String description
) {
}
