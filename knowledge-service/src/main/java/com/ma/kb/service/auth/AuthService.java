package com.ma.kb.service.auth;

import com.ma.kb.service.auth.dto.LoginRequest;
import com.ma.kb.service.auth.dto.LoginResponse;
import com.ma.kb.service.auth.dto.UserInfoDTO;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);

    /**
     * 获取当前用户信息
     */
    UserInfoDTO getCurrentUser(String token);
}
