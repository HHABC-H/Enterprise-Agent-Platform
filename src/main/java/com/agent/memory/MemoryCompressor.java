package com.agent.memory;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class MemoryCompressor {
    private final Clock clock;
    private final MemorySummarizationPort summarizationPort;

    @Autowired
    public MemoryCompressor(Clock clock, MemorySummarizationPort summarizationPort) {
        this.clock = clock;
        this.summarizationPort = summarizationPort;
    }
    /** 兼容既有单元测试和未迁移调用方，生产装配使用摘要端口构造器。 */
    public MemoryCompressor(Clock clock) {
        this(clock, (entries, maximumCharacters) -> {
            StringBuilder summary = new StringBuilder("此前对话摘要（兼容降级）：");
            for (MemoryEntry entry : entries) {
                if (summary.length() >= maximumCharacters) break;
                summary.append(entry.role()).append('：').append(entry.content()).append('；');
            }
            return summary.length() > maximumCharacters ? summary.substring(0, maximumCharacters) : summary.toString();
        });
    }

    public List<MemoryEntry> compress(List<MemoryEntry> entries, int retainedRecentRounds) {
        int retainedMessages = Math.max(2, retainedRecentRounds * 2);
        int split = Math.max(0, entries.size() - retainedMessages);
        List<MemoryEntry> oldEntries = entries.subList(0, split).stream().filter(entry -> !"system".equals(entry.role())).toList();
        List<MemoryEntry> retained = entries.subList(split, entries.size());
        if (oldEntries.isEmpty()) {
            return List.copyOf(entries);
        }
        List<MemoryEntry> result = new ArrayList<>();
        result.add(new MemoryEntry("system", summarizationPort.summarize(oldEntries, 1600), clock.instant()));
        result.addAll(retained);
        return List.copyOf(result);
    }
}
