package com.ma.kb.dal.mapper.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ma.kb.dal.model.chat.AnswerCitationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 回答引用 Mapper
 */
@Mapper
public interface AnswerCitationMapper extends BaseMapper<AnswerCitationDO> {

    /**
     * 根据消息ID查询引用列表
     */
    List<AnswerCitationDO> selectByMessageId(@Param("messageId") Long messageId);

    /**
     * 根据消息ID删除引用
     */
    int deleteByMessageId(@Param("messageId") Long messageId);
}
