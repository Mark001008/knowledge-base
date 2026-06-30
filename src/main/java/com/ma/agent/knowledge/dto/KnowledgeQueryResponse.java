package com.ma.agent.knowledge.dto;

import java.util.List;

/**
 * 知识库查询响应 DTO。
 *
 * @param answer  检索生成的答案
 * @param sources 引用的知识来源列表
 */
public record KnowledgeQueryResponse(
        String answer,
        List<KnowledgeSource> sources
) {
}
