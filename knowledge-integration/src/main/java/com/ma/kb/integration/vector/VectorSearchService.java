package com.ma.kb.integration.vector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ma.kb.integration.llm.EmbeddingModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Milvus 向量检索服务。
 */
@Service
public class VectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchService.class);
    private static final String COLLECTIONS_HAS_PATH = "/v2/vectordb/collections/has";
    private static final String COLLECTIONS_CREATE_PATH = "/v2/vectordb/collections/create";
    private static final String COLLECTIONS_LOAD_PATH = "/v2/vectordb/collections/load";
    private static final String ENTITIES_INSERT_PATH = "/v2/vectordb/entities/insert";
    private static final String ENTITIES_SEARCH_PATH = "/v2/vectordb/entities/search";
    private static final String ENTITIES_DELETE_PATH = "/v2/vectordb/entities/delete";

    private final String endpoint;
    private final String token;
    private final String collectionName;
    private final int dimension;
    private final int timeoutSeconds;
    private final EmbeddingModelService embeddingModelService;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean collectionReady = new AtomicBoolean(false);

    public VectorSearchService(
            @Value("${vector.milvus.endpoint:http://localhost:19530}") String endpoint,
            @Value("${vector.milvus.token:}") String token,
            @Value("${vector.milvus.collection-name:kb_document_chunks}") String collectionName,
            @Value("${vector.milvus.dimension:1536}") int dimension,
            @Value("${vector.milvus.timeout:15}") int timeoutSeconds,
            EmbeddingModelService embeddingModelService) {
        this.endpoint = trimTrailingSlash(endpoint);
        this.token = token;
        this.collectionName = collectionName;
        this.dimension = dimension;
        this.timeoutSeconds = timeoutSeconds;
        this.embeddingModelService = embeddingModelService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public List<SearchResult> search(float[] queryEmbedding, Long spaceId, int topK,
                                     BigDecimal threshold) {
        ensureCollectionReady();

        ObjectNode body = objectMapper.createObjectNode();
        body.put("collectionName", collectionName);
        body.put("annsField", "vector");
        body.put("limit", topK);
        body.put("filter", "spaceId == " + spaceId);
        body.putArray("outputFields")
                .add("chunkId")
                .add("documentId")
                .add("documentName")
                .add("pageNumber")
                .add("chunkIndex")
                .add("content");
        ArrayNode data = body.putArray("data");
        data.add(toArrayNode(queryEmbedding));

        JsonNode response = post(ENTITIES_SEARCH_PATH, body);
        List<SearchResult> results = new ArrayList<>();
        JsonNode rows = response.path("data").path(0);
        if (!rows.isArray()) {
            rows = response.path("data");
        }
        for (JsonNode row : rows) {
            BigDecimal score = parseScore(row);
            if (threshold != null && score.compareTo(threshold) < 0) {
                continue;
            }
            SearchResult result = new SearchResult();
            result.setChunkId(row.path("chunkId").asLong());
            result.setDocumentId(row.path("documentId").asLong());
            result.setDocumentName(row.path("documentName").asText(null));
            result.setPageNumber(row.path("pageNumber").isMissingNode() || row.path("pageNumber").isNull() ? null : row.path("pageNumber").asInt());
            result.setChunkIndex(row.path("chunkIndex").asInt());
            result.setContent(row.path("content").asText(""));
            result.setScore(score);
            results.add(result);
        }
        log.info("Milvus 检索完成: spaceId={}, topK={}, results={}", spaceId, topK, results.size());
        return results;
    }

    public String store(Long chunkId, Long spaceId, Long documentId, String content,
                        float[] embedding, Map<String, Object> metadata) {
        ensureCollectionReady();

        String vectorId = "chunk_" + chunkId;
        ObjectNode entity = objectMapper.createObjectNode();
        entity.put("id", vectorId);
        entity.put("spaceId", spaceId);
        entity.put("documentId", documentId);
        entity.put("chunkId", chunkId);
        entity.put("documentName", String.valueOf(metadata.getOrDefault("documentName", "")));
        entity.put("chunkIndex", ((Number) metadata.getOrDefault("chunkIndex", 0)).intValue());
        Object pageNumber = metadata.get("pageNumber");
        if (pageNumber instanceof Number number) {
            entity.put("pageNumber", number.intValue());
        } else {
            entity.putNull("pageNumber");
        }
        entity.put("content", content);
        entity.set("vector", toArrayNode(embedding));

        ObjectNode body = objectMapper.createObjectNode();
        body.put("collectionName", collectionName);
        body.putArray("data").add(entity);
        post(ENTITIES_INSERT_PATH, body);

        log.info("Milvus 向量写入完成: chunkId={}, vectorId={}", chunkId, vectorId);
        return vectorId;
    }

    public void deleteByDocumentId(Long documentId) {
        ensureCollectionReady();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("collectionName", collectionName);
        body.put("filter", "documentId == " + documentId);
        post(ENTITIES_DELETE_PATH, body);
        log.info("Milvus 文档向量删除完成: documentId={}", documentId);
    }

    public float[] embed(String text) {
        return embeddingModelService.embed(text);
    }

    private void ensureCollectionReady() {
        if (collectionReady.get()) return;
        synchronized (collectionReady) {
            if (collectionReady.get()) return;
            if (!hasCollection()) {
                createCollection();
            }
            loadCollection();
            collectionReady.set(true);
        }
    }

    private boolean hasCollection() {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("collectionName", collectionName);
        JsonNode response = post(COLLECTIONS_HAS_PATH, body);
        return response.path("data").asBoolean(false);
    }

    private void createCollection() {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("collectionName", collectionName);
        body.put("dimension", dimension);
        body.put("metricType", "COSINE");
        body.put("primaryFieldName", "id");
        body.put("vectorFieldName", "vector");
        body.put("idType", "VarChar");
        body.put("autoID", false);
        post(COLLECTIONS_CREATE_PATH, body);
        log.info("Milvus 集合创建完成: collection={}, dimension={}", collectionName, dimension);
    }

    private void loadCollection() {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("collectionName", collectionName);
        post(COLLECTIONS_LOAD_PATH, body);
    }

    private JsonNode post(String path, ObjectNode body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + path))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
            if (!token.isBlank()) {
                builder.header("Authorization", "Bearer " + token);
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            int code = root.path("code").asInt(response.statusCode());
            if (response.statusCode() >= 400 || code != 0) {
                throw new RuntimeException("Milvus 请求失败: path=" + path
                        + ", status=" + response.statusCode()
                        + ", body=" + response.body());
            }
            return root;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Milvus 请求被中断: " + path, e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Milvus 请求异常: " + path + ", " + e.getMessage(), e);
        }
    }

    private ArrayNode toArrayNode(float[] values) {
        ArrayNode array = objectMapper.createArrayNode();
        for (float value : values) {
            array.add(value);
        }
        return array;
    }

    private BigDecimal parseScore(JsonNode row) {
        double value = row.path("score").asDouble(row.path("distance").asDouble(0));
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) return "http://localhost:19530";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
