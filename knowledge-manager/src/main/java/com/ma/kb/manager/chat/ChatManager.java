package com.ma.kb.manager.chat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ma.kb.dal.mapper.chat.AnswerCitationMapper;
import com.ma.kb.dal.mapper.chat.ChatMessageMapper;
import com.ma.kb.dal.mapper.chat.ChatSessionMapper;
import com.ma.kb.dal.model.chat.AnswerCitationDO;
import com.ma.kb.dal.model.chat.ChatMessageDO;
import com.ma.kb.dal.model.chat.ChatSessionDO;
import com.ma.kb.manager.chat.bo.AnswerCitationBO;
import com.ma.kb.manager.chat.bo.ChatMessageBO;
import com.ma.kb.manager.chat.bo.ChatSessionBO;
import com.ma.kb.manager.chat.converter.ChatConverter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 聊天数据管理器
 */
@Component
public class ChatManager {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final AnswerCitationMapper citationMapper;
    private final ChatConverter chatConverter;

    public ChatManager(ChatSessionMapper sessionMapper, ChatMessageMapper messageMapper,
                       AnswerCitationMapper citationMapper, ChatConverter chatConverter) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.citationMapper = citationMapper;
        this.chatConverter = chatConverter;
    }

    public ChatSessionBO createSession(ChatSessionBO sessionBO) {
        ChatSessionDO sessionDO = chatConverter.toSessionDO(sessionBO);
        sessionMapper.insert(sessionDO);
        sessionBO.setId(sessionDO.getId());
        return sessionBO;
    }

    public ChatSessionBO getSessionById(Long id) {
        ChatSessionDO sessionDO = sessionMapper.selectById(id);
        return sessionDO != null ? chatConverter.toSessionBO(sessionDO) : null;
    }

    public List<ChatSessionBO> listSessionsBySpaceIdAndUserId(Long spaceId, Long userId) {
        List<ChatSessionDO> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<ChatSessionDO>()
                        .eq(ChatSessionDO::getSpaceId, spaceId)
                        .eq(ChatSessionDO::getUserId, userId)
                        .orderByDesc(ChatSessionDO::getUpdatedAt)
        );
        return sessions.stream().map(chatConverter::toSessionBO).toList();
    }

    public List<ChatSessionBO> listSessionsBySpaceId(Long spaceId) {
        List<ChatSessionDO> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<ChatSessionDO>()
                        .eq(ChatSessionDO::getSpaceId, spaceId)
                        .orderByDesc(ChatSessionDO::getUpdatedAt)
        );
        return sessions.stream().map(chatConverter::toSessionBO).toList();
    }

    public List<ChatSessionBO> listRecentSessionsByUserId(Long userId, int limit) {
        List<ChatSessionDO> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<ChatSessionDO>()
                        .eq(ChatSessionDO::getUserId, userId)
                        .orderByDesc(ChatSessionDO::getUpdatedAt)
                        .last("LIMIT " + limit)
        );
        return sessions.stream().map(chatConverter::toSessionBO).toList();
    }

    public ChatMessageBO saveMessage(ChatMessageBO messageBO) {
        ChatMessageDO messageDO = chatConverter.toMessageDO(messageBO);
        messageMapper.insert(messageDO);
        messageBO.setId(messageDO.getId());
        return messageBO;
    }

    public List<ChatMessageBO> listMessagesBySessionId(Long sessionId) {
        List<ChatMessageDO> messages = messageMapper.selectList(
                new LambdaQueryWrapper<ChatMessageDO>()
                        .eq(ChatMessageDO::getSessionId, sessionId)
                        .orderByAsc(ChatMessageDO::getCreatedAt)
        );
        return messages.stream().map(chatConverter::toMessageBO).toList();
    }

    public void saveCitations(List<AnswerCitationBO> citations) {
        for (AnswerCitationBO citation : citations) {
            AnswerCitationDO citationDO = chatConverter.toCitationDO(citation);
            citationMapper.insert(citationDO);
        }
    }

    public List<AnswerCitationBO> getCitationsByMessageId(Long messageId) {
        List<AnswerCitationDO> citations = citationMapper.selectByMessageId(messageId);
        return citations.stream().map(chatConverter::toCitationBO).toList();
    }

    public void updateSessionTitle(Long sessionId, String title) {
        ChatSessionDO sessionDO = sessionMapper.selectById(sessionId);
        if (sessionDO != null) {
            sessionDO.setTitle(title);
            sessionMapper.updateById(sessionDO);
        }
    }

    public void deleteSession(Long sessionId) {
        // 先删除关联的消息和引用
        List<ChatMessageDO> messages = messageMapper.selectList(
                new LambdaQueryWrapper<ChatMessageDO>()
                        .eq(ChatMessageDO::getSessionId, sessionId)
        );
        for (ChatMessageDO message : messages) {
            citationMapper.deleteByMessageId(message.getId());
        }
        messageMapper.delete(
                new LambdaQueryWrapper<ChatMessageDO>()
                        .eq(ChatMessageDO::getSessionId, sessionId)
        );
        // 删除会话
        sessionMapper.deleteById(sessionId);
    }
}
