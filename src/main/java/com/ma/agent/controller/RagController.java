package com.ma.agent.controller;

import com.ma.agent.knowledge.rag.RagPipeline;
import com.ma.agent.shared.LogMarkers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RAG 管道 REST API。
 *
 * 提供文档索引和语义检索的 HTTP 接口。
 */
@RestController
@RequestMapping("/api/rag")
class RagController {

    private static final Logger log = LoggerFactory.getLogger(RagController.class);

    private final RagPipeline ragPipeline;

    RagController(RagPipeline ragPipeline) {
        this.ragPipeline = ragPipeline;
    }

    /**
     * 索引文档：将文本内容索引到向量库。
     *
     * POST /api/rag/index
     * {
     *   "documentId": "doc-001",
     *   "filename": "退货政策.txt",
     *   "content": "7天无理由退货..."
     * }
     */
    @PostMapping("/index")
    public ResponseEntity<Map<String, Object>> indexDocument(@RequestBody IndexRequest request) {
        log.info(LogMarkers.API, "POST /api/rag/index docId={} filename={}", request.documentId(), request.filename());

        int chunkCount = ragPipeline.indexDocument(request.documentId(), request.filename(), request.content());

        return ResponseEntity.ok(Map.of(
                "documentId", request.documentId(),
                "chunkCount", chunkCount,
                "status", "indexed"
        ));
    }

    /**
     * 语义检索：根据问题检索相关文档片段。
     *
     * POST /api/rag/query
     * {
     *   "question": "如何退货？",
     *   "topK": 3
     * }
     */
    @PostMapping("/query")
    public ResponseEntity<QueryResponse> query(@RequestBody QueryRequest request) {
        log.info(LogMarkers.API, "POST /api/rag/query question={}", request.question());

        int topK = request.topK() != null ? request.topK() : 3;
        List<RagPipeline.RagResult> results = ragPipeline.query(request.question(), topK);

        List<QueryResponse.ResultItem> items = results.stream()
                .map(r -> new QueryResponse.ResultItem(
                        r.chunkId(), r.content(), r.score(), r.documentId(), r.filename()
                ))
                .toList();

        return ResponseEntity.ok(new QueryResponse(request.question(), items));
    }

    /**
     * 带上下文的查询：检索并生成带上下文的提示词。
     *
     * POST /api/rag/query-with-context
     * {
     *   "question": "如何退货？",
     *   "topK": 3
     * }
     */
    @PostMapping("/query-with-context")
    public ResponseEntity<Map<String, Object>> queryWithContext(@RequestBody QueryRequest request) {
        log.info(LogMarkers.API, "POST /api/rag/query-with-context question={}", request.question());

        int topK = request.topK() != null ? request.topK() : 3;
        List<RagPipeline.RagResult> results = ragPipeline.query(request.question(), topK);
        String promptWithContext = ragPipeline.buildPromptWithContext(request.question(), results);

        return ResponseEntity.ok(Map.of(
                "question", request.question(),
                "contextChunks", results.size(),
                "prompt", promptWithContext
        ));
    }

    /**
     * 获取向量库统计信息。
     *
     * GET /api/rag/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        log.info(LogMarkers.API, "GET /api/rag/stats");
        return ResponseEntity.ok(Map.of("vectorCount", ragPipeline.query("*", 0).size()));
    }

    // ========== 请求/响应 DTO ==========

    record IndexRequest(String documentId, String filename, String content) {
    }

    record QueryRequest(String question, Integer topK) {
    }

    record QueryResponse(String question, List<ResultItem> results) {
        record ResultItem(String chunkId, String content, float score, String documentId, String filename) {
        }
    }
}
