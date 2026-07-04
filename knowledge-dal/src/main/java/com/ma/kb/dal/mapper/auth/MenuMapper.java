package com.ma.kb.dal.mapper.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ma.kb.dal.model.auth.MenuDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单 Mapper
 */
@Mapper
public interface MenuMapper extends BaseMapper<MenuDO> {

    /**
     * 根据角色ID查询菜单列表
     */
    List<MenuDO> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据用户ID查询菜单列表
     */
    List<MenuDO> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询所有菜单（按排序）
     */
    List<MenuDO> selectAllMenus();
}
