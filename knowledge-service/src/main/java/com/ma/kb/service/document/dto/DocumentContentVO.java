package com.ma.kb.service.document.dto;

/**
 * 在线文档正文视图。
 */
public record DocumentContentVO(
        Long documentId,
        String title,
        String content,
        String fileType,
        String status
) {
}
