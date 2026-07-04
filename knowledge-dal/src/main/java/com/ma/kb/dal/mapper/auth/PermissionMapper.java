package com.ma.kb.dal.mapper.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ma.kb.dal.model.auth.PermissionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 权限 Mapper
 */
@Mapper
public interface PermissionMapper extends BaseMapper<PermissionDO> {

    /**
     * 根据角色ID查询权限列表
     */
    List<PermissionDO> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据用户ID查询权限编码列表
     */
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

    /**
     * 根据权限编码查询权限
     */
    PermissionDO selectByPermissionCode(@Param("permissionCode") String permissionCode);
}
