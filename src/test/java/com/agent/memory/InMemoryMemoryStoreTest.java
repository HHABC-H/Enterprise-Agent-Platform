package com.agent.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.agent.infrastructure.memory.InMemoryMemoryStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
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

    @Test
    void 并发追加完整轮次不会丢失消息() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        InMemoryMemoryStore store = new InMemoryMemoryStore(clock);
        int workers = 20;
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        Thread[] threads = new Thread[workers];
        for (int index = 0; index < workers; index++) {
            final int value = index;
            threads[index] = new Thread(() -> { ready.countDown(); try { start.await(); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
                store.appendTurn("parallel", new MemoryEntry("user", "q" + value, clock.instant()), new MemoryEntry("assistant", "a" + value, clock.instant()), 40, Duration.ofHours(24)); });
            threads[index].start();
        }
        ready.await(); start.countDown(); for (Thread thread : threads) thread.join();
        assertThat(store.read("parallel")).hasSize(40).allSatisfy(entry -> assertThat(entry.role()).isIn("user", "assistant"));
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
