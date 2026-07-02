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

    // 知识库 40300-40399
    SPACE_NOT_FOUND(40300, "知识库不存在"),
    SPACE_ALREADY_EXISTS(40301, "知识库名称已存在"),
    SPACE_ACCESS_DENIED(40302, "无权访问该知识库"),
    SPACE_MEMBER_ALREADY_EXISTS(40303, "该用户已是知识库成员"),
    SPACE_MEMBER_NOT_FOUND(40304, "知识库成员不存在"),
    SPACE_OWNER_CANNOT_LEAVE(40305, "知识库所有者不能移除"),

    // 文档 40400-40499
    DOCUMENT_NOT_FOUND(40400, "文档不存在"),
    DOCUMENT_TYPE_NOT_SUPPORTED(40401, "不支持的文件类型"),
    DOCUMENT_UPLOAD_FAILED(40402, "文件上传失败"),
    DOCUMENT_PARSE_FAILED(40403, "文档解析失败"),
    DOCUMENT_NOT_COMPLETED(40404, "文档尚未处理完成"),

    // 问答 40500-40599
    CHAT_SESSION_NOT_FOUND(40500, "会话不存在"),
    CHAT_SESSION_ACCESS_DENIED(40501, "无权访问该会话"),

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
