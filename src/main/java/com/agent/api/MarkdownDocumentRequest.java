/**
 * 本文件定义 {@code MarkdownDocumentRequest}，负责对外 HTTP 接口、请求模型与响应模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
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
