package com.ma.agent.knowledge.base;

/**
 * 更新知识库请求
 */
public record KnowledgeBaseUpdateRequest(
        String name,
        String description
) {
}
