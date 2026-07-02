package com.ma.kb.common.enums;

/**
 * 文档处理状态枚举
 */
public enum DocumentStatusEnum {

    PENDING("PENDING", "待处理"),
    PARSING("PARSING", "解析中"),
    INDEXING("INDEXING", "索引中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "处理失败");

    private final String code;
    private final String description;

    DocumentStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static DocumentStatusEnum fromCode(String code) {
        for (DocumentStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的文档状态: " + code);
    }
}
