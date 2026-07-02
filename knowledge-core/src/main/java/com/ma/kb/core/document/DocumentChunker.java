package com.ma.kb.core.document;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档切片器
 * 按固定字符长度切片，支持重叠窗口
 */
@Component
public class DocumentChunker {

    /**
     * 将文本切片
     *
     * @param text      清洗后的文本
     * @param chunkSize 每片最大字符数
     * @param overlap   重叠字符数
     * @return 切片列表
     */
    public List<String> chunk(String text, int chunkSize, int overlap) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        int textLength = text.length();
        int start = 0;

        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);

            // 如果不是最后一片，尝试在句子边界或换行处截断
            if (end < textLength) {
                int breakPoint = findBreakPoint(text, start, end);
                if (breakPoint > start) {
                    end = breakPoint;
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // 下一片的起始位置
            int nextStart = end - overlap;
            if (nextStart <= start) {
                // 防止死循环：至少前进一个字符
                nextStart = start + 1;
            }
            start = nextStart;
            if (end >= textLength) break;
        }

        return chunks;
    }

    /**
     * 估算文本的 token 数（粗略：1个中文约2 token，1个英文单词约1.3 token）
     */
    public int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) return 0;
        // 简单估算：字符数 / 2
        return Math.max(1, text.length() / 2);
    }

    /**
     * 在合理位置断句：优先在句号、换行处断开
     */
    private int findBreakPoint(String text, int start, int end) {
        // 从 end 往前找断句点
        for (int i = end - 1; i > start + (end - start) / 2; i--) {
            char c = text.charAt(i);
            if (c == '\n' || c == '。' || c == '！' || c == '？'
                    || c == '.' || c == '!' || c == '?') {
                return i + 1;
            }
        }
        // 找不到句子边界，在空格处断开
        for (int i = end - 1; i > start + (end - start) / 2; i--) {
            if (text.charAt(i) == ' ') {
                return i + 1;
            }
        }
        return end;
    }
}
