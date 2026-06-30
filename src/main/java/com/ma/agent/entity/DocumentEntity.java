package com.ma.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 文档实体 - 持久化到 MySQL
 * <p>存储上传到知识库的文档元信息和内容</p>
 */
@TableName("documents")
public class DocumentEntity {

    /** 文档ID（雪花算法生成） */
    @TableId(type = IdType.ASSIGN_ID)
    private String documentId;

    /** 所属知识库ID */
    private String kbId;

    /** 文档文件名 */
    private String filename;

    /** 文档文本内容 */
    private String content;

    /** 文档字符数 */
    private Long charCount;

    /** 文件类型 (txt, pdf, docx, xlsx, md) */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 分类标签 */
    private String category;

    /** 索引状态 (pending, indexed, failed) */
    private String status;

    /** 上传时间 */
    private LocalDateTime uploadedAt;

    // Getters and Setters
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getKbId() {
        return kbId;
    }

    public void setKbId(String kbId) {
        this.kbId = kbId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getCharCount() {
        return charCount;
    }

    public void setCharCount(Long charCount) {
        this.charCount = charCount;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
