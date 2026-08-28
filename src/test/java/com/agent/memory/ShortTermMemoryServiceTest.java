package com.agent.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.agent.config.AgentPlatformProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

class ShortTermMemoryServiceTest {
    @Test
    void 超过二十条消息时压缩并隔离用户会话() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-27T00:00:00Z"), ZoneOffset.UTC);
        AgentPlatformProperties properties = new AgentPlatformProperties();
        properties.getMemory().setMaxMessages(20);
        properties.getMemory().setMaxTokens(15000);
        ShortTermMemoryService service = new ShortTermMemoryService(new TestMemoryStore(), new MemoryCompressor(clock), properties, clock);

        for (int index = 0; index < 11; index++) service.appendTurn("tenant-a", "alice", "session-1", "问题" + index, "回答" + index);

        List<MemoryEntry> entries = service.read("tenant-a", "alice", "session-1");
        assertThat(entries).hasSize(11);
        assertThat(entries.get(0).role()).isEqualTo("system");
        assertThat(entries.get(1).content()).isEqualTo("问题6");
        assertThat(service.read("tenant-a", "bob", "session-1")).isEmpty();
    }

    private static final class TestMemoryStore implements MemoryStore {
        private final Map<String, List<MemoryEntry>> values = new ConcurrentHashMap<>();
        @Override public List<MemoryEntry> read(String sessionId) { return List.copyOf(values.getOrDefault(sessionId, List.of())); }
        @Override public void appendTurn(String sessionId, MemoryEntry userEntry, MemoryEntry assistantEntry, int maxMessages, java.time.Duration ttl) {
            values.compute(sessionId, (key, old) -> {
                List<MemoryEntry> next = new ArrayList<>(old == null ? List.of() : old);
                next.add(userEntry); next.add(assistantEntry);
                while (next.size() > maxMessages) next.remove(0);
                return next;
            });
        }
        @Override public void replace(String sessionId, List<MemoryEntry> entries, java.time.Duration ttl) { values.put(sessionId, new ArrayList<>(entries)); }
    }
}
