package com.ma.kb.core.document;

import org.springframework.stereotype.Component;

/**
 * 文本清洗器
 */
@Component
public class TextCleaner {

    /**
     * 清洗文档文本
     * - 去除多余空白行
     * - 合并连续空格
     * - 去除首尾空白
     */
    public String clean(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        // 统一换行符
        String cleaned = text.replace("\r\n", "\n").replace("\r", "\n");

        // 去除多余空白行（保留最多一个空行）
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");

        // 合并连续空格为单个空格（保留换行）
        cleaned = cleaned.replaceAll("[^\\S\n]+", " ");

        // 去除每行首尾空格（保留换行符）
        cleaned = cleaned.replaceAll("(?m)^ +| +$", "");

        return cleaned.trim();
    }
}
