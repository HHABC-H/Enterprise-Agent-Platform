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
