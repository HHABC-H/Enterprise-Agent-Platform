/**
 * 本文件定义 {@code Chunk}，负责文档解析、切分、索引及父子块模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.document;

import java.util.List;

public record Chunk(
        String chunkId,
        String documentId,
        String content,
        List<String> headingPath,
        DocumentMetadata metadata,
        String parentChunkId) {

    public Chunk(String chunkId, String documentId, String content, List<String> headingPath, DocumentMetadata metadata) {
        this(chunkId, documentId, content, headingPath, metadata, null);
    }

    public Chunk {
        headingPath = headingPath == null ? List.of() : List.copyOf(headingPath);
    }
}
