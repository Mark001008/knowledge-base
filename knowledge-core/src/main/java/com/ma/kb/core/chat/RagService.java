package com.ma.kb.core.chat;

import com.ma.kb.integration.llm.ChatModelService;
import com.ma.kb.integration.vector.SearchResult;
import com.ma.kb.integration.vector.VectorSearchService;
import com.ma.kb.manager.document.DocumentManager;
import com.ma.kb.manager.document.bo.DocumentBO;
import com.ma.kb.manager.document.bo.DocumentChunkBO;
import com.ma.kb.manager.space.SpaceManager;
import com.ma.kb.manager.space.bo.SpaceBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RAG 问答编排服务
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final VectorSearchService vectorSearchService;
    private final ChatModelService chatModelService;
    private final PromptBuilder promptBuilder;
    private final SpaceManager spaceManager;
    private final DocumentManager documentManager;

    public RagService(VectorSearchService vectorSearchService, ChatModelService chatModelService,
                      PromptBuilder promptBuilder, SpaceManager spaceManager, DocumentManager documentManager) {
        this.vectorSearchService = vectorSearchService;
        this.chatModelService = chatModelService;
        this.promptBuilder = promptBuilder;
        this.spaceManager = spaceManager;
        this.documentManager = documentManager;
    }

    /**
     * 执行 RAG 问答
     *
     * @param question 用户问题
     * @param spaceId  知识库ID
     * @return RAG 回答结果
     */
    public RagResult ask(String question, Long spaceId) {
        long startTime = System.currentTimeMillis();
        RetrievalContext retrievalContext = retrieve(question, spaceId);

        return answerWithContext(question, spaceId, retrievalContext, startTime, null);
    }

    /**
     * 执行流式 RAG 问答。
     */
    public RagResult askStream(String question, Long spaceId, Consumer<String> deltaConsumer) {
        long startTime = System.currentTimeMillis();
        RetrievalContext retrievalContext = retrieve(question, spaceId);

        return answerWithContext(question, spaceId, retrievalContext, startTime, deltaConsumer);
    }

    private RetrievalContext retrieve(String question, Long spaceId) {
        SpaceBO space = spaceManager.getById(spaceId);
        int topK = space != null ? space.getTopK() : 5;
        BigDecimal threshold = space != null ? space.getSimilarityThreshold()
                : new BigDecimal("0.7");

        List<SearchResult> vectorResults = List.of();
        if (vectorSearchService.isEnabled()) {
            // 2. 生成问题向量
            float[] queryEmbedding = vectorSearchService.embed(question);

            // 3. 向量检索
            vectorResults = vectorSearchService.search(
                    queryEmbedding, spaceId, topK, threshold);
        } else {
            log.info("RAG 问答: spaceId={}, 向量库未启用, 尝试关键词兜底检索", spaceId);
        }

        List<SearchResult> keywordResults = keywordFallbackSearch(question, spaceId, topK);
        List<SearchResult> searchResults = mergeResults(vectorResults, keywordResults, question, topK);
        boolean keywordFallbackUsed = vectorResults.isEmpty() && !keywordResults.isEmpty();
        String retrievalMode = resolveRetrievalMode(vectorResults, keywordResults);
        IndexHealth indexHealth = buildIndexHealth(spaceId);

        return new RetrievalContext(searchResults, topK, threshold, retrievalMode, keywordFallbackUsed, indexHealth);
    }

    private RagResult answerWithContext(String question, Long spaceId, RetrievalContext retrievalContext,
                                        long startTime, Consumer<String> deltaConsumer) {
        List<SearchResult> searchResults = retrievalContext.searchResults();
        // 4. 构造 Prompt 并调用 LLM
        String answer;
        String modelName;
        int promptTokens;
        int completionTokens;

        if (searchResults.isEmpty()) {
            // 无相关上下文，直接返回固定回答
            answer = promptBuilder.buildNoContextAnswer();
            modelName = "none";
            promptTokens = 0;
            completionTokens = 0;
            if (deltaConsumer != null) {
                deltaConsumer.accept(answer);
            }
            log.info("RAG 问答: spaceId={}, 无相关上下文, 返回固定回答", spaceId);
        } else {
            // 有上下文，调用 LLM
            String systemPrompt = promptBuilder.buildSystemPrompt();
            String userMessage = promptBuilder.buildUserMessageWith(question, searchResults);

            ChatModelService.ChatResponse chatResponse = deltaConsumer == null
                    ? chatModelService.chat(systemPrompt, userMessage)
                    : chatModelService.chatStream(systemPrompt, userMessage, deltaConsumer);
            answer = chatResponse.answer();
            modelName = chatResponse.modelName();
            promptTokens = chatResponse.promptTokens();
            completionTokens = chatResponse.completionTokens();

            log.info("RAG 问答: spaceId={}, 检索到{}条结果, 模型={}, 耗时={}ms",
                    spaceId, searchResults.size(), modelName,
                    System.currentTimeMillis() - startTime);
        }

        long latencyMs = System.currentTimeMillis() - startTime;
        RetrievalDiagnostics diagnostics = buildDiagnostics(
                searchResults, retrievalContext.indexHealth(), retrievalContext.topK(), retrievalContext.threshold(),
                retrievalContext.retrievalMode(), retrievalContext.keywordFallbackUsed(), !searchResults.isEmpty());

        return new RagResult(answer, searchResults, modelName, promptTokens,
                completionTokens, latencyMs, diagnostics);
    }

    private record RetrievalContext(
            List<SearchResult> searchResults,
            int topK,
            BigDecimal threshold,
            String retrievalMode,
            boolean keywordFallbackUsed,
            IndexHealth indexHealth
    ) {
    }

    /**
     * RAG 问答结果
     */
    public record RagResult(
            String answer,
            List<SearchResult> citations,
            String modelName,
            int promptTokens,
            int completionTokens,
            long latencyMs,
            RetrievalDiagnostics diagnostics
    ) {
    }

    public record RetrievalDiagnostics(
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
            IndexHealth indexHealth
    ) {
    }

    public record IndexHealth(
            int totalDocuments,
            int completedDocuments,
            int processingDocuments,
            int failedDocuments,
            int chunkCount,
            boolean vectorEnabled,
            LocalDateTime lastIndexedAt
    ) {
    }

    private List<SearchResult> keywordFallbackSearch(String question, Long spaceId, int topK) {
        List<String> keywords = extractKeywords(question);
        if (keywords.isEmpty()) {
            return List.of();
        }
        List<DocumentChunkBO> chunks = documentManager.searchChunksByKeywords(spaceId, keywords, Math.max(topK * 4, topK));
        if (chunks.isEmpty()) {
            log.info("RAG 关键词兜底无结果: spaceId={}, keywords={}", spaceId, keywords);
            return List.of();
        }

        List<SearchResult> results = chunks.stream()
                .map(chunk -> toSearchResult(chunk, keywords))
                .sorted(Comparator.comparing(SearchResult::getScore).reversed())
                .limit(topK)
                .toList();
        log.info("RAG 关键词兜底完成: spaceId={}, keywords={}, results={}", spaceId, keywords, results.size());
        return results;
    }

    private List<SearchResult> mergeResults(List<SearchResult> vectorResults, List<SearchResult> keywordResults,
                                            String question, int topK) {
        Map<Long, SearchResult> merged = new HashMap<>();
        Map<Long, BigDecimal> scoreBoosts = new HashMap<>();
        for (SearchResult result : vectorResults) {
            if (result.getChunkId() == null) {
                continue;
            }
            merged.put(result.getChunkId(), result);
            scoreBoosts.put(result.getChunkId(), BigDecimal.ZERO);
        }
        for (SearchResult result : keywordResults) {
            if (result.getChunkId() == null) {
                continue;
            }
            SearchResult existing = merged.get(result.getChunkId());
            if (existing == null) {
                merged.put(result.getChunkId(), result);
                scoreBoosts.put(result.getChunkId(), new BigDecimal("0.08"));
            } else {
                scoreBoosts.put(result.getChunkId(), new BigDecimal("0.12"));
            }
        }

        return merged.values().stream()
                .sorted(Comparator.comparing((SearchResult result) -> fusedScore(result, question, scoreBoosts))
                        .reversed())
                .limit(topK)
                .toList();
    }

    private BigDecimal fusedScore(SearchResult result, String question, Map<Long, BigDecimal> scoreBoosts) {
        BigDecimal baseScore = result.getScore() == null ? BigDecimal.ZERO : result.getScore();
        BigDecimal titleBoost = titleMatchesQuestion(result.getDocumentName(), question)
                ? new BigDecimal("0.04") : BigDecimal.ZERO;
        BigDecimal keywordBoost = scoreBoosts.getOrDefault(result.getChunkId(), BigDecimal.ZERO);
        return baseScore.add(titleBoost).add(keywordBoost);
    }

    private boolean titleMatchesQuestion(String title, String question) {
        if (title == null || question == null) {
            return false;
        }
        String normalizedTitle = title.toLowerCase(Locale.ROOT);
        return extractKeywords(question).stream()
                .anyMatch(keyword -> normalizedTitle.contains(keyword.toLowerCase(Locale.ROOT)));
    }

    private String resolveRetrievalMode(List<SearchResult> vectorResults, List<SearchResult> keywordResults) {
        if (!vectorResults.isEmpty() && !keywordResults.isEmpty()) {
            return "HYBRID";
        }
        if (!vectorResults.isEmpty()) {
            return "VECTOR";
        }
        if (!keywordResults.isEmpty()) {
            return "KEYWORD";
        }
        return vectorSearchService.isEnabled() ? "VECTOR_EMPTY" : "KEYWORD_EMPTY";
    }

    private IndexHealth buildIndexHealth(Long spaceId) {
        List<DocumentBO> documents = documentManager.listBySpaceId(spaceId);
        if (documents == null) {
            documents = List.of();
        }
        int completed = 0;
        int processing = 0;
        int failed = 0;
        LocalDateTime lastIndexedAt = null;
        for (DocumentBO document : documents) {
            String status = document.getParseStatus();
            if ("COMPLETED".equals(status)) {
                completed++;
                if (document.getUpdatedAt() != null &&
                        (lastIndexedAt == null || document.getUpdatedAt().isAfter(lastIndexedAt))) {
                    lastIndexedAt = document.getUpdatedAt();
                }
            } else if ("FAILED".equals(status)) {
                failed++;
            } else {
                processing++;
            }
        }
        return new IndexHealth(
                documents.size(),
                completed,
                processing,
                failed,
                documentManager.countChunksBySpaceId(spaceId),
                vectorSearchService.isEnabled(),
                lastIndexedAt
        );
    }

    private RetrievalDiagnostics buildDiagnostics(List<SearchResult> searchResults, IndexHealth indexHealth,
                                                  int topK, BigDecimal threshold, String retrievalMode,
                                                  boolean keywordFallbackUsed, boolean enteredPrompt) {
        BigDecimal bestScore = searchResults.stream()
                .map(SearchResult::getScore)
                .filter(score -> score != null)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        boolean lowConfidence = enteredPrompt && threshold != null && bestScore.compareTo(threshold) < 0;
        String noAnswerReason = resolveNoAnswerReason(searchResults, indexHealth, lowConfidence);
        String explanation = buildExplanation(searchResults, indexHealth, lowConfidence, retrievalMode, bestScore, threshold);
        return new RetrievalDiagnostics(
                searchResults.size(),
                bestScore,
                threshold,
                topK,
                retrievalMode,
                keywordFallbackUsed,
                enteredPrompt,
                lowConfidence,
                noAnswerReason,
                explanation,
                indexHealth
        );
    }

    private String resolveNoAnswerReason(List<SearchResult> searchResults, IndexHealth indexHealth,
                                         boolean lowConfidence) {
        if (!searchResults.isEmpty() && lowConfidence) {
            return "LOW_CONFIDENCE";
        }
        if (!searchResults.isEmpty()) {
            return "";
        }
        if (indexHealth.totalDocuments() == 0) {
            return "NO_DOCUMENTS";
        }
        if (indexHealth.completedDocuments() == 0 && indexHealth.processingDocuments() > 0) {
            return "INDEXING";
        }
        if (indexHealth.completedDocuments() == 0 && indexHealth.failedDocuments() > 0) {
            return "INDEX_FAILED";
        }
        if (indexHealth.processingDocuments() > 0) {
            return "PARTIAL_INDEX";
        }
        return "NO_MATCH";
    }

    private String buildExplanation(List<SearchResult> searchResults, IndexHealth indexHealth, boolean lowConfidence,
                                    String retrievalMode, BigDecimal bestScore, BigDecimal threshold) {
        if (searchResults.isEmpty()) {
            return switch (resolveNoAnswerReason(searchResults, indexHealth, false)) {
                case "NO_DOCUMENTS" -> "当前知识库还没有可检索文档。";
                case "INDEXING" -> "文档仍在解析或索引中，本次没有可参与检索的内容。";
                case "INDEX_FAILED" -> "当前文档索引失败，需在文档页处理失败原因或重建索引。";
                case "PARTIAL_INDEX" -> "部分文档仍在索引中，本次只检索了已完成索引的内容但没有命中。";
                default -> "已检索完成索引的文档，但没有找到与问题足够相关的片段。";
            };
        }
        if (lowConfidence) {
            return "已找到参考片段，但最高命中分低于当前阈值，请结合引用原文核验。";
        }
        if (indexHealth.processingDocuments() > 0) {
            return "已基于完成索引的文档回答；仍有文档处理中，结果可能不完整。";
        }
        return "已通过 " + retrievalMode + " 检索命中 " + searchResults.size()
                + " 个片段，最高分 " + bestScore + "，当前阈值 " + threshold + "。";
    }

    private SearchResult toSearchResult(DocumentChunkBO chunk, List<String> keywords) {
        SearchResult result = new SearchResult();
        result.setChunkId(chunk.getId());
        result.setDocumentId(chunk.getDocumentId());
        DocumentBO document = documentManager.getById(chunk.getDocumentId());
        result.setDocumentName(document == null ? null : document.getFileName());
        result.setPageNumber(chunk.getPageNumber());
        result.setChunkIndex(chunk.getChunkIndex());
        result.setContent(chunk.getContent());
        result.setScore(keywordScore(chunk.getContent(), keywords));
        return result;
    }

    private BigDecimal keywordScore(String content, List<String> keywords) {
        if (content == null || content.isBlank() || keywords.isEmpty()) {
            return BigDecimal.ZERO;
        }
        String normalized = content.toLowerCase(Locale.ROOT);
        int matched = 0;
        for (String keyword : keywords) {
            if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                matched++;
            }
        }
        return BigDecimal.valueOf((double) matched / keywords.size())
                .setScale(6, RoundingMode.HALF_UP);
    }

    private List<String> extractKeywords(String question) {
        if (question == null || question.isBlank()) {
            return List.of();
        }
        Set<String> keywords = new LinkedHashSet<>();
        String normalized = question.trim().toLowerCase(Locale.ROOT);

        Matcher alnumMatcher = Pattern.compile("[a-z0-9_\\-]{2,}").matcher(normalized);
        while (alnumMatcher.find()) {
            keywords.add(alnumMatcher.group());
        }

        Matcher hanMatcher = Pattern.compile("[\\p{IsHan}]{2,}").matcher(normalized);
        while (hanMatcher.find()) {
            String text = hanMatcher.group();
            for (int i = 0; i < text.length() - 1; i++) {
                String token = text.substring(i, i + 2);
                if (!isStopToken(token)) {
                    keywords.add(token);
                }
            }
        }

        return new ArrayList<>(keywords).stream().limit(12).toList();
    }

    private boolean isStopToken(String token) {
        return List.of("多少", "什么", "怎么", "如何", "是否", "时候", "这个", "那个", "当前", "一下")
                .contains(token);
    }
}
