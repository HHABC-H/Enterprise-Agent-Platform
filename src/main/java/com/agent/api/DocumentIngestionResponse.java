package com.agent.api;

import com.agent.document.Chunk;
import java.util.List;

public record DocumentIngestionResponse(String documentId, int chunkCount, List<Chunk> chunks) {
    public DocumentIngestionResponse { chunks = List.copyOf(chunks); }
}
