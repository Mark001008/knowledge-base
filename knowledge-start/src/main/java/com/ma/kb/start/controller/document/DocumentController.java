package com.ma.kb.start.controller.document;

import com.ma.kb.common.response.ApiResponse;
import com.ma.kb.core.auth.JwtService;
import com.ma.kb.core.auth.SecurityUtils;
import com.ma.kb.service.document.DocumentService;
import com.ma.kb.service.document.dto.DocumentUploadResponse;
import com.ma.kb.service.document.dto.DocumentVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档控制器
 */
@RestController
@RequestMapping("/api")
public class DocumentController {

    private final DocumentService documentService;
    private final JwtService jwtService;

    public DocumentController(DocumentService documentService, JwtService jwtService) {
        this.documentService = documentService;
        this.jwtService = jwtService;
    }

    /**
     * 上传文档
     */
    @PostMapping("/spaces/{spaceId}/documents")
    public ApiResponse<DocumentUploadResponse> upload(HttpServletRequest request,
                                                      @PathVariable Long spaceId,
                                                      @RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        DocumentUploadResponse response = documentService.upload(userId, spaceId, file);
        return ApiResponse.success(response);
    }

    /**
     * 查询知识库文档列表
     */
    @GetMapping("/spaces/{spaceId}/documents")
    public ApiResponse<List<DocumentVO>> listBySpaceId(HttpServletRequest request,
                                                       @PathVariable Long spaceId) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        List<DocumentVO> docs = documentService.listBySpaceId(userId, spaceId);
        return ApiResponse.success(docs);
    }

    /**
     * 查询文档详情
     */
    @GetMapping("/documents/{id}")
    public ApiResponse<DocumentVO> getById(HttpServletRequest request, @PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        DocumentVO doc = documentService.getById(userId, id);
        return ApiResponse.success(doc);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/documents/{id}")
    public ApiResponse<Void> delete(HttpServletRequest request, @PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        documentService.delete(userId, id);
        return ApiResponse.success();
    }

    /**
     * 重建索引
     */
    @PostMapping("/documents/{id}/reindex")
    public ApiResponse<Void> reindex(HttpServletRequest request, @PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId(request.getHeader("Authorization"), jwtService);
        documentService.reindex(userId, id);
        return ApiResponse.success();
    }
}
