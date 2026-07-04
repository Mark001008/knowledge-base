package com.ma.kb.dal.mapper.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ma.kb.dal.model.auth.RoleMenuDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色菜单关联 Mapper
 */
@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenuDO> {

    /**
     * 删除角色的所有菜单关联
     */
    int deleteByRoleId(@Param("roleId") Long roleId);
}
