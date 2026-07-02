package com.ma.kb.common.exception;

import com.ma.kb.common.response.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void constructWithErrorCode() {
        BusinessException ex = new BusinessException(ErrorCode.USER_NOT_FOUND);

        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
        assertEquals(ErrorCode.USER_NOT_FOUND.getMessage(), ex.getMessage());
    }

    @Test
    void constructWithCodeAndMessage() {
        BusinessException ex = new BusinessException(60001, "自定义错误");

        assertEquals(60001, ex.getCode());
        assertEquals("自定义错误", ex.getMessage());
    }

    @Test
    void constructWithErrorCodeAndCustomMessage() {
        BusinessException ex = new BusinessException(ErrorCode.INTERNAL_ERROR, "详细错误信息");

        assertEquals(ErrorCode.INTERNAL_ERROR.getCode(), ex.getCode());
        assertEquals("详细错误信息", ex.getMessage());
    }

    @Test
    void isRuntimeException() {
        BusinessException ex = new BusinessException(ErrorCode.BAD_REQUEST);
        assertInstanceOf(RuntimeException.class, ex);
    }
}
