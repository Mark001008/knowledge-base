package com.ma.kb.service.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建用户请求
 */
public record CreateUserRequest(
        @NotBlank(message = "用户名不能为空") @Size(max = 50, message = "用户名长度不能超过50") String username,
        @NotBlank(message = "密码不能为空") @Size(min = 6, max = 100, message = "密码长度6-100") String password,
        @Size(max = 100, message = "显示名称长度不能超过100") String displayName,
        @Size(max = 200, message = "邮箱长度不能超过200") String email
) {
}
