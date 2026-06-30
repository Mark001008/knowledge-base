package com.ma.agent.controller;

import com.ma.agent.agent.KnowledgeQAService;
import com.ma.agent.agent.dto.KnowledgeQAResponse;
import com.ma.agent.entity.ConversationEntity;
import com.ma.agent.shared.LogMarkers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 知识库问答 API
 * <p>基于 RAG 检索增强生成，结合知识库文档内容回答用户问题</p>
 */
@RestController
@RequestMapping("/api/qa")
public class KnowledgeQAController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeQAController.class);

    private final KnowledgeQAService knowledgeQAService;

    public KnowledgeQAController(KnowledgeQAService knowledgeQAService) {
        this.knowledgeQAService = knowledgeQAService;
    }

    /**
     * 知识库问答（同步）
     */
    @PostMapping("/ask")
    public KnowledgeQAResponse ask(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        String kbId = request.getOrDefault("kbId", "default");
        String conversationId = request.get("conversationId");

        log.info(LogMarkers.API, "POST /api/qa/ask kbId={} questionLength={}", kbId, question != null ? question.length() : 0);

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("问题不能为空");
        }

        var response = knowledgeQAService.ask(question, kbId, conversationId);
        log.info(LogMarkers.API, "POST /api/qa/ask conv={} sources={}", response.conversationId(), response.sources().size());
        return response;
    }

    /**
     * 知识库问答（SSE 流式）
     */
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        String kbId = request.getOrDefault("kbId", "default");
        String conversationId = request.get("conversationId");

        log.info(LogMarkers.API, "POST /api/qa/ask/stream kbId={} questionLength={}", kbId, question != null ? question.length() : 0);

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("问题不能为空");
        }

        return knowledgeQAService.askStream(question, kbId, conversationId);
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/history/{conversationId}")
    public List<ConversationEntity> getHistory(@PathVariable String conversationId) {
        log.info(LogMarkers.API, "GET /api/qa/history/{}", conversationId);
        return knowledgeQAService.getConversationHistory(conversationId);
    }
}
