package com.agent.memory;

import com.agent.config.AgentPlatformProperties;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ShortTermMemoryService {
    private static final Logger log = LoggerFactory.getLogger(ShortTermMemoryService.class);
    private final MemoryStore store;
    private final MemoryCompressor compressor;
    private final AgentPlatformProperties properties;
    private final Clock clock;

    public ShortTermMemoryService(MemoryStore store, MemoryCompressor compressor, AgentPlatformProperties properties, Clock clock) {
        this.store = store;
        this.compressor = compressor;
        this.properties = properties;
        this.clock = clock;
    }

    public List<MemoryEntry> read(String tenantId, String userId, String sessionId) {
        try {
            return store.read(key(tenantId, userId, sessionId));
        } catch (RuntimeException exception) {
            log.warn("短期记忆读取失败，已降级为空上下文: {}", exception.getClass().getSimpleName());
            return List.of();
        }
    }

    public void appendTurn(String tenantId, String userId, String sessionId, String question, String answer) {
        try {
            String memoryKey = key(tenantId, userId, sessionId);
            store.appendTurn(memoryKey, new MemoryEntry("user", question, clock.instant()), new MemoryEntry("assistant", answer, clock.instant()),
                    properties.getMemory().getMaxMessages() + 2, ttl());
            List<MemoryEntry> entries = store.read(memoryKey);
            if (entries.size() > properties.getMemory().getMaxMessages() || estimateTokens(entries) > properties.getMemory().getMaxTokens()) {
                entries = compressor.compress(entries, properties.getMemory().getRetainedRecentRounds());
                log.info("会话 {} 已完成短期记忆压缩", sessionId);
                store.replace(memoryKey, entries, ttl());
            }
        } catch (RuntimeException exception) {
            log.warn("短期记忆写入失败，主对话继续执行: {}", exception.getClass().getSimpleName());
        }
    }

    public void replace(String tenantId, String userId, String sessionId, List<MemoryEntry> entries) {
        store.replace(key(tenantId, userId, sessionId), entries, ttl());
    }

    int estimateTokens(List<MemoryEntry> entries) {
        return entries.stream().mapToInt(item -> Math.max(1, item.content().length() / 2)).sum();
    }

    private Duration ttl() {
        return Duration.ofSeconds(properties.getMemory().getTtlSeconds());
    }

    private String key(String tenantId, String userId, String sessionId) {
        return "memory:" + tenantId + ':' + userId + ':' + sessionId;
    }
}
