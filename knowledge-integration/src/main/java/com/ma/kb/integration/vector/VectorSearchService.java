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
 * 向量检索服务。
 * 支持 Milvus 和 Qdrant，默认关闭。
 */
@Service
public class VectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchService.class);
    private static final String PROVIDER_MILVUS = "milvus";
    private static final String PROVIDER_QDRANT = "qdrant";
    private static final String COLLECTIONS_HAS_PATH = "/v2/vectordb/collections/has";
    private static final String COLLECTIONS_CREATE_PATH = "/v2/vectordb/collections/create";
    private static final String COLLECTIONS_LOAD_PATH = "/v2/vectordb/collections/load";
    private static final String ENTITIES_INSERT_PATH = "/v2/vectordb/entities/insert";
    private static final String ENTITIES_SEARCH_PATH = "/v2/vectordb/entities/search";
    private static final String ENTITIES_DELETE_PATH = "/v2/vectordb/entities/delete";

    private final String provider;
    private final boolean enabled;
    private final String milvusEndpoint;
    private final String milvusToken;
    private final String qdrantEndpoint;
    private final String qdrantApiKey;
    private final String collectionName;
    private final int dimension;
    private final int timeoutSeconds;
    private final EmbeddingModelService embeddingModelService;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean collectionReady = new AtomicBoolean(false);

    public VectorSearchService(
            @Value("${vector.enabled:${vector.milvus.enabled:false}}") boolean enabled,
            @Value("${vector.provider:milvus}") String provider,
            @Value("${vector.milvus.endpoint:http://localhost:19530}") String milvusEndpoint,
            @Value("${vector.milvus.token:}") String milvusToken,
            @Value("${vector.qdrant.endpoint:http://localhost:6333}") String qdrantEndpoint,
            @Value("${vector.qdrant.api-key:}") String qdrantApiKey,
            @Value("${vector.collection-name:${vector.milvus.collection-name:kb_document_chunks}}") String collectionName,
            @Value("${vector.dimension:${vector.milvus.dimension:1536}}") int dimension,
            @Value("${vector.timeout:${vector.milvus.timeout:15}}") int timeoutSeconds,
            EmbeddingModelService embeddingModelService) {
        this.enabled = enabled;
        this.provider = provider == null || provider.isBlank() ? PROVIDER_MILVUS : provider.toLowerCase();
        this.milvusEndpoint = trimTrailingSlash(milvusEndpoint, "http://localhost:19530");
        this.milvusToken = milvusToken;
        this.qdrantEndpoint = trimTrailingSlash(qdrantEndpoint, "http://localhost:6333");
        this.qdrantApiKey = qdrantApiKey;
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
        if (!enabled) {
            log.info("向量库未启用，跳过向量检索: spaceId={}", spaceId);
            return List.of();
        }
        if (PROVIDER_QDRANT.equals(provider)) {
            return searchQdrant(queryEmbedding, spaceId, topK, threshold);
        }
        if (!PROVIDER_MILVUS.equals(provider)) {
            log.warn("未知向量库 provider={}，跳过向量检索: spaceId={}", provider, spaceId);
            return List.of();
        }
        return searchMilvus(queryEmbedding, spaceId, topK, threshold);
    }

    public boolean isEnabled() {
        return enabled;
    }

    private List<SearchResult> searchMilvus(float[] queryEmbedding, Long spaceId, int topK,
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

    private List<SearchResult> searchQdrant(float[] queryEmbedding, Long spaceId, int topK,
                                            BigDecimal threshold) {
        ensureCollectionReady();

        ObjectNode body = objectMapper.createObjectNode();
        body.set("vector", toArrayNode(queryEmbedding));
        body.put("limit", topK);
        body.put("with_payload", true);
        ObjectNode filter = body.putObject("filter");
        ArrayNode must = filter.putArray("must");
        ObjectNode condition = must.addObject();
        condition.put("key", "spaceId");
        condition.putObject("match").put("value", spaceId);

        JsonNode response = request("POST",
                qdrantEndpoint + "/collections/" + collectionName + "/points/search",
                body,
                true);
        List<SearchResult> results = new ArrayList<>();
        JsonNode rows = response.path("result");
        if (!rows.isArray()) {
            return results;
        }
        for (JsonNode row : rows) {
            BigDecimal score = BigDecimal.valueOf(row.path("score").asDouble(0))
                    .setScale(6, RoundingMode.HALF_UP);
            if (threshold != null && score.compareTo(threshold) < 0) {
                continue;
            }
            JsonNode payload = row.path("payload");
            SearchResult result = new SearchResult();
            result.setChunkId(payload.path("chunkId").asLong());
            result.setDocumentId(payload.path("documentId").asLong());
            result.setDocumentName(payload.path("documentName").asText(null));
            result.setPageNumber(payload.path("pageNumber").isMissingNode() || payload.path("pageNumber").isNull() ? null : payload.path("pageNumber").asInt());
            result.setChunkIndex(payload.path("chunkIndex").asInt());
            result.setContent(payload.path("content").asText(""));
            result.setScore(score);
            results.add(result);
        }
        log.info("Qdrant 检索完成: spaceId={}, topK={}, results={}", spaceId, topK, results.size());
        return results;
    }

    public String store(Long chunkId, Long spaceId, Long documentId, String content,
                        float[] embedding, Map<String, Object> metadata) {
        if (!enabled) {
            String vectorId = "disabled_" + chunkId;
            log.info("向量库未启用，跳过向量写入: chunkId={}, vectorId={}", chunkId, vectorId);
            return vectorId;
        }
        if (PROVIDER_QDRANT.equals(provider)) {
            return storeQdrant(chunkId, spaceId, documentId, content, embedding, metadata);
        }
        if (!PROVIDER_MILVUS.equals(provider)) {
            String vectorId = "unsupported_" + chunkId;
            log.warn("未知向量库 provider={}，跳过向量写入: chunkId={}, vectorId={}", provider, chunkId, vectorId);
            return vectorId;
        }
        return storeMilvus(chunkId, spaceId, documentId, content, embedding, metadata);
    }

    private String storeMilvus(Long chunkId, Long spaceId, Long documentId, String content,
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

    private String storeQdrant(Long chunkId, Long spaceId, Long documentId, String content,
                               float[] embedding, Map<String, Object> metadata) {
        ensureCollectionReady();

        String vectorId = "qdrant_" + chunkId;
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("spaceId", spaceId);
        payload.put("documentId", documentId);
        payload.put("chunkId", chunkId);
        payload.put("documentName", String.valueOf(metadata.getOrDefault("documentName", "")));
        payload.put("chunkIndex", ((Number) metadata.getOrDefault("chunkIndex", 0)).intValue());
        Object pageNumber = metadata.get("pageNumber");
        if (pageNumber instanceof Number number) {
            payload.put("pageNumber", number.intValue());
        } else {
            payload.putNull("pageNumber");
        }
        payload.put("content", content);

        ObjectNode point = objectMapper.createObjectNode();
        point.put("id", chunkId);
        point.set("vector", toArrayNode(embedding));
        point.set("payload", payload);

        ObjectNode body = objectMapper.createObjectNode();
        body.putArray("points").add(point);
        request("PUT",
                qdrantEndpoint + "/collections/" + collectionName + "/points?wait=true",
                body,
                true);

        log.info("Qdrant 向量写入完成: chunkId={}, vectorId={}", chunkId, vectorId);
        return vectorId;
    }

    public void deleteByDocumentId(Long documentId) {
        if (!enabled) {
            log.info("向量库未启用，跳过向量删除: documentId={}", documentId);
            return;
        }
        if (PROVIDER_QDRANT.equals(provider)) {
            deleteQdrantByDocumentId(documentId);
            return;
        }
        if (!PROVIDER_MILVUS.equals(provider)) {
            log.warn("未知向量库 provider={}，跳过向量删除: documentId={}", provider, documentId);
            return;
        }
        deleteMilvusByDocumentId(documentId);
    }

    private void deleteMilvusByDocumentId(Long documentId) {
        ensureCollectionReady();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("collectionName", collectionName);
        body.put("filter", "documentId == " + documentId);
        post(ENTITIES_DELETE_PATH, body);
        log.info("Milvus 文档向量删除完成: documentId={}", documentId);
    }

    private void deleteQdrantByDocumentId(Long documentId) {
        ensureCollectionReady();
        ObjectNode body = objectMapper.createObjectNode();
        ObjectNode filter = body.putObject("filter");
        ArrayNode must = filter.putArray("must");
        ObjectNode condition = must.addObject();
        condition.put("key", "documentId");
        condition.putObject("match").put("value", documentId);
        request("POST",
                qdrantEndpoint + "/collections/" + collectionName + "/points/delete?wait=true",
                body,
                true);
        log.info("Qdrant 文档向量删除完成: documentId={}", documentId);
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
        if (PROVIDER_QDRANT.equals(provider)) {
            try {
                request("GET", qdrantEndpoint + "/collections/" + collectionName, null, true);
                return true;
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().contains("status=404")) {
                    return false;
                }
                throw e;
            }
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("collectionName", collectionName);
        JsonNode response = post(COLLECTIONS_HAS_PATH, body);
        return response.path("data").asBoolean(false);
    }

    private void createCollection() {
        if (PROVIDER_QDRANT.equals(provider)) {
            ObjectNode body = objectMapper.createObjectNode();
            ObjectNode vectors = body.putObject("vectors");
            vectors.put("size", dimension);
            vectors.put("distance", "Cosine");
            request("PUT", qdrantEndpoint + "/collections/" + collectionName, body, true);
            log.info("Qdrant 集合创建完成: collection={}, dimension={}", collectionName, dimension);
            return;
        }
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
        if (PROVIDER_QDRANT.equals(provider)) {
            return;
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("collectionName", collectionName);
        post(COLLECTIONS_LOAD_PATH, body);
    }

    private JsonNode post(String path, ObjectNode body) {
        return request("POST", milvusEndpoint + path, body, false);
    }

    private JsonNode request(String method, String url, ObjectNode body, boolean qdrant) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(timeoutSeconds));
            if (qdrant && !qdrantApiKey.isBlank()) {
                builder.header("api-key", qdrantApiKey);
            }
            if (!qdrant && !milvusToken.isBlank()) {
                builder.header("Authorization", "Bearer " + milvusToken);
            }
            if ("GET".equals(method)) {
                builder.GET();
            } else if ("PUT".equals(method)) {
                builder.PUT(HttpRequest.BodyPublishers.ofString(body == null ? "" : body.toString()));
            } else if ("POST".equals(method)) {
                builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body.toString()));
            } else {
                throw new IllegalArgumentException("不支持的 HTTP 方法: " + method);
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            if (qdrant) {
                if (response.statusCode() >= 400 || "error".equals(root.path("status").asText())) {
                    throw new RuntimeException("Qdrant 请求失败: url=" + url
                            + ", status=" + response.statusCode()
                            + ", body=" + response.body());
                }
                return root;
            }
            int code = root.path("code").asInt(response.statusCode());
            if (response.statusCode() >= 400 || code != 0) {
                throw new RuntimeException("Milvus 请求失败: url=" + url
                        + ", status=" + response.statusCode()
                        + ", body=" + response.body());
            }
            return root;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException((qdrant ? "Qdrant" : "Milvus") + " 请求被中断: " + url, e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException((qdrant ? "Qdrant" : "Milvus") + " 请求异常: " + url + ", " + e.getMessage(), e);
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

    private String trimTrailingSlash(String value, String defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
