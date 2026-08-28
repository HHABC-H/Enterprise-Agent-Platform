package com.agent.infrastructure.memory;

import com.agent.memory.LongTermMemory;
import com.agent.memory.LongTermMemoryStore;
import com.agent.memory.MemoryType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class InMemoryLongTermMemoryStore implements LongTermMemoryStore {
    private final List<Value> values = new ArrayList<>();
    @Override public synchronized void save(String tenantId, String userId, String sessionId, String content, float[] embedding, MemoryType type, double importance, Instant createdAt, String source, double confidence, Instant expiresAt, String dedupeKey, String conflictKey) {
        if (values.stream().anyMatch(value -> value.tenantId.equals(tenantId) && value.userId.equals(userId) && value.memory.dedupeKey().equals(dedupeKey))) return;
        if (!conflictKey.isBlank()) values.replaceAll(value -> value.tenantId.equals(tenantId) && value.userId.equals(userId) && conflictKey.equals(value.memory.conflictKey()) && !value.memory.obsolete()
                ? new Value(value.tenantId, value.userId, new LongTermMemory(value.memory.content(), value.memory.sessionId(), value.memory.createdAt(), value.memory.type(), value.memory.importance(), value.memory.score(), value.memory.source(), value.memory.confidence(), value.memory.expiresAt(), true, dedupeKey, value.memory.dedupeKey(), value.memory.conflictKey())) : value);
        values.add(new Value(tenantId, userId, new LongTermMemory(content, sessionId, createdAt, type, importance, 1.0, source, confidence, expiresAt, false, null, dedupeKey, conflictKey)));
    }
    @Override public synchronized List<LongTermMemory> retrieve(String tenantId, String userId, float[] embedding, int limit) { Instant now = Instant.now(); return values.stream().filter(value -> value.tenantId.equals(tenantId) && value.userId.equals(userId) && !value.memory.obsolete() && (value.memory.expiresAt() == null || value.memory.expiresAt().isAfter(now))).map(Value::memory).sorted(Comparator.comparing(LongTermMemory::createdAt).reversed()).limit(limit).toList(); }
    @Override public synchronized void cleanup(Instant olderThan, double maximumImportance) { values.removeIf(value -> value.memory.createdAt().isBefore(olderThan) && value.memory.importance() < maximumImportance); }
    private record Value(String tenantId, String userId, LongTermMemory memory) { }
}
