package com.ma.agent.agent;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ma.agent.agent.dto.KnowledgeQAResponse;
import com.ma.agent.cache.RedisCacheService;
import com.ma.agent.entity.ConversationEntity;
import com.ma.agent.knowledge.rag.RagPipeline;
import com.ma.agent.mapper.ConversationMapper;
import com.ma.agent.model.ModelChatRequest;
import com.ma.agent.model.ModelGateway;
import com.ma.agent.model.ModelStreamChunk;
import com.ma.agent.shared.LogMarkers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 知识库问答服务实现
 * <p>基于 RAG 检索增强生成：用户问题 → 知识库检索 → 构建增强 Prompt → LLM 生成答案</p>
 */
@Service
public class KnowledgeQAServiceImpl implements KnowledgeQAService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeQAServiceImpl.class);

    private static final String CONVERSATION_CACHE_KEY = "conversation:";
    private static final long CONVERSATION_CACHE_TTL = 30; // 30 分钟
    private static final int TOP_K = 3; // 检索返回的最大结果数

    private final ModelGateway modelGateway;
    private final ConversationMapper conversationMapper;
    private final RedisCacheService redisCacheService;
    private final RagPipeline ragPipeline;
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

    KnowledgeQAServiceImpl(ModelGateway modelGateway, ConversationMapper conversationMapper,
                           RedisCacheService redisCacheService, RagPipeline ragPipeline) {
        this.modelGateway = modelGateway;
        this.conversationMapper = conversationMapper;
        this.redisCacheService = redisCacheService;
        this.ragPipeline = ragPipeline;
    }

    @Override
    public KnowledgeQAResponse ask(String question, String kbId, String conversationId) {
        conversationId = resolveConversationId(conversationId);
        log.info(LogMarkers.BIZ, "KnowledgeQA.ask conv={} kbId={} questionLength={}",
                conversationId, kbId, question.length());

        // 保存用户消息
        saveMessage(conversationId, kbId, "user", question, null);

        // RAG 检索相关文档片段
        List<RagPipeline.RagResult> ragResults = ragPipeline.query(question, TOP_K);
        log.info(LogMarkers.BIZ, "KnowledgeQA.ask conv={} retrievedChunks={}", conversationId, ragResults.size());

        // 构建增强 Prompt
        String enhancedPrompt = ragPipeline.buildPromptWithContext(question, ragResults);

        // 调用 LLM
        var modelResponse = modelGateway.chat(new ModelChatRequest(conversationId, enhancedPrompt));

        // 保存助手回复
        saveMessage(conversationId, kbId, "assistant", modelResponse.message(), modelResponse.model());

        // 构建引用来源
        List<KnowledgeQAResponse.KnowledgeSource> sources = ragResults.stream()
                .map(r -> new KnowledgeQAResponse.KnowledgeSource(
                        r.documentId(),
                        r.filename(),
                        truncateSnippet(r.content(), 200),
                        r.score()
                ))
                .toList();

        log.info(LogMarkers.BIZ, "KnowledgeQA.ask conv={} model={} sources={}",
                conversationId, modelResponse.model(), sources.size());

        return new KnowledgeQAResponse(
                conversationId,
                modelResponse.message(),
                sources,
                modelResponse.model(),
                Instant.now()
        );
    }

    @Override
    public SseEmitter askStream(String question, String kbId, String conversationId) {
        final String resolvedConversationId = resolveConversationId(conversationId);
        SseEmitter emitter = new SseEmitter(60_000L);

        // 保存用户消息
        saveMessage(resolvedConversationId, kbId, "user", question, null);

        // RAG 检索
        List<RagPipeline.RagResult> ragResults = ragPipeline.query(question, TOP_K);
        String enhancedPrompt = ragPipeline.buildPromptWithContext(question, ragResults);

        // 构建引用来源
        List<KnowledgeQAResponse.KnowledgeSource> sources = ragResults.stream()
                .map(r -> new KnowledgeQAResponse.KnowledgeSource(
                        r.documentId(),
                        r.filename(),
                        truncateSnippet(r.content(), 200),
                        r.score()
                ))
                .toList();

        streamExecutor.execute(() -> {
            StringBuilder fullResponse = new StringBuilder();
            try {
                // 先发送来源信息
                emitter.send(SseEmitter.event()
                        .name("sources")
                        .data(sources));

                // 流式调用 LLM
                modelGateway.stream(
                        new ModelChatRequest(resolvedConversationId, enhancedPrompt),
                        chunk -> {
                            sendChunk(emitter, resolvedConversationId, chunk);
                            fullResponse.append(chunk.delta());
                        }
                );

                // 保存助手回复
                saveMessage(resolvedConversationId, kbId, "assistant", fullResponse.toString(), null);

                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(new KnowledgeQAResponse(resolvedConversationId, fullResponse.toString(), sources, null, Instant.now())));
                emitter.complete();
            } catch (Exception ex) {
                log.error(LogMarkers.BIZ, "KnowledgeQA.askStream error: conv={}", resolvedConversationId, ex);
                emitter.completeWithError(ex);
            }
        });

        return emitter;
    }

    @Override
    public List<ConversationEntity> getConversationHistory(String conversationId) {
        String cacheKey = CONVERSATION_CACHE_KEY + conversationId;

        // 先尝试从 Redis 缓存获取
        List<ConversationEntity> cachedList = redisCacheService.getList(cacheKey, ConversationEntity.class);
        if (cachedList != null && !cachedList.isEmpty()) {
            log.debug("Conversation history loaded from Redis cache: conv={}", conversationId);
            return cachedList;
        }

        // 缓存未命中，从 MySQL 查询
        QueryWrapper<ConversationEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", conversationId).orderByAsc("created_at");
        List<ConversationEntity> dbList = conversationMapper.selectList(queryWrapper);

        // 写入 Redis 缓存
        if (!dbList.isEmpty()) {
            redisCacheService.setList(cacheKey, dbList, CONVERSATION_CACHE_TTL, TimeUnit.MINUTES);
        }

        return dbList;
    }

    private void sendChunk(SseEmitter emitter, String conversationId, ModelStreamChunk chunk) {
        try {
            emitter.send(SseEmitter.event()
                    .name("chunk")
                    .data(new com.ma.agent.agent.dto.ChatStreamChunk(conversationId, chunk.delta(), false)));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private String resolveConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return conversationId;
    }

    private void saveMessage(String conversationId, String kbId, String role, String content, String model) {
        try {
            ConversationEntity entity = new ConversationEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setConversationId(conversationId);
            entity.setKbId(kbId != null ? kbId : "default");
            entity.setRole(role);
            entity.setContent(content);
            entity.setModel(model);
            entity.setCreatedAt(LocalDateTime.now());

            // 保存到 MySQL
            conversationMapper.insert(entity);

            // 更新 Redis 缓存
            updateConversationCache(conversationId, entity);
        } catch (Exception e) {
            log.error("Failed to save conversation message", e);
        }
    }

    private void updateConversationCache(String conversationId, ConversationEntity newMessage) {
        String cacheKey = CONVERSATION_CACHE_KEY + conversationId;
        try {
            List<ConversationEntity> cachedList = redisCacheService.getList(cacheKey, ConversationEntity.class);
            if (cachedList == null) {
                cachedList = new ArrayList<>();
            }
            cachedList.add(newMessage);
            redisCacheService.setList(cacheKey, cachedList, CONVERSATION_CACHE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to update conversation cache", e);
        }
    }

    private String truncateSnippet(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
}
