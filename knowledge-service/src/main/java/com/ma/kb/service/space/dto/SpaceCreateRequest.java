package com.ma.kb.service.space.dto;

import java.math.BigDecimal;

/**
 * 创建知识库请求
 */
public record SpaceCreateRequest(
        String name,
        String description,
        String visibility,
        Integer topK,
        BigDecimal similarityThreshold,
        BigDecimal temperature
) {
}
