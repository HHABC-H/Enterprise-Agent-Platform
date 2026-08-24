package com.agent.document;

import java.util.List;

/** 一次版本化重建生成的父子索引。 */
public record DocumentIndex(String documentId, List<ParentChunk> parents, List<Chunk> chunks) {
    public DocumentIndex {
        parents = List.copyOf(parents);
        chunks = List.copyOf(chunks);
    }
}
