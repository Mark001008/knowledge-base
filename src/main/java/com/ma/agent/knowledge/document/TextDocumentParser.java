package com.ma.agent.knowledge.document;

import com.ma.agent.shared.LogMarkers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 纯文本文件解析器 - 支持 .txt 文件
 */
@Component
class TextDocumentParser implements FileParser {

    private static final Logger log = LoggerFactory.getLogger(TextDocumentParser.class);

    @Override
    public String supportedExtension() {
        return "txt";
    }

    @Override
    public String parse(MultipartFile file) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            log.info(LogMarkers.DATA, "Text parsed: filename={} chars={}",
                    file.getOriginalFilename(), content.length());
            return content;
        } catch (IOException e) {
            throw new DocumentParseException("Failed to parse text file: " + file.getOriginalFilename(), e);
        }
    }
}
