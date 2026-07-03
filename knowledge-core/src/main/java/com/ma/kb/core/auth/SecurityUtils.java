package com.ma.kb.core.auth;

/**
 * 安全相关工具方法
 */
public final class SecurityUtils {

    private static final String BEARER_PREFIX = "Bearer ";

    private SecurityUtils() {
    }

    /**
     * 从 Authorization 头中提取当前登录用户 ID
     *
     * @param authorizationHeader Authorization 请求头的值
     * @param jwtService          JWT 服务
     * @return 用户 ID，未登录时返回 null
     */
    public static Long getCurrentUserId(String authorizationHeader, JwtService jwtService) {
        String token = extractBearerToken(authorizationHeader);
        if (token == null) {
            return null;
        }
        return jwtService.getUserId(token);
    }

    /**
     * 从 Authorization 头中提取 Bearer Token
     *
     * @param authorizationHeader Authorization 请求头的值
     * @return token 字符串，不存在时返回 null
     */
    public static String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            return authorizationHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
