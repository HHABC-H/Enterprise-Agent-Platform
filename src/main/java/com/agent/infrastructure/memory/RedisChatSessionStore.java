package com.agent.infrastructure.memory;

import com.agent.memory.ChatSession;
import com.agent.memory.ChatSessionStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Redis 使用有序索引查询活跃会话，不扫描未知键。 */
@Component
@ConditionalOnProperty(prefix = "ai-platform.memory", name = "type", havingValue = "redis")
public class RedisChatSessionStore implements ChatSessionStore {
    private static final String ACTIVE_INDEX = "chat:sessions:active";
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final Clock clock;
    public RedisChatSessionStore(StringRedisTemplate redis, ObjectMapper mapper, Clock clock) { this.redis = redis; this.mapper = mapper; this.clock = clock; }
    @Override public ChatSession create(String tenantId, String userId, String sessionId, Duration ttl) {
        Instant now = clock.instant(); ChatSession session = new ChatSession(sessionId, "新对话", now, now, false, null, null); save(tenantId, userId, session, ttl); return session;
    }
    @Override public void touch(String tenantId, String userId, String sessionId, String title, Duration ttl) {
        Instant now = clock.instant(); ChatSession current = find(tenantId, userId, sessionId).orElse(new ChatSession(sessionId, title, now, now, false, null, null));
        save(tenantId, userId, new ChatSession(sessionId, "新对话".equals(current.title()) ? title : current.title(), current.createdAt(), now, false, null, current.lastSummarizedAt()), ttl);
    }
    @Override public List<ChatSession> list(String tenantId, String userId) {
        return redis.opsForZSet().reverseRange(userIndex(tenantId, userId), 0, -1).stream().map(id -> find(tenantId, userId, id)).flatMap(Optional::stream).sorted(Comparator.comparing(ChatSession::updatedAt).reversed()).toList();
    }
    @Override public Optional<ChatSession> find(String tenantId, String userId, String sessionId) {
        String value = redis.opsForValue().get(sessionKey(tenantId, userId, sessionId)); if (value == null) return Optional.empty();
        try { return Optional.of(mapper.readValue(value, ChatSession.class)); } catch (Exception exception) { throw new IllegalStateException("会话元数据读取失败。", exception); }
    }
    @Override public boolean close(String tenantId, String userId, String sessionId, Duration ttl) {
        Optional<ChatSession> current = find(tenantId, userId, sessionId); if (current.isEmpty()) return false; Instant now = clock.instant();
        ChatSession old = current.get(); save(tenantId, userId, new ChatSession(sessionId, old.title(), old.createdAt(), now, true, now, old.lastSummarizedAt()), ttl); return true;
    }
    @Override public List<SessionCandidate> findPendingSummary(Instant activeSince) {
        return redis.opsForZSet().rangeByScore(ACTIVE_INDEX, activeSince.toEpochMilli(), Double.POSITIVE_INFINITY).stream().map(this::decode)
                .map(owner -> find(owner.tenantId(), owner.userId(), owner.sessionId()).map(session -> new SessionCandidate(owner.tenantId(), owner.userId(), session))).flatMap(Optional::stream)
                .filter(candidate -> candidate.session().lastSummarizedAt() == null || candidate.session().lastSummarizedAt().isBefore(candidate.session().updatedAt())).toList();
    }
    @Override public boolean markSummarized(String tenantId, String userId, String sessionId, Instant expectedUpdatedAt, Instant summarizedAt, Duration ttl) {
        Optional<ChatSession> current = find(tenantId, userId, sessionId); if (current.isEmpty() || !current.get().updatedAt().equals(expectedUpdatedAt)) return false;
        ChatSession old = current.get(); save(tenantId, userId, new ChatSession(sessionId, old.title(), old.createdAt(), old.updatedAt(), old.closed(), old.closedAt(), summarizedAt), ttl); return true;
    }
    private void save(String tenantId, String userId, ChatSession session, Duration ttl) {
        try { redis.opsForValue().set(sessionKey(tenantId, userId, session.sessionId()), mapper.writeValueAsString(session), ttl); }
        catch (Exception exception) { throw new IllegalStateException("会话元数据写入失败。", exception); }
        redis.opsForZSet().add(userIndex(tenantId, userId), session.sessionId(), session.updatedAt().toEpochMilli());
        redis.opsForZSet().add(ACTIVE_INDEX, encode(tenantId, userId, session.sessionId()), session.updatedAt().toEpochMilli());
    }
    private String sessionKey(String tenantId, String userId, String sessionId) { return "chat:session:" + encode(tenantId, userId, sessionId); }
    private String userIndex(String tenantId, String userId) { return "chat:sessions:user:" + encode(tenantId, userId, ""); }
    private String encode(String tenantId, String userId, String sessionId) { return Base64.getUrlEncoder().withoutPadding().encodeToString((tenantId + "\u0000" + userId + "\u0000" + sessionId).getBytes(StandardCharsets.UTF_8)); }
    private Owner decode(String value) { String[] parts = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8).split("\u0000", -1); return new Owner(parts[0], parts[1], parts[2]); }
    private record Owner(String tenantId, String userId, String sessionId) { }
}
