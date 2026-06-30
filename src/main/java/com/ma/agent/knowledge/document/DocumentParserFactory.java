package com.ma.agent.knowledge.document;

import com.ma.agent.shared.LogMarkers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档解析器工厂 - 根据文件扩展名选择对应的解析器
 */
@Component
public class DocumentParserFactory {

    private static final Logger log = LoggerFactory.getLogger(DocumentParserFactory.class);

    private final Map<String, FileParser> parsers = new HashMap<>();

    public DocumentParserFactory(List<FileParser> parserList) {
        for (FileParser parser : parserList) {
            parsers.put(parser.supportedExtension().toLowerCase(), parser);
            log.info("Registered document parser: .{} -> {}", parser.supportedExtension(), parser.getClass().getSimpleName());
        }
    }

    /**
     * 根据文件扩展名获取解析器
     *
     * @param filename 文件名
     * @return 对应的解析器
     * @throws IllegalArgumentException 不支持的文件格式
     */
    public FileParser getParser(String filename) {
        String extension = extractExtension(filename);
        FileParser parser = parsers.get(extension.toLowerCase());
        if (parser == null) {
            throw new IllegalArgumentException("不支持的文件格式: ." + extension + "，支持的格式: " + parsers.keySet());
        }
        return parser;
    }

    /**
     * 解析文件
     *
     * @param file 上传的文件
     * @return 解析后的文本内容
     */
    public String parseFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        FileParser parser = getParser(filename);
        return parser.parse(file);
    }

    /**
     * 检查是否支持该文件格式
     */
    public boolean isSupported(String filename) {
        String extension = extractExtension(filename);
        return parsers.containsKey(extension.toLowerCase());
    }

    /**
     * 获取所有支持的文件格式
     */
    public java.util.Set<String> supportedFormats() {
        return parsers.keySet();
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
