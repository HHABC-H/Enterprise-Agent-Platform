package com.agent.infrastructure.memory;

import com.agent.memory.LongTermMemory;
import com.agent.memory.LongTermMemoryStore;
import com.agent.memory.MemoryType;
import java.time.Instant;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile({"docker", "local-docker"})
public class JdbcLongTermMemoryStore implements LongTermMemoryStore {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcLongTermMemoryStore(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public void save(String tenantId, String userId, String sessionId, String content, float[] embedding, MemoryType type, double importance, Instant createdAt, String source, double confidence, Instant expiresAt, String dedupeKey, String conflictKey) {
        if (!conflictKey.isBlank()) jdbc.update("UPDATE user_long_term_memory SET obsolete = TRUE, replaced_by = :dedupeKey WHERE tenant_id = :tenantId AND user_id = :userId AND conflict_key = :conflictKey AND obsolete = FALSE", Map.of("tenantId", tenantId, "userId", userId, "conflictKey", conflictKey, "dedupeKey", dedupeKey));
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", UUID.randomUUID().toString()); parameters.put("tenantId", tenantId); parameters.put("userId", userId); parameters.put("sessionId", sessionId); parameters.put("content", content); parameters.put("embedding", vector(embedding)); parameters.put("type", type.name()); parameters.put("importance", importance); parameters.put("createdAt", java.sql.Timestamp.from(createdAt)); parameters.put("source", source); parameters.put("confidence", confidence); parameters.put("expiresAt", expiresAt == null ? null : java.sql.Timestamp.from(expiresAt)); parameters.put("dedupeKey", dedupeKey); parameters.put("conflictKey", conflictKey);
        jdbc.update("INSERT INTO user_long_term_memory (memory_id, tenant_id, user_id, session_id, content, embedding, memory_type, importance, created_at, source, confidence, expires_at, obsolete, dedupe_key, conflict_key) VALUES (:id, :tenantId, :userId, :sessionId, :content, CAST(:embedding AS vector), :type, :importance, :createdAt, :source, :confidence, :expiresAt, FALSE, :dedupeKey, :conflictKey) ON CONFLICT (tenant_id, user_id, dedupe_key) DO NOTHING", parameters);
    }
    @Override public List<LongTermMemory> retrieve(String tenantId, String userId, float[] embedding, int limit) { String sql = "SELECT content, session_id, created_at, memory_type, importance, 1 - (embedding <=> CAST(:embedding AS vector)) AS score, source, confidence, expires_at, obsolete, replaced_by, dedupe_key, conflict_key FROM user_long_term_memory WHERE tenant_id = :tenantId AND user_id = :userId AND obsolete = FALSE AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP) ORDER BY embedding <=> CAST(:embedding AS vector) LIMIT :limit"; return jdbc.query(sql, Map.of("tenantId", tenantId, "userId", userId, "embedding", vector(embedding), "limit", limit), (rs, row) -> new LongTermMemory(rs.getString("content"), rs.getString("session_id"), rs.getTimestamp("created_at").toInstant(), MemoryType.valueOf(rs.getString("memory_type")), rs.getDouble("importance"), rs.getDouble("score"), rs.getString("source"), rs.getDouble("confidence"), rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toInstant(), rs.getBoolean("obsolete"), rs.getString("replaced_by"), rs.getString("dedupe_key"), rs.getString("conflict_key"))); }
    @Override public void cleanup(Instant olderThan, double maximumImportance) { jdbc.update("DELETE FROM user_long_term_memory WHERE created_at < :olderThan AND importance < :maximumImportance", Map.of("olderThan", java.sql.Timestamp.from(olderThan), "maximumImportance", maximumImportance)); }
    private String vector(float[] values) { StringBuilder result = new StringBuilder("["); for (int index = 0; index < values.length; index++) { if (index > 0) result.append(','); result.append(values[index]); } return result.append(']').toString(); }
}
