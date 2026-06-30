package com.ma.agent.knowledge.base;

/**
 * 创建知识库请求
 */
public record KnowledgeBaseCreateRequest(
        String name,
        String description
) {
}
