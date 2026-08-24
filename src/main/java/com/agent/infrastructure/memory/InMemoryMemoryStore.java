package com.agent.infrastructure.memory;

import com.agent.memory.MemoryEntry;
import com.agent.memory.MemoryStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai-platform.memory", name = "type", havingValue = "memory", matchIfMissing = true)
public class InMemoryMemoryStore implements MemoryStore {
    private final ConcurrentHashMap<String, SessionMemory> sessions = new ConcurrentHashMap<>();
    private final Clock clock;
    public InMemoryMemoryStore(Clock clock) { this.clock = clock; }
    @Override
    public List<MemoryEntry> read(String sessionId) {
        SessionMemory session = sessions.get(key(sessionId));
        if (session == null || session.expired(clock)) {
            sessions.remove(key(sessionId));
            return List.of();
        }
        return List.copyOf(session.entries);
    }
    @Override
    public void append(String sessionId, MemoryEntry entry, int maxMessages, Duration ttl) {
        sessions.compute(key(sessionId), (key, current) -> {
            SessionMemory session = current == null || current.expired(clock) ? new SessionMemory() : current;
            session.entries.addLast(entry);
            while (session.entries.size() > maxMessages) { session.entries.removeFirst(); }
            session.expiresAt = clock.instant().plus(ttl);
            return session;
        });
    }
    private String key(String sessionId) { return "session:" + sessionId; }
    private static final class SessionMemory {
        private final ArrayDeque<MemoryEntry> entries = new ArrayDeque<>();
        private Instant expiresAt = Instant.EPOCH;
        private boolean expired(Clock clock) { return clock.instant().isAfter(expiresAt); }
    }
}
