package com.agent.memory;

import java.time.Instant;

/** 长期记忆的脱敏元数据模型。 */
public record LongTermMemory(String content, String sessionId, Instant createdAt, MemoryType type, double importance, double score,
                             String source, double confidence, Instant expiresAt, boolean obsolete, String replacedBy,
                             String dedupeKey, String conflictKey) {
}
