package com.ma.agent.knowledge.search;

import com.ma.agent.knowledge.dto.KnowledgeQueryRequest;
import com.ma.agent.knowledge.dto.KnowledgeQueryResponse;

public interface KnowledgeService {

    KnowledgeQueryResponse query(KnowledgeQueryRequest request);
}
