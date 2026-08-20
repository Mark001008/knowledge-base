package com.ma.kb.service.space.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 更新知识库请求
 */
public record SpaceUpdateRequest(
        @Size(max = 100, message = "知识库名称长度不能超过100") String name,
        @Size(max = 500, message = "描述长度不能超过500") String description,
        @Size(max = 20, message = "可见性值长度不能超过20") String visibility,
        Integer topK,
        BigDecimal similarityThreshold,
        BigDecimal temperature
) {
}
