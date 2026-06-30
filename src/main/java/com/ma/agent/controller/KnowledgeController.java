package com.ma.agent.controller;

import com.ma.agent.knowledge.dto.KnowledgeQueryRequest;
import com.ma.agent.knowledge.dto.KnowledgeQueryResponse;
import com.ma.agent.knowledge.search.KnowledgeService;
import com.ma.agent.shared.LogMarkers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeController.class);

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @PostMapping("/query")
    public KnowledgeQueryResponse query(@RequestBody KnowledgeQueryRequest request) {
        log.info(LogMarkers.API, "POST /api/knowledge/query question={}", request.question());
        var response = knowledgeService.query(request);
        log.info(LogMarkers.API, "POST /api/knowledge/query sourcesCount={}", response.sources().size());
        return response;
    }
}
