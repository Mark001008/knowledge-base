package com.ma.kb.service.document.impl;

import com.ma.kb.common.enums.DocumentStatusEnum;
import com.ma.kb.common.enums.FileTypeEnum;
import com.ma.kb.common.enums.SpaceRoleEnum;
import com.ma.kb.common.exception.BusinessException;
import com.ma.kb.common.response.ErrorCode;
import com.ma.kb.integration.documentreader.MarkdownDocumentReader;
import com.ma.kb.integration.documentreader.PdfDocumentReader;
import com.ma.kb.integration.documentreader.TxtDocumentReader;
import com.ma.kb.integration.storage.StorageService;
import com.ma.kb.integration.vector.VectorSearchService;
import com.ma.kb.manager.document.DocumentManager;
import com.ma.kb.manager.document.bo.DocumentBO;
import com.ma.kb.manager.space.SpaceManager;
import com.ma.kb.service.document.DocumentIngestionService;
import com.ma.kb.service.document.DocumentService;
import com.ma.kb.service.document.converter.DocumentDTOConverter;
import com.ma.kb.service.document.dto.DocumentContentVO;
import com.ma.kb.service.document.dto.DocumentUploadResponse;
import com.ma.kb.service.document.dto.DocumentVO;
import com.ma.kb.service.document.dto.OnlineDocumentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
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
    private final DocumentIngestionService documentIngestionService;
    private final VectorSearchService vectorSearchService;
    private final MarkdownDocumentReader markdownDocumentReader;
    private final TxtDocumentReader txtDocumentReader;
    private final PdfDocumentReader pdfDocumentReader;

    public DocumentServiceImpl(DocumentManager documentManager, SpaceManager spaceManager,
                               StorageService storageService, DocumentDTOConverter documentDTOConverter,
                               DocumentIngestionService documentIngestionService,
                               VectorSearchService vectorSearchService,
                               MarkdownDocumentReader markdownDocumentReader,
                               TxtDocumentReader txtDocumentReader,
                               PdfDocumentReader pdfDocumentReader) {
        this.documentManager = documentManager;
        this.spaceManager = spaceManager;
        this.storageService = storageService;
        this.documentDTOConverter = documentDTOConverter;
        this.documentIngestionService = documentIngestionService;
        this.vectorSearchService = vectorSearchService;
        this.markdownDocumentReader = markdownDocumentReader;
        this.txtDocumentReader = txtDocumentReader;
        this.pdfDocumentReader = pdfDocumentReader;
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
        documentIngestionService.ingest(created.getId());

        DocumentBO indexed = documentManager.getById(created.getId());
        return new DocumentUploadResponse(created.getId(), originalFilename, indexed.getParseStatus());
    }

    @Override
    public DocumentUploadResponse createOnlineDocument(Long userId, Long spaceId, OnlineDocumentRequest request) {
        checkSpaceRole(userId, spaceId, SpaceRoleEnum.ADMIN);
        validateOnlineDocumentRequest(request);

        byte[] contentBytes = request.content().getBytes(StandardCharsets.UTF_8);
        String fileName = normalizeOnlineDocumentName(request.title());
        String objectKey = spaceId + "/online/" + UUID.randomUUID() + ".md";
        String bucket = storageService.getDefaultBucket();

        uploadMarkdown(bucket, objectKey, contentBytes);

        DocumentBO documentBO = documentDTOConverter.toBO(
                spaceId, fileName, FileTypeEnum.MARKDOWN.getCode(), (long) contentBytes.length,
                bucket, objectKey, DocumentStatusEnum.PENDING.getCode(), userId);

        DocumentBO created = documentManager.create(documentBO);
        log.info("在线文档创建成功: id={}, fileName={}, spaceId={}", created.getId(), fileName, spaceId);
        documentIngestionService.ingest(created.getId());

        DocumentBO indexed = documentManager.getById(created.getId());
        return new DocumentUploadResponse(created.getId(), fileName, indexed.getParseStatus());
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
    public DocumentContentVO getContent(Long userId, Long documentId) {
        DocumentBO document = getAndCheckAccess(userId, documentId);

        try (var inputStream = storageService.download(document.getStorageBucket(), document.getStorageObjectKey())) {
            String content = readPreviewContent(document, inputStream);
            return new DocumentContentVO(document.getId(), displayTitle(document), content, document.getFileType(), document.getParseStatus());
        } catch (Exception e) {
            log.error("读取文档正文失败: id={}", documentId, e);
            throw new BusinessException(ErrorCode.DOCUMENT_UPLOAD_FAILED);
        }
    }

    @Override
    public DocumentUploadResponse updateContent(Long userId, Long documentId, OnlineDocumentRequest request) {
        DocumentBO document = getAndCheckAccess(userId, documentId);
        checkSpaceRole(userId, document.getSpaceId(), SpaceRoleEnum.ADMIN);
        ensureEditableDocument(document);
        validateOnlineDocumentRequest(request);

        byte[] contentBytes = request.content().getBytes(StandardCharsets.UTF_8);
        String fileName = normalizeOnlineDocumentName(request.title());
        uploadMarkdown(document.getStorageBucket(), document.getStorageObjectKey(), contentBytes);

        try {
            vectorSearchService.deleteByDocumentId(documentId);
        } catch (Exception e) {
            log.warn("更新在线文档时删除旧向量失败，继续重建: id={}", documentId, e);
        }
        documentManager.deleteChunksByDocumentId(documentId);

        document.setFileName(fileName);
        document.setFileType(FileTypeEnum.MARKDOWN.getCode());
        document.setFileSize((long) contentBytes.length);
        document.setParseStatus(DocumentStatusEnum.PENDING.getCode());
        document.setErrorMessage("");
        documentManager.update(document);

        documentIngestionService.ingest(documentId);
        DocumentBO indexed = documentManager.getById(documentId);
        log.info("在线文档更新成功: id={}, fileName={}", documentId, fileName);
        return new DocumentUploadResponse(documentId, fileName, indexed.getParseStatus());
    }

    @Override
    public void delete(Long userId, Long documentId) {
        DocumentBO document = getAndCheckAccess(userId, documentId);
        checkSpaceRole(userId, document.getSpaceId(), SpaceRoleEnum.ADMIN);

        storageService.delete(document.getStorageBucket(), document.getStorageObjectKey());
        try {
            vectorSearchService.deleteByDocumentId(documentId);
        } catch (Exception e) {
            log.warn("删除文档向量失败，继续删除业务数据: id={}", documentId, e);
        }
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
        documentIngestionService.ingest(documentId);

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

    private void uploadMarkdown(String bucket, String objectKey, byte[] contentBytes) {
        try {
            storageService.upload(bucket, objectKey, new ByteArrayInputStream(contentBytes),
                    contentBytes.length, FileTypeEnum.MARKDOWN.getMimeType());
        } catch (Exception e) {
            log.error("在线文档保存失败: objectKey={}", objectKey, e);
            throw new BusinessException(ErrorCode.DOCUMENT_UPLOAD_FAILED);
        }
    }

    private void validateOnlineDocumentRequest(OnlineDocumentRequest request) {
        if (request == null || request.title() == null || request.title().trim().isEmpty()
                || request.content() == null || request.content().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        if (request.title().trim().length() > 120 || request.content().length() > 200000) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }

    private String readPreviewContent(DocumentBO document, java.io.InputStream inputStream) {
        if (FileTypeEnum.MARKDOWN.getCode().equals(document.getFileType())) {
            return markdownDocumentReader.read(inputStream, document.getFileName());
        }
        if (FileTypeEnum.TXT.getCode().equals(document.getFileType())) {
            return txtDocumentReader.read(inputStream, document.getFileName());
        }
        if (FileTypeEnum.PDF.getCode().equals(document.getFileType())) {
            return pdfDocumentReader.read(inputStream, document.getFileName());
        }
        throw new BusinessException(ErrorCode.DOCUMENT_TYPE_NOT_SUPPORTED);
    }

    private String displayTitle(DocumentBO document) {
        if (FileTypeEnum.MARKDOWN.getCode().equals(document.getFileType())) {
            return stripMarkdownExtension(document.getFileName());
        }
        return document.getFileName();
    }

    private String normalizeOnlineDocumentName(String title) {
        String normalized = title.trim();
        if (normalized.toLowerCase().endsWith(".md") || normalized.toLowerCase().endsWith(".markdown")) {
            return normalized;
        }
        return normalized + ".md";
    }

    private String stripMarkdownExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        if (fileName.toLowerCase().endsWith(".markdown")) {
            return fileName.substring(0, fileName.length() - ".markdown".length());
        }
        if (fileName.toLowerCase().endsWith(".md")) {
            return fileName.substring(0, fileName.length() - ".md".length());
        }
        return fileName;
    }

    private void ensureEditableDocument(DocumentBO document) {
        if (!FileTypeEnum.MARKDOWN.getCode().equals(document.getFileType())) {
            throw new BusinessException(ErrorCode.DOCUMENT_TYPE_NOT_SUPPORTED);
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
