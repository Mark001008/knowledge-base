package com.ma.kb.service.system;

import com.ma.kb.service.system.dto.CreateUserRequest;
import com.ma.kb.service.system.dto.UpdateUserRequest;
import com.ma.kb.service.system.dto.UserDTO;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 获取用户列表
     */
    List<UserDTO> getAllUsers();

    /**
     * 根据ID获取用户
     */
    UserDTO getUserById(Long id);

    /**
     * 创建用户
     */
    UserDTO createUser(CreateUserRequest request);

    /**
     * 更新用户
     */
    UserDTO updateUser(Long id, UpdateUserRequest request);

    /**
     * 更新用户状态
     */
    void updateUserStatus(Long id, String status);

    /**
     * 重置用户密码
     */
    void resetPassword(Long id, String newPassword);

    /**
     * 分配角色
     */
    void assignRoles(Long id, List<Long> roleIds);
}
