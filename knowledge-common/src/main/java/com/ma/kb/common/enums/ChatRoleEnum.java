package com.ma.kb.common.enums;

/**
 * 聊天角色枚举
 */
public enum ChatRoleEnum {

    USER("user", "用户"),
    ASSISTANT("assistant", "助手");

    private final String code;
    private final String description;

    ChatRoleEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ChatRoleEnum fromCode(String code) {
        for (ChatRoleEnum role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("未知的聊天角色: " + code);
    }
}
