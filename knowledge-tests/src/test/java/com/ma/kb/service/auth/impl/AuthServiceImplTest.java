package com.ma.kb.service.auth.impl;

import com.ma.kb.common.enums.UserStatusEnum;
import com.ma.kb.common.exception.BusinessException;
import com.ma.kb.common.response.ErrorCode;
import com.ma.kb.core.auth.JwtService;
import com.ma.kb.manager.auth.UserManager;
import com.ma.kb.manager.auth.bo.UserBO;
import com.ma.kb.service.auth.converter.UserDTOConverter;
import com.ma.kb.service.auth.dto.LoginRequest;
import com.ma.kb.service.auth.dto.LoginResponse;
import com.ma.kb.service.auth.dto.MenuDTO;
import com.ma.kb.service.auth.dto.RoleDTO;
import com.ma.kb.service.auth.dto.UserInfoDTO;
import com.ma.kb.service.system.MenuService;
import com.ma.kb.service.system.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserManager userManager;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private UserDTOConverter userDTOConverter;
    @Mock
    private PermissionService permissionService;
    @Mock
    private MenuService menuService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userManager, passwordEncoder, jwtService, userDTOConverter, permissionService, menuService);
    }

    // ==================== login ====================

    @Test
    void loginSuccess() {
        LoginRequest request = new LoginRequest("admin", "password123");
        UserBO userBO = buildUserBO(1L, "admin", UserStatusEnum.ENABLED.getCode());
        when(userManager.getByUsername("admin")).thenReturn(userBO);
        when(passwordEncoder.matches("password123", "$2a$10$encoded")).thenReturn(true);
        when(jwtService.generateToken(1L, "admin", List.of("ROLE_ADMIN"))).thenReturn("access-token");
        when(jwtService.getExpiresIn()).thenReturn(7200L);
        when(permissionService.getCurrentUserPermissionCodes(1L)).thenReturn(List.of("space:view", "space:create"));
        when(menuService.getCurrentUserMenus(1L)).thenReturn(List.of());

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access-token", response.accessToken());
        assertEquals(7200L, response.expiresIn());
        assertEquals("Test User", response.user().displayName());
        assertEquals("ROLE_ADMIN", response.user().roles().getFirst().roleCode());
    }

    @Test
    void loginWithUserNotFound() {
        LoginRequest request = new LoginRequest("unknown", "password");
        when(userManager.getByUsername("unknown")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(request));

        assertEquals(ErrorCode.INVALID_CREDENTIALS.getCode(), ex.getCode());
    }

    @Test
    void loginWithDisabledUser() {
        LoginRequest request = new LoginRequest("disabled", "password");
        UserBO userBO = buildUserBO(2L, "disabled", UserStatusEnum.DISABLED.getCode());

        when(userManager.getByUsername("disabled")).thenReturn(userBO);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(request));

        assertEquals(ErrorCode.USER_DISABLED.getCode(), ex.getCode());
    }

    @Test
    void loginWithWrongPassword() {
        LoginRequest request = new LoginRequest("admin", "wrongpassword");
        UserBO userBO = buildUserBO(1L, "admin", UserStatusEnum.ENABLED.getCode());

        when(userManager.getByUsername("admin")).thenReturn(userBO);
        when(passwordEncoder.matches("wrongpassword", "$2a$10$encoded")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(request));

        assertEquals(ErrorCode.INVALID_CREDENTIALS.getCode(), ex.getCode());
    }

    // ==================== getCurrentUser ====================

    @Test
    void getCurrentUserSuccess() {
        String token = "valid-token";
        UserBO userBO = buildUserBO(1L, "admin", UserStatusEnum.ENABLED.getCode());

        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.getUserId(token)).thenReturn(1L);
        when(userManager.getById(1L)).thenReturn(userBO);

        UserInfoDTO result = authService.getCurrentUser(token);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("admin", result.username());
    }

    @Test
    void getCurrentUserWithInvalidToken() {
        when(jwtService.validateToken("invalid-token")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.getCurrentUser("invalid-token"));

        assertEquals(ErrorCode.TOKEN_INVALID.getCode(), ex.getCode());
    }

    @Test
    void getCurrentUserWithUserNotFound() {
        String token = "valid-token";

        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.getUserId(token)).thenReturn(999L);
        when(userManager.getById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.getCurrentUser(token));

        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
    }

    // ==================== helper ====================

    private UserBO buildUserBO(Long id, String username, String status) {
        UserBO userBO = new UserBO();
        userBO.setId(id);
        userBO.setUsername(username);
        userBO.setPasswordHash("$2a$10$encoded");
        userBO.setDisplayName("Test User");
        userBO.setEmail("test@example.com");
        userBO.setStatus(status);
        userBO.setRoles(List.of("ROLE_ADMIN"));
        return userBO;
    }
}
