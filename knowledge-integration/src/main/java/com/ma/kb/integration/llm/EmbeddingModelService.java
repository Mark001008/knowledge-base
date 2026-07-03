package com.ma.kb.integration.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Locale;

/**
 * Embedding 生成服务。
 * 未配置 embedding 模型时使用本地哈希向量，保证 Milvus 入库链路可运行。
 */
@Service
public class EmbeddingModelService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingModelService.class);
    private static final String EMBEDDINGS_PATH = "/embeddings";

    private final String provider;
    private final String baseUrl;
    private final String apiKey;
    private final String embeddingModel;
    private final int dimension;
    private final int connectTimeoutSeconds;
    private final int readTimeoutSeconds;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmbeddingModelService(
            @Value("${agent.model.provider:mock}") String provider,
            @Value("${agent.model.base-url:}") String baseUrl,
            @Value("${agent.model.api-key:}") String apiKey,
            @Value("${agent.model.embedding-model:}") String embeddingModel,
            @Value("${vector.milvus.dimension:1536}") int dimension,
            @Value("${agent.model.timeout.connect:10}") int connectTimeoutSeconds,
            @Value("${agent.model.timeout.read:60}") int readTimeoutSeconds) {
        this.provider = provider;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.embeddingModel = embeddingModel;
        this.dimension = dimension;
        this.connectTimeoutSeconds = connectTimeoutSeconds;
        this.readTimeoutSeconds = readTimeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public float[] embed(String text) {
        if ("mock".equals(provider) || baseUrl.isBlank() || embeddingModel.isBlank()) {
            return localHashEmbedding(text);
        }
        return realEmbedding(text);
    }

    private float[] realEmbedding(String text) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", embeddingModel);
            root.put("input", text);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + EMBEDDINGS_PATH))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(readTimeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Embedding 调用失败: statusCode={}, body={}", response.statusCode(), response.body());
                throw new RuntimeException("Embedding 调用失败: HTTP " + response.statusCode());
            }

            JsonNode embeddingNode = objectMapper.readTree(response.body())
                    .path("data").path(0).path("embedding");
            if (!embeddingNode.isArray() || embeddingNode.isEmpty()) {
                throw new RuntimeException("Embedding 响应缺少向量数据");
            }

            float[] vector = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                vector[i] = (float) embeddingNode.get(i).asDouble();
            }
            return vector;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Embedding 调用被中断", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Embedding 调用异常: " + e.getMessage(), e);
        }
    }

    private float[] localHashEmbedding(String text) {
        float[] vector = new float[dimension];
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        String[] terms = normalized.split("[^\\p{IsHan}\\p{Alnum}]+");

        for (String term : terms) {
            if (term.isBlank()) continue;
            byte[] digest = digest(term);
            int bucket = Math.floorMod(toInt(digest), dimension);
            vector[bucket] += 1.0f;
        }
        normalize(vector);
        return vector;
    }

    private byte[] digest(String term) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(term.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("本地 Embedding 生成失败", e);
        }
    }

    private int toInt(byte[] bytes) {
        return ((bytes[0] & 0xff) << 24)
                | ((bytes[1] & 0xff) << 16)
                | ((bytes[2] & 0xff) << 8)
                | (bytes[3] & 0xff);
    }

    private void normalize(float[] vector) {
        double sum = 0;
        for (float value : vector) {
            sum += value * value;
        }
        if (sum == 0) return;
        float norm = (float) Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / norm;
        }
    }
}
