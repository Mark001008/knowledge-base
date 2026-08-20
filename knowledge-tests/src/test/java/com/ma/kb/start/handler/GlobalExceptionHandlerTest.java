package com.ma.kb.start.handler;

import com.ma.kb.common.exception.BusinessException;
import com.ma.kb.common.response.ApiResponse;
import com.ma.kb.common.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
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

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);
        ApiResponse<Void> body = response.getBody();

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(body);
        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), body.code());
        assertEquals(ErrorCode.USER_NOT_FOUND.getMessage(), body.message());
    }

    @Test
    void handleAuthenticationException() {
        AuthenticationException ex = new BadCredentialsException("认证失败");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAuthenticationException(ex);
        ApiResponse<Void> body = response.getBody();

        assertEquals(401, response.getStatusCode().value());
        assertNotNull(body);
        assertEquals(ErrorCode.UNAUTHORIZED.getCode(), body.code());
        assertEquals(ErrorCode.UNAUTHORIZED.getMessage(), body.message());
    }

    @Test
    void handleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("无权限");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDeniedException(ex);
        ApiResponse<Void> body = response.getBody();

        assertEquals(403, response.getStatusCode().value());
        assertNotNull(body);
        assertEquals(ErrorCode.ACCESS_DENIED.getCode(), body.code());
        assertEquals(ErrorCode.ACCESS_DENIED.getMessage(), body.message());
    }

    @Test
    void handleUnknownException() {
        Exception ex = new RuntimeException("未知错误");

        ResponseEntity<ApiResponse<Void>> response = handler.handleException(ex);
        ApiResponse<Void> body = response.getBody();

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(body);
        assertEquals(ErrorCode.INTERNAL_ERROR.getCode(), body.code());
        assertEquals(ErrorCode.INTERNAL_ERROR.getMessage(), body.message());
    }
}
