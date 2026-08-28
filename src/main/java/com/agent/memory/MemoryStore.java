/**
 * 本文件定义 {@code MemoryStore}，负责短期记忆与长期用户画像抽象。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.memory;

import java.time.Duration;
import java.util.List;

public interface MemoryStore {
    List<MemoryEntry> read(String sessionId);
    void appendTurn(String sessionId, MemoryEntry userEntry, MemoryEntry assistantEntry, int maxMessages, Duration ttl);
    void replace(String sessionId, List<MemoryEntry> entries, Duration ttl);
}
