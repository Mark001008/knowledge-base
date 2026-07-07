package com.ma.kb.service.chat.impl;

import com.ma.kb.common.enums.ChatRoleEnum;
import com.ma.kb.common.exception.BusinessException;
import com.ma.kb.common.response.ErrorCode;
import com.ma.kb.core.chat.RagService;
import com.ma.kb.manager.auth.UserManager;
import com.ma.kb.manager.auth.bo.UserBO;
import com.ma.kb.integration.vector.SearchResult;
import com.ma.kb.manager.chat.ChatManager;
import com.ma.kb.manager.chat.bo.AnswerCitationBO;
import com.ma.kb.manager.chat.bo.ChatMessageBO;
import com.ma.kb.manager.chat.bo.ChatSessionBO;
import com.ma.kb.manager.space.SpaceManager;
import com.ma.kb.service.chat.ChatService;
import com.ma.kb.service.chat.ChatStreamSink;
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
    private static final String SYSTEM_ADMIN_ROLE = "SYSTEM_ADMIN";

    private final ChatManager chatManager;
    private final SpaceManager spaceManager;
    private final UserManager userManager;
    private final RagService ragService;
    private final ChatDTOConverter chatDTOConverter;

    public ChatServiceImpl(ChatManager chatManager, SpaceManager spaceManager, UserManager userManager,
                           RagService ragService, ChatDTOConverter chatDTOConverter) {
        this.chatManager = chatManager;
        this.spaceManager = spaceManager;
        this.userManager = userManager;
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

        List<ChatSessionBO> sessions = isSystemAdmin(userId)
                ? chatManager.listSessionsBySpaceId(spaceId)
                : chatManager.listSessionsBySpaceIdAndUserId(spaceId, userId);
        return sessions.stream()
                .map(s -> new ChatSessionVO(s.getId(), s.getSpaceId(),
                        s.getTitle(), s.getCreatedAt(), s.getUpdatedAt()))
                .toList();
    }

    @Override
    public List<RecentSessionVO> listRecentSessions(Long userId, int limit) {
        List<ChatSessionBO> sessions = chatManager.listRecentSessionsByUserId(userId, limit);
        return sessions.stream().map(s -> {
            String spaceName = "未知知识库";
            var space = spaceManager.getById(s.getSpaceId());
            if (space != null) {
                spaceName = space.getName();
            }
            return new RecentSessionVO(s.getId(), s.getSpaceId(), spaceName, s.getTitle(), s.getUpdatedAt());
        }).toList();
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

        List<CitationDTO> citations = saveCitations(savedMsg.getId(), ragResult.citations());

        log.info("问答完成: sessionId={}, answerLength={}, citations={}",
                sessionId, ragResult.answer().length(), citations.size());

        return new ChatMessageResponse(savedMsg.getId(), ragResult.answer(), citations, toDiagnosticsDTO(ragResult.diagnostics()));
    }

    @Override
    public void streamMessage(Long userId, Long sessionId, ChatMessageRequest request, ChatStreamSink sink) {
        try {
            ChatSessionBO session = chatManager.getSessionById(sessionId);
            if (session == null) {
                throw new BusinessException(ErrorCode.CHAT_SESSION_NOT_FOUND);
            }
            checkSpaceAccess(userId, session.getSpaceId());

            ChatMessageBO userMsg = chatDTOConverter.toUserMessageBO(sessionId, request.question());
            chatManager.saveMessage(userMsg);

            sink.status("正在检索知识库");
            RagService.RagResult ragResult = ragService.askStream(request.question(), session.getSpaceId(), sink::delta);

            ChatMessageBO assistantMsg = chatDTOConverter.toAssistantMessageBO(
                    sessionId, ragResult.answer(), ragResult.modelName(),
                    ragResult.promptTokens(), ragResult.completionTokens(), ragResult.latencyMs());
            ChatMessageBO savedMsg = chatManager.saveMessage(assistantMsg);
            List<CitationDTO> citations = saveCitations(savedMsg.getId(), ragResult.citations());
            ChatMessageResponse response = new ChatMessageResponse(
                    savedMsg.getId(), ragResult.answer(), citations, toDiagnosticsDTO(ragResult.diagnostics()));

            log.info("流式问答完成: sessionId={}, answerLength={}, citations={}",
                    sessionId, ragResult.answer().length(), citations.size());
            sink.complete(response);
        } catch (Exception e) {
            log.error("流式问答失败: sessionId={}", sessionId, e);
            sink.error(e.getMessage() == null ? "流式问答失败" : e.getMessage());
        }
    }

    @Override
    public ChatMessageResponse diagnose(Long userId, Long spaceId, ChatMessageRequest request) {
        checkSpaceAccess(userId, spaceId);
        RagService.RagResult ragResult = ragService.ask(request.question(), spaceId);
        List<CitationDTO> citations = ragResult.citations() == null
                ? List.of()
                : ragResult.citations().stream()
                        .map(sr -> toCitationDTO(sr, sr.getDocumentId() + "_" + sr.getChunkId()))
                        .toList();
        log.info("RAG 查询诊断完成: userId={}, spaceId={}, hitCount={}, mode={}",
                userId, spaceId, citations.size(), ragResult.diagnostics().retrievalMode());
        return new ChatMessageResponse(null, ragResult.answer(), citations, toDiagnosticsDTO(ragResult.diagnostics()));
    }

    @Override
    public void submitFeedback(Long userId, ChatFeedbackRequest request) {
        log.info("问答反馈: userId={}, messageId={}, rating={}, reason={}",
                userId, request.messageId(), request.rating(), request.reason());
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
                        .map(c -> {
                            String citationId = c.getDocumentId() + "_" + c.getChunkId();
                            return new CitationDTO(citationId, c.getDocumentId(), c.getDocumentName(),
                                    c.getChunkId(), c.getPageNumber(), null, c.getScore(), c.getQuoteText());
                        })
                        .toList();
            }
            result.add(new ChatMessageVO(msg.getId(), msg.getRole(), msg.getContent(),
                    msg.getModelName(), citations, null, msg.getCreatedAt()));
        }

        return result;
    }

    @Override
    public void updateSession(Long userId, Long sessionId, String title) {
        ChatSessionBO session = chatManager.getSessionById(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.CHAT_SESSION_NOT_FOUND);
        }
        checkSpaceAccess(userId, session.getSpaceId());

        chatManager.updateSessionTitle(sessionId, title);
        log.info("会话重命名成功: sessionId={}, newTitle={}", sessionId, title);
    }

    @Override
    public void deleteSession(Long userId, Long sessionId) {
        ChatSessionBO session = chatManager.getSessionById(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.CHAT_SESSION_NOT_FOUND);
        }
        checkSpaceAccess(userId, session.getSpaceId());

        chatManager.deleteSession(sessionId);
        log.info("会话删除成功: sessionId={}", sessionId);
    }

    // ==================== 私有方法 ====================

    private void checkSpaceAccess(Long userId, Long spaceId) {
        if (isSystemAdmin(userId)) {
            return;
        }

        String role = spaceManager.getMemberRole(spaceId, userId);
        if (role == null) {
            throw new BusinessException(ErrorCode.SPACE_ACCESS_DENIED);
        }
    }

    private boolean isSystemAdmin(Long userId) {
        UserBO user = userManager.getById(userId);
        return user != null && user.getRoles() != null && user.getRoles().contains(SYSTEM_ADMIN_ROLE);
    }

    private CitationDTO toCitationDTO(SearchResult sr, String citationId) {
        return new CitationDTO(
                citationId,
                sr.getDocumentId(), sr.getDocumentName(),
                sr.getChunkId(), sr.getPageNumber(), sr.getChunkIndex(),
                sr.getScore(), sr.getContent()
        );
    }

    private List<CitationDTO> saveCitations(Long messageId, List<SearchResult> searchResults) {
        List<CitationDTO> citations = new ArrayList<>();
        if (searchResults == null || searchResults.isEmpty()) {
            return citations;
        }
        List<AnswerCitationBO> citationBOs = new ArrayList<>();
        for (SearchResult sr : searchResults) {
            AnswerCitationBO citation = chatDTOConverter.toCitationBO(messageId, sr);
            citationBOs.add(citation);

            String citationId = sr.getDocumentId() + "_" + sr.getChunkId();
            citations.add(toCitationDTO(sr, citationId));
        }
        chatManager.saveCitations(citationBOs);
        return citations;
    }

    private RetrievalDiagnosticsDTO toDiagnosticsDTO(RagService.RetrievalDiagnostics diagnostics) {
        if (diagnostics == null) {
            return null;
        }
        RagService.IndexHealth health = diagnostics.indexHealth();
        IndexHealthDTO indexHealth = health == null ? null : new IndexHealthDTO(
                health.totalDocuments(),
                health.completedDocuments(),
                health.processingDocuments(),
                health.failedDocuments(),
                health.chunkCount(),
                health.vectorEnabled(),
                health.lastIndexedAt()
        );
        return new RetrievalDiagnosticsDTO(
                diagnostics.hitCount(),
                diagnostics.bestScore(),
                diagnostics.threshold(),
                diagnostics.topK(),
                diagnostics.retrievalMode(),
                diagnostics.keywordFallbackUsed(),
                diagnostics.enteredPrompt(),
                diagnostics.lowConfidence(),
                diagnostics.noAnswerReason(),
                diagnostics.explanation(),
                indexHealth
        );
    }
}
