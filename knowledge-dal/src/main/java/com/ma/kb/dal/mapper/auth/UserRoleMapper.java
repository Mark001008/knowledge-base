package com.ma.kb.dal.mapper.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ma.kb.dal.model.auth.UserRoleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户角色关联 Mapper
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleDO> {

    /**
     * 删除用户的所有角色关联
     */
    int deleteByUserId(@Param("userId") Long userId);
}
