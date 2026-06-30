package com.ma.agent.knowledge.vector;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存向量存储：用于开发和测试。
 *
 * 使用 ConcurrentHashMap 存储所有向量，支持精确的 Top-K 检索。
 * 优点：实现简单，无外部依赖
 * 缺点：数据不持久化，大规模数据性能差
 *
 * 生产环境应替换为 FAISS 或分布式向量数据库。
 */
@Component
@ConditionalOnProperty(prefix = "agent.vector", name = "store", havingValue = "memory", matchIfMissing = true)
class InMemoryVectorStore implements VectorStore {

    /**
     * 向量存储：id → VectorEntry
     */
    private final Map<String, StoredVector> vectors = new ConcurrentHashMap<>();

    @Override
    public void store(String id, float[] vector, String content, VectorMetadata metadata) {
        vectors.put(id, new StoredVector(vector.clone(), content, metadata));
    }

    @Override
    public void storeBatch(List<VectorEntry> entries) {
        for (VectorEntry entry : entries) {
            store(entry.id(), entry.vector(), entry.content(), entry.metadata());
        }
    }

    @Override
    public List<VectorSearchResult> search(float[] queryVector, int topK) {
        if (queryVector == null || vectors.isEmpty()) {
            return List.of();
        }

        // 计算所有向量与查询向量的余弦相似度
        List<VectorSearchResult> results = vectors.entrySet().stream()
                .map(entry -> {
                    float score = cosineSimilarity(queryVector, entry.getValue().vector());
                    return new VectorSearchResult(
                            entry.getKey(),
                            entry.getValue().content(),
                            score,
                            entry.getValue().metadata()
                    );
                })
                .sorted(Comparator.comparingDouble(VectorSearchResult::score).reversed())  // 按相似度降序
                .limit(topK)  // 取 Top-K
                .collect(Collectors.toList());

        return results;
    }

    @Override
    public void deleteByDocumentId(String documentId) {
        vectors.entrySet().removeIf(entry ->
            documentId.equals(entry.getValue().metadata().documentId())
        );
    }

    @Override
    public int count() {
        return vectors.size();
    }

    /**
     * 余弦相似度计算。
     *
     * cos(a, b) = (a · b) / (|a| * |b|)
     *
     * 如果向量已归一化（|a| = 1），则 cos(a, b) = a · b（点积）
     *
     * @param a 向量 a
     * @param b 向量 b
     * @return 余弦相似度，范围 [-1, 1]，越接近 1 越相似
     */
    private float cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vector dimensions mismatch: " + a.length + " vs " + b.length);
        }

        double dotProduct = 0;
        double normA = 0;
        double normB = 0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        double norm = Math.sqrt(normA) * Math.sqrt(normB);
        if (norm == 0) {
            return 0;
        }

        return (float) (dotProduct / norm);
    }

    /**
     * 内部存储结构。
     */
    private record StoredVector(float[] vector, String content, VectorMetadata metadata) {
    }
}
