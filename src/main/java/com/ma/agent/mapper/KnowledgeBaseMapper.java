package com.ma.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ma.agent.entity.KnowledgeBaseEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库 Mapper
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBaseEntity> {
}
