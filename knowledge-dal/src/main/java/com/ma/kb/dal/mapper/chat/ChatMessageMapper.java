package com.ma.kb.dal.mapper.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ma.kb.dal.model.chat.ChatMessageDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 问答消息 Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessageDO> {
}
