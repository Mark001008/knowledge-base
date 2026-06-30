package com.ma.agent.knowledge.document;

import com.ma.agent.shared.LogMarkers;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * PDF 文档解析器 - 使用 Apache PDFBox
 */
@Component
class PdfDocumentParser implements FileParser {

    private static final Logger log = LoggerFactory.getLogger(PdfDocumentParser.class);

    @Override
    public String supportedExtension() {
        return "pdf";
    }

    @Override
    public String parse(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String content = stripper.getText(document);
            log.info(LogMarkers.DATA, "PDF parsed: filename={} pages={} chars={}",
                    file.getOriginalFilename(), document.getNumberOfPages(), content.length());
            return content;
        } catch (IOException e) {
            throw new DocumentParseException("Failed to parse PDF: " + file.getOriginalFilename(), e);
        }
    }
}
