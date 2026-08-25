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
import java.util.UUID;
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
    public DocumentController(DocumentService documentService, DocumentIngestionFacade ingestionFacade) {
        this.documentService = documentService; this.ingestionFacade = ingestionFacade;
    }

    /** 保存 Markdown 当前版本并发布重建事件；未指定文档 ID 时由服务端生成。 */
    @PostMapping("/markdown")
    public ApiResponse<DocumentIngestionResponse> ingest(@Valid @RequestBody MarkdownDocumentRequest request) {
        String documentId = request.documentId() == null || request.documentId().isBlank() ? UUID.randomUUID().toString() : request.documentId();
        DocumentMetadata metadata = new DocumentMetadata(request.tenantId(), request.source(), request.version(), request.permissionTags(), request.allowedUserIds());
        List<Chunk> chunks = ingestionFacade.upsert(documentId, request.markdown(), metadata);
        return ApiResponse.of(new DocumentIngestionResponse(documentId, chunks.size(), chunks));
    }

    /** 查询最近一次入库任务，供异步部署场景轮询处理进度。 */
    @GetMapping("/{documentId}/ingestion-status")
    public ApiResponse<IngestionTaskStatus> status(@PathVariable String documentId, @RequestParam String tenantId) {
        return ApiResponse.of(ingestionFacade.status(tenantId, documentId));
    }
}
