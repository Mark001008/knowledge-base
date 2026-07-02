package com.ma.kb.common.enums;

/**
 * 用户状态枚举
 */
public enum UserStatusEnum {

    ENABLED("ENABLED", "启用"),
    DISABLED("DISABLED", "禁用");

    private final String code;
    private final String description;

    UserStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取枚举
     */
    public static UserStatusEnum fromCode(String code) {
        for (UserStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的用户状态: " + code);
    }

    /**
     * 判断是否为启用状态
     */
    public boolean isEnabled() {
        return this == ENABLED;
    }
}
