package com.agent.memory;

import java.util.List;

/** 为短期记忆和会话摘要提供可替换的摘要能力。 */
public interface MemorySummarizationPort {
    String summarize(List<MemoryEntry> entries, int maximumCharacters);
}
