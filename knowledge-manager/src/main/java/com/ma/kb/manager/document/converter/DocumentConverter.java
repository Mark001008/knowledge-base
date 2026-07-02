package com.ma.kb.manager.document.converter;

import com.ma.kb.dal.model.document.DocumentDO;
import com.ma.kb.dal.model.document.DocumentChunkDO;
import com.ma.kb.manager.document.bo.DocumentBO;
import com.ma.kb.manager.document.bo.DocumentChunkBO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Document DO <-> BO 转换器
 */
@Mapper(componentModel = "spring")
public interface DocumentConverter {

    DocumentBO toBO(DocumentDO documentDO);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DocumentDO toDO(DocumentBO documentBO);

    @Mapping(target = "createdAt", ignore = true)
    DocumentChunkDO toChunkDO(DocumentChunkBO chunkBO);
}
