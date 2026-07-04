package com.ma.kb.service.document.dto;

/**
 * 在线文档创建/更新请求。
 */
public record OnlineDocumentRequest(
        String title,
        String content
) {
}
