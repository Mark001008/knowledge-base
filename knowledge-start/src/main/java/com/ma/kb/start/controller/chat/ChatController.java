package com.ma.kb.start.controller.chat;

import com.ma.kb.common.response.ApiResponse;
import com.ma.kb.core.auth.JwtService;
import com.ma.kb.core.auth.RequirePermission;
import com.ma.kb.core.auth.SecurityUtils;
import com.ma.kb.service.chat.ChatService;
import com.ma.kb.service.chat.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 问答控制器
 */
@RestController
@RequestMapping("/api")
public class ChatController {

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
    @RequirePermission("qa:view")
    public ApiResponse<ChatSessionVO> createSession(HttpServletRequest request,
                                                    @PathVariable Long spaceId,
                                                    @RequestBody ChatSessionCreateRequest body) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        ChatSessionVO session = chatService.createSession(userId, spaceId, body);
        return ApiResponse.success(session);
    }

    /**
     * 查询会话列表
     */
    @GetMapping("/spaces/{spaceId}/chat/sessions")
    @RequirePermission("qa:view")
    public ApiResponse<List<ChatSessionVO>> listSessions(HttpServletRequest request,
                                                         @PathVariable Long spaceId) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        List<ChatSessionVO> sessions = chatService.listSessions(userId, spaceId);
        return ApiResponse.success(sessions);
    }

    /**
     * 发送问题
     */
    @PostMapping("/chat/sessions/{sessionId}/messages")
    @RequirePermission("qa:ask")
    public ApiResponse<ChatMessageResponse> sendMessage(HttpServletRequest request,
                                                        @PathVariable Long sessionId,
                                                        @RequestBody ChatMessageRequest body) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        ChatMessageResponse response = chatService.sendMessage(userId, sessionId, body);
        return ApiResponse.success(response);
    }

    /**
     * 查询会话消息列表
     */
    @GetMapping("/chat/sessions/{sessionId}/messages")
    @RequirePermission("qa:view")
    public ApiResponse<List<ChatMessageVO>> listMessages(HttpServletRequest request,
                                                         @PathVariable Long sessionId) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        List<ChatMessageVO> messages = chatService.listMessages(userId, sessionId);
        return ApiResponse.success(messages);
    }

    /**
     * 更新会话（重命名）
     */
    @PutMapping("/chat/sessions/{sessionId}")
    @RequirePermission("qa:view")
    public ApiResponse<Void> updateSession(HttpServletRequest request,
                                           @PathVariable Long sessionId,
                                           @RequestBody ChatSessionUpdateRequest body) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        chatService.updateSession(userId, sessionId, body.title());
        return ApiResponse.success(null);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/chat/sessions/{sessionId}")
    @RequirePermission("qa:view")
    public ApiResponse<Void> deleteSession(HttpServletRequest request,
                                           @PathVariable Long sessionId) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        chatService.deleteSession(userId, sessionId);
        return ApiResponse.success(null);
    }

    /**
     * 获取最近会话（跨知识库）
     */
    @GetMapping("/chat/recent-sessions")
    @RequirePermission("qa:view")
    public ApiResponse<List<RecentSessionVO>> listRecentSessions(HttpServletRequest request,
                                                                  @RequestParam(defaultValue = "20") int limit) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        List<RecentSessionVO> sessions = chatService.listRecentSessions(userId, limit);
        return ApiResponse.success(sessions);
    }
}
