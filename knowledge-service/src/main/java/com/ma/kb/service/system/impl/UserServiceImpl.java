package com.ma.kb.service.system.impl;

import com.ma.kb.common.exception.BusinessException;
import com.ma.kb.common.response.ErrorCode;
import com.ma.kb.dal.mapper.auth.UserMapper;
import com.ma.kb.dal.mapper.auth.UserRoleMapper;
import com.ma.kb.dal.model.auth.UserDO;
import com.ma.kb.dal.model.auth.UserRoleDO;
import com.ma.kb.manager.auth.UserManager;
import com.ma.kb.manager.auth.bo.UserBO;
import com.ma.kb.service.auth.dto.RoleDTO;
import com.ma.kb.service.system.UserService;
import com.ma.kb.service.system.dto.CreateUserRequest;
import com.ma.kb.service.system.dto.UpdateUserRequest;
import com.ma.kb.service.system.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserManager userManager;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserManager userManager, UserMapper userMapper,
                           UserRoleMapper userRoleMapper, PasswordEncoder passwordEncoder) {
        this.userManager = userManager;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<UserDTO> getAllUsers() {
        List<UserDO> userList = userMapper.selectList(null);
        return userList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserDTO getUserById(Long id) {
        UserDO userDO = userMapper.selectById(id);
        if (userDO == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return convertToDTO(userDO);
    }

    @Override
    @Transactional
    public UserDTO createUser(CreateUserRequest request) {
        // 检查用户名是否已存在
        UserBO existing = userManager.getByUsername(request.username());
        if (existing != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        UserDO userDO = new UserDO();
        userDO.setUsername(request.username());
        userDO.setPasswordHash(passwordEncoder.encode(request.password()));
        userDO.setDisplayName(request.displayName());
        userDO.setEmail(request.email());
        userDO.setStatus("ENABLED");

        userMapper.insert(userDO);
        log.info("创建用户成功: {}", request.username());

        return convertToDTO(userDO);
    }

    @Override
    @Transactional
    public UserDTO updateUser(Long id, UpdateUserRequest request) {
        UserDO userDO = userMapper.selectById(id);
        if (userDO == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (request.displayName() != null) {
            userDO.setDisplayName(request.displayName());
        }
        if (request.email() != null) {
            userDO.setEmail(request.email());
        }
        if (request.status() != null) {
            userDO.setStatus(request.status());
        }

        userMapper.updateById(userDO);
        log.info("更新用户成功: {}", id);

        return convertToDTO(userDO);
    }

    @Override
    @Transactional
    public void updateUserStatus(Long id, String status) {
        UserDO userDO = userMapper.selectById(id);
        if (userDO == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 不能禁用admin用户
        if ("admin".equals(userDO.getUsername()) && "DISABLED".equals(status)) {
            throw new BusinessException(ErrorCode.USER_BUILTIN_CANNOT_DELETE);
        }

        userDO.setStatus(status);
        userMapper.updateById(userDO);
        log.info("更新用户状态成功: {} -> {}", id, status);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        UserDO userDO = userMapper.selectById(id);
        if (userDO == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        userDO.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(userDO);
        log.info("重置用户密码成功: {}", id);
    }

    @Override
    @Transactional
    public void assignRoles(Long id, List<Long> roleIds) {
        UserDO userDO = userMapper.selectById(id);
        if (userDO == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 删除原有角色关联
        userRoleMapper.deleteByUserId(id);

        // 添加新的角色关联
        for (Long roleId : roleIds) {
            UserRoleDO userRoleDO = new UserRoleDO();
            userRoleDO.setUserId(id);
            userRoleDO.setRoleId(roleId);
            userRoleMapper.insert(userRoleDO);
        }

        log.info("分配用户角色成功: userId={}, roleIds={}", id, roleIds);
    }

    private UserDTO convertToDTO(UserDO userDO) {
        UserBO userBO = userManager.getById(userDO.getId());
        List<RoleDTO> roles = userBO.getRoles() != null ?
                userBO.getRoles().stream()
                        .map(roleCode -> new RoleDTO(null, roleCode, roleCode))
                        .collect(Collectors.toList()) :
                List.of();

        return new UserDTO(
                userDO.getId(),
                userDO.getUsername(),
                userDO.getDisplayName(),
                userDO.getEmail(),
                userDO.getStatus(),
                roles,
                userDO.getCreatedAt(),
                userDO.getUpdatedAt()
        );
    }
}
