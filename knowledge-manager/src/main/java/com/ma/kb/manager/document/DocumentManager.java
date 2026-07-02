package com.ma.kb.manager.document;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ma.kb.dal.mapper.document.DocumentChunkMapper;
import com.ma.kb.dal.mapper.document.DocumentMapper;
import com.ma.kb.dal.model.document.DocumentChunkDO;
import com.ma.kb.dal.model.document.DocumentDO;
import com.ma.kb.manager.document.bo.DocumentBO;
import com.ma.kb.manager.document.bo.DocumentChunkBO;
import com.ma.kb.manager.document.converter.DocumentConverter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档数据管理器
 */
@Component
public class DocumentManager {

    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final DocumentConverter documentConverter;

    public DocumentManager(DocumentMapper documentMapper, DocumentChunkMapper documentChunkMapper,
                           DocumentConverter documentConverter) {
        this.documentMapper = documentMapper;
        this.documentChunkMapper = documentChunkMapper;
        this.documentConverter = documentConverter;
    }

    public DocumentBO create(DocumentBO documentBO) {
        DocumentDO documentDO = documentConverter.toDO(documentBO);
        documentMapper.insert(documentDO);
        documentBO.setId(documentDO.getId());
        return documentBO;
    }

    public DocumentBO getById(Long id) {
        DocumentDO documentDO = documentMapper.selectById(id);
        return documentDO != null ? documentConverter.toBO(documentDO) : null;
    }

    public void update(DocumentBO documentBO) {
        DocumentDO documentDO = documentConverter.toDO(documentBO);
        documentMapper.updateById(documentDO);
    }

    public void deleteById(Long id) {
        documentMapper.deleteById(id);
    }

    public List<DocumentBO> listBySpaceId(Long spaceId) {
        List<DocumentDO> docs = documentMapper.selectList(
                new LambdaQueryWrapper<DocumentDO>()
                        .eq(DocumentDO::getSpaceId, spaceId)
                        .orderByDesc(DocumentDO::getCreatedAt)
        );
        return docs.stream().map(documentConverter::toBO).toList();
    }

    public int countBySpaceId(Long spaceId) {
        return documentMapper.countBySpaceId(spaceId);
    }

    public void saveChunks(List<DocumentChunkBO> chunks) {
        for (DocumentChunkBO chunk : chunks) {
            DocumentChunkDO chunkDO = documentConverter.toChunkDO(chunk);
            documentChunkMapper.insert(chunkDO);
            chunk.setId(chunkDO.getId());
        }
    }

    public void deleteChunksByDocumentId(Long documentId) {
        documentChunkMapper.deleteByDocumentId(documentId);
    }
}
