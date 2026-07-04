package com.ma.kb.start.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * 链路追踪过滤器
 * 生成 traceId 并放入 MDC，用于日志追踪
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceFilter implements Filter {

    private static final String TRACE_ID = "traceId";
    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            String traceId = extractTraceId(request);
            MDC.put(TRACE_ID, traceId);

            // 将 traceId 写入响应头
            if (response instanceof jakarta.servlet.http.HttpServletResponse httpResponse) {
                httpResponse.setHeader(HEADER_TRACE_ID, traceId);
            }

            chain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }

    private String extractTraceId(ServletRequest request) {
        // 优先从请求头获取
        if (request instanceof HttpServletRequest httpRequest) {
            String traceId = httpRequest.getHeader(HEADER_TRACE_ID);
            if (traceId != null && !traceId.isBlank()) {
                return traceId;
            }
        }
        // 否则生成新的 traceId
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
