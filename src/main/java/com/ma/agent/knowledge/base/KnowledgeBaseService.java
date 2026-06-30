package com.ma.agent.knowledge.base;

import com.ma.agent.entity.KnowledgeBaseEntity;
import java.util.List;

/**
 * 知识库管理服务接口
 */
public interface KnowledgeBaseService {

    /**
     * 创建知识库
     */
    KnowledgeBaseEntity create(String name, String description);

    /**
     * 更新知识库
     */
    KnowledgeBaseEntity update(String kbId, String name, String description);

    /**
     * 删除知识库
     */
    void delete(String kbId);

    /**
     * 根据ID获取知识库
     */
    KnowledgeBaseEntity getById(String kbId);

    /**
     * 获取所有知识库列表
     */
    List<KnowledgeBaseEntity> list();

    /**
     * 增加知识库文档计数
     */
    void incrementDocumentCount(String kbId);

    /**
     * 减少知识库文档计数
     */
    void decrementDocumentCount(String kbId);
}
