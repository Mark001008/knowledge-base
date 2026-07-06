package com.ma.kb.service.chat.dto;

import java.math.BigDecimal;

/**
 * 引用来源 DTO
 */
public record CitationDTO(
        String id,
        Long documentId,
        String documentName,
        Long chunkId,
        Integer pageNumber,
        BigDecimal score,
        String quoteText
) {
}
