package com.ma.kb.service.chat;

import com.ma.kb.service.chat.dto.*;

import java.util.List;

/**
 * 问答服务接口
 */
public interface ChatService {

    /**
     * 创建会话
     */
    ChatSessionVO createSession(Long userId, Long spaceId, ChatSessionCreateRequest request);

    /**
     * 查询会话列表
     */
    List<ChatSessionVO> listSessions(Long userId, Long spaceId);

    /**
     * 发送问题
     */
    ChatMessageResponse sendMessage(Long userId, Long sessionId, ChatMessageRequest request);

    /**
     * 查询会话消息列表
     */
    List<ChatMessageVO> listMessages(Long userId, Long sessionId);

    /**
     * 更新会话（重命名）
     */
    void updateSession(Long userId, Long sessionId, String title);

    /**
     * 删除会话
     */
    void deleteSession(Long userId, Long sessionId);
}
