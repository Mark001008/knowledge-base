package com.ma.agent.knowledge.rag;

import com.ma.agent.knowledge.chunk.DocumentChunker;
import com.ma.agent.knowledge.embedding.EmbeddingService;
import com.ma.agent.knowledge.vector.VectorStore;
import com.ma.agent.shared.LogMarkers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG（Retrieval-Augmented Generation）管道。
 *
 * 整合文档分块、向量化、存储和检索的完整流程。
 *
 * 离线索引流程：
 *   文档内容 → 分块(Chunker) → 向量化(Embedding) → 存储(VectorStore)
 *
 * 在线查询流程：
 *   用户问题 → 向量化(Embedding) → 语义检索(VectorStore) → 返回相关片段
 */
@Service
public class RagPipeline {

    private static final Logger log = LoggerFactory.getLogger(RagPipeline.class);

    /**
     * 检索时返回的最大结果数。
     */
    private static final int DEFAULT_TOP_K = 3;

    private final DocumentChunker chunker;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;

    RagPipeline(DocumentChunker chunker, EmbeddingService embeddingService, VectorStore vectorStore) {
        this.chunker = chunker;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    /**
     * 离线索引：将文档内容索引到向量库。
     *
     * 流程：
     * 1. 将文档内容切分为多个块
     * 2. 将每个块向量化
     * 3. 将向量和原始文本存入向量库
     *
     * @param documentId 文档 ID
     * @param filename   文件名
     * @param content    文档内容
     * @return 索引的块数量
     */
    public int indexDocument(String documentId, String filename, String content) {
        log.info(LogMarkers.BIZ, "RagPipeline.indexDocument docId={} filename={} contentLength={}",
                documentId, filename, content.length());

        // Step 1: 分块
        DocumentChunker.ChunkMetadata metadata = new DocumentChunker.ChunkMetadata(documentId, filename, 0);
        List<DocumentChunker.Chunk> chunks = chunker.chunk(content, metadata);
        log.info(LogMarkers.BIZ, "RagPipeline.indexDocument docId={} chunkCount={}", documentId, chunks.size());

        if (chunks.isEmpty()) {
            return 0;
        }

        // Step 2: 批量向量化
        List<String> texts = chunks.stream()
                .map(DocumentChunker.Chunk::content)
                .toList();
        List<float[]> vectors = embeddingService.embedBatch(texts);
        log.info(LogMarkers.BIZ, "RagPipeline.indexDocument docId={} vectorsGenerated={}", documentId, vectors.size());

        // Step 3: 存入向量库
        List<VectorStore.VectorEntry> entries = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunker.Chunk chunk = chunks.get(i);
            VectorStore.VectorMetadata vectorMeta = new VectorStore.VectorMetadata(
                    documentId, filename, chunk.index()
            );
            entries.add(new VectorStore.VectorEntry(
                    chunk.chunkId(), vectors.get(i), chunk.content(), vectorMeta
            ));
        }
        vectorStore.storeBatch(entries);
        log.info(LogMarkers.BIZ, "RagPipeline.indexDocument docId={} storedInVectorStore", documentId);

        return chunks.size();
    }

    /**
     * 在线查询：根据用户问题检索相关文档片段。
     *
     * 流程：
     * 1. 将用户问题向量化
     * 2. 在向量库中检索最相似的 Top-K 个块
     * 3. 返回检索结果
     *
     * @param question 用户问题
     * @return 检索结果列表（按相似度降序）
     */
    public List<RagResult> query(String question) {
        return query(question, DEFAULT_TOP_K);
    }

    /**
     * 在线查询：根据用户问题检索相关文档片段。
     *
     * @param question 用户问题
     * @param topK     返回的最大结果数
     * @return 检索结果列表（按相似度降序）
     */
    public List<RagResult> query(String question, int topK) {
        log.info(LogMarkers.BIZ, "RagPipeline.query question={} topK={}", question, topK);

        if (vectorStore.count() == 0) {
            log.info(LogMarkers.BIZ, "RagPipeline.query vectorStore is empty");
            return List.of();
        }

        // Step 1: 将问题向量化
        float[] queryVector = embeddingService.embed(question);

        // Step 2: 语义检索
        List<VectorStore.VectorSearchResult> searchResults = vectorStore.search(queryVector, topK);
        log.info(LogMarkers.BIZ, "RagPipeline.query matchedChunks={}", searchResults.size());

        // Step 3: 转换为 RAG 结果
        List<RagResult> results = new ArrayList<>();
        for (VectorStore.VectorSearchResult sr : searchResults) {
            results.add(new RagResult(
                    sr.id(),
                    sr.content(),
                    sr.score(),
                    sr.metadata().documentId(),
                    sr.metadata().filename()
            ));
        }

        return results;
    }

    /**
     * 生成带上下文的提示词。
     *
     * 将检索到的文档片段拼接成上下文，供 LLM 生成回答使用。
     *
     * @param question 用户问题
     * @param results  检索结果
     * @return 包含上下文的提示词
     */
    public String buildPromptWithContext(String question, List<RagResult> results) {
        if (results.isEmpty()) {
            return question;
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("请基于以下参考资料回答用户问题。");
        prompt.append("如果参考资料中没有相关信息，请说明无法从已有文档中找到答案。\n\n");
        prompt.append("【参考资料】\n");

        for (int i = 0; i < results.size(); i++) {
            RagResult result = results.get(i);
            prompt.append(String.format("\n--- 片段 %d (来源: %s, 相似度: %.2f) ---\n",
                    i + 1, result.filename(), result.score()));
            prompt.append(result.content());
            prompt.append("\n");
        }

        prompt.append("\n【用户问题】\n");
        prompt.append(question);

        return prompt.toString();
    }

    /**
     * RAG 检索结果。
     */
    public record RagResult(
            String chunkId,
            String content,
            float score,
            String documentId,
            String filename
    ) {
    }
}
