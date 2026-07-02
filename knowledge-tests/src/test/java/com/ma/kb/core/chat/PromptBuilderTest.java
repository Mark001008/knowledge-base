package com.ma.kb.core.chat;

import com.ma.kb.integration.vector.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PromptBuilderTest {

    private PromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new PromptBuilder();
    }

    @Test
    void buildSystemPrompt() {
        String prompt = promptBuilder.buildSystemPrompt();
        assertNotNull(prompt);
        assertTrue(prompt.contains("知识库助手"));
        assertTrue(prompt.contains("上下文"));
    }

    @Test
    void buildUserMessageWithResults() {
        SearchResult sr = new SearchResult();
        sr.setDocumentName("产品手册.pdf");
        sr.setPageNumber(5);
        sr.setChunkIndex(12);
        sr.setContent("这是文档内容片段");

        String message = promptBuilder.buildUserMessageWith("如何配置？", List.of(sr));

        assertTrue(message.contains("上下文"));
        assertTrue(message.contains("产品手册.pdf"));
        assertTrue(message.contains("页码：5"));
        assertTrue(message.contains("分片：12"));
        assertTrue(message.contains("这是文档内容片段"));
        assertTrue(message.contains("如何配置？"));
    }

    @Test
    void buildUserMessageWithEmptyResults() {
        String message = promptBuilder.buildUserMessageWith("测试问题", List.of());
        assertEquals("测试问题", message);
    }

    @Test
    void buildUserMessageWithNullResults() {
        String message = promptBuilder.buildUserMessageWith("测试问题", null);
        assertEquals("测试问题", message);
    }

    @Test
    void buildNoContextAnswer() {
        String answer = promptBuilder.buildNoContextAnswer();
        assertEquals("当前知识库中未找到相关信息", answer);
    }
}
