package com.ma.kb.start.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ma.kb.common.response.ApiResponse;
import com.ma.kb.common.response.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 登录接口限流拦截器
 * 同一 IP 5 分钟内最多 10 次失败登录，超限返回 429
 */
@Component
public class LoginRateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitInterceptor.class);
    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_SECONDS = 300; // 5 minutes

    private final ConcurrentMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 记录：首次失败时间和失败次数
     */
    private static class AttemptRecord {
        long firstAttemptEpochSecond;
        int count;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = getClientIp(request);
        AttemptRecord record = attempts.get(ip);

        if (record != null) {
            long elapsed = Instant.now().getEpochSecond() - record.firstAttemptEpochSecond;
            if (elapsed < WINDOW_SECONDS && record.count >= MAX_ATTEMPTS) {
                log.warn("登录限流触发: IP={}, attempts={}", ip, record.count);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                ApiResponse<Void> body = ApiResponse.error(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "登录尝试次数过多，请 " + (WINDOW_SECONDS - elapsed) + " 秒后重试"
                );
                response.getWriter().write(objectMapper.writeValueAsString(body));
                return false;
            }
            // 窗口已过期，清除
            if (elapsed >= WINDOW_SECONDS) {
                attempts.remove(ip);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 仅对 POST /api/auth/login 的 401 响应计数
        if (!"POST".equalsIgnoreCase(request.getMethod())) return;
        if (response.getStatus() != HttpStatus.UNAUTHORIZED.value()) return;

        String ip = getClientIp(request);
        attempts.compute(ip, (key, existing) -> {
            AttemptRecord record = existing;
            if (record == null) {
                record = new AttemptRecord();
                record.firstAttemptEpochSecond = Instant.now().getEpochSecond();
                record.count = 0;
            } else {
                long elapsed = Instant.now().getEpochSecond() - record.firstAttemptEpochSecond;
                if (elapsed >= WINDOW_SECONDS) {
                    // 窗口过期，重置
                    record.firstAttemptEpochSecond = Instant.now().getEpochSecond();
                    record.count = 0;
                }
            }
            record.count++;
            return record;
        });
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        String xReal = request.getHeader("X-Real-IP");
        if (xReal != null && !xReal.isEmpty()) {
            return xReal;
        }
        return request.getRemoteAddr();
    }
}
