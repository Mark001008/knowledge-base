package com.ma.kb.service.chat.impl;

import com.ma.kb.common.enums.ChatRoleEnum;
import com.ma.kb.common.exception.BusinessException;
import com.ma.kb.common.response.ErrorCode;
import com.ma.kb.core.chat.RagService;
import com.ma.kb.integration.vector.SearchResult;
import com.ma.kb.manager.chat.ChatManager;
import com.ma.kb.manager.chat.bo.AnswerCitationBO;
import com.ma.kb.manager.chat.bo.ChatMessageBO;
import com.ma.kb.manager.chat.bo.ChatSessionBO;
import com.ma.kb.manager.space.SpaceManager;
import com.ma.kb.service.chat.ChatService;
import com.ma.kb.service.chat.converter.ChatDTOConverter;
import com.ma.kb.service.chat.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 问答服务实现
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    private final ChatManager chatManager;
    private final SpaceManager spaceManager;
    private final RagService ragService;
    private final ChatDTOConverter chatDTOConverter;

    public ChatServiceImpl(ChatManager chatManager, SpaceManager spaceManager,
                           RagService ragService, ChatDTOConverter chatDTOConverter) {
        this.chatManager = chatManager;
        this.spaceManager = spaceManager;
        this.ragService = ragService;
        this.chatDTOConverter = chatDTOConverter;
    }

    @Override
    public ChatSessionVO createSession(Long userId, Long spaceId,
                                       ChatSessionCreateRequest request) {
        checkSpaceAccess(userId, spaceId);

        ChatSessionBO sessionBO = chatDTOConverter.toSessionBO(request, spaceId, userId);

        ChatSessionBO created = chatManager.createSession(sessionBO);
        log.info("会话创建成功: id={}, spaceId={}, userId={}", created.getId(), spaceId, userId);

        return new ChatSessionVO(created.getId(), created.getSpaceId(),
                created.getTitle(), created.getCreatedAt(), created.getUpdatedAt());
    }

    @Override
    public List<ChatSessionVO> listSessions(Long userId, Long spaceId) {
        checkSpaceAccess(userId, spaceId);

        List<ChatSessionBO> sessions = chatManager.listSessionsBySpaceIdAndUserId(spaceId, userId);
        return sessions.stream()
                .map(s -> new ChatSessionVO(s.getId(), s.getSpaceId(),
                        s.getTitle(), s.getCreatedAt(), s.getUpdatedAt()))
                .toList();
    }

    @Override
    public ChatMessageResponse sendMessage(Long userId, Long sessionId,
                                           ChatMessageRequest request) {
        ChatSessionBO session = chatManager.getSessionById(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.CHAT_SESSION_NOT_FOUND);
        }
        checkSpaceAccess(userId, session.getSpaceId());

        // 保存用户消息
        ChatMessageBO userMsg = chatDTOConverter.toUserMessageBO(sessionId, request.question());
        chatManager.saveMessage(userMsg);

        // 执行 RAG 问答
        RagService.RagResult ragResult = ragService.ask(request.question(), session.getSpaceId());

        // 保存助手消息
        ChatMessageBO assistantMsg = chatDTOConverter.toAssistantMessageBO(
                sessionId, ragResult.answer(), ragResult.modelName(),
                ragResult.promptTokens(), ragResult.completionTokens(), ragResult.latencyMs());
        ChatMessageBO savedMsg = chatManager.saveMessage(assistantMsg);

        // 保存引用
        List<CitationDTO> citations = new ArrayList<>();
        if (ragResult.citations() != null && !ragResult.citations().isEmpty()) {
            List<AnswerCitationBO> citationBOs = new ArrayList<>();
            for (SearchResult sr : ragResult.citations()) {
                AnswerCitationBO citation = chatDTOConverter.toCitationBO(savedMsg.getId(), sr);
                citationBOs.add(citation);

                citations.add(new CitationDTO(
                        sr.getDocumentId(), sr.getDocumentName(),
                        sr.getChunkId(), sr.getPageNumber(),
                        sr.getScore(), sr.getContent()
                ));
            }
            chatManager.saveCitations(citationBOs);
        }

        log.info("问答完成: sessionId={}, answerLength={}, citations={}",
                sessionId, ragResult.answer().length(), citations.size());

        return new ChatMessageResponse(savedMsg.getId(), ragResult.answer(), citations);
    }

    @Override
    public List<ChatMessageVO> listMessages(Long userId, Long sessionId) {
        ChatSessionBO session = chatManager.getSessionById(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.CHAT_SESSION_NOT_FOUND);
        }
        checkSpaceAccess(userId, session.getSpaceId());

        List<ChatMessageBO> messages = chatManager.listMessagesBySessionId(sessionId);
        List<ChatMessageVO> result = new ArrayList<>();

        for (ChatMessageBO msg : messages) {
            List<CitationDTO> citations = null;
            if (ChatRoleEnum.ASSISTANT.getCode().equals(msg.getRole())) {
                List<AnswerCitationBO> citationBOs = chatManager.getCitationsByMessageId(msg.getId());
                citations = citationBOs.stream()
                        .map(c -> new CitationDTO(c.getDocumentId(), c.getDocumentName(),
                                c.getChunkId(), c.getPageNumber(), c.getScore(), c.getQuoteText()))
                        .toList();
            }
            result.add(new ChatMessageVO(msg.getId(), msg.getRole(), msg.getContent(),
                    msg.getModelName(), citations, msg.getCreatedAt()));
        }

        return result;
    }

    // ==================== 私有方法 ====================

    private void checkSpaceAccess(Long userId, Long spaceId) {
        String role = spaceManager.getMemberRole(spaceId, userId);
        if (role == null) {
            throw new BusinessException(ErrorCode.SPACE_ACCESS_DENIED);
        }
    }
}
