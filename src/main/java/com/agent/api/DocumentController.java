package com.agent.api;

import com.agent.document.Chunk;
import com.agent.document.DocumentMetadata;
import com.agent.document.DocumentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService documentService;
    public DocumentController(DocumentService documentService) { this.documentService = documentService; }
    @PostMapping("/markdown")
    public ApiResponse<DocumentIngestionResponse> ingest(@Valid @RequestBody MarkdownDocumentRequest request) {
        String documentId = request.documentId() == null || request.documentId().isBlank() ? UUID.randomUUID().toString() : request.documentId();
        List<Chunk> chunks = documentService.ingest(documentId, request.markdown(), new DocumentMetadata(request.tenantId(), request.source(),
                request.version(), request.permissionTags(), request.allowedUserIds()));
        return ApiResponse.of(new DocumentIngestionResponse(documentId, chunks.size(), chunks));
    }
}
