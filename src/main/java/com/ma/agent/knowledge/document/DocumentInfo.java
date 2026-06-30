package com.ma.agent.knowledge.document;

/**
 * 文档元信息，用于列表展示。
 *
 * @param documentId 文档ID
 * @param kbId       所属知识库ID
 * @param filename   文件名
 * @param charCount  字符数
 * @param fileType   文件类型
 * @param fileSize   文件大小（字节）
 * @param category   分类标签
 * @param status     索引状态
 * @param uploadedAt 上传时间（格式化字符串）
 */
public record DocumentInfo(
        String documentId,
        String kbId,
        String filename,
        long charCount,
        String fileType,
        long fileSize,
        String category,
        String status,
        String uploadedAt
) {
}
