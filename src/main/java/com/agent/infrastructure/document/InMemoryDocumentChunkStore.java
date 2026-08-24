package com.agent.infrastructure.document;

import com.agent.document.Chunk;
import com.agent.document.DocumentChunkStore;
import com.agent.document.ParentChunk;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryDocumentChunkStore implements DocumentChunkStore {

    private final CopyOnWriteArrayList<Chunk> chunks = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, ParentChunk> parents = new ConcurrentHashMap<>();

    @Override
    public void save(String documentId, List<ParentChunk> newParents, List<Chunk> newChunks) {
        chunks.removeIf(chunk -> chunk.documentId().equals(documentId));
        parents.entrySet().removeIf(entry -> entry.getValue().documentId().equals(documentId));
        newParents.forEach(parent -> parents.put(parent.parentChunkId(), parent));
        chunks.addAll(newChunks);
    }

    @Override
    public List<Chunk> findAll() {
        return List.copyOf(new ArrayList<>(chunks));
    }

    @Override
    public Optional<ParentChunk> findParent(String parentChunkId) { return Optional.ofNullable(parents.get(parentChunkId)); }
}
