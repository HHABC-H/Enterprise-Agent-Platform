package com.agent.memory;

import com.agent.config.AgentPlatformProperties;
import java.time.Clock;
import java.time.Duration;
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
        store.append(sessionId, new MemoryEntry(role, content, clock.instant()), properties.getMemory().getMaxMessages(),
                Duration.ofSeconds(properties.getMemory().getTtlSeconds()));
    }
}
