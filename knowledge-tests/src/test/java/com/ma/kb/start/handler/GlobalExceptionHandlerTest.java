package com.ma.kb.start.handler;

import com.ma.kb.common.exception.BusinessException;
import com.ma.kb.common.response.ApiResponse;
import com.ma.kb.common.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleBusinessException() {
        BusinessException ex = new BusinessException(ErrorCode.USER_NOT_FOUND);

        ApiResponse<Void> response = handler.handleBusinessException(ex);

        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), response.code());
        assertEquals(ErrorCode.USER_NOT_FOUND.getMessage(), response.message());
    }

    @Test
    void handleAuthenticationException() {
        AuthenticationException ex = new BadCredentialsException("认证失败");

        ApiResponse<Void> response = handler.handleAuthenticationException(ex);

        assertEquals(ErrorCode.UNAUTHORIZED.getCode(), response.code());
        assertEquals(ErrorCode.UNAUTHORIZED.getMessage(), response.message());
    }

    @Test
    void handleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("无权限");

        ApiResponse<Void> response = handler.handleAccessDeniedException(ex);

        assertEquals(ErrorCode.ACCESS_DENIED.getCode(), response.code());
        assertEquals(ErrorCode.ACCESS_DENIED.getMessage(), response.message());
    }

    @Test
    void handleUnknownException() {
        Exception ex = new RuntimeException("未知错误");

        ApiResponse<Void> response = handler.handleException(ex);

        assertEquals(ErrorCode.INTERNAL_ERROR.getCode(), response.code());
        assertEquals(ErrorCode.INTERNAL_ERROR.getMessage(), response.message());
    }
}
