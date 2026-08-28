package com.agent.memory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ChatSessionStore {
    ChatSession create(String tenantId, String userId, String sessionId, Duration ttl);
    void touch(String tenantId, String userId, String sessionId, String title, Duration ttl);
    List<ChatSession> list(String tenantId, String userId);
    Optional<ChatSession> find(String tenantId, String userId, String sessionId);
    boolean close(String tenantId, String userId, String sessionId, Duration ttl);
    List<SessionCandidate> findPendingSummary(Instant activeSince);
    boolean markSummarized(String tenantId, String userId, String sessionId, Instant expectedUpdatedAt, Instant summarizedAt, Duration ttl);
    record SessionCandidate(String tenantId, String userId, ChatSession session) { }
}
