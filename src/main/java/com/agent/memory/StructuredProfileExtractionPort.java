package com.agent.memory;

import java.util.List;

/** 返回候选画像 JSON；领域层负责校验和合并。 */
public interface StructuredProfileExtractionPort {
    String extractJson(List<MemoryEntry> entries);
}
