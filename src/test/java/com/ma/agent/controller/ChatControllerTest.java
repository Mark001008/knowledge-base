package com.ma.agent.controller;

import com.ma.agent.agent.KnowledgeQAService;
import com.ma.agent.agent.dto.KnowledgeQAResponse;
import com.ma.agent.model.ModelProviderException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KnowledgeQAController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KnowledgeQAService knowledgeQAService;

    @Test
    @DisplayName("POST /api/qa/ask 成功返回知识库问答响应")
    void ask_shouldReturnQAResponse() throws Exception {
        var sources = List.of(
                new KnowledgeQAResponse.KnowledgeSource("doc-1", "test.txt", "相关片段", 0.95f)
        );
        var response = new KnowledgeQAResponse("conv-1", "这是答案", sources, "mock-model", Instant.now());
        when(knowledgeQAService.ask(anyString(), anyString(), any())).thenReturn(response);

        mockMvc.perform(post("/api/qa/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"测试问题","kbId":"default"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value("conv-1"))
                .andExpect(jsonPath("$.answer").value("这是答案"))
                .andExpect(jsonPath("$.sources").isArray())
                .andExpect(jsonPath("$.sources[0].filename").value("test.txt"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("POST /api/qa/ask 模型提供商异常时返回 502")
    void ask_shouldReturn502WhenProviderFails() throws Exception {
        when(knowledgeQAService.ask(anyString(), anyString(), any()))
                .thenThrow(new ModelProviderException("Xiaomi MiMo API key is missing"));

        mockMvc.perform(post("/api/qa/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"测试问题","kbId":"default"}"""))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("MODEL_PROVIDER_ERROR"))
                .andExpect(jsonPath("$.message").value("Xiaomi MiMo API key is missing"))
                .andExpect(jsonPath("$.path").value("/api/qa/ask"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("POST /api/qa/ask 问题为空时返回 400")
    void ask_shouldReturn400WhenQuestionIsEmpty() throws Exception {
        mockMvc.perform(post("/api/qa/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"kbId":"default"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("POST /api/qa/ask/stream 成功返回 SSE 流")
    void askStream_shouldReturnSseEmitter() throws Exception {
        when(knowledgeQAService.askStream(anyString(), anyString(), any())).thenReturn(new SseEmitter());

        mockMvc.perform(post("/api/qa/ask/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"测试问题","kbId":"default"}"""))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }
}
