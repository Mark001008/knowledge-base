package com.ma.agent.knowledge.document;

import com.ma.agent.shared.LogMarkers;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Word 文档解析器 - 使用 Apache POI，支持 .docx 格式
 */
@Component
class WordDocumentParser implements FileParser {

    private static final Logger log = LoggerFactory.getLogger(WordDocumentParser.class);

    @Override
    public String supportedExtension() {
        return "docx";
    }

    @Override
    public String parse(MultipartFile file) {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            String content = document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .filter(text -> !text.isBlank())
                    .collect(Collectors.joining("\n"));
            log.info(LogMarkers.DATA, "Word parsed: filename={} paragraphs={} chars={}",
                    file.getOriginalFilename(), document.getParagraphs().size(), content.length());
            return content;
        } catch (IOException e) {
            throw new DocumentParseException("Failed to parse Word document: " + file.getOriginalFilename(), e);
        }
    }
}
