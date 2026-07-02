package com.ma.kb.service.space.dto;

import java.math.BigDecimal;

/**
 * 更新知识库请求
 */
public record SpaceUpdateRequest(
        String name,
        String description,
        String visibility,
        Integer topK,
        BigDecimal similarityThreshold,
        BigDecimal temperature
) {
}
