package com.ma.kb.service.document;

import com.ma.kb.common.enums.DocumentStatusEnum;
import com.ma.kb.core.document.DocumentChunker;
import com.ma.kb.core.document.TextCleaner;
import com.ma.kb.integration.documentreader.MarkdownDocumentReader;
import com.ma.kb.integration.documentreader.PdfDocumentReader;
import com.ma.kb.integration.documentreader.TxtDocumentReader;
import com.ma.kb.integration.storage.StorageService;
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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceTest {

    @Mock
    private DocumentManager documentManager;
    @Mock
    private SpaceManager spaceManager;
    @Mock
    private StorageService storageService;
    @Mock
    private TextCleaner textCleaner;
    @Mock
    private DocumentChunker documentChunker;
    @Mock
    private VectorSearchService vectorSearchService;
    @Mock
    private MarkdownDocumentReader markdownDocumentReader;
    @Mock
    private TxtDocumentReader txtDocumentReader;
    @Mock
    private PdfDocumentReader pdfDocumentReader;

    private DocumentIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new DocumentIngestionService(
                documentManager,
                spaceManager,
                storageService,
                textCleaner,
                documentChunker,
                vectorSearchService,
                markdownDocumentReader,
                txtDocumentReader,
                pdfDocumentReader
        );
    }

    @Test
    void ingestCompletesChunksWhenVectorDisabled() throws Exception {
        DocumentBO document = buildDocument();
        SpaceBO space = new SpaceBO();
        space.setChunkSize(800);
        space.setChunkOverlap(120);

        when(documentManager.getById(10L)).thenReturn(document);
        when(storageService.download("kb-documents", "1/test.md"))
                .thenReturn(new ByteArrayInputStream("raw".getBytes(StandardCharsets.UTF_8)));
        when(markdownDocumentReader.read(any(), eq("test.md"))).thenReturn("raw text");
        when(textCleaner.clean("raw text")).thenReturn("clean text");
        when(spaceManager.getById(1L)).thenReturn(space);
        when(documentChunker.chunk("clean text", 800, 120)).thenReturn(List.of("clean text"));
        when(documentChunker.estimateTokenCount("clean text")).thenReturn(3);
        when(vectorSearchService.isEnabled()).thenReturn(false);

        ingestionService.ingest(10L);

        verify(documentManager).saveChunks(argThat(chunks -> chunks.size() == 1));
        verify(vectorSearchService, never()).embed(any());
        verify(vectorSearchService, never()).store(any(), any(), any(), any(), any(), any());
        verify(documentManager).updateStatus(10L, DocumentStatusEnum.COMPLETED.getCode(), "");
    }

    @Test
    void ingestCleansPartialChunksWhenVectorStoreFails() throws Exception {
        DocumentBO document = buildDocument();
        SpaceBO space = new SpaceBO();
        space.setChunkSize(800);
        space.setChunkOverlap(120);

        when(documentManager.getById(10L)).thenReturn(document);
        when(storageService.download("kb-documents", "1/test.md"))
                .thenReturn(new ByteArrayInputStream("raw".getBytes(StandardCharsets.UTF_8)));
        when(markdownDocumentReader.read(any(), eq("test.md"))).thenReturn("raw text");
        when(textCleaner.clean("raw text")).thenReturn("clean text");
        when(spaceManager.getById(1L)).thenReturn(space);
        when(documentChunker.chunk("clean text", 800, 120)).thenReturn(List.of("clean text"));
        when(documentChunker.estimateTokenCount("clean text")).thenReturn(3);
        when(vectorSearchService.isEnabled()).thenReturn(true);
        when(vectorSearchService.embed("clean text")).thenReturn(new float[]{1.0f});
        when(vectorSearchService.store(any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("vector unavailable"));

        ingestionService.ingest(10L);

        verify(documentManager, atLeastOnce()).deleteChunksByDocumentId(10L);
        verify(vectorSearchService, atLeastOnce()).deleteByDocumentId(10L);
        verify(documentManager).updateStatus(10L, DocumentStatusEnum.FAILED.getCode(), "vector unavailable");
    }

    private DocumentBO buildDocument() {
        DocumentBO document = new DocumentBO();
        document.setId(10L);
        document.setSpaceId(1L);
        document.setFileName("test.md");
        document.setFileType("MARKDOWN");
        document.setStorageBucket("kb-documents");
        document.setStorageObjectKey("1/test.md");
        return document;
    }
}
