package com.ma.kb.service.space.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 知识库视图对象
 */
public record SpaceVO(
        Long id,
        String name,
        String description,
        Long ownerId,
        String ownerName,
        String visibility,
        Integer topK,
        BigDecimal similarityThreshold,
        BigDecimal temperature,
        Integer chunkSize,
        Integer chunkOverlap,
        Integer documentCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
