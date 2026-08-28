package com.agent.infrastructure.memory;

import com.agent.memory.ChatSession;
import com.agent.memory.ChatSessionStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** local 环境的会话元数据存储，语义与 Redis 实现保持一致。 */
@Component
@ConditionalOnProperty(prefix = "ai-platform.memory", name = "type", havingValue = "memory", matchIfMissing = true)
public class InMemoryChatSessionStore implements ChatSessionStore {
    private final Map<String, SessionValue> values = new ConcurrentHashMap<>();
    private final Clock clock;
    public InMemoryChatSessionStore(Clock clock) { this.clock = clock; }
    @Override public ChatSession create(String tenantId, String userId, String sessionId, Duration ttl) {
        Instant now = clock.instant(); ChatSession session = new ChatSession(sessionId, "新对话", now, now, false, null, null);
        values.put(key(tenantId, userId, sessionId), new SessionValue(tenantId, userId, session, now.plus(ttl))); return session;
    }
    @Override public void touch(String tenantId, String userId, String sessionId, String title, Duration ttl) {
        Instant now = clock.instant(); values.compute(key(tenantId, userId, sessionId), (ignored, value) -> {
            ChatSession old = value == null ? new ChatSession(sessionId, title, now, now, false, null, null) : value.session();
            String actualTitle = "新对话".equals(old.title()) ? title : old.title();
            return new SessionValue(tenantId, userId, new ChatSession(sessionId, actualTitle, old.createdAt(), now, false, null, old.lastSummarizedAt()), now.plus(ttl));
        });
    }
    @Override public List<ChatSession> list(String tenantId, String userId) { return owned(tenantId, userId).stream().map(SessionValue::session).sorted(Comparator.comparing(ChatSession::updatedAt).reversed()).toList(); }
    @Override public Optional<ChatSession> find(String tenantId, String userId, String sessionId) { return Optional.ofNullable(valid(values.get(key(tenantId, userId, sessionId)))).map(SessionValue::session); }
    @Override public boolean close(String tenantId, String userId, String sessionId, Duration ttl) {
        Instant now = clock.instant(); return values.computeIfPresent(key(tenantId, userId, sessionId), (ignored, value) -> new SessionValue(tenantId, userId,
                new ChatSession(sessionId, value.session().title(), value.session().createdAt(), now, true, now, value.session().lastSummarizedAt()), now.plus(ttl))) != null;
    }
    @Override public List<SessionCandidate> findPendingSummary(Instant activeSince) {
        return values.values().stream().map(this::valid).filter(java.util.Objects::nonNull).filter(value -> value.session().updatedAt().isAfter(activeSince) || value.session().closed())
                .filter(value -> value.session().lastSummarizedAt() == null || value.session().lastSummarizedAt().isBefore(value.session().updatedAt()))
                .map(value -> new SessionCandidate(value.tenantId(), value.userId(), value.session())).toList();
    }
    @Override public boolean markSummarized(String tenantId, String userId, String sessionId, Instant expectedUpdatedAt, Instant summarizedAt, Duration ttl) {
        String key = key(tenantId, userId, sessionId); final boolean[] changed = {false}; values.computeIfPresent(key, (ignored, value) -> {
            if (!value.session().updatedAt().equals(expectedUpdatedAt)) return value;
            changed[0] = true; return new SessionValue(tenantId, userId, new ChatSession(sessionId, value.session().title(), value.session().createdAt(), value.session().updatedAt(), value.session().closed(), value.session().closedAt(), summarizedAt), clock.instant().plus(ttl));
        }); return changed[0];
    }
    private List<SessionValue> owned(String tenantId, String userId) { return values.values().stream().map(this::valid).filter(java.util.Objects::nonNull).filter(value -> value.tenantId().equals(tenantId) && value.userId().equals(userId)).toList(); }
    private SessionValue valid(SessionValue value) { if (value != null && clock.instant().isAfter(value.expiresAt())) { values.remove(key(value.tenantId(), value.userId(), value.session().sessionId())); return null; } return value; }
    private String key(String tenantId, String userId, String sessionId) { return tenantId + '\u0000' + userId + '\u0000' + sessionId; }
    private record SessionValue(String tenantId, String userId, ChatSession session, Instant expiresAt) { }
}
