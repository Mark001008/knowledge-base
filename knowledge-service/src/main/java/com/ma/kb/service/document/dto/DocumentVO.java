package com.ma.kb.service.document.dto;

import java.time.LocalDateTime;

/**
 * 文档视图对象
 */
public record DocumentVO(
        Long id,
        Long spaceId,
        String fileName,
        String fileType,
        Long fileSize,
        String parseStatus,
        String errorMessage,
        Long uploadedBy,
        String uploadedByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
