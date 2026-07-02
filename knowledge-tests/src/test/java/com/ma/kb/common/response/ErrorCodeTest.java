package com.ma.kb.common.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorCodeTest {

    @Test
    void successCodeIsZero() {
        assertEquals(0, ErrorCode.SUCCESS.getCode());
        assertEquals("success", ErrorCode.SUCCESS.getMessage());
    }

    @Test
    void authErrorCodes() {
        assertEquals(40001, ErrorCode.UNAUTHORIZED.getCode());
        assertEquals(40002, ErrorCode.INVALID_CREDENTIALS.getCode());
        assertEquals(40003, ErrorCode.USER_DISABLED.getCode());
        assertEquals(40004, ErrorCode.TOKEN_EXPIRED.getCode());
        assertEquals(40005, ErrorCode.TOKEN_INVALID.getCode());
        assertEquals(40006, ErrorCode.ACCESS_DENIED.getCode());
    }

    @Test
    void paramErrorCodes() {
        assertEquals(40100, ErrorCode.BAD_REQUEST.getCode());
        assertEquals(40101, ErrorCode.PARAM_MISSING.getCode());
    }

    @Test
    void businessErrorCodes() {
        assertEquals(40200, ErrorCode.USER_NOT_FOUND.getCode());
        assertEquals(40201, ErrorCode.USER_ALREADY_EXISTS.getCode());
    }

    @Test
    void systemErrorCodes() {
        assertEquals(50000, ErrorCode.INTERNAL_ERROR.getCode());
        assertEquals(50001, ErrorCode.DATABASE_ERROR.getCode());
    }

    @Test
    void allEnumValuesExist() {
        ErrorCode[] values = ErrorCode.values();
        assertTrue(values.length >= 12);
    }
}
