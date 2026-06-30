package com.ma.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ma.agent.entity.ConversationEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对话历史 Mapper - MyBatis-Plus
 */
@Mapper
public interface ConversationMapper extends BaseMapper<ConversationEntity> {
}
