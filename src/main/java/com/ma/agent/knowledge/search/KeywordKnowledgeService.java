package com.ma.agent.knowledge.search;

import com.ma.agent.knowledge.dto.KnowledgeQueryRequest;
import com.ma.agent.knowledge.dto.KnowledgeQueryResponse;
import com.ma.agent.knowledge.dto.KnowledgeSource;
import com.ma.agent.knowledge.store.InMemoryDocumentStore;
import com.ma.agent.shared.LogMarkers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 基于关键词的知识搜索服务。
 *
 * 通过 agent.knowledge.provider=keyword 激活。
 * 当前阶段用内存中的 contains 匹配，后续替换为向量检索。
 */
@Service
@ConditionalOnProperty(prefix = "agent.knowledge", name = "provider", havingValue = "keyword")
class KeywordKnowledgeService implements KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KeywordKnowledgeService.class);

    private final InMemoryDocumentStore documentStore;

    KeywordKnowledgeService(InMemoryDocumentStore documentStore) {
        this.documentStore = documentStore;
    }

    @Override
    public KnowledgeQueryResponse query(KnowledgeQueryRequest request) {
        String question = request.question();
        log.info(LogMarkers.BIZ, "KeywordKnowledgeService.query question={} docCount={}",
                question, documentStore.count());

        List<InMemoryDocumentStore.DocumentMatch> matches = documentStore.search(question);

        if (matches.isEmpty()) {
            return new KnowledgeQueryResponse(
                    "未找到相关文档。请先通过 /api/documents/upload 上传文本文档。",
                    List.of()
            );
        }

        List<KnowledgeSource> sources = matches.stream()
                .map(m -> new KnowledgeSource(m.documentId(), m.filename(), m.snippet()))
                .toList();

        // 将搜索到的片段拼接为一个简要回答
        StringBuilder answer = new StringBuilder();
        answer.append("在 ").append(matches.size()).append(" 篇文档中找到相关内容：\n\n");
        for (int i = 0; i < matches.size(); i++) {
            var m = matches.get(i);
            answer.append("【").append(i + 1).append("】").append(m.filename()).append("\n");
            answer.append(m.snippet()).append("\n\n");
        }

        log.info(LogMarkers.BIZ, "KeywordKnowledgeService.query matched={}", matches.size());
        return new KnowledgeQueryResponse(answer.toString(), sources);
    }
}
