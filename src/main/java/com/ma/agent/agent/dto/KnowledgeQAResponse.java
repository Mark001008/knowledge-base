package com.ma.agent.agent.dto;

import java.time.Instant;
import java.util.List;

/**
 * 知识库问答响应 DTO。
 *
 * @param conversationId 会话ID
 * @param answer         AI 生成的回答
 * @param sources        引用的知识来源
 * @param model          使用的模型名称
 * @param createdAt      响应时间
 */
public record KnowledgeQAResponse(
        String conversationId,
        String answer,
        List<KnowledgeSource> sources,
        String model,
        Instant createdAt
) {
    /**
     * 知识来源
     *
     * @param documentId 文档ID
     * @param filename   文件名
     * @param snippet    相关片段（截取）
     * @param score      相似度分数
     */
    public record KnowledgeSource(
            String documentId,
            String filename,
            String snippet,
            float score
    ) {
    }
}
