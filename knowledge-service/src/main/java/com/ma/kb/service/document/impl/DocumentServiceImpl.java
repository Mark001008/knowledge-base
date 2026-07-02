package com.ma.kb.service.document.impl;

import com.ma.kb.common.enums.DocumentStatusEnum;
import com.ma.kb.common.enums.FileTypeEnum;
import com.ma.kb.common.enums.SpaceRoleEnum;
import com.ma.kb.common.exception.BusinessException;
import com.ma.kb.common.response.ErrorCode;
import com.ma.kb.integration.storage.StorageService;
import com.ma.kb.manager.document.DocumentManager;
import com.ma.kb.manager.document.bo.DocumentBO;
import com.ma.kb.manager.space.SpaceManager;
import com.ma.kb.service.document.DocumentService;
import com.ma.kb.service.document.converter.DocumentDTOConverter;
import com.ma.kb.service.document.dto.DocumentUploadResponse;
import com.ma.kb.service.document.dto.DocumentVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * 文档服务实现
 */
@Service
public class DocumentServiceImpl implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);

    private final DocumentManager documentManager;
    private final SpaceManager spaceManager;
    private final StorageService storageService;
    private final DocumentDTOConverter documentDTOConverter;

    public DocumentServiceImpl(DocumentManager documentManager, SpaceManager spaceManager,
                               StorageService storageService, DocumentDTOConverter documentDTOConverter) {
        this.documentManager = documentManager;
        this.spaceManager = spaceManager;
        this.storageService = storageService;
        this.documentDTOConverter = documentDTOConverter;
    }

    @Override
    public DocumentUploadResponse upload(Long userId, Long spaceId, MultipartFile file) {
        checkSpaceRole(userId, spaceId, SpaceRoleEnum.ADMIN);

        String originalFilename = file.getOriginalFilename();
        if (!FileTypeEnum.isSupported(originalFilename)) {
            throw new BusinessException(ErrorCode.DOCUMENT_TYPE_NOT_SUPPORTED);
        }

        FileTypeEnum fileType = FileTypeEnum.fromFilename(originalFilename);
        String objectKey = spaceId + "/" + UUID.randomUUID() + "." + fileType.getExtension();

        String bucket = storageService.getDefaultBucket();
        try {
            storageService.upload(bucket, objectKey, file.getInputStream(),
                    file.getSize(), file.getContentType());
        } catch (Exception e) {
            log.error("文件上传失败: fileName={}", originalFilename, e);
            throw new BusinessException(ErrorCode.DOCUMENT_UPLOAD_FAILED);
        }

        DocumentBO documentBO = documentDTOConverter.toBO(
                spaceId, originalFilename, fileType.getCode(), file.getSize(),
                bucket, objectKey, DocumentStatusEnum.PENDING.getCode(), userId);

        DocumentBO created = documentManager.create(documentBO);
        log.info("文档上传成功: id={}, fileName={}, spaceId={}", created.getId(), originalFilename, spaceId);

        return new DocumentUploadResponse(created.getId(), originalFilename,
                DocumentStatusEnum.PENDING.getCode());
    }

    @Override
    public List<DocumentVO> listBySpaceId(Long userId, Long spaceId) {
        checkSpaceAccess(userId, spaceId);

        List<DocumentBO> docs = documentManager.listBySpaceId(spaceId);
        return docs.stream().map(this::toVO).toList();
    }

    @Override
    public DocumentVO getById(Long userId, Long documentId) {
        DocumentBO document = getAndCheckAccess(userId, documentId);
        return toVO(document);
    }

    @Override
    public void delete(Long userId, Long documentId) {
        DocumentBO document = getAndCheckAccess(userId, documentId);
        checkSpaceRole(userId, document.getSpaceId(), SpaceRoleEnum.ADMIN);

        storageService.delete(document.getStorageBucket(), document.getStorageObjectKey());
        documentManager.deleteChunksByDocumentId(documentId);
        documentManager.deleteById(documentId);
        log.info("文档删除成功: id={}", documentId);
    }

    @Override
    public void reindex(Long userId, Long documentId) {
        DocumentBO document = getAndCheckAccess(userId, documentId);
        checkSpaceRole(userId, document.getSpaceId(), SpaceRoleEnum.ADMIN);

        documentManager.deleteChunksByDocumentId(documentId);

        DocumentBO reindexBO = documentDTOConverter.toReindexBO(documentId);
        documentManager.update(reindexBO);

        log.info("文档重建索引已提交: id={}", documentId);
    }

    // ==================== 私有方法 ====================

    private DocumentBO getAndCheckAccess(Long userId, Long documentId) {
        DocumentBO document = documentManager.getById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        checkSpaceAccess(userId, document.getSpaceId());
        return document;
    }

    private void checkSpaceAccess(Long userId, Long spaceId) {
        String role = spaceManager.getMemberRole(spaceId, userId);
        if (role == null) {
            throw new BusinessException(ErrorCode.SPACE_ACCESS_DENIED);
        }
    }

    private void checkSpaceRole(Long userId, Long spaceId, SpaceRoleEnum minRole) {
        String role = spaceManager.getMemberRole(spaceId, userId);
        if (role == null) {
            throw new BusinessException(ErrorCode.SPACE_ACCESS_DENIED);
        }
        SpaceRoleEnum userRole = SpaceRoleEnum.fromCode(role);
        if (userRole.ordinal() > minRole.ordinal()) {
            throw new BusinessException(ErrorCode.SPACE_ACCESS_DENIED);
        }
    }

    private DocumentVO toVO(DocumentBO doc) {
        return new DocumentVO(
                doc.getId(), doc.getSpaceId(), doc.getFileName(), doc.getFileType(),
                doc.getFileSize(), doc.getParseStatus(), doc.getErrorMessage(),
                doc.getUploadedBy(), null, doc.getCreatedAt(), doc.getUpdatedAt()
        );
    }
}
