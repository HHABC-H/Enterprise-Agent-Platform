package com.agent.memory;

import com.agent.config.AgentPlatformProperties;
import com.agent.observability.BusinessOperation;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ChatSessionService {
    private final ChatSessionStore store;
    private final AgentPlatformProperties properties;
    private final Clock clock;

    public ChatSessionService(ChatSessionStore store, AgentPlatformProperties properties, Clock clock) { this.store = store; this.properties = properties; this.clock = clock; }
    @BusinessOperation("CHAT_SESSION_CREATE")
    public ChatSession create(String tenantId, String userId) { return store.create(tenantId, userId, UUID.randomUUID().toString(), ttl()); }
    public void touch(String tenantId, String userId, String sessionId, String firstQuestion) {
        String title = firstQuestion == null || firstQuestion.isBlank() ? "新对话" : firstQuestion.trim().substring(0, Math.min(30, firstQuestion.trim().length()));
        store.touch(tenantId, userId, sessionId, title, ttl());
    }
    public List<ChatSession> list(String tenantId, String userId) { return store.list(tenantId, userId); }
    @BusinessOperation("CHAT_SESSION_CLOSE")
    public boolean close(String tenantId, String userId, String sessionId) { return store.close(tenantId, userId, sessionId, ttl()); }
    public Optional<ChatSessionStore.SessionCandidate> candidate(String tenantId, String userId, String sessionId) { return store.find(tenantId, userId, sessionId).map(session -> new ChatSessionStore.SessionCandidate(tenantId, userId, session)); }
    public List<ChatSessionStore.SessionCandidate> pendingSummary() { return store.findPendingSummary(clock.instant().minus(Duration.ofHours(3))); }
    public boolean markSummarized(ChatSessionStore.SessionCandidate candidate) { return store.markSummarized(candidate.tenantId(), candidate.userId(), candidate.session().sessionId(), candidate.session().updatedAt(), clock.instant(), ttl()); }
    private Duration ttl() { return Duration.ofSeconds(properties.getMemory().getTtlSeconds()); }
}
