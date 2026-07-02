package com.ma.kb.service.document.impl;

import com.ma.kb.common.enums.DocumentStatusEnum;
import com.ma.kb.common.enums.SpaceRoleEnum;
import com.ma.kb.common.exception.BusinessException;
import com.ma.kb.integration.storage.StorageService;
import com.ma.kb.manager.document.DocumentManager;
import com.ma.kb.manager.document.bo.DocumentBO;
import com.ma.kb.manager.space.SpaceManager;
import com.ma.kb.service.document.converter.DocumentDTOConverter;
import com.ma.kb.service.document.dto.DocumentUploadResponse;
import com.ma.kb.service.document.dto.DocumentVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private DocumentManager documentManager;
    @Mock
    private SpaceManager spaceManager;
    @Mock
    private StorageService storageService;
    @Mock
    private DocumentDTOConverter documentDTOConverter;

    private DocumentServiceImpl documentService;

    private static final Long USER_ID = 1L;
    private static final Long SPACE_ID = 100L;
    private static final Long DOC_ID = 200L;

    @BeforeEach
    void setUp() {
        documentService = new DocumentServiceImpl(documentManager, spaceManager, storageService, documentDTOConverter);
    }

    @Test
    void uploadSuccess() {
        when(spaceManager.getMemberRole(SPACE_ID, USER_ID)).thenReturn(SpaceRoleEnum.ADMIN.getCode());
        when(storageService.getDefaultBucket()).thenReturn("kb-documents");

        DocumentBO docBO = buildDocumentBO(DOC_ID, "test.pdf", "PDF");
        when(documentDTOConverter.toBO(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(docBO);
        when(documentManager.create(any())).thenReturn(docBO);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "test content".getBytes());

        DocumentUploadResponse response = documentService.upload(USER_ID, SPACE_ID, file);

        assertNotNull(response);
        assertEquals(DOC_ID, response.documentId());
        assertEquals("test.pdf", response.fileName());
        assertEquals(DocumentStatusEnum.PENDING.getCode(), response.status());
    }

    @Test
    void uploadUnsupportedFileType() {
        when(spaceManager.getMemberRole(SPACE_ID, USER_ID)).thenReturn(SpaceRoleEnum.ADMIN.getCode());

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.exe", "application/octet-stream", "content".getBytes());

        assertThrows(BusinessException.class,
                () -> documentService.upload(USER_ID, SPACE_ID, file));
    }

    @Test
    void listBySpaceIdSuccess() {
        when(spaceManager.getMemberRole(SPACE_ID, USER_ID)).thenReturn(SpaceRoleEnum.READER.getCode());
        when(documentManager.listBySpaceId(SPACE_ID)).thenReturn(List.of(
                buildDocumentBO(DOC_ID, "doc1.pdf", "PDF"),
                buildDocumentBO(DOC_ID + 1, "doc2.txt", "TXT")
        ));

        List<DocumentVO> docs = documentService.listBySpaceId(USER_ID, SPACE_ID);

        assertEquals(2, docs.size());
    }

    @Test
    void deleteSuccess() {
        DocumentBO docBO = buildDocumentBO(DOC_ID, "test.pdf", "PDF");
        when(documentManager.getById(DOC_ID)).thenReturn(docBO);
        when(spaceManager.getMemberRole(SPACE_ID, USER_ID)).thenReturn(SpaceRoleEnum.ADMIN.getCode());

        documentService.delete(USER_ID, DOC_ID);

        verify(storageService).delete(anyString(), anyString());
        verify(documentManager).deleteChunksByDocumentId(DOC_ID);
        verify(documentManager).deleteById(DOC_ID);
    }

    @Test
    void deleteNotFound() {
        when(documentManager.getById(999L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> documentService.delete(USER_ID, 999L));
    }

    @Test
    void reindexSuccess() {
        DocumentBO docBO = buildDocumentBO(DOC_ID, "test.pdf", "PDF");
        when(documentManager.getById(DOC_ID)).thenReturn(docBO);
        when(spaceManager.getMemberRole(SPACE_ID, USER_ID)).thenReturn(SpaceRoleEnum.ADMIN.getCode());

        DocumentBO reindexBO = new DocumentBO();
        reindexBO.setId(DOC_ID);
        reindexBO.setParseStatus("PENDING");
        when(documentDTOConverter.toReindexBO(DOC_ID)).thenReturn(reindexBO);

        documentService.reindex(USER_ID, DOC_ID);

        verify(documentManager).deleteChunksByDocumentId(DOC_ID);
        verify(documentManager).update(any(DocumentBO.class));
    }

    private DocumentBO buildDocumentBO(Long id, String fileName, String fileType) {
        DocumentBO bo = new DocumentBO();
        bo.setId(id);
        bo.setSpaceId(SPACE_ID);
        bo.setFileName(fileName);
        bo.setFileType(fileType);
        bo.setFileSize(1024L);
        bo.setStorageBucket("kb-documents");
        bo.setStorageObjectKey(SPACE_ID + "/" + id + ".pdf");
        bo.setParseStatus(DocumentStatusEnum.PENDING.getCode());
        bo.setUploadedBy(USER_ID);
        return bo;
    }
}
