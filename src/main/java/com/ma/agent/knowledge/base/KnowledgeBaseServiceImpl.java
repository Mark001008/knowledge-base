package com.ma.agent.knowledge.base;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ma.agent.entity.KnowledgeBaseEntity;
import com.ma.agent.mapper.KnowledgeBaseMapper;
import com.ma.agent.shared.LogMarkers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库管理服务实现
 */
@Service
@ConditionalOnProperty(prefix = "agent.knowledge-base", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseServiceImpl.class);

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    public KnowledgeBaseServiceImpl(KnowledgeBaseMapper knowledgeBaseMapper) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
    }

    @Override
    public KnowledgeBaseEntity create(String name, String description) {
        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setName(name);
        entity.setDescription(description);
        entity.setDocumentCount(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        knowledgeBaseMapper.insert(entity);
        log.info(LogMarkers.BIZ, "知识库创建成功: kbId={}, name={}", entity.getKbId(), name);
        return entity;
    }

    @Override
    public KnowledgeBaseEntity update(String kbId, String name, String description) {
        KnowledgeBaseEntity entity = knowledgeBaseMapper.selectById(kbId);
        if (entity == null) {
            throw new IllegalArgumentException("知识库不存在: " + kbId);
        }

        LambdaUpdateWrapper<KnowledgeBaseEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(KnowledgeBaseEntity::getKbId, kbId)
                .set(name != null, KnowledgeBaseEntity::getName, name)
                .set(description != null, KnowledgeBaseEntity::getDescription, description)
                .set(KnowledgeBaseEntity::getUpdatedAt, LocalDateTime.now());

        knowledgeBaseMapper.update(null, updateWrapper);
        log.info(LogMarkers.BIZ, "知识库更新成功: kbId={}", kbId);
        return knowledgeBaseMapper.selectById(kbId);
    }

    @Override
    public void delete(String kbId) {
        KnowledgeBaseEntity entity = knowledgeBaseMapper.selectById(kbId);
        if (entity == null) {
            throw new IllegalArgumentException("知识库不存在: " + kbId);
        }

        knowledgeBaseMapper.deleteById(kbId);
        log.info(LogMarkers.BIZ, "知识库删除成功: kbId={}, name={}", kbId, entity.getName());
    }

    @Override
    public KnowledgeBaseEntity getById(String kbId) {
        return knowledgeBaseMapper.selectById(kbId);
    }

    @Override
    public List<KnowledgeBaseEntity> list() {
        return knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBaseEntity>()
                        .orderByDesc(KnowledgeBaseEntity::getCreatedAt)
        );
    }

    @Override
    public void incrementDocumentCount(String kbId) {
        LambdaUpdateWrapper<KnowledgeBaseEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(KnowledgeBaseEntity::getKbId, kbId)
                .setSql("document_count = document_count + 1")
                .set(KnowledgeBaseEntity::getUpdatedAt, LocalDateTime.now());
        knowledgeBaseMapper.update(null, updateWrapper);
    }

    @Override
    public void decrementDocumentCount(String kbId) {
        LambdaUpdateWrapper<KnowledgeBaseEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(KnowledgeBaseEntity::getKbId, kbId)
                .setSql("document_count = GREATEST(document_count - 1, 0)")
                .set(KnowledgeBaseEntity::getUpdatedAt, LocalDateTime.now());
        knowledgeBaseMapper.update(null, updateWrapper);
    }
}
