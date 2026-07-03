package com.ma.kb.service.document;

import com.ma.kb.common.enums.DocumentStatusEnum;
import com.ma.kb.common.enums.FileTypeEnum;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文档解析、切片、向量化入库流程。
 */
@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final DocumentManager documentManager;
    private final SpaceManager spaceManager;
    private final StorageService storageService;
    private final TextCleaner textCleaner;
    private final DocumentChunker documentChunker;
    private final VectorSearchService vectorSearchService;
    private final MarkdownDocumentReader markdownDocumentReader;
    private final TxtDocumentReader txtDocumentReader;
    private final PdfDocumentReader pdfDocumentReader;

    public DocumentIngestionService(DocumentManager documentManager,
                                    SpaceManager spaceManager,
                                    StorageService storageService,
                                    TextCleaner textCleaner,
                                    DocumentChunker documentChunker,
                                    VectorSearchService vectorSearchService,
                                    MarkdownDocumentReader markdownDocumentReader,
                                    TxtDocumentReader txtDocumentReader,
                                    PdfDocumentReader pdfDocumentReader) {
        this.documentManager = documentManager;
        this.spaceManager = spaceManager;
        this.storageService = storageService;
        this.textCleaner = textCleaner;
        this.documentChunker = documentChunker;
        this.vectorSearchService = vectorSearchService;
        this.markdownDocumentReader = markdownDocumentReader;
        this.txtDocumentReader = txtDocumentReader;
        this.pdfDocumentReader = pdfDocumentReader;
    }

    public void ingest(Long documentId) {
        DocumentBO document = documentManager.getById(documentId);
        if (document == null) {
            return;
        }
        try {
            updateStatus(document, DocumentStatusEnum.PARSING, null);
            String rawText = readText(document);
            String cleanedText = textCleaner.clean(rawText);
            if (cleanedText.isBlank()) {
                throw new RuntimeException("文档内容为空，无法建立索引");
            }

            SpaceBO space = spaceManager.getById(document.getSpaceId());
            int chunkSize = space != null && space.getChunkSize() != null ? space.getChunkSize() : 800;
            int chunkOverlap = space != null && space.getChunkOverlap() != null ? space.getChunkOverlap() : 120;
            List<String> texts = documentChunker.chunk(cleanedText, chunkSize, chunkOverlap);
            if (texts.isEmpty()) {
                throw new RuntimeException("文档切片结果为空");
            }

            updateStatus(document, DocumentStatusEnum.INDEXING, null);
            vectorSearchService.deleteByDocumentId(document.getId());
            documentManager.deleteChunksByDocumentId(document.getId());

            List<DocumentChunkBO> chunks = buildChunks(document, texts);
            documentManager.saveChunks(chunks);
            for (DocumentChunkBO chunk : chunks) {
                float[] embedding = vectorSearchService.embed(chunk.getContent());
                String vectorId = vectorSearchService.store(
                        chunk.getId(),
                        chunk.getSpaceId(),
                        chunk.getDocumentId(),
                        chunk.getContent(),
                        embedding,
                        Map.of(
                                "documentName", document.getFileName(),
                                "chunkIndex", chunk.getChunkIndex(),
                                "pageNumber", chunk.getPageNumber() == null ? 0 : chunk.getPageNumber()
                        )
                );
                chunk.setVectorId(vectorId);
                documentManager.updateChunk(chunk);
            }

            updateStatus(document, DocumentStatusEnum.COMPLETED, null);
            log.info("文档入库完成: documentId={}, chunks={}", document.getId(), chunks.size());
        } catch (Exception e) {
            log.error("文档入库失败: documentId={}", document.getId(), e);
            updateStatus(document, DocumentStatusEnum.FAILED, e.getMessage());
        }
    }

    private String readText(DocumentBO document) {
        try (InputStream inputStream = storageService.download(document.getStorageBucket(), document.getStorageObjectKey())) {
            if (FileTypeEnum.MARKDOWN.getCode().equals(document.getFileType())) {
                return markdownDocumentReader.read(inputStream, document.getFileName());
            }
            if (FileTypeEnum.TXT.getCode().equals(document.getFileType())) {
                return txtDocumentReader.read(inputStream, document.getFileName());
            }
            if (FileTypeEnum.PDF.getCode().equals(document.getFileType())) {
                return pdfDocumentReader.read(inputStream, document.getFileName());
            }
            throw new RuntimeException("暂不支持解析该文件类型: " + document.getFileType());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("文档读取失败: " + e.getMessage(), e);
        }
    }

    private List<DocumentChunkBO> buildChunks(DocumentBO document, List<String> texts) {
        List<DocumentChunkBO> chunks = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            DocumentChunkBO chunk = new DocumentChunkBO();
            chunk.setSpaceId(document.getSpaceId());
            chunk.setDocumentId(document.getId());
            chunk.setChunkIndex(i);
            chunk.setContent(texts.get(i));
            chunk.setPageNumber(null);
            chunk.setTokenCount(documentChunker.estimateTokenCount(texts.get(i)));
            chunks.add(chunk);
        }
        return chunks;
    }

    private void updateStatus(DocumentBO document, DocumentStatusEnum status, String errorMessage) {
        documentManager.updateStatus(document.getId(), status.getCode(), errorMessage == null ? "" : errorMessage);
        document.setParseStatus(status.getCode());
        document.setErrorMessage(errorMessage);
    }
}
