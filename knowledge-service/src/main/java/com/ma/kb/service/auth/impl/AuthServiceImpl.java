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
import com.ma.kb.service.auth.dto.MenuDTO;
import com.ma.kb.service.auth.dto.RoleDTO;
import com.ma.kb.service.auth.dto.UserInfoDTO;
import com.ma.kb.service.system.MenuService;
import com.ma.kb.service.system.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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
    private final PermissionService permissionService;
    private final MenuService menuService;

    public AuthServiceImpl(UserManager userManager, PasswordEncoder passwordEncoder,
                           JwtService jwtService, UserDTOConverter userDTOConverter,
                           PermissionService permissionService, MenuService menuService) {
        this.userManager = userManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userDTOConverter = userDTOConverter;
        this.permissionService = permissionService;
        this.menuService = menuService;
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

        // 查询权限码
        List<String> permissions = permissionService.getCurrentUserPermissionCodes(userBO.getId());

        // 查询菜单树
        List<MenuDTO> menus = menuService.getCurrentUserMenus(userBO.getId());

        // 构建用户信息
        UserInfoDTO userInfo = convertToUserInfoDTO(userBO);

        log.info("用户登录成功: {}", request.username());
        return new LoginResponse(accessToken, jwtService.getExpiresIn(), userInfo, permissions, menus);
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

        return convertToUserInfoDTO(userBO);
    }

    private UserInfoDTO convertToUserInfoDTO(UserBO userBO) {
        List<RoleDTO> roles = userBO.getRoles() != null ?
                userBO.getRoles().stream()
                        .map(roleCode -> new RoleDTO(null, roleCode, roleCode))
                        .collect(Collectors.toList()) :
                List.of();
        return new UserInfoDTO(
                userBO.getId(),
                userBO.getUsername(),
                userBO.getDisplayName(),
                roles
        );
    }
}
