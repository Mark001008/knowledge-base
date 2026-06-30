package com.ma.agent.knowledge.document;

import java.time.Instant;

/**
 * 解析后的文档内部模型，持有文档全文内容和元信息。
 *
 * @param documentId 文档ID
 * @param filename   文件名
 * @param content    文档全文内容
 * @param charCount  字符数
 * @param uploadedAt 上传时间
 */
public record ParsedDocument(
        String documentId,
        String filename,
        String content,
        long charCount,
        Instant uploadedAt
) {
}
