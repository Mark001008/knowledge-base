package com.ma.kb.dal.mapper.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ma.kb.dal.model.chat.ChatSessionDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 问答会话 Mapper
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSessionDO> {
}
