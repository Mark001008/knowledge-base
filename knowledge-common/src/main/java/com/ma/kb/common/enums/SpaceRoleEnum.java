package com.ma.kb.common.enums;

/**
 * 知识库成员角色枚举
 */
public enum SpaceRoleEnum {

    OWNER("OWNER", "所有者"),
    ADMIN("ADMIN", "管理员"),
    READER("READER", "只读用户");

    private final String code;
    private final String description;

    SpaceRoleEnum(String code, String description) {
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
    public static SpaceRoleEnum fromCode(String code) {
        for (SpaceRoleEnum role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("未知的知识库角色: " + code);
    }
}
