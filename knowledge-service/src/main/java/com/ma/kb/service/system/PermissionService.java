package com.ma.kb.service.system;

import com.ma.kb.service.system.dto.CreatePermissionRequest;
import com.ma.kb.service.system.dto.PermissionDTO;
import com.ma.kb.service.system.dto.UpdatePermissionRequest;

import java.util.List;

/**
 * 权限服务接口
 */
public interface PermissionService {

    /**
     * 获取当前用户权限编码列表
     */
    List<String> getCurrentUserPermissionCodes(Long userId);

    /**
     * 获取所有权限列表
     */
    List<PermissionDTO> getAllPermissions();

    /**
     * 根据ID获取权限
     */
    PermissionDTO getPermissionById(Long id);

    /**
     * 创建权限
     */
    PermissionDTO createPermission(CreatePermissionRequest request);

    /**
     * 更新权限
     */
    PermissionDTO updatePermission(Long id, UpdatePermissionRequest request);

    /**
     * 删除权限
     */
    void deletePermission(Long id);
}
