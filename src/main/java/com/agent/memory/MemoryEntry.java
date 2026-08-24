package com.agent.memory;

import java.time.Instant;

public record MemoryEntry(String role, String content, Instant createdAt) {
}
