package com.ma.kb.start.controller.system;

import com.ma.kb.common.response.ApiResponse;
import com.ma.kb.core.auth.RequirePermission;
import com.ma.kb.service.system.RoleService;
import com.ma.kb.service.system.dto.CreateRoleRequest;
import com.ma.kb.service.system.dto.RoleDetailDTO;
import com.ma.kb.service.system.dto.UpdateRoleRequest;
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
 * 角色管理控制器
 */
@RestController
@RequestMapping("/api/system/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 获取角色列表
     */
    @GetMapping
    @RequirePermission("role:view")
    public ApiResponse<List<RoleDetailDTO>> getAllRoles() {
        List<RoleDetailDTO> roles = roleService.getAllRoles();
        return ApiResponse.success(roles);
    }

    /**
     * 根据ID获取角色
     */
    @GetMapping("/{id}")
    @RequirePermission("role:view")
    public ApiResponse<RoleDetailDTO> getRoleById(@PathVariable Long id) {
        RoleDetailDTO role = roleService.getRoleById(id);
        return ApiResponse.success(role);
    }

    /**
     * 创建角色
     */
    @PostMapping
    @RequirePermission("role:create")
    public ApiResponse<RoleDetailDTO> createRole(@RequestBody CreateRoleRequest request) {
        RoleDetailDTO role = roleService.createRole(request);
        return ApiResponse.success(role);
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    @RequirePermission("role:update")
    public ApiResponse<RoleDetailDTO> updateRole(@PathVariable Long id, @RequestBody UpdateRoleRequest request) {
        RoleDetailDTO role = roleService.updateRole(id, request);
        return ApiResponse.success(role);
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @RequirePermission("role:delete")
    public ApiResponse<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ApiResponse.success(null);
    }

    /**
     * 获取角色的权限ID列表
     */
    @GetMapping("/{id}/permissions")
    @RequirePermission("role:view")
    public ApiResponse<List<Long>> getRolePermissionIds(@PathVariable Long id) {
        List<Long> permissionIds = roleService.getRolePermissionIds(id);
        return ApiResponse.success(permissionIds);
    }

    /**
     * 分配权限
     */
    @PutMapping("/{id}/permissions")
    @RequirePermission("role:assign-permission")
    public ApiResponse<Void> assignPermissions(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
        roleService.assignPermissions(id, permissionIds);
        return ApiResponse.success(null);
    }

    /**
     * 分配菜单
     */
    @PutMapping("/{id}/menus")
    @RequirePermission("role:assign-menu")
    public ApiResponse<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return ApiResponse.success(null);
    }
}
