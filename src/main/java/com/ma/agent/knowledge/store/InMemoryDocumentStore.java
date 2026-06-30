package com.ma.agent.knowledge.store;

import com.ma.agent.knowledge.document.ParsedDocument;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存文档存储，线程安全，支持按 ID 检索和全文关键词搜索。
 *
 * 当前阶段用内存索引。后续可替换为向量数据库。
 */
@Component
public class InMemoryDocumentStore {

    private final Map<String, ParsedDocument> documents = new ConcurrentHashMap<>();

    public void store(ParsedDocument document) {
        documents.put(document.documentId(), document);
    }

    public Optional<ParsedDocument> get(String documentId) {
        return Optional.ofNullable(documents.get(documentId));
    }

    public List<ParsedDocument> getAll() {
        return List.copyOf(documents.values());
    }

    public int count() {
        return documents.size();
    }

    /**
     * 关键词搜索：匹配文档内容，返回包含关键词的文档及片段。
     *
     * 当前为基础实现：对每个文档做 contains 匹配，截取关键词前后的上下文。
     * 后续可以替换为倒排索引或向量检索。
     */
    public List<DocumentMatch> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String lowerQuery = query.toLowerCase();

        return documents.values().stream()
                .filter(doc -> doc.content().toLowerCase().contains(lowerQuery))
                .map(doc -> {
                    String snippet = extractSnippet(doc.content(), query, 200);
                    return new DocumentMatch(doc.documentId(), doc.filename(), snippet);
                })
                .collect(Collectors.toList());
    }

    /**
     * 从文档内容中提取关键词附近的片段（含前后各 maxLen/2 字符）。
     */
    private String extractSnippet(String content, String keyword, int maxLen) {
        int idx = content.toLowerCase().indexOf(keyword.toLowerCase());
        if (idx < 0) {
	        return limit(content, maxLen);
        }

        int half = maxLen / 2;
        int start = Math.max(0, idx - half);
        int end = Math.min(content.length(), idx + keyword.length() + half);

        StringBuilder sb = new StringBuilder();
        if (start > 0) {
	        sb.append("...");
        }
        sb.append(content, start, end);
        if (end < content.length()) {
	        sb.append("...");
        }
        return sb.toString();
    }

    private String limit(String content, int maxLen) {
        if (content.length() <= maxLen) {
	        return content;
        }
        return content.substring(0, maxLen) + "...";
    }

    public record DocumentMatch(String documentId, String filename, String snippet) {
    }
}
