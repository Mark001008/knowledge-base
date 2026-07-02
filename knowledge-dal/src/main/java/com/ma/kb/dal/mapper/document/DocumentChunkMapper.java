package com.ma.kb.dal.mapper.document;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ma.kb.dal.model.document.DocumentChunkDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 文档分片 Mapper
 */
@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunkDO> {

    /**
     * 删除指定文档的所有分片
     */
    int deleteByDocumentId(@Param("documentId") Long documentId);
}
