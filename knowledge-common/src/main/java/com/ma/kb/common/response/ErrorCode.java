package com.ma.kb.common.response;

/**
 * 错误码枚举
 */
public enum ErrorCode {

    SUCCESS(0, "success"),

    // 认证相关 40001-40099
    UNAUTHORIZED(40001, "未登录或登录已过期"),
    INVALID_CREDENTIALS(40002, "用户名或密码错误"),
    USER_DISABLED(40003, "用户已被禁用"),
    TOKEN_EXPIRED(40004, "Token已过期"),
    TOKEN_INVALID(40005, "Token无效"),
    ACCESS_DENIED(40006, "无权限访问"),

    // 参数校验 40100-40199
    BAD_REQUEST(40100, "请求参数错误"),
    PARAM_MISSING(40101, "缺少必要参数"),

    // 业务异常 40200-40299
    USER_NOT_FOUND(40200, "用户不存在"),
    USER_ALREADY_EXISTS(40201, "用户名已存在"),

    // 系统异常 50000-50099
    INTERNAL_ERROR(50000, "系统内部错误"),
    DATABASE_ERROR(50001, "数据库错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
