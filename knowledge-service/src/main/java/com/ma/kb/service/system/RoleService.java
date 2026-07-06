package com.ma.kb.service.system;

import com.ma.kb.service.system.dto.CreateRoleRequest;
import com.ma.kb.service.system.dto.RoleDetailDTO;
import com.ma.kb.service.system.dto.UpdateRoleRequest;

import java.util.List;

/**
 * 角色服务接口
 */
public interface RoleService {

    /**
     * 获取角色列表
     */
    List<RoleDetailDTO> getAllRoles();

    /**
     * 根据ID获取角色
     */
    RoleDetailDTO getRoleById(Long id);

    /**
     * 创建角色
     */
    RoleDetailDTO createRole(CreateRoleRequest request);

    /**
     * 更新角色
     */
    RoleDetailDTO updateRole(Long id, UpdateRoleRequest request);

    /**
     * 删除角色
     */
    void deleteRole(Long id);

    /**
     * 分配权限
     */
    void assignPermissions(Long id, List<Long> permissionIds);

    /**
     * 分配菜单
     */
    void assignMenus(Long id, List<Long> menuIds);

    /**
     * 获取角色的权限ID列表
     */
    List<Long> getRolePermissionIds(Long roleId);
}
