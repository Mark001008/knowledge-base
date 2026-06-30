package com.ma.agent.knowledge.document;

import com.ma.agent.shared.LogMarkers;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Excel 文档解析器 - 使用 Apache POI，支持 .xlsx 格式
 */
@Component
class ExcelDocumentParser implements FileParser {

    private static final Logger log = LoggerFactory.getLogger(ExcelDocumentParser.class);

    @Override
    public String supportedExtension() {
        return "xlsx";
    }

    @Override
    public String parse(MultipartFile file) {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            StringBuilder content = new StringBuilder();
            DataFormatter formatter = new DataFormatter();

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                content.append("=== ").append(sheet.getSheetName()).append(" ===\n");

                for (Row row : sheet) {
                    StringBuilder rowContent = new StringBuilder();
                    for (Cell cell : row) {
                        String cellValue = formatter.formatCellValue(cell);
                        if (!cellValue.isBlank()) {
                            if (!rowContent.isEmpty()) {
                                rowContent.append("\t");
                            }
                            rowContent.append(cellValue);
                        }
                    }
                    if (!rowContent.isEmpty()) {
                        content.append(rowContent).append("\n");
                    }
                }
                content.append("\n");
            }

            String result = content.toString().trim();
            log.info(LogMarkers.DATA, "Excel parsed: filename={} sheets={} chars={}",
                    file.getOriginalFilename(), workbook.getNumberOfSheets(), result.length());
            return result;
        } catch (IOException e) {
            throw new DocumentParseException("Failed to parse Excel: " + file.getOriginalFilename(), e);
        }
    }
}
