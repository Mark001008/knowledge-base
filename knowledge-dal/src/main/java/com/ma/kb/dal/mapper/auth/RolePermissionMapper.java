package com.ma.kb.dal.mapper.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ma.kb.dal.model.auth.RolePermissionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色权限关联 Mapper
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermissionDO> {

    /**
     * 删除角色的所有权限关联
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 查询角色的权限ID列表
     */
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 统计绑定指定权限的角色数
     */
    long countByPermissionId(@Param("permissionId") Long permissionId);
}
