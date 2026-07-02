package com.ma.kb.common.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserStatusEnumTest {

    @Test
    void enabledStatus() {
        UserStatusEnum enabled = UserStatusEnum.ENABLED;

        assertEquals("ENABLED", enabled.getCode());
        assertEquals("启用", enabled.getDescription());
        assertTrue(enabled.isEnabled());
    }

    @Test
    void disabledStatus() {
        UserStatusEnum disabled = UserStatusEnum.DISABLED;

        assertEquals("DISABLED", disabled.getCode());
        assertEquals("禁用", disabled.getDescription());
        assertFalse(disabled.isEnabled());
    }

    @Test
    void fromCodeWithValidCode() {
        assertEquals(UserStatusEnum.ENABLED, UserStatusEnum.fromCode("ENABLED"));
        assertEquals(UserStatusEnum.DISABLED, UserStatusEnum.fromCode("DISABLED"));
    }

    @Test
    void fromCodeWithInvalidCode() {
        assertThrows(IllegalArgumentException.class, () -> UserStatusEnum.fromCode("UNKNOWN"));
    }

    @Test
    void fromCodeWithNull() {
        assertThrows(IllegalArgumentException.class, () -> UserStatusEnum.fromCode(null));
    }

    @Test
    void valuesContainsBothStatuses() {
        UserStatusEnum[] values = UserStatusEnum.values();
        assertEquals(2, values.length);
    }
}
