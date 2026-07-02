package com.ma.kb.common.enums;

/**
 * 知识库可见范围枚举
 */
public enum SpaceVisibilityEnum {

    PRIVATE("PRIVATE", "私有"),
    INTERNAL("INTERNAL", "内部可见");

    private final String code;
    private final String description;

    SpaceVisibilityEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static SpaceVisibilityEnum fromCode(String code) {
        for (SpaceVisibilityEnum v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        throw new IllegalArgumentException("未知的可见范围: " + code);
    }
}
