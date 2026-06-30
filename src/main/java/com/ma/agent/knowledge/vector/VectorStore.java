package com.ma.agent.knowledge.vector;

import java.util.List;

/**
 * 向量存储接口。
 *
 * 存储文本块的向量，支持语义检索（找最相似的 Top-K 个块）。
 *
 * 实现类可以对接不同的向量存储：
 * - InMemoryVectorStore（本地内存，适合开发和小规模数据）
 * - FAISS（本地文件，适合中等规模数据）
 * - Milvus/Qdrant（分布式，适合生产环境大规模数据）
 */
public interface VectorStore {

    /**
     * 存储一个向量及其关联的文本块。
     *
     * @param id        块的唯一标识（如 documentId_chunk_0）
     * @param vector    块的向量表示
     * @param content   块的原始文本内容
     * @param metadata  块的元数据（文件名、文档ID等）
     */
    void store(String id, float[] vector, String content, VectorMetadata metadata);

    /**
     * 批量存储向量。
     *
     * @param entries 向量条目列表
     */
    void storeBatch(List<VectorEntry> entries);

    /**
     * 语义检索：找到与查询向量最相似的 Top-K 个块。
     *
     * @param queryVector 查询向量
     * @param topK        返回的最大结果数
     * @return 按相似度降序排列的检索结果
     */
    List<VectorSearchResult> search(float[] queryVector, int topK);

    /**
     * 删除指定文档的所有向量。
     *
     * @param documentId 文档 ID
     */
    void deleteByDocumentId(String documentId);

    /**
     * 获取存储的向量总数。
     */
    int count();

    /**
     * 向量条目：用于批量存储。
     */
    record VectorEntry(String id, float[] vector, String content, VectorMetadata metadata) {
    }

    /**
     * 向量元数据。
     */
    record VectorMetadata(String documentId, String filename, int chunkIndex) {
    }

    /**
     * 向量检索结果。
     */
    record VectorSearchResult(String id, String content, float score, VectorMetadata metadata) {
    }
}
