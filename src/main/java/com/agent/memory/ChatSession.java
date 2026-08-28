package com.agent.memory;

import java.time.Instant;

/** 已持久化的会话元数据，不包含对话正文。 */
public record ChatSession(String sessionId, String title, Instant createdAt, Instant updatedAt,
                          boolean closed, Instant closedAt, Instant lastSummarizedAt) {
}
