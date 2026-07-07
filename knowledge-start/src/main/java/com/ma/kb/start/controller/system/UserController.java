package com.ma.kb.start.controller.system;

import com.ma.kb.common.response.ApiResponse;
import com.ma.kb.core.auth.RequirePermission;
import com.ma.kb.service.system.UserService;
import com.ma.kb.service.system.dto.CreateUserRequest;
import com.ma.kb.service.system.dto.UpdateUserRequest;
import com.ma.kb.service.system.dto.UserDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/system/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取用户列表
     */
    @GetMapping
    @RequirePermission("user:view")
    public ApiResponse<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ApiResponse.success(users);
    }

    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    @RequirePermission("user:view")
    public ApiResponse<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return ApiResponse.success(user);
    }

    /**
     * 创建用户
     */
    @PostMapping
    @RequirePermission("user:create")
    public ApiResponse<UserDTO> createUser(@RequestBody CreateUserRequest request) {
        UserDTO user = userService.createUser(request);
        return ApiResponse.success(user);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    @RequirePermission("user:update")
    public ApiResponse<UserDTO> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        UserDTO user = userService.updateUser(id, request);
        return ApiResponse.success(user);
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/{id}/status")
    @RequirePermission("user:disable")
    public ApiResponse<Void> updateUserStatus(@PathVariable Long id, @RequestBody String status) {
        userService.updateUserStatus(id, status);
        return ApiResponse.success(null);
    }

    /**
     * 重置用户密码
     */
    @PutMapping("/{id}/password")
    @RequirePermission("user:reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @RequestBody String newPassword) {
        userService.resetPassword(id, newPassword);
        return ApiResponse.success(null);
    }

    /**
     * 分配角色
     */
    @PutMapping("/{id}/roles")
    @RequirePermission("user:assign-role")
    public ApiResponse<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        userService.assignRoles(id, roleIds);
        return ApiResponse.success(null);
    }
}
