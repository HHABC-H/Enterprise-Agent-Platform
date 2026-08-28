package com.agent.memory;

import com.agent.config.AgentPlatformProperties;
import com.agent.observability.BusinessOperation;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LongTermMemoryService {
    private static final Logger log = LoggerFactory.getLogger(LongTermMemoryService.class);
    private final LongTermMemoryStore store;
    private final MemoryEmbeddingClient embeddingClient;
    private final AgentPlatformProperties properties;
    private final Clock clock;
    public LongTermMemoryService(LongTermMemoryStore store, MemoryEmbeddingClient embeddingClient, AgentPlatformProperties properties, Clock clock) {
        this.store = store; this.embeddingClient = embeddingClient; this.properties = properties; this.clock = clock;
    }
    public List<LongTermMemory> retrieve(String tenantId, String userId, String question) {
        try {
            return embeddingClient.embed(question).map(vector -> store.retrieve(tenantId, userId, vector, properties.getMemory().getLongTerm().getTopK() * 3).stream()
                    .map(item -> new LongTermMemory(item.content(), item.sessionId(), item.createdAt(), item.type(), item.importance(), item.score() * decay(item.createdAt()),
                            item.source(), item.confidence(), item.expiresAt(), item.obsolete(), item.replacedBy(), item.dedupeKey(), item.conflictKey()))
                    .filter(item -> item.score() >= properties.getMemory().getLongTerm().getSimilarityThreshold()).sorted(Comparator.comparing(LongTermMemory::score).reversed())
                    .limit(properties.getMemory().getLongTerm().getTopK()).toList()).orElse(List.of());
        } catch (RuntimeException exception) { log.warn("长期记忆检索失败，已降级为空结果：{}", exception.getClass().getSimpleName()); return List.of(); }
    }
    public void save(String tenantId, String userId, String sessionId, String content, MemoryType type, double importance) {
        save(tenantId, userId, sessionId, content, type, importance, "会话摘要", 0.7, null);
    }
    @BusinessOperation("LONG_TERM_MEMORY_SAVE")
    public void saveManual(String tenantId, String userId, String content, MemoryType type, double importance) {
        save(tenantId, userId, "manual", content, type, importance, "用户主动保存", 1.0, null);
    }
    public void save(String tenantId, String userId, String sessionId, String content, MemoryType type, double importance, String source, double confidence, Instant expiresAt) {
        if (content == null || content.isBlank()) return;
        try {
            String normalized = content.trim().replaceAll("\\s+", " ");
            embeddingClient.embed(normalized).ifPresent(vector -> store.save(tenantId, userId, sessionId, normalized, vector, type, importance, clock.instant(), source,
                    confidence, expiresAt, digest(type.name() + "|" + normalized), conflictKey(type, normalized)));
        } catch (RuntimeException exception) { log.warn("长期记忆写入失败，主对话继续执行：{}", exception.getClass().getSimpleName()); }
    }
    public void cleanup() {
        try { store.cleanup(clock.instant().minus(Duration.ofDays(30)), 0.5); log.info("长期记忆清理任务已完成"); }
        catch (RuntimeException exception) { log.warn("长期记忆清理失败：{}", exception.getClass().getSimpleName()); }
    }
    private double decay(Instant createdAt) { long days = Duration.between(createdAt, clock.instant()).toDays(); if (days <= properties.getMemory().getLongTerm().getRecentDays()) return properties.getMemory().getLongTerm().getRecentFactor(); if (days <= properties.getMemory().getLongTerm().getMediumDays()) return properties.getMemory().getLongTerm().getMediumFactor(); return properties.getMemory().getLongTerm().getOldFactor(); }
    private String conflictKey(MemoryType type, String content) {
        if (type != MemoryType.SEMANTIC) return ""; String text = content.toLowerCase();
        if (text.contains("语言") || text.contains("java") || text.contains("python")) return "preferred-language";
        if (text.contains("代码风格") || text.contains("简洁") || text.contains("注释")) return "coding-style";
        if (text.contains("技术栈")) return "tech-stack"; if (text.contains("预算")) return "project-budget"; if (text.contains("角色")) return "role";
        return "";
    }
    private String digest(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception exception) { throw new IllegalStateException("记忆去重键生成失败。", exception); } }
}
