package com.ma.kb.service.document.converter;

import com.ma.kb.manager.document.bo.DocumentBO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Document DTO -> BO 转换器
 */
@Mapper(componentModel = "spring")
public interface DocumentDTOConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DocumentBO toBO(Long spaceId, String fileName, String fileType, Long fileSize,
                    String storageBucket, String storageObjectKey, String parseStatus,
                    Long uploadedBy);

    @Mapping(target = "id", source = "documentId")
    @Mapping(target = "parseStatus", constant = "PENDING")
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(target = "spaceId", ignore = true)
    @Mapping(target = "fileName", ignore = true)
    @Mapping(target = "fileType", ignore = true)
    @Mapping(target = "fileSize", ignore = true)
    @Mapping(target = "storageBucket", ignore = true)
    @Mapping(target = "storageObjectKey", ignore = true)
    @Mapping(target = "uploadedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DocumentBO toReindexBO(Long documentId);
}
