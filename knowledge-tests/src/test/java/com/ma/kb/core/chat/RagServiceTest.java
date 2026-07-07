package com.ma.kb.core.chat;

import com.ma.kb.integration.llm.ChatModelService;
import com.ma.kb.integration.vector.SearchResult;
import com.ma.kb.integration.vector.VectorSearchService;
import com.ma.kb.manager.document.DocumentManager;
import com.ma.kb.manager.document.bo.DocumentBO;
import com.ma.kb.manager.document.bo.DocumentChunkBO;
import com.ma.kb.manager.space.SpaceManager;
import com.ma.kb.manager.space.bo.SpaceBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private VectorSearchService vectorSearchService;
    @Mock
    private ChatModelService chatModelService;
    @Mock
    private SpaceManager spaceManager;
    @Mock
    private DocumentManager documentManager;

    private RagService ragService;

    @BeforeEach
    void setUp() {
        ragService = new RagService(vectorSearchService, chatModelService, new PromptBuilder(), spaceManager, documentManager);
    }

    @Test
    void askFallsBackToKeywordChunksWhenVectorHasNoResult() {
        SpaceBO space = new SpaceBO();
        space.setTopK(3);
        space.setSimilarityThreshold(new BigDecimal("0.7"));
        DocumentChunkBO chunk = new DocumentChunkBO();
        chunk.setId(10L);
        chunk.setDocumentId(20L);
        chunk.setChunkIndex(1);
        chunk.setContent("前端对外固定端口为 1008，后端通过内部网络访问。");
        DocumentBO document = new DocumentBO();
        document.setId(20L);
        document.setFileName("部署手册.md");

        when(spaceManager.getById(1L)).thenReturn(space);
        when(vectorSearchService.isEnabled()).thenReturn(true);
        when(vectorSearchService.embed(anyString())).thenReturn(new float[]{1.0f});
        when(vectorSearchService.search(any(), eq(1L), eq(3), eq(new BigDecimal("0.7")))).thenReturn(List.of());
        when(documentManager.searchChunksByKeywords(eq(1L), anyList(), eq(12))).thenReturn(List.of(chunk));
        when(documentManager.getById(20L)).thenReturn(document);
        when(chatModelService.chat(anyString(), contains("前端对外固定端口为 1008")))
                .thenReturn(new ChatModelService.ChatResponse("前端端口是 1008。", "mock", 10, 5));

        RagService.RagResult result = ragService.ask("部署时前端端口是多少？", 1L);

        assertEquals("前端端口是 1008。", result.answer());
        assertEquals(1, result.citations().size());
        assertEquals("部署手册.md", result.citations().getFirst().getDocumentName());
        assertNotNull(result.diagnostics());
        assertEquals("KEYWORD", result.diagnostics().retrievalMode());
        assertTrue(result.diagnostics().keywordFallbackUsed());
        assertTrue(result.diagnostics().enteredPrompt());
        assertEquals(1, result.diagnostics().hitCount());
    }

    @Test
    void askMergesVectorAndKeywordResultsWithDiagnostics() {
        SpaceBO space = new SpaceBO();
        space.setTopK(3);
        space.setSimilarityThreshold(new BigDecimal("0.7"));

        SearchResult vectorResult = new SearchResult();
        vectorResult.setChunkId(10L);
        vectorResult.setDocumentId(20L);
        vectorResult.setDocumentName("部署手册.md");
        vectorResult.setChunkIndex(1);
        vectorResult.setContent("后端服务通过 8080 暴露内部健康检查。");
        vectorResult.setScore(new BigDecimal("0.82"));

        DocumentChunkBO keywordChunk = new DocumentChunkBO();
        keywordChunk.setId(11L);
        keywordChunk.setDocumentId(20L);
        keywordChunk.setChunkIndex(2);
        keywordChunk.setContent("前端对外固定端口为 1008。");
        DocumentBO document = new DocumentBO();
        document.setId(20L);
        document.setFileName("部署手册.md");

        when(spaceManager.getById(1L)).thenReturn(space);
        when(vectorSearchService.isEnabled()).thenReturn(true);
        when(vectorSearchService.embed(anyString())).thenReturn(new float[]{1.0f});
        when(vectorSearchService.search(any(), eq(1L), eq(3), eq(new BigDecimal("0.7")))).thenReturn(List.of(vectorResult));
        when(documentManager.searchChunksByKeywords(eq(1L), anyList(), eq(12))).thenReturn(List.of(keywordChunk));
        when(documentManager.getById(20L)).thenReturn(document);
        when(chatModelService.chat(anyString(), contains("前端对外固定端口为 1008")))
                .thenReturn(new ChatModelService.ChatResponse("前端端口是 1008。", "mock", 12, 6));

        RagService.RagResult result = ragService.ask("部署时前端端口是多少？", 1L);

        assertEquals("前端端口是 1008。", result.answer());
        assertEquals(2, result.citations().size());
        assertEquals("HYBRID", result.diagnostics().retrievalMode());
        assertFalse(result.diagnostics().keywordFallbackUsed());
        assertEquals(2, result.diagnostics().hitCount());
    }
}
