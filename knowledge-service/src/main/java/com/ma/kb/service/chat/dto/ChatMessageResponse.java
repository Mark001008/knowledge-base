package com.ma.kb.service.chat.dto;

import java.util.List;

/**
 * 问答消息响应
 */
public record ChatMessageResponse(
        Long messageId,
        String answer,
        List<CitationDTO> citations,
        RetrievalDiagnosticsDTO diagnostics
) {
}
