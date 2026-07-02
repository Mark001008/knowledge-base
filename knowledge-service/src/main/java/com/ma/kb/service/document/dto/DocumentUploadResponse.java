package com.ma.kb.service.document.dto;

/**
 * 文档上传响应
 */
public record DocumentUploadResponse(
        Long documentId,
        String fileName,
        String status
) {
}
