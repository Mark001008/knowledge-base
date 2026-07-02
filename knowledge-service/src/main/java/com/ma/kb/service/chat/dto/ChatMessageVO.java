package com.ma.kb.service.chat.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息视图对象
 */
public record ChatMessageVO(
        Long id,
        String role,
        String content,
        String modelName,
        List<CitationDTO> citations,
        LocalDateTime createdAt
) {
}
