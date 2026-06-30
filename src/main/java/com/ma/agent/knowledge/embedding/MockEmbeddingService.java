package com.ma.agent.knowledge.embedding;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mock Embedding 服务：用于开发和测试。
 *
 * 通过简单的哈希算法生成伪向量，模拟真实 Embedding 的行为：
 * - 相同文本产生相同向量（可复现）
 * - 相似文本产生相似向量（基于字符重叠）
 *
 * 生产环境应替换为真实的 Embedding 模型 API。
 */
@Component
@ConditionalOnProperty(prefix = "agent.embedding", name = "provider", havingValue = "mock", matchIfMissing = true)
class MockEmbeddingService implements EmbeddingService {

    private static final int DIMENSION = 128;  // Mock 使用较小维度

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[DIMENSION];
        }

        // 使用文本的哈希值作为随机种子，确保相同文本产生相同向量
        Random random = new Random(hashText(text));
        float[] vector = new float[DIMENSION];

        for (int i = 0; i < DIMENSION; i++) {
            vector[i] = random.nextFloat() * 2 - 1;  // 范围 [-1, 1]
        }

        // 归一化：使向量长度为 1（余弦相似度计算需要）
        normalize(vector);

        return vector;
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>(texts.size());
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }

    @Override
    public int dimensions() {
        return DIMENSION;
    }

    /**
     * 文本哈希：将文本转换为一个长整型种子。
     * 使用 FNV-1a 算法，碰撞率低。
     */
    private long hashText(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        long hash = 0xcbf29ce484222325L;  // FNV offset basis
        for (byte b : bytes) {
            hash ^= b;
            hash *= 0x100000001b3L;  // FNV prime
        }
        return hash;
    }

    /**
     * 向量归一化：使向量长度为 1。
     * 归一化后，余弦相似度等价于向量点积，计算更快。
     */
    private void normalize(float[] vector) {
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
    }
}
