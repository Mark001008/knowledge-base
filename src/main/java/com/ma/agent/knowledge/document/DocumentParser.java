package com.ma.agent.knowledge.document;

import com.ma.agent.shared.LogMarkers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

/**
 * 文档解析器 - 委托给 DocumentParserFactory 进行多格式解析
 * <p>保留此类作为向后兼容入口，内部使用工厂模式分发到具体解析器</p>
 */
@Component
class DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(DocumentParser.class);

    private final DocumentParserFactory parserFactory;

    DocumentParser(DocumentParserFactory parserFactory) {
        this.parserFactory = parserFactory;
    }

    /**
     * 将上传的文件解析为 ParsedDocument，支持多格式。
     */
    ParsedDocument parse(String documentId, MultipartFile file) {
        String filename = file.getOriginalFilename();
        String content = parserFactory.parseFile(file);

        String fileType = extractExtension(filename);
        log.info(LogMarkers.DATA, "Parsed document: docId={} filename={} type={} chars={}",
                documentId, filename, fileType, content.length());

        return new ParsedDocument(documentId, filename, content, content.length(), Instant.now());
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "txt";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
