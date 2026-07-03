package com.ma.kb.start.controller.auth;

import com.ma.kb.common.response.ApiResponse;
import com.ma.kb.core.auth.SecurityUtils;
import com.ma.kb.service.auth.AuthService;
import com.ma.kb.service.auth.dto.LoginRequest;
import com.ma.kb.service.auth.dto.LoginResponse;
import com.ma.kb.service.auth.dto.UserInfoDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success(response);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ApiResponse<UserInfoDTO> getCurrentUser(HttpServletRequest request) {
        String token = SecurityUtils.extractBearerToken(request.getHeader("Authorization"));
        UserInfoDTO userInfo = authService.getCurrentUser(token);
        return ApiResponse.success(userInfo);
    }
}
