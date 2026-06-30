package com.ma.agent.controller;

import com.ma.agent.knowledge.document.DocumentInfo;
import com.ma.agent.knowledge.document.DocumentService;
import com.ma.agent.knowledge.dto.DocumentUploadResponse;
import com.ma.agent.shared.LogMarkers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 文档管理 API
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * 上传文档到指定知识库
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentUploadResponse upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "kbId", defaultValue = "default") String kbId) {
        log.info(LogMarkers.API, "POST /api/documents/upload file={} kbId={} size={}",
                file.getOriginalFilename(), kbId, file.getSize());
        var response = documentService.upload(file, kbId);
        log.info(LogMarkers.API, "POST /api/documents/upload docId={} status={}", response.documentId(), response.status());
        return response;
    }

    /**
     * 获取文档列表（可按知识库过滤）
     */
    @GetMapping
    public List<DocumentInfo> listDocuments(
            @RequestParam(value = "kbId", required = false) String kbId) {
        log.info(LogMarkers.API, "GET /api/documents kbId={}", kbId);
        if (kbId != null && !kbId.isBlank()) {
            return documentService.listDocumentsByKbId(kbId);
        }
        return documentService.listDocuments();
    }

    /**
     * 获取文档内容
     */
    @GetMapping("/{documentId}/content")
    public Map<String, Object> getContent(@PathVariable String documentId) {
        log.info(LogMarkers.API, "GET /api/documents/{}/content", documentId);
        return documentService.getContent(documentId)
                .map(content -> Map.<String, Object>of("documentId", documentId, "content", content))
                .orElse(Map.of("documentId", documentId, "error", "Document not found"));
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{documentId}")
    public Map<String, String> delete(@PathVariable String documentId) {
        log.info(LogMarkers.API, "DELETE /api/documents/{}", documentId);
        documentService.delete(documentId);
        return Map.of("documentId", documentId, "status", "deleted");
    }

    /**
     * 更新文档分类
     */
    @PutMapping("/{documentId}/category")
    public Map<String, String> updateCategory(
            @PathVariable String documentId,
            @RequestBody Map<String, String> request) {
        String category = request.get("category");
        log.info(LogMarkers.API, "PUT /api/documents/{}/category category={}", documentId, category);
        documentService.updateCategory(documentId, category);
        return Map.of("documentId", documentId, "category", category != null ? category : "");
    }
}
