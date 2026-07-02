package com.ma.kb.dal.mapper.space;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ma.kb.dal.model.space.SpaceDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识库 Mapper
 */
@Mapper
public interface SpaceMapper extends BaseMapper<SpaceDO> {

    /**
     * 查询用户可访问的知识库ID列表
     */
    List<Long> selectAccessibleSpaceIds(@Param("userId") Long userId);
}
