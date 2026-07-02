package com.ma.kb.service.auth.impl;

import com.ma.kb.common.enums.UserStatusEnum;
import com.ma.kb.common.exception.BusinessException;
import com.ma.kb.common.response.ErrorCode;
import com.ma.kb.core.auth.JwtService;
import com.ma.kb.manager.auth.UserManager;
import com.ma.kb.manager.auth.bo.UserBO;
import com.ma.kb.service.auth.AuthService;
import com.ma.kb.service.auth.converter.UserDTOConverter;
import com.ma.kb.service.auth.dto.LoginRequest;
import com.ma.kb.service.auth.dto.LoginResponse;
import com.ma.kb.service.auth.dto.UserInfoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserManager userManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDTOConverter userDTOConverter;

    public AuthServiceImpl(UserManager userManager, PasswordEncoder passwordEncoder,
                           JwtService jwtService, UserDTOConverter userDTOConverter) {
        this.userManager = userManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userDTOConverter = userDTOConverter;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // 查询用户
        UserBO userBO = userManager.getByUsername(request.username());
        if (userBO == null) {
            log.warn("登录失败，用户不存在: {}", request.username());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 校验用户状态
        if (!UserStatusEnum.ENABLED.getCode().equals(userBO.getStatus())) {
            log.warn("登录失败，用户已禁用: {}", request.username());
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        // 校验密码
        if (!passwordEncoder.matches(request.password(), userBO.getPasswordHash())) {
            log.warn("登录失败，密码错误: {}", request.username());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 生成 Token
        String accessToken = jwtService.generateToken(userBO.getId(), userBO.getUsername(), userBO.getRoles());

        // 构建响应
        UserInfoDTO userInfo = userDTOConverter.toUserInfoDTO(userBO);

        log.info("用户登录成功: {}", request.username());
        return new LoginResponse(accessToken, jwtService.getExpiresIn(), userInfo);
    }

    @Override
    public UserInfoDTO getCurrentUser(String token) {
        // 解析 Token
        if (!jwtService.validateToken(token)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        Long userId = jwtService.getUserId(token);
        UserBO userBO = userManager.getById(userId);
        if (userBO == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return userDTOConverter.toUserInfoDTO(userBO);
    }
}
