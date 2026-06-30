package com.ma.agent.knowledge.dto;

/**
 * 知识检索来源引用 DTO。
 *
 * @param documentId 来源文档ID
 * @param title      文档标题
 * @param snippet    相关片段摘要
 */
public record KnowledgeSource(
        String documentId,
        String title,
        String snippet
) {
}
