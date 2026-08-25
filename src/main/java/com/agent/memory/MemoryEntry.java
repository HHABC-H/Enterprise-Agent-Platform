/**
 * 本文件定义 {@code MemoryEntry}，负责短期记忆与长期用户画像抽象。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.memory;

import java.time.Instant;

public record MemoryEntry(String role, String content, Instant createdAt) {
}
