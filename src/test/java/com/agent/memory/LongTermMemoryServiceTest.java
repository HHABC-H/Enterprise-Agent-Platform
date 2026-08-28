package com.agent.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.agent.config.AgentPlatformProperties;
import com.agent.infrastructure.memory.InMemoryLongTermMemoryStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LongTermMemoryServiceTest {
    @Test
    void 只返回当前租户和用户的长期记忆() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-27T00:00:00Z"), ZoneOffset.UTC);
        LongTermMemoryService service = new LongTermMemoryService(new InMemoryLongTermMemoryStore(), text -> Optional.of(new float[] { 1F }), new AgentPlatformProperties(), clock);
        service.save("tenant-a", "alice", "s1", "喜欢简洁代码", MemoryType.SEMANTIC, 0.9);
        service.save("tenant-a", "bob", "s2", "不应被 alice 看见", MemoryType.SEMANTIC, 0.9);

        assertThat(service.retrieve("tenant-a", "alice", "代码风格")).extracting(LongTermMemory::content).containsExactly("喜欢简洁代码");
    }

    @Test
    void 相同主动记忆幂等且稳定偏好会替代旧值() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-27T00:00:00Z"), ZoneOffset.UTC);
        LongTermMemoryService service = new LongTermMemoryService(new InMemoryLongTermMemoryStore(), text -> Optional.of(new float[] { 1F }), new AgentPlatformProperties(), clock);
        service.saveManual("tenant-a", "alice", "语言偏好 Java", MemoryType.SEMANTIC, 0.9);
        service.saveManual("tenant-a", "alice", "语言偏好 Java", MemoryType.SEMANTIC, 0.9);
        service.saveManual("tenant-a", "alice", "语言偏好 Python", MemoryType.SEMANTIC, 0.9);
        assertThat(service.retrieve("tenant-a", "alice", "偏好")).extracting(LongTermMemory::content).containsExactly("语言偏好 Python");
    }
}
