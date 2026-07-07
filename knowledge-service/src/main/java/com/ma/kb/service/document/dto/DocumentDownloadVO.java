package com.ma.kb.service.document.dto;

import java.io.InputStream;

/**
 * 原始文档下载视图。
 */
public record DocumentDownloadVO(
        String fileName,
        String fileType,
        Long fileSize,
        InputStream inputStream
) {
}
