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
