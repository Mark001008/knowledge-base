package com.ma.kb.integration.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * LLM 聊天模型服务
 * 对接小米 MiLM（OpenAI 兼容接口）
 */
@Service
public class ChatModelService {

    private static final Logger log = LoggerFactory.getLogger(ChatModelService.class);
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final String provider;
    private final String baseUrl;
    private final String apiKey;
    private final String modelName;
    private final int connectTimeoutSeconds;
    private final int readTimeoutSeconds;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ChatModelService(
            @Value("${agent.model.provider:mock}") String provider,
            @Value("${agent.model.base-url:}") String baseUrl,
            @Value("${agent.model.api-key:}") String apiKey,
            @Value("${agent.model.chat-model:MiLM}") String modelName,
            @Value("${agent.model.timeout.connect:10}") int connectTimeoutSeconds,
            @Value("${agent.model.timeout.read:60}") int readTimeoutSeconds) {
        this.provider = provider;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.connectTimeoutSeconds = connectTimeoutSeconds;
        this.readTimeoutSeconds = readTimeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 调用 LLM 生成回答
     *
     * @param systemPrompt 系统提示词
     * @param userMessage  用户消息
     * @return 模型回答
     */
    public ChatResponse chat(String systemPrompt, String userMessage) {
        if ("mock".equals(provider) || baseUrl.isBlank()) {
            return mockChat(userMessage);
        }
        return realChat(systemPrompt, userMessage);
    }

    private ChatResponse realChat(String systemPrompt, String userMessage) {
        long startTime = System.currentTimeMillis();
        try {
            String requestBody = buildRequestBody(systemPrompt, userMessage);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + CHAT_COMPLETIONS_PATH))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(readTimeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("LLM 调用失败: statusCode={}, body={}", response.statusCode(), response.body());
                throw new RuntimeException("LLM 调用失败: HTTP " + response.statusCode());
            }

            return parseResponse(response.body(), startTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("LLM 调用被中断", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM 调用异常", e);
            throw new RuntimeException("LLM 调用异常: " + e.getMessage(), e);
        }
    }

    private String buildRequestBody(String systemPrompt, String userMessage) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", modelName);

        ArrayNode messages = root.putArray("messages");
        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);

        return root.toString();
    }

    private ChatResponse parseResponse(String responseBody, long startTime) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String answer = root.path("choices").path(0).path("message").path("content").asText();
            String actualModel = root.path("model").asText(modelName);
            int promptTokens = root.path("usage").path("prompt_tokens").asInt(0);
            int completionTokens = root.path("usage").path("completion_tokens").asInt(0);

            long latencyMs = System.currentTimeMillis() - startTime;
            log.info("LLM 调用完成: model={}, promptTokens={}, completionTokens={}, latency={}ms",
                    actualModel, promptTokens, completionTokens, latencyMs);

            return new ChatResponse(answer, actualModel, promptTokens, completionTokens);
        } catch (Exception e) {
            log.error("LLM 响应解析失败: {}", responseBody, e);
            throw new RuntimeException("LLM 响应解析失败", e);
        }
    }

    private ChatResponse mockChat(String userMessage) {
        log.info("LLM 调用 (mock): userMessageLength={}", userMessage.length());
        String answer = "这是 Mock 回答。当前系统未配置真实的 LLM 模型服务。\n\n"
                + "您的问题是：" + userMessage + "\n\n"
                + "请配置 AI_BASE_URL 和 AI_API_KEY 环境变量以启用真实的 AI 问答功能。";
        return new ChatResponse(answer, "mock-model", 100, 50);
    }

    /**
     * LLM 响应
     */
    public record ChatResponse(
            String answer,
            String modelName,
            int promptTokens,
            int completionTokens
    ) {
    }
}
