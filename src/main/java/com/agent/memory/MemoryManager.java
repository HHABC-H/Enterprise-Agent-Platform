/**
 * 本文件定义 {@code MemoryManager}，负责短期记忆与长期用户画像抽象。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.memory;

import com.agent.config.AgentPlatformProperties;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MemoryManager {
    private final MemoryStore store;
    private final AgentPlatformProperties properties;
    private final Clock clock;
    public MemoryManager(MemoryStore store, AgentPlatformProperties properties, Clock clock) {
        this.store = store;
        this.properties = properties;
        this.clock = clock;
    }
    public List<MemoryEntry> read(String sessionId) { return store.read(sessionId); }
    public void append(String sessionId, String role, String content) {
        List<MemoryEntry> entries = new ArrayList<>(store.read(sessionId));
        entries.add(new MemoryEntry(role, content, clock.instant()));
        while (entries.size() > properties.getMemory().getMaxMessages()) entries.remove(0);
        store.replace(sessionId, entries, Duration.ofSeconds(properties.getMemory().getTtlSeconds()));
    }
}
