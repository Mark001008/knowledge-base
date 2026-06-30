package com.ma.agent.knowledge.embedding;

import java.util.List;

/**
 * 向量化服务接口。
 *
 * 将文本转换为高维向量（Embedding），用于语义检索。
 * 语义相似的文本会被转换为距离相近的向量。
 *
 * 实现类可以对接不同的 Embedding 模型：
 * - OpenAI text-embedding-3-small（1536 维）
 * - BGE-M3（1024 维，中文效果好）
 * - M3E（768 维，轻量级）
 */
public interface EmbeddingService {

    /**
     * 将单个文本转换为向量。
     *
     * @param text 待向量化的文本
     * @return 向量（浮点数数组）
     */
    float[] embed(String text);

    /**
     * 批量将文本转换为向量。
     * 批量调用通常比逐条调用更高效（减少网络往返）。
     *
     * @param texts 待向量化的文本列表
     * @return 向量列表，与输入文本一一对应
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * 获取向量维度。
     * 不同模型的维度不同，如 OpenAI 为 1536，BGE-M3 为 1024。
     *
     * @return 向量维度
     */
    int dimensions();
}
