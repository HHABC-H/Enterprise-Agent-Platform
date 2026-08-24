package com.agent.memory;

import java.time.Duration;
import java.util.List;

public interface MemoryStore {
    List<MemoryEntry> read(String sessionId);
    void append(String sessionId, MemoryEntry entry, int maxMessages, Duration ttl);
}
