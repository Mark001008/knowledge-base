package com.ma.agent.controller;

import com.ma.agent.entity.KnowledgeBaseEntity;
import com.ma.agent.knowledge.base.KnowledgeBaseCreateRequest;
import com.ma.agent.knowledge.base.KnowledgeBaseService;
import com.ma.agent.knowledge.base.KnowledgeBaseUpdateRequest;
import com.ma.agent.shared.LogMarkers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库管理 API
 */
@RestController
@RequestMapping("/api/knowledge-bases")
public class KnowledgeBaseController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseController.class);

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /**
     * 创建知识库
     */
    @PostMapping
    public KnowledgeBaseEntity create(@RequestBody KnowledgeBaseCreateRequest request) {
        log.info(LogMarkers.API, "创建知识库: name={}", request.name());
        return knowledgeBaseService.create(request.name(), request.description());
    }

    /**
     * 获取知识库列表
     */
    @GetMapping
    public List<KnowledgeBaseEntity> list() {
        log.info(LogMarkers.API, "获取知识库列表");
        return knowledgeBaseService.list();
    }

    /**
     * 获取知识库详情
     */
    @GetMapping("/{kbId}")
    public KnowledgeBaseEntity getById(@PathVariable String kbId) {
        log.info(LogMarkers.API, "获取知识库详情: kbId={}", kbId);
        KnowledgeBaseEntity entity = knowledgeBaseService.getById(kbId);
        if (entity == null) {
            throw new IllegalArgumentException("知识库不存在: " + kbId);
        }
        return entity;
    }

    /**
     * 更新知识库
     */
    @PutMapping("/{kbId}")
    public KnowledgeBaseEntity update(@PathVariable String kbId, @RequestBody KnowledgeBaseUpdateRequest request) {
        log.info(LogMarkers.API, "更新知识库: kbId={}", kbId);
        return knowledgeBaseService.update(kbId, request.name(), request.description());
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{kbId}")
    public void delete(@PathVariable String kbId) {
        log.info(LogMarkers.API, "删除知识库: kbId={}", kbId);
        knowledgeBaseService.delete(kbId);
    }
}
