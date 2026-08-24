package com.agent.api;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public record MarkdownDocumentRequest(
        String documentId,
        @NotBlank(message = "tenantId 不能为空") String tenantId,
        @NotBlank(message = "markdown 不能为空") String markdown,
        @NotBlank(message = "source 不能为空") String source,
        @NotBlank(message = "version 不能为空") String version,
        Set<String> permissionTags,
        Set<String> allowedUserIds) {
}
