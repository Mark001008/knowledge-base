package com.ma.agent.knowledge.chunk;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 递归分块器：按段落 → 句子 → 字符逐级细分。
 *
 * 这是 LangChain 等框架的默认分块策略，平衡了语义完整性和块大小均匀性。
 *
 * 算法流程：
 * 1. 按段落（\n\n）分割
 * 2. 如果段落超过 chunkSize，按句子（。！？.!?）分割
 * 3. 如果句子还超过 chunkSize，按字符强制切分
 * 4. 相邻块之间保留 overlap 个字符的重叠，保持上下文连贯
 */
@Component
class RecursiveChunker implements DocumentChunker {

    /**
     * 每块的目标大小（字符数）。
     * 生产环境应从配置读取。
     */
    private static final int DEFAULT_CHUNK_SIZE = 500;

    /**
     * 相邻块的重叠字符数。
     * 重叠可以保持跨块的上下文连贯，通常为 chunkSize 的 10-20%。
     */
    private static final int DEFAULT_OVERLAP = 100;

    /**
     * 分段分隔符，按优先级排列。
     * 先尝试用 \n\n 分割，不行再用句子分隔符，最后用字符。
     */
    private static final String[] SEPARATORS = {"\n\n", "。", "！", "？", ".", "!", "?", "\n", " "};

    @Override
    public List<Chunk> chunk(String content, ChunkMetadata metadata) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        List<String> rawChunks = recursiveSplit(content, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);

        List<Chunk> chunks = new ArrayList<>();
        for (int i = 0; i < rawChunks.size(); i++) {
            chunks.add(new Chunk(rawChunks.get(i).trim(), metadata, i));
        }

        return chunks;
    }

    /**
     * 递归分割文本。
     *
     * @param text      待分割文本
     * @param chunkSize 目标块大小
     * @param overlap   重叠大小
     * @return 分割后的文本块
     */
    private List<String> recursiveSplit(String text, int chunkSize, int overlap) {
        if (text.length() <= chunkSize) {
            return List.of(text);
        }

        // 尝试用每个分隔符分割
        for (String separator : SEPARATORS) {
            if (!text.contains(separator)) {
                continue;
            }

            String[] parts = text.split(escapeRegex(separator), -1);
            if (parts.length <= 1) {
                continue;
            }

            return mergeSmallParts(parts, separator, chunkSize, overlap);
        }

        // 所有分隔符都不行，强制按字符切分
        return forceSplit(text, chunkSize, overlap);
    }

    /**
     * 合并小块，确保每块尽可能接近 chunkSize。
     */
    private List<String> mergeSmallParts(String[] parts, String separator, int chunkSize, int overlap) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String part : parts) {
            // 如果当前块加上新部分超过 chunkSize
            if (current.length() + part.length() + separator.length() > chunkSize) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    // 计算重叠：从当前块末尾取 overlap 个字符
                    String overlapText = getLastNChars(current.toString(), overlap);
                    current = new StringBuilder(overlapText);
                }

                // 如果单个部分就超过 chunkSize，递归分割
                if (part.length() > chunkSize) {
                    List<String> subChunks = recursiveSplit(part, chunkSize, overlap);
                    for (int i = 0; i < subChunks.size() - 1; i++) {
                        result.add(subChunks.get(i));
                    }
                    // 最后一块留给下一轮合并
                    current.append(subChunks.get(subChunks.size() - 1));
                } else {
                    current.append(part);
                }
            } else {
                if (current.length() > 0) {
                    current.append(separator);
                }
                current.append(part);
            }
        }

        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }

    /**
     * 强制按字符切分（最后手段）。
     */
    private List<String> forceSplit(String text, int chunkSize, int overlap) {
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            result.add(text.substring(start, end));
            start = end - overlap;
            if (start >= text.length()) break;
        }
        return result;
    }

    /**
     * 获取字符串末尾的 N 个字符。
     */
    private String getLastNChars(String text, int n) {
        if (text.length() <= n) {
            return text;
        }
        return text.substring(text.length() - n);
    }

    /**
     * 转义正则表达式特殊字符。
     */
    private String escapeRegex(String separator) {
        return separator.replaceAll("([.!?\\\\*+\\[\\](){}|^$])", "\\\\$1");
    }
}
