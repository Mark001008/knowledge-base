package com.ma.kb.start.controller.chat;

import com.ma.kb.common.response.ApiResponse;
import com.ma.kb.core.auth.JwtService;
import com.ma.kb.core.auth.RequirePermission;
import com.ma.kb.core.auth.SecurityUtils;
import com.ma.kb.service.chat.ChatService;
import com.ma.kb.service.chat.ChatStreamSink;
import com.ma.kb.service.chat.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
    @RequirePermission("qa:create")
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
     * 流式发送问题。
     */
    @PostMapping(value = "/chat/sessions/{sessionId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequirePermission("qa:ask")
    public SseEmitter streamMessage(HttpServletRequest request,
                                    @PathVariable Long sessionId,
                                    @RequestBody ChatMessageRequest body) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        SseEmitter emitter = new SseEmitter(120_000L);
        ChatStreamSink sink = new SseChatStreamSink(emitter);
        CompletableFuture.runAsync(() -> chatService.streamMessage(userId, sessionId, body, sink));
        return emitter;
    }

    /**
     * 查询诊断，不写入会话。
     */
    @PostMapping("/spaces/{spaceId}/chat/diagnose")
    @RequirePermission("qa:view")
    public ApiResponse<ChatMessageResponse> diagnose(HttpServletRequest request,
                                                     @PathVariable Long spaceId,
                                                     @RequestBody ChatMessageRequest body) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        ChatMessageResponse response = chatService.diagnose(userId, spaceId, body);
        return ApiResponse.success(response);
    }

    /**
     * 提交问答反馈。
     */
    @PostMapping("/chat/feedback")
    @RequirePermission("qa:view")
    public ApiResponse<Void> submitFeedback(HttpServletRequest request,
                                            @RequestBody ChatFeedbackRequest body) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        chatService.submitFeedback(userId, body);
        return ApiResponse.success(null);
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
    @RequirePermission("qa:update")
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
    @RequirePermission("qa:delete")
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

    private static class SseChatStreamSink implements ChatStreamSink {
        private final SseEmitter emitter;

        private SseChatStreamSink(SseEmitter emitter) {
            this.emitter = emitter;
        }

        @Override
        public void status(String message) {
            send("status", new ChatStreamEvent("status", message, null), false);
        }

        @Override
        public void delta(String content) {
            send("delta", new ChatStreamEvent("delta", content, null), false);
        }

        @Override
        public void complete(ChatMessageResponse response) {
            send("complete", new ChatStreamEvent("complete", "", response), true);
        }

        @Override
        public void error(String message) {
            send("error", new ChatStreamEvent("error", message, null), true);
        }

        private synchronized void send(String name, ChatStreamEvent event, boolean complete) {
            try {
                emitter.send(SseEmitter.event().name(name).data(event));
                if (complete) {
                    emitter.complete();
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }
    }
}
