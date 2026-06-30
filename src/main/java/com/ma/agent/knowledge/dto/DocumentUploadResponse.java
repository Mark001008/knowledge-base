package com.ma.agent.knowledge.dto;

/**
 * 文档上传响应 DTO。
 *
 * @param documentId 文档ID
 * @param filename   文件名
 * @param status     上传状态（SUCCESS / FAILED）
 */
public record DocumentUploadResponse(
        String documentId,
        String filename,
        String status
) {
}
