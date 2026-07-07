package com.ma.kb.service.document;

import com.ma.kb.service.document.dto.DocumentUploadResponse;
import com.ma.kb.service.document.dto.DocumentVO;
import com.ma.kb.service.document.dto.DocumentContentVO;
import com.ma.kb.service.document.dto.DocumentDownloadVO;
import com.ma.kb.service.document.dto.OnlineDocumentRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档服务接口
 */
public interface DocumentService {

    /**
     * 上传文档
     */
    DocumentUploadResponse upload(Long userId, Long spaceId, MultipartFile file);

    /**
     * 创建在线文档
     */
    DocumentUploadResponse createOnlineDocument(Long userId, Long spaceId, OnlineDocumentRequest request);

    /**
     * 查询在线文档正文
     */
    DocumentContentVO getContent(Long userId, Long documentId);

    /**
     * 下载原始文档
     */
    DocumentDownloadVO downloadOriginal(Long userId, Long documentId);

    /**
     * 更新在线文档正文
     */
    DocumentUploadResponse updateContent(Long userId, Long documentId, OnlineDocumentRequest request);

    /**
     * 查询知识库文档列表
     */
    List<DocumentVO> listBySpaceId(Long userId, Long spaceId);

    /**
     * 查询文档详情
     */
    DocumentVO getById(Long userId, Long documentId);

    /**
     * 删除文档
     */
    void delete(Long userId, Long documentId);

    /**
     * 重建索引
     */
    void reindex(Long userId, Long documentId);
}
