package com.ma.kb.start.config;

import com.ma.kb.common.exception.BusinessException;
import com.ma.kb.common.response.ErrorCode;
import com.ma.kb.core.auth.JwtService;
import com.ma.kb.core.auth.RequirePermission;
import com.ma.kb.core.auth.SecurityUtils;
import com.ma.kb.manager.auth.PermissionManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 权限拦截器
 * 检查请求是否具有访问接口所需的权限
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PermissionInterceptor.class);

    private final PermissionManager permissionManager;
    private final JwtService jwtService;

    public PermissionInterceptor(PermissionManager permissionManager, JwtService jwtService) {
        this.permissionManager = permissionManager;
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 只处理Controller方法
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 检查是否有权限注解
        RequirePermission annotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (annotation == null) {
            return true;
        }

        // 获取当前用户ID
        String authorizationHeader = request.getHeader("Authorization");
        Long userId = SecurityUtils.getCurrentUserId(authorizationHeader, jwtService);
        if (userId == null) {
            log.warn("权限校验失败：用户未登录");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 获取用户权限列表
        List<String> userPermissions = permissionManager.getPermissionCodesByUserId(userId);

        // 检查权限
        String requiredPermission = annotation.value();
        if (!userPermissions.contains(requiredPermission)) {
            log.warn("权限校验失败：用户{}没有权限{}", userId, requiredPermission);
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        return true;
    }
}
