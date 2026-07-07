package com.ma.kb.service.chat.dto;

import java.math.BigDecimal;

/**
 * 单次 RAG 检索诊断信息。
 */
public record RetrievalDiagnosticsDTO(
        int hitCount,
        BigDecimal bestScore,
        BigDecimal threshold,
        int topK,
        String retrievalMode,
        boolean keywordFallbackUsed,
        boolean enteredPrompt,
        boolean lowConfidence,
        String noAnswerReason,
        String explanation,
        IndexHealthDTO indexHealth
) {
}
