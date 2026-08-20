package com.ma.kb.service.system.dto;

import jakarta.validation.constraints.Size;

/**
 * 更新用户请求
 */
public record UpdateUserRequest(
        @Size(max = 100, message = "显示名称长度不能超过100") String displayName,
        @Size(max = 200, message = "邮箱长度不能超过200") String email,
        @Size(max = 20, message = "状态值长度不能超过20") String status
) {
}
