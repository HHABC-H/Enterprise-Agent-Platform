package com.agent.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.agent.config.AgentPlatformProperties;
import com.agent.infrastructure.memory.InMemoryChatSessionStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ChatSessionServiceTest {
    @Test
    void 关闭会话后仅允许摘要一次对应窗口() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-27T00:00:00Z"), ZoneOffset.UTC);
        ChatSessionService service = new ChatSessionService(new InMemoryChatSessionStore(clock), new AgentPlatformProperties(), clock);
        service.touch("tenant-a", "alice", "s1", "问题");
        assertThat(service.close("tenant-a", "alice", "s1")).isTrue();
        ChatSessionStore.SessionCandidate candidate = service.pendingSummary().get(0);
        assertThat(service.markSummarized(candidate)).isTrue();
        assertThat(service.pendingSummary()).isEmpty();
    }
}
