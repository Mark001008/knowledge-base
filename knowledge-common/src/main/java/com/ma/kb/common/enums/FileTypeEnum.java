package com.ma.kb.common.enums;

import java.util.Set;

/**
 * 文件类型枚举
 */
public enum FileTypeEnum {

    PDF("PDF", "application/pdf", "pdf"),
    TXT("TXT", "text/plain", "txt"),
    MARKDOWN("MARKDOWN", "text/markdown", "md"),
    DOCX("DOCX", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");

    private final String code;
    private final String mimeType;
    private final String extension;

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pdf", "txt", "md", "markdown");

    FileTypeEnum(String code, String mimeType, String extension) {
        this.code = code;
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public String getCode() {
        return code;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getExtension() {
        return extension;
    }

    /**
     * 根据文件扩展名判断是否支持
     */
    public static boolean isSupported(String filename) {
        if (filename == null) return false;
        String ext = getFileExtension(filename);
        return SUPPORTED_EXTENSIONS.contains(ext.toLowerCase());
    }

    /**
     * 根据文件名获取文件类型
     */
    public static FileTypeEnum fromFilename(String filename) {
        String ext = getFileExtension(filename).toLowerCase();
        return switch (ext) {
            case "pdf" -> PDF;
            case "txt" -> TXT;
            case "md", "markdown" -> MARKDOWN;
            case "docx" -> DOCX;
            default -> throw new IllegalArgumentException("不支持的文件类型: " + ext);
        };
    }

    /**
     * 获取文件扩展名
     */
    public static String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0) return "";
        return filename.substring(lastDot + 1);
    }
}
