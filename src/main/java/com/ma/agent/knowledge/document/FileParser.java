package com.ma.agent.knowledge.document;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件解析器接口 - 支持多格式文档解析
 * <p>每种文件格式实现此接口，由 DocumentParserFactory 统一调度</p>
 */
public interface FileParser {

    /**
     * 返回支持的文件扩展名（不含点号），如 "txt", "pdf", "docx"
     */
    String supportedExtension();

    /**
     * 解析文件内容，返回纯文本
     */
    String parse(MultipartFile file);
}
