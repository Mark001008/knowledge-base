package com.ma.kb.service.system.dto;

import jakarta.validation.constraints.Size;

/**
 * 更新角色请求
 */
public record UpdateRoleRequest(
        @Size(max = 100, message = "角色名称长度不能超过100") String roleName,
        @Size(max = 500, message = "描述长度不能超过500") String description,
        @Size(max = 20, message = "状态值长度不能超过20") String status
) {
}
