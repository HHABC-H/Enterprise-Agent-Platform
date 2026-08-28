/**
 * 本文件定义 {@code InMemoryMemoryStore}，负责外部基础设施和本地替代实现适配器。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
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
    public synchronized List<MemoryEntry> read(String sessionId) {
        SessionMemory session = sessions.get(key(sessionId));
        if (session == null || session.expired(clock)) {
            sessions.remove(key(sessionId));
            return List.of();
        }
        return List.copyOf(session.entries);
    }
    @Override
    public synchronized void appendTurn(String sessionId, MemoryEntry userEntry, MemoryEntry assistantEntry, int maxMessages, Duration ttl) {
        sessions.compute(key(sessionId), (key, current) -> {
            SessionMemory session = current == null || current.expired(clock) ? new SessionMemory() : current;
            session.entries.addLast(userEntry);
            session.entries.addLast(assistantEntry);
            while (session.entries.size() > maxMessages) { session.entries.removeFirst(); }
            session.expiresAt = clock.instant().plus(ttl);
            return session;
        });
    }
    /** 仅保留给历史测试与旧调用方，新的对话链路必须使用原子双消息追加。 */
    public synchronized void append(String sessionId, MemoryEntry entry, int maxMessages, Duration ttl) {
        sessions.compute(key(sessionId), (key, current) -> {
            SessionMemory session = current == null || current.expired(clock) ? new SessionMemory() : current;
            session.entries.addLast(entry);
            while (session.entries.size() > maxMessages) session.entries.removeFirst();
            session.expiresAt = clock.instant().plus(ttl);
            return session;
        });
    }
    @Override
    public synchronized void replace(String sessionId, List<MemoryEntry> entries, Duration ttl) {
        SessionMemory session = new SessionMemory();
        session.entries.addAll(entries);
        session.expiresAt = clock.instant().plus(ttl);
        sessions.put(key(sessionId), session);
    }
    private String key(String sessionId) { return "session:" + sessionId; }
    private static final class SessionMemory {
        private final ArrayDeque<MemoryEntry> entries = new ArrayDeque<>();
        private Instant expiresAt = Instant.EPOCH;
        private boolean expired(Clock clock) { return clock.instant().isAfter(expiresAt); }
    }
}
