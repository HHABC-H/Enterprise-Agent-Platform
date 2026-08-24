package com.agent.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.agent.infrastructure.memory.InMemoryMemoryStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class InMemoryMemoryStoreTest {
    @Test
    void shouldKeepSlidingWindowAndExpireEntries() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        InMemoryMemoryStore store = new InMemoryMemoryStore(clock);
        store.append("one", new MemoryEntry("user", "一", clock.instant()), 2, Duration.ofSeconds(5));
        store.append("one", new MemoryEntry("assistant", "二", clock.instant()), 2, Duration.ofSeconds(5));
        store.append("one", new MemoryEntry("user", "三", clock.instant()), 2, Duration.ofSeconds(5));
        assertThat(store.read("one")).extracting(MemoryEntry::content).containsExactly("二", "三");
        clock.advance(Duration.ofSeconds(6));
        assertThat(store.read("one")).isEmpty();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private MutableClock(Instant instant) { this.instant = instant; }
        private void advance(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
