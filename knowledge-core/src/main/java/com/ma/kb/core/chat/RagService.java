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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

        // 1. 获取知识库配置
        SpaceBO space = spaceManager.getById(spaceId);
        int topK = space != null ? space.getTopK() : 5;
        BigDecimal threshold = space != null ? space.getSimilarityThreshold()
                : new BigDecimal("0.7");

        List<SearchResult> searchResults;
        if (vectorSearchService.isEnabled()) {
            // 2. 生成问题向量
            float[] queryEmbedding = vectorSearchService.embed(question);

            // 3. 向量检索
            searchResults = vectorSearchService.search(
                    queryEmbedding, spaceId, topK, threshold);
        } else {
            searchResults = List.of();
            log.info("RAG 问答: spaceId={}, 向量库未启用, 尝试关键词兜底检索", spaceId);
        }

        if (searchResults.isEmpty()) {
            searchResults = keywordFallbackSearch(question, spaceId, topK);
        }

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
            log.info("RAG 问答: spaceId={}, 无相关上下文, 返回固定回答", spaceId);
        } else {
            // 有上下文，调用 LLM
            String systemPrompt = promptBuilder.buildSystemPrompt();
            String userMessage = promptBuilder.buildUserMessageWith(question, searchResults);

            ChatModelService.ChatResponse chatResponse = chatModelService.chat(systemPrompt, userMessage);
            answer = chatResponse.answer();
            modelName = chatResponse.modelName();
            promptTokens = chatResponse.promptTokens();
            completionTokens = chatResponse.completionTokens();

            log.info("RAG 问答: spaceId={}, 检索到{}条结果, 模型={}, 耗时={}ms",
                    spaceId, searchResults.size(), modelName,
                    System.currentTimeMillis() - startTime);
        }

        long latencyMs = System.currentTimeMillis() - startTime;

        return new RagResult(answer, searchResults, modelName, promptTokens,
                completionTokens, latencyMs);
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
            long latencyMs
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
