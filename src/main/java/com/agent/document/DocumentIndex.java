/**
 * 本文件定义 {@code DocumentIndex}，负责文档解析、切分、索引及父子块模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.document;

import java.util.List;

/** 一次版本化重建生成的父子索引。 */
public record DocumentIndex(String documentId, List<ParentChunk> parents, List<Chunk> chunks) {
    public DocumentIndex {
        parents = List.copyOf(parents);
        chunks = List.copyOf(chunks);
    }
}
