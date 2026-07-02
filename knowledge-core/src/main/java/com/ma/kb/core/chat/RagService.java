package com.ma.kb.core.chat;

import com.ma.kb.integration.llm.ChatModelService;
import com.ma.kb.integration.vector.SearchResult;
import com.ma.kb.integration.vector.VectorSearchService;
import com.ma.kb.manager.space.SpaceManager;
import com.ma.kb.manager.space.bo.SpaceBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

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

    public RagService(VectorSearchService vectorSearchService, ChatModelService chatModelService,
                      PromptBuilder promptBuilder, SpaceManager spaceManager) {
        this.vectorSearchService = vectorSearchService;
        this.chatModelService = chatModelService;
        this.promptBuilder = promptBuilder;
        this.spaceManager = spaceManager;
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

        // 2. 生成问题向量
        float[] queryEmbedding = vectorSearchService.embed(question);

        // 3. 向量检索
        List<SearchResult> searchResults = vectorSearchService.search(
                queryEmbedding, spaceId, topK, threshold);

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
}
