package com.agent.memory;

import java.time.Instant;
import java.util.List;

public interface LongTermMemoryStore {
    void save(String tenantId, String userId, String sessionId, String content, float[] embedding, MemoryType type, double importance,
              Instant createdAt, String source, double confidence, Instant expiresAt, String dedupeKey, String conflictKey);
    List<LongTermMemory> retrieve(String tenantId, String userId, float[] embedding, int limit);
    void cleanup(Instant olderThan, double maximumImportance);
}
