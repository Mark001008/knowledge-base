package com.ma.kb.common.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void successWithData() {
        ApiResponse<String> response = ApiResponse.success("hello");

        assertEquals(0, response.code());
        assertEquals("success", response.message());
        assertEquals("hello", response.data());
    }

    @Test
    void successWithoutData() {
        ApiResponse<Object> response = ApiResponse.success();

        assertEquals(0, response.code());
        assertEquals("success", response.message());
        assertNull(response.data());
    }

    @Test
    void errorWithCodeAndMessage() {
        ApiResponse<Void> response = ApiResponse.error(500, "系统错误");

        assertEquals(500, response.code());
        assertEquals("系统错误", response.message());
        assertNull(response.data());
    }

    @Test
    void errorWithErrorCode() {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.UNAUTHORIZED);

        assertEquals(ErrorCode.UNAUTHORIZED.getCode(), response.code());
        assertEquals(ErrorCode.UNAUTHORIZED.getMessage(), response.message());
        assertNull(response.data());
    }

    @Test
    void errorWithInternalErrorCode() {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.INTERNAL_ERROR);

        assertEquals(50000, response.code());
        assertEquals("系统内部错误", response.message());
    }
}
