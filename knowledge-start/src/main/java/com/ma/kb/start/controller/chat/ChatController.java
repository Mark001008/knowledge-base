package com.ma.kb.start.controller.chat;

import com.ma.kb.common.response.ApiResponse;
import com.ma.kb.core.auth.JwtService;
import com.ma.kb.service.chat.ChatService;
import com.ma.kb.service.chat.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 问答控制器
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final ChatService chatService;
    private final JwtService jwtService;

    public ChatController(ChatService chatService, JwtService jwtService) {
        this.chatService = chatService;
        this.jwtService = jwtService;
    }

    /**
     * 创建会话
     */
    @PostMapping("/spaces/{spaceId}/chat/sessions")
    public ApiResponse<ChatSessionVO> createSession(HttpServletRequest request,
                                                    @PathVariable Long spaceId,
                                                    @RequestBody ChatSessionCreateRequest body) {
        Long userId = getCurrentUserId(request);
        ChatSessionVO session = chatService.createSession(userId, spaceId, body);
        return ApiResponse.success(session);
    }

    /**
     * 查询会话列表
     */
    @GetMapping("/spaces/{spaceId}/chat/sessions")
    public ApiResponse<List<ChatSessionVO>> listSessions(HttpServletRequest request,
                                                         @PathVariable Long spaceId) {
        Long userId = getCurrentUserId(request);
        List<ChatSessionVO> sessions = chatService.listSessions(userId, spaceId);
        return ApiResponse.success(sessions);
    }

    /**
     * 发送问题
     */
    @PostMapping("/chat/sessions/{sessionId}/messages")
    public ApiResponse<ChatMessageResponse> sendMessage(HttpServletRequest request,
                                                        @PathVariable Long sessionId,
                                                        @RequestBody ChatMessageRequest body) {
        Long userId = getCurrentUserId(request);
        ChatMessageResponse response = chatService.sendMessage(userId, sessionId, body);
        return ApiResponse.success(response);
    }

    /**
     * 查询会话消息列表
     */
    @GetMapping("/chat/sessions/{sessionId}/messages")
    public ApiResponse<List<ChatMessageVO>> listMessages(HttpServletRequest request,
                                                         @PathVariable Long sessionId) {
        Long userId = getCurrentUserId(request);
        List<ChatMessageVO> messages = chatService.listMessages(userId, sessionId);
        return ApiResponse.success(messages);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            String token = bearerToken.substring(BEARER_PREFIX.length());
            return jwtService.getUserId(token);
        }
        return null;
    }
}
