/**
 * 本文件定义 {@code DocumentController}，负责对外 HTTP 接口、请求模型与响应模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.api;

import com.agent.document.Chunk;
import com.agent.document.DocumentMetadata;
import com.agent.document.DocumentService;
import com.agent.ingestion.DocumentIngestionFacade;
import com.agent.ingestion.IngestionTaskStatus;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService documentService;
    private final DocumentIngestionFacade ingestionFacade;
    private final IdentityGuard identityGuard;
    private final DocumentUploadMetadataService uploadMetadata;
    public DocumentController(DocumentService documentService, DocumentIngestionFacade ingestionFacade, IdentityGuard identityGuard,
                              DocumentUploadMetadataService uploadMetadata) {
        this.documentService = documentService; this.ingestionFacade = ingestionFacade; this.identityGuard = identityGuard; this.uploadMetadata = uploadMetadata;
    }

    /** 保存 Markdown 当前版本并发布重建事件；未指定文档 ID 时由服务端生成。 */
    @PostMapping("/markdown")
    public ApiResponse<DocumentIngestionResponse> ingest(@Valid @RequestBody MarkdownDocumentRequest request) {
        identityGuard.assertTenant(request.tenantId());
        DocumentUploadMetadata resolved = resolveMetadata(request);
        String documentId = resolved.documentId();
        DocumentMetadata metadata = new DocumentMetadata(request.tenantId(), resolved.source(), resolved.version(), request.permissionTags(), request.allowedUserIds());
        List<Chunk> chunks = ingestionFacade.upsert(documentId, request.markdown(), metadata);
        return ApiResponse.of(new DocumentIngestionResponse(documentId, metadata.source(), metadata.version(), chunks.size(), chunks));
    }

    /** 根据上传文件名预览本次入库将使用的文档标识、来源与版本。 */
    @PostMapping("/metadata-preview")
    public ApiResponse<DocumentUploadMetadata> preview(@Valid @RequestBody DocumentUploadMetadataPreviewRequest request) {
        identityGuard.assertTenant(request.tenantId());
        return ApiResponse.of(uploadMetadata.resolve(request.tenantId(), request.originalFileName()));
    }

    /** 查询最近一次入库任务，供异步部署场景轮询处理进度。 */
    @GetMapping("/{documentId}/ingestion-status")
    public ApiResponse<IngestionTaskStatus> status(@PathVariable String documentId, @RequestParam String tenantId) {
        identityGuard.assertTenant(tenantId);
        return ApiResponse.of(ingestionFacade.status(tenantId, documentId));
    }

    private DocumentUploadMetadata resolveMetadata(MarkdownDocumentRequest request) {
        if (request.originalFileName() != null && !request.originalFileName().isBlank()) {
            return uploadMetadata.resolve(request.tenantId(), request.originalFileName());
        }
        if (request.source() == null || request.source().isBlank() || request.version() == null || request.version().isBlank()) {
            throw new IllegalArgumentException("未提供上传文件名时，source 和 version 不能为空。");
        }
        String documentId = request.documentId() == null || request.documentId().isBlank() ? java.util.UUID.randomUUID().toString() : request.documentId();
        return new DocumentUploadMetadata(documentId, request.source(), request.version(), null);
    }
}
