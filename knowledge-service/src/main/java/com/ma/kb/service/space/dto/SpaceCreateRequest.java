package com.ma.kb.service.space.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 创建知识库请求
 */
public record SpaceCreateRequest(
        @NotBlank(message = "知识库名称不能为空") @Size(max = 100, message = "知识库名称长度不能超过100") String name,
        @Size(max = 500, message = "描述长度不能超过500") String description,
        @Size(max = 20, message = "可见性值长度不能超过20") String visibility,
        Integer topK,
        BigDecimal similarityThreshold,
        BigDecimal temperature
) {
}
