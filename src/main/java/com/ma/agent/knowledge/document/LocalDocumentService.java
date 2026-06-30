package com.ma.agent.knowledge.document;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ma.agent.entity.DocumentEntity;
import com.ma.agent.knowledge.dto.DocumentUploadResponse;
import com.ma.agent.mapper.DocumentMapper;
import com.ma.agent.shared.LogMarkers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 本地文档服务：上传即解析，存入 MySQL。
 * <p>支持多格式文档解析（txt, pdf, docx, xlsx, md）</p>
 *
 * 通过 agent.document.provider=local 激活。
 */
@Service
@ConditionalOnProperty(prefix = "agent.document", name = "provider", havingValue = "local")
class LocalDocumentService implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(LocalDocumentService.class);

    private final DocumentMapper documentMapper;
    private final DocumentParser parser;
    private final DocumentParserFactory parserFactory;

    LocalDocumentService(DocumentMapper documentMapper, DocumentParser parser, DocumentParserFactory parserFactory) {
        this.documentMapper = documentMapper;
        this.parser = parser;
        this.parserFactory = parserFactory;
    }

    @Override
    public DocumentUploadResponse upload(MultipartFile file, String kbId) {
        String documentId = UUID.randomUUID().toString();
        String filename = file.getOriginalFilename();
        String fileType = extractExtension(filename);

        log.info(LogMarkers.DATA, "LocalDocumentService.upload docId={} filename={} type={} kbId={} size={}",
                documentId, filename, fileType, kbId, file.getSize());

        // 解析文件内容（支持多格式）
        ParsedDocument parsed = parser.parse(documentId, file);

        // 保存到 MySQL
        DocumentEntity entity = new DocumentEntity();
        entity.setDocumentId(documentId);
        entity.setKbId(kbId != null ? kbId : "default");
        entity.setFilename(parsed.filename());
        entity.setContent(parsed.content());
        entity.setCharCount(parsed.charCount());
        entity.setFileType(fileType);
        entity.setFileSize(file.getSize());
        entity.setCategory("");
        entity.setStatus("indexed");
        entity.setUploadedAt(LocalDateTime.now());
        documentMapper.insert(entity);

        return new DocumentUploadResponse(documentId, filename, "stored");
    }

    @Override
    public List<DocumentInfo> listDocuments() {
        return documentMapper.selectList(null).stream()
                .map(this::toDocumentInfo)
                .toList();
    }

    @Override
    public List<DocumentInfo> listDocumentsByKbId(String kbId) {
        return documentMapper.selectList(
                new LambdaQueryWrapper<DocumentEntity>()
                        .eq(DocumentEntity::getKbId, kbId)
                        .orderByDesc(DocumentEntity::getUploadedAt)
        ).stream().map(this::toDocumentInfo).toList();
    }

    @Override
    public Optional<String> getContent(String documentId) {
        DocumentEntity entity = documentMapper.selectById(documentId);
        return entity != null ? Optional.of(entity.getContent()) : Optional.empty();
    }

    @Override
    public void delete(String documentId) {
        documentMapper.deleteById(documentId);
        log.info(LogMarkers.BIZ, "文档删除: documentId={}", documentId);
    }

    @Override
    public void updateCategory(String documentId, String category) {
        DocumentEntity entity = documentMapper.selectById(documentId);
        if (entity != null) {
            entity.setCategory(category);
            documentMapper.updateById(entity);
            log.info(LogMarkers.BIZ, "文档分类更新: documentId={} category={}", documentId, category);
        }
    }

    private DocumentInfo toDocumentInfo(DocumentEntity entity) {
        return new DocumentInfo(
                entity.getDocumentId(),
                entity.getKbId(),
                entity.getFilename(),
                entity.getCharCount(),
                entity.getFileType(),
                entity.getFileSize(),
                entity.getCategory(),
                entity.getStatus(),
                entity.getUploadedAt() != null ? entity.getUploadedAt().toString() : ""
        );
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "txt";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
