package com.ma.agent.knowledge.document;

import com.ma.agent.knowledge.dto.DocumentUploadResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "agent.document", name = "provider", havingValue = "mock", matchIfMissing = true)
class MockDocumentService implements DocumentService {

    @Override
    public DocumentUploadResponse upload(MultipartFile file, String kbId) {
        return new DocumentUploadResponse(
                UUID.randomUUID().toString(),
                file.getOriginalFilename(),
                "accepted"
        );
    }

    @Override
    public List<DocumentInfo> listDocuments() {
        return List.of();
    }

    @Override
    public List<DocumentInfo> listDocumentsByKbId(String kbId) {
        return List.of();
    }

    @Override
    public Optional<String> getContent(String documentId) {
        return Optional.empty();
    }

    @Override
    public void delete(String documentId) {
        // no-op
    }

    @Override
    public void updateCategory(String documentId, String category) {
        // no-op
    }
}
