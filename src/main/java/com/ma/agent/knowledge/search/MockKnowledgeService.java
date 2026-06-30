package com.ma.agent.knowledge.search;

import com.ma.agent.knowledge.dto.KnowledgeQueryRequest;
import com.ma.agent.knowledge.dto.KnowledgeQueryResponse;
import com.ma.agent.knowledge.dto.KnowledgeSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(prefix = "agent.knowledge", name = "provider", havingValue = "mock", matchIfMissing = true)
class MockKnowledgeService implements KnowledgeService {

    @Override
    public KnowledgeQueryResponse query(KnowledgeQueryRequest request) {
        KnowledgeSource source = new KnowledgeSource(
                "demo-doc",
                "Getting Started",
                "Knowledge retrieval is currently mocked. Connect parsing, embeddings, and vector search next."
        );
        return new KnowledgeQueryResponse(
                "Knowledge query scaffold is ready. Next step: add document parsing and vector retrieval.",
                List.of(source)
        );
    }
}
