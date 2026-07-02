package com.ma.kb.integration.vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 向量检索服务
 * MVP 阶段使用 Mock 实现，后续接入 Milvus
 */
@Service
public class VectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchService.class);

    /**
     * 检索相关文档片段
     *
     * @param queryEmbedding 问题向量
     * @param spaceId        知识库ID
     * @param topK           返回数量
     * @param threshold      相似度阈值
     * @return 检索结果列表
     */
    public List<SearchResult> search(float[] queryEmbedding, Long spaceId, int topK,
                                     BigDecimal threshold) {
        // MVP Mock: 返回空列表，后续接入 Milvus VectorStore
        log.info("向量检索: spaceId={}, topK={}, threshold={} (mock, 返回空)", spaceId, topK, threshold);
        return Collections.emptyList();
    }

    /**
     * 写入向量
     *
     * @param chunkId    分片ID
     * @param spaceId    知识库ID
     * @param documentId 文档ID
     * @param content    分片内容
     * @param embedding  向量
     * @param metadata   元数据
     * @return 向量记录ID
     */
    public String store(Long chunkId, Long spaceId, Long documentId, String content,
                        float[] embedding, java.util.Map<String, Object> metadata) {
        // MVP Mock: 返回假的 vectorId
        String vectorId = "vec_" + chunkId;
        log.info("向量写入: chunkId={}, vectorId={} (mock)", chunkId, vectorId);
        return vectorId;
    }

    /**
     * 按文档ID删除向量
     */
    public void deleteByDocumentId(Long documentId) {
        log.info("向量删除: documentId={} (mock)", documentId);
    }

    /**
     * 生成文本的 Embedding 向量
     * MVP Mock: 返回随机向量，后续接入 EmbeddingModel
     */
    public float[] embed(String text) {
        // MVP Mock: 返回固定维度的零向量
        log.info("Embedding 生成: textLength={} (mock)", text.length());
        return new float[1536];
    }
}
