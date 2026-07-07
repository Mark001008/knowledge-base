package com.ma.kb.start.controller.system;

import com.ma.kb.common.response.ApiResponse;
import com.ma.kb.core.auth.JwtService;
import com.ma.kb.core.auth.RequirePermission;
import com.ma.kb.core.auth.SecurityUtils;
import com.ma.kb.service.system.PermissionService;
import com.ma.kb.service.system.dto.CreatePermissionRequest;
import com.ma.kb.service.system.dto.PermissionDTO;
import com.ma.kb.service.system.dto.UpdatePermissionRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限管理控制器
 */
@RestController
@RequestMapping("/api/system/permissions")
public class PermissionController {

    private final PermissionService permissionService;
    private final JwtService jwtService;

    public PermissionController(PermissionService permissionService, JwtService jwtService) {
        this.permissionService = permissionService;
        this.jwtService = jwtService;
    }

    /**
     * 获取所有权限列表
     */
    @GetMapping
    @RequirePermission("permission:view")
    public ApiResponse<List<PermissionDTO>> getAllPermissions() {
        List<PermissionDTO> permissions = permissionService.getAllPermissions();
        return ApiResponse.success(permissions);
    }

    /**
     * 根据ID获取权限
     */
    @GetMapping("/{id}")
    @RequirePermission("permission:view")
    public ApiResponse<PermissionDTO> getPermissionById(@PathVariable Long id) {
        PermissionDTO permission = permissionService.getPermissionById(id);
        return ApiResponse.success(permission);
    }

    /**
     * 创建权限
     */
    @PostMapping
    @RequirePermission("permission:create")
    public ApiResponse<PermissionDTO> createPermission(@RequestBody CreatePermissionRequest request) {
        PermissionDTO permission = permissionService.createPermission(request);
        return ApiResponse.success(permission);
    }

    /**
     * 更新权限
     */
    @PutMapping("/{id}")
    @RequirePermission("permission:update")
    public ApiResponse<PermissionDTO> updatePermission(@PathVariable Long id, @RequestBody UpdatePermissionRequest request) {
        PermissionDTO permission = permissionService.updatePermission(id, request);
        return ApiResponse.success(permission);
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{id}")
    @RequirePermission("permission:delete")
    public ApiResponse<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return ApiResponse.success(null);
    }

    /**
     * 获取当前用户权限编码列表
     */
    @GetMapping("/current")
    public ApiResponse<List<String>> getCurrentUserPermissions(HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        return ApiResponse.success(permissionService.getCurrentUserPermissionCodes(userId));
    }
}
