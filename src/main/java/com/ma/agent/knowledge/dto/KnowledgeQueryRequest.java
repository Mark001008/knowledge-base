package com.ma.agent.knowledge.dto;

/**
 * 知识库查询请求 DTO。
 *
 * @param question 用户查询问题
 */
public record KnowledgeQueryRequest(
        String question
) {
}
