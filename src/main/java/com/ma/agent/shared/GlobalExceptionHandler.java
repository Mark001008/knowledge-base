package com.ma.agent.shared;

import com.ma.agent.model.ModelProviderException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器，将异常映射为结构化的 JSON 错误响应。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 模型提供商异常 → 502 Bad Gateway
     */
    @ExceptionHandler(ModelProviderException.class)
    public ResponseEntity<ErrorResponse> handleModelProvider(
            ModelProviderException ex,
            HttpServletRequest request) {
        log.error(LogMarkers.DATA, "ModelProviderException path={} message={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.of("MODEL_PROVIDER_ERROR", ex.getMessage(), request.getRequestURI()));
    }

    /**
     * 请求体解析失败 / 参数校验失败 → 400 Bad Request
     */
    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(
            Exception ex,
            HttpServletRequest request) {
        log.warn(LogMarkers.API, "BadRequest path={} message={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("BAD_REQUEST", ex.getMessage(), request.getRequestURI()));
    }

    /**
     * 未预期的异常 → 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex,
            HttpServletRequest request) {
        log.error(LogMarkers.BIZ, "Unexpected error path={}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "Internal server error", request.getRequestURI()));
    }
}
