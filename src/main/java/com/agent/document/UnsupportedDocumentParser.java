/**
 * 本文件定义 {@code UnsupportedDocumentParser}，负责文档解析、切分、索引及父子块模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
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
