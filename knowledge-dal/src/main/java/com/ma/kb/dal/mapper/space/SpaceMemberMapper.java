package com.ma.kb.dal.mapper.space;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ma.kb.dal.model.space.SpaceMemberDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 知识库成员 Mapper
 */
@Mapper
public interface SpaceMemberMapper extends BaseMapper<SpaceMemberDO> {

    /**
     * 查询用户在指定知识库中的角色
     */
    String selectRoleBySpaceIdAndUserId(@Param("spaceId") Long spaceId, @Param("userId") Long userId);
}
