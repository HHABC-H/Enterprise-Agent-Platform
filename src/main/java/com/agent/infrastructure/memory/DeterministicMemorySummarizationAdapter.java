package com.agent.infrastructure.memory;

import com.agent.memory.MemoryEntry;
import com.agent.memory.MemorySummarizationPort;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** local 环境的确定性摘要降级实现，不调用大模型。 */
@Component
@ConditionalOnProperty(prefix = "ai-platform.memory.summarization", name = "llm-enabled", havingValue = "false", matchIfMissing = true)
public class DeterministicMemorySummarizationAdapter implements MemorySummarizationPort {
    @Override
    public String summarize(List<MemoryEntry> entries, int maximumCharacters) {
        StringBuilder summary = new StringBuilder("此前对话摘要（确定性降级）：");
        for (MemoryEntry entry : entries) {
            if (summary.length() >= maximumCharacters) break;
            summary.append(entry.role()).append('：').append(entry.content().replace('\n', ' ')).append('；');
        }
        return summary.length() > maximumCharacters ? summary.substring(0, maximumCharacters) : summary.toString();
    }
}
