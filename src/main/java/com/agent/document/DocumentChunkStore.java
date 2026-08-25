/**
 * 本文件定义 {@code DocumentChunkStore}，负责文档解析、切分、索引及父子块模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.document;

import java.util.List;
import java.util.Optional;

public interface DocumentChunkStore {

    void save(String documentId, List<ParentChunk> parents, List<Chunk> chunks);

    default void save(String documentId, List<Chunk> chunks) {
        save(documentId, List.of(), chunks);
    }

    List<Chunk> findAll();

    Optional<ParentChunk> findParent(String parentChunkId);

    default void delete(String documentId) { save(documentId, List.of(), List.of()); }
}
