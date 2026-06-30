package com.ma.agent.knowledge.document;

import com.ma.agent.knowledge.dto.DocumentUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface DocumentService {

    /**
     * 上传文档到指定知识库
     */
    DocumentUploadResponse upload(MultipartFile file, String kbId);

    /**
     * 列出所有已存储文档的元信息。
     */
    List<DocumentInfo> listDocuments();

    /**
     * 按知识库列出文档
     */
    List<DocumentInfo> listDocumentsByKbId(String kbId);

    /**
     * 获取指定文档的全文内容。
     */
    Optional<String> getContent(String documentId);

    /**
     * 删除文档
     */
    void delete(String documentId);

    /**
     * 更新文档分类
     */
    void updateCategory(String documentId, String category);
}
