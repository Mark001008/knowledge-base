package com.ma.kb.manager.auth;

import com.ma.kb.dal.mapper.auth.PermissionMapper;
import com.ma.kb.dal.model.auth.PermissionDO;
import com.ma.kb.manager.auth.bo.PermissionBO;
import com.ma.kb.manager.auth.converter.PermissionConverter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限数据管理器
 */
@Component
public class PermissionManager {

    private final PermissionMapper permissionMapper;
    private final PermissionConverter permissionConverter;

    public PermissionManager(PermissionMapper permissionMapper, PermissionConverter permissionConverter) {
        this.permissionMapper = permissionMapper;
        this.permissionConverter = permissionConverter;
    }

    /**
     * 根据用户ID查询权限编码列表
     */
    public List<String> getPermissionCodesByUserId(Long userId) {
        return permissionMapper.selectPermissionCodesByUserId(userId);
    }

    /**
     * 根据角色ID查询权限列表
     */
    public List<PermissionBO> getByRoleId(Long roleId) {
        List<PermissionDO> permissionDOList = permissionMapper.selectByRoleId(roleId);
        return permissionDOList.stream()
                .map(permissionConverter::toBO)
                .collect(Collectors.toList());
    }

    /**
     * 根据权限编码查询权限
     */
    public PermissionBO getByPermissionCode(String permissionCode) {
        PermissionDO permissionDO = permissionMapper.selectByPermissionCode(permissionCode);
        if (permissionDO == null) {
            return null;
        }
        return permissionConverter.toBO(permissionDO);
    }

    /**
     * 查询所有权限
     */
    public List<PermissionBO> getAll() {
        List<PermissionDO> permissionDOList = permissionMapper.selectList(null);
        return permissionDOList.stream()
                .map(permissionConverter::toBO)
                .collect(Collectors.toList());
    }
}
