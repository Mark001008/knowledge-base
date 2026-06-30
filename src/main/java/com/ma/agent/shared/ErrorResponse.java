package com.ma.agent.shared;

import java.time.Instant;

/**
 * 统一的 JSON 错误响应，用于替代默认的 500 堆栈追踪。
 *
 * @param error     错误类型标识
 * @param message   人类可读的错误描述
 * @param path      触发错误的请求路径
 * @param timestamp 错误发生时间
 */
public record ErrorResponse(
        String error,
        String message,
        String path,
        Instant timestamp
) {

    public static ErrorResponse of(String error, String message, String path) {
        return new ErrorResponse(error, message, path, Instant.now());
    }
}
