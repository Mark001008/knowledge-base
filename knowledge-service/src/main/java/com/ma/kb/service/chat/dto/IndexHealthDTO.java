package com.ma.kb.service.chat.dto;

import java.time.LocalDateTime;

/**
 * 知识库索引健康状态。
 */
public record IndexHealthDTO(
        int totalDocuments,
        int completedDocuments,
        int processingDocuments,
        int failedDocuments,
        int chunkCount,
        boolean vectorEnabled,
        LocalDateTime lastIndexedAt
) {
}
