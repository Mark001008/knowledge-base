package com.ma.kb.core.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentChunkerTest {

    private DocumentChunker chunker;

    @BeforeEach
    void setUp() {
        chunker = new DocumentChunker();
    }

    @Test
    void chunkShortText() {
        String text = "Hello World";
        List<String> chunks = chunker.chunk(text, 100, 10);
        assertEquals(1, chunks.size());
        assertEquals("Hello World", chunks.get(0));
    }

    @Test
    void chunkLongText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("这是第").append(i).append("个句子。");
        }
        String text = sb.toString();

        List<String> chunks = chunker.chunk(text, 200, 30);
        assertTrue(chunks.size() > 1);

        // 每片不超过200字符（允许断句偏移）
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 230); // 允许一定的断句偏移
        }
    }

    @Test
    void chunkWithOverlap() {
        String text = "AAAAABBBBBCCCCCDDDDDEEEEE";
        List<String> chunks = chunker.chunk(text, 10, 3);
        assertTrue(chunks.size() >= 2);
    }

    @Test
    void chunkEmptyText() {
        List<String> chunks = chunker.chunk("", 100, 10);
        assertTrue(chunks.isEmpty());
    }

    @Test
    void chunkNullText() {
        List<String> chunks = chunker.chunk(null, 100, 10);
        assertTrue(chunks.isEmpty());
    }

    @Test
    void estimateTokenCount() {
        assertEquals(0, chunker.estimateTokenCount(null));
        assertEquals(0, chunker.estimateTokenCount(""));
        assertTrue(chunker.estimateTokenCount("Hello World") > 0);
        assertTrue(chunker.estimateTokenCount("你好世界测试") > 0);
    }

    @Test
    void chunkChineseText() {
        String text = "第一段内容在这里。第二段内容在这里。第三段内容在这里。第四段内容在这里。第五段内容在这里。";
        List<String> chunks = chunker.chunk(text, 20, 5);
        assertTrue(chunks.size() >= 2);
    }
}
