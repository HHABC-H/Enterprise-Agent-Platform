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
