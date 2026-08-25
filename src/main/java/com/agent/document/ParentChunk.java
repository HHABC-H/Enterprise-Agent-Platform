/**
 * 本文件定义 {@code ParentChunk}，负责文档解析、切分、索引及父子块模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.document;

import java.util.List;

/** 文档标题节的父上下文，子块只负责检索。 */
public record ParentChunk(String parentChunkId, String documentId, String content, List<String> headingPath,
                          DocumentMetadata metadata) {
    public ParentChunk {
        headingPath = headingPath == null ? List.of() : List.copyOf(headingPath);
    }
}
