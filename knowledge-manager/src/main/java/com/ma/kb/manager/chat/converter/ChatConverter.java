package com.ma.kb.manager.chat.converter;

import com.ma.kb.dal.model.chat.ChatSessionDO;
import com.ma.kb.dal.model.chat.ChatMessageDO;
import com.ma.kb.dal.model.chat.AnswerCitationDO;
import com.ma.kb.manager.chat.bo.ChatSessionBO;
import com.ma.kb.manager.chat.bo.ChatMessageBO;
import com.ma.kb.manager.chat.bo.AnswerCitationBO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Chat DO <-> BO 转换器
 */
@Mapper(componentModel = "spring")
public interface ChatConverter {

    ChatSessionBO toSessionBO(ChatSessionDO sessionDO);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ChatSessionDO toSessionDO(ChatSessionBO sessionBO);

    ChatMessageBO toMessageBO(ChatMessageDO messageDO);

    @Mapping(target = "createdAt", ignore = true)
    ChatMessageDO toMessageDO(ChatMessageBO messageBO);

    AnswerCitationBO toCitationBO(AnswerCitationDO citationDO);

    @Mapping(target = "createdAt", ignore = true)
    AnswerCitationDO toCitationDO(AnswerCitationBO citationBO);
}
