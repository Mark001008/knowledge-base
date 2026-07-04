package com.ma.kb.service.chat.impl;

import com.ma.kb.common.enums.ChatRoleEnum;
import com.ma.kb.common.exception.BusinessException;
import com.ma.kb.common.response.ErrorCode;
import com.ma.kb.core.chat.RagService;
import com.ma.kb.manager.auth.UserManager;
import com.ma.kb.manager.chat.ChatManager;
import com.ma.kb.manager.chat.bo.ChatMessageBO;
import com.ma.kb.manager.chat.bo.ChatSessionBO;
import com.ma.kb.manager.space.SpaceManager;
import com.ma.kb.service.chat.converter.ChatDTOConverter;
import com.ma.kb.service.chat.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatManager chatManager;
    @Mock
    private SpaceManager spaceManager;
    @Mock
    private UserManager userManager;
    @Mock
    private RagService ragService;
    @Mock
    private ChatDTOConverter chatDTOConverter;

    private ChatServiceImpl chatService;

    private static final Long USER_ID = 1L;
    private static final Long SPACE_ID = 100L;
    private static final Long SESSION_ID = 500L;

    @BeforeEach
    void setUp() {
        chatService = new ChatServiceImpl(chatManager, spaceManager, userManager, ragService, chatDTOConverter);
    }

    @Test
    void createSessionSuccess() {
        when(spaceManager.getMemberRole(SPACE_ID, USER_ID)).thenReturn("READER");

        ChatSessionBO sessionBO = buildSessionBO(SESSION_ID);
        when(chatDTOConverter.toSessionBO(any(), any(), any())).thenReturn(sessionBO);
        when(chatManager.createSession(any())).thenReturn(sessionBO);

        ChatSessionCreateRequest request = new ChatSessionCreateRequest("测试会话");
        ChatSessionVO result = chatService.createSession(USER_ID, SPACE_ID, request);

        assertNotNull(result);
        assertEquals(SESSION_ID, result.id());
    }

    @Test
    void createSessionAccessDenied() {
        when(spaceManager.getMemberRole(SPACE_ID, USER_ID)).thenReturn(null);

        ChatSessionCreateRequest request = new ChatSessionCreateRequest("测试会话");
        assertThrows(BusinessException.class,
                () -> chatService.createSession(USER_ID, SPACE_ID, request));
    }

    @Test
    void sendMessageSuccess() {
        ChatSessionBO sessionBO = buildSessionBO(SESSION_ID);
        when(chatManager.getSessionById(SESSION_ID)).thenReturn(sessionBO);
        when(spaceManager.getMemberRole(SPACE_ID, USER_ID)).thenReturn("READER");

        ChatMessageBO userMsgBO = buildMessageBO(1L, ChatRoleEnum.USER.getCode(), "问题");
        when(chatDTOConverter.toUserMessageBO(SESSION_ID, "问题")).thenReturn(userMsgBO);
        when(chatManager.saveMessage(any())).thenReturn(userMsgBO);

        RagService.RagResult ragResult = new RagService.RagResult(
                "回答内容", List.of(), "mock-model", 100, 50, 200L);
        when(ragService.ask("问题", SPACE_ID)).thenReturn(ragResult);

        ChatMessageBO assistantMsgBO = buildMessageBO(2L, ChatRoleEnum.ASSISTANT.getCode(), "回答内容");
        when(chatDTOConverter.toAssistantMessageBO(any(), any(), any(), anyInt(), anyInt(), anyLong())).thenReturn(assistantMsgBO);
        when(chatManager.saveMessage(any())).thenReturn(assistantMsgBO);

        ChatMessageRequest request = new ChatMessageRequest("问题");
        ChatMessageResponse response = chatService.sendMessage(USER_ID, SESSION_ID, request);

        assertNotNull(response);
        assertEquals(2L, response.messageId());
        assertEquals("回答内容", response.answer());
    }

    @Test
    void sendMessageSessionNotFound() {
        when(chatManager.getSessionById(999L)).thenReturn(null);

        ChatMessageRequest request = new ChatMessageRequest("问题");
        assertThrows(BusinessException.class,
                () -> chatService.sendMessage(USER_ID, 999L, request));
    }

    @Test
    void sendMessageAccessDenied() {
        ChatSessionBO sessionBO = buildSessionBO(SESSION_ID);
        when(chatManager.getSessionById(SESSION_ID)).thenReturn(sessionBO);
        when(spaceManager.getMemberRole(SPACE_ID, USER_ID)).thenReturn(null);

        ChatMessageRequest request = new ChatMessageRequest("问题");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> chatService.sendMessage(USER_ID, SESSION_ID, request));
        assertEquals(ErrorCode.SPACE_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    void listMessagesSuccess() {
        ChatSessionBO sessionBO = buildSessionBO(SESSION_ID);
        when(chatManager.getSessionById(SESSION_ID)).thenReturn(sessionBO);
        when(spaceManager.getMemberRole(SPACE_ID, USER_ID)).thenReturn("READER");

        ChatMessageBO userMsg = buildMessageBO(1L, ChatRoleEnum.USER.getCode(), "问题");
        ChatMessageBO assistantMsg = buildMessageBO(2L, ChatRoleEnum.ASSISTANT.getCode(), "回答");
        when(chatManager.listMessagesBySessionId(SESSION_ID)).thenReturn(List.of(userMsg, assistantMsg));
        when(chatManager.getCitationsByMessageId(2L)).thenReturn(List.of());

        List<ChatMessageVO> messages = chatService.listMessages(USER_ID, SESSION_ID);

        assertEquals(2, messages.size());
        assertEquals("user", messages.get(0).role());
        assertEquals("assistant", messages.get(1).role());
    }

    private ChatSessionBO buildSessionBO(Long id) {
        ChatSessionBO bo = new ChatSessionBO();
        bo.setId(id);
        bo.setSpaceId(SPACE_ID);
        bo.setUserId(USER_ID);
        bo.setTitle("测试会话");
        bo.setCreatedAt(LocalDateTime.now());
        bo.setUpdatedAt(LocalDateTime.now());
        return bo;
    }

    private ChatMessageBO buildMessageBO(Long id, String role, String content) {
        ChatMessageBO bo = new ChatMessageBO();
        bo.setId(id);
        bo.setSessionId(SESSION_ID);
        bo.setRole(role);
        bo.setContent(content);
        bo.setCreatedAt(LocalDateTime.now());
        return bo;
    }
}
