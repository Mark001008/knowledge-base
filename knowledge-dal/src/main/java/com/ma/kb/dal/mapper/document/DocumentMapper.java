package com.ma.kb.dal.mapper.document;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ma.kb.dal.model.document.DocumentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 文档 Mapper
 */
@Mapper
public interface DocumentMapper extends BaseMapper<DocumentDO> {

    /**
     * 统计知识库下的文档数量
     */
    int countBySpaceId(@Param("spaceId") Long spaceId);
}
