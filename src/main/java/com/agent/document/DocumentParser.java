/**
 * 本文件定义 {@code DocumentParser}，负责文档解析、切分、索引及父子块模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.document;

public interface DocumentParser {

    ParsedDocument parse(String documentId, String content, DocumentMetadata metadata);
}
