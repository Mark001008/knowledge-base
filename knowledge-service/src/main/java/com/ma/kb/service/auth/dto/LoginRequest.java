package com.ma.kb.service.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求
 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空") @Size(max = 50, message = "用户名长度不能超过50") String username,
        @NotBlank(message = "密码不能为空") @Size(min = 6, max = 100, message = "密码长度6-100") String password
) {
}
