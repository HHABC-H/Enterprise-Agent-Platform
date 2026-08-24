package com.agent.document;

import org.springframework.stereotype.Component;

@Component
public class MarkdownDocumentParser implements DocumentParser {

    @Override
    public ParsedDocument parse(String documentId, String content, DocumentMetadata metadata) {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("文档 ID 不能为空。");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Markdown 内容不能为空。");
        }
        if (metadata == null || metadata.tenantId() == null || metadata.tenantId().isBlank()) {
            throw new IllegalArgumentException("租户 ID 不能为空。");
        }
        return new ParsedDocument(documentId, content.replace("\r\n", "\n"), metadata);
    }
}
