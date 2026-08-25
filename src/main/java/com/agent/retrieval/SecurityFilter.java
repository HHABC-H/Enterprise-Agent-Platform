/**
 * 本文件定义 {@code SecurityFilter}，负责检索、权限过滤、证据校验与排序流程。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.retrieval;

import com.agent.document.Chunk;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SecurityFilter {
    public List<Chunk> filter(String tenantId, String userId, List<Chunk> chunks) {
        return chunks.stream()
                .filter(chunk -> tenantId.equals(chunk.metadata().tenantId()))
                .filter(chunk -> canAccess(userId, chunk))
                .toList();
    }

    private boolean canAccess(String userId, Chunk chunk) {
        return chunk.metadata().permissionTags().isEmpty()
                || chunk.metadata().permissionTags().contains("public")
                || chunk.metadata().permissionTags().contains("user:" + userId)
                || chunk.metadata().allowedUserIds().contains(userId);
    }
}
