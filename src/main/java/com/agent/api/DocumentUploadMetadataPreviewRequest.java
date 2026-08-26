package com.agent.api;

import jakarta.validation.constraints.NotBlank;

/** 上传文件元数据预览请求；仅使用文件名，不上传正文。 */
public record DocumentUploadMetadataPreviewRequest(
        @NotBlank(message = "tenantId 不能为空") String tenantId,
        @NotBlank(message = "originalFileName 不能为空") String originalFileName) {
}
