package com.agent.api;

/** 由上传文件名和当前文档版本生成的受控元数据。 */
public record DocumentUploadMetadata(String documentId, String source, String version, String previousVersion) {
}
