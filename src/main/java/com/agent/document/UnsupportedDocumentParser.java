package com.agent.document;

public class UnsupportedDocumentParser implements DocumentParser {

    private final String type;

    public UnsupportedDocumentParser(String type) {
        this.type = type;
    }

    @Override
    public ParsedDocument parse(String documentId, String content, DocumentMetadata metadata) {
        throw new UnsupportedOperationException(type + " 解析在 P0 阶段未实现，请提交 Markdown 文本。");
    }
}
