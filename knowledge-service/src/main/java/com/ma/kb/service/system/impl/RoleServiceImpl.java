package com.ma.kb.service.system.impl;

import com.ma.kb.common.exception.BusinessException;
import com.ma.kb.common.response.ErrorCode;
import com.ma.kb.dal.mapper.auth.RoleMapper;
import com.ma.kb.dal.mapper.auth.RoleMenuMapper;
import com.ma.kb.dal.mapper.auth.RolePermissionMapper;
import com.ma.kb.dal.mapper.auth.UserRoleMapper;
import com.ma.kb.dal.model.auth.RoleDO;
import com.ma.kb.dal.model.auth.RoleMenuDO;
import com.ma.kb.dal.model.auth.RolePermissionDO;
import com.ma.kb.service.system.RoleService;
import com.ma.kb.service.system.dto.CreateRoleRequest;
import com.ma.kb.service.system.dto.RoleDetailDTO;
import com.ma.kb.service.system.dto.UpdateRoleRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色服务实现
 */
@Service
public class RoleServiceImpl implements RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleServiceImpl.class);

    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final UserRoleMapper userRoleMapper;

    public RoleServiceImpl(RoleMapper roleMapper, RolePermissionMapper rolePermissionMapper,
                           RoleMenuMapper roleMenuMapper, UserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public List<RoleDetailDTO> getAllRoles() {
        List<RoleDO> roleList = roleMapper.selectList(null);
        return roleList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public RoleDetailDTO getRoleById(Long id) {
        RoleDO roleDO = roleMapper.selectById(id);
        if (roleDO == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        return convertToDTO(roleDO);
    }

    @Override
    @Transactional
    public RoleDetailDTO createRole(CreateRoleRequest request) {
        // 检查角色编码是否已存在
        RoleDO existing = roleMapper.selectByRoleCode(request.roleCode());
        if (existing != null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }

        RoleDO roleDO = new RoleDO();
        roleDO.setRoleCode(request.roleCode());
        roleDO.setRoleName(request.roleName());
        roleDO.setDescription(request.description());
        roleDO.setBuiltin(0);
        roleDO.setStatus("ENABLED");

        roleMapper.insert(roleDO);
        log.info("创建角色成功: {}", request.roleCode());

        return convertToDTO(roleDO);
    }

    @Override
    @Transactional
    public RoleDetailDTO updateRole(Long id, UpdateRoleRequest request) {
        RoleDO roleDO = roleMapper.selectById(id);
        if (roleDO == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }

        // 内置角色不能修改状态
        if (roleDO.getBuiltin() == 1 && request.status() != null) {
            throw new BusinessException(ErrorCode.ROLE_BUILTIN_CANNOT_DELETE);
        }

        if (request.roleName() != null) {
            roleDO.setRoleName(request.roleName());
        }
        if (request.description() != null) {
            roleDO.setDescription(request.description());
        }
        if (request.status() != null) {
            roleDO.setStatus(request.status());
        }

        roleMapper.updateById(roleDO);
        log.info("更新角色成功: {}", id);

        return convertToDTO(roleDO);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        RoleDO roleDO = roleMapper.selectById(id);
        if (roleDO == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }

        // 内置角色不能删除
        if (roleDO.getBuiltin() == 1) {
            throw new BusinessException(ErrorCode.ROLE_BUILTIN_CANNOT_DELETE);
        }

        // 检查是否有用户绑定此角色
        // TODO: 添加检查逻辑

        roleMapper.deleteById(id);
        log.info("删除角色成功: {}", id);
    }

    @Override
    @Transactional
    public void assignPermissions(Long id, List<Long> permissionIds) {
        RoleDO roleDO = roleMapper.selectById(id);
        if (roleDO == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }

        // 删除原有权限关联
        rolePermissionMapper.deleteByRoleId(id);

        // 添加新的权限关联
        for (Long permissionId : permissionIds) {
            RolePermissionDO rolePermissionDO = new RolePermissionDO();
            rolePermissionDO.setRoleId(id);
            rolePermissionDO.setPermissionId(permissionId);
            rolePermissionMapper.insert(rolePermissionDO);
        }

        log.info("分配角色权限成功: roleId={}, permissionIds={}", id, permissionIds);
    }

    @Override
    @Transactional
    public void assignMenus(Long id, List<Long> menuIds) {
        RoleDO roleDO = roleMapper.selectById(id);
        if (roleDO == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }

        // 删除原有菜单关联
        roleMenuMapper.deleteByRoleId(id);

        // 添加新的菜单关联
        for (Long menuId : menuIds) {
            RoleMenuDO roleMenuDO = new RoleMenuDO();
            roleMenuDO.setRoleId(id);
            roleMenuDO.setMenuId(menuId);
            roleMenuMapper.insert(roleMenuDO);
        }

        log.info("分配角色菜单成功: roleId={}, menuIds={}", id, menuIds);
    }

    private RoleDetailDTO convertToDTO(RoleDO roleDO) {
        return new RoleDetailDTO(
                roleDO.getId(),
                roleDO.getRoleCode(),
                roleDO.getRoleName(),
                roleDO.getDescription(),
                roleDO.getBuiltin(),
                roleDO.getStatus(),
                roleDO.getCreatedAt(),
                roleDO.getUpdatedAt()
        );
    }
}
