package com.agent.document;

import java.util.Set;

public record DocumentMetadata(
        String tenantId,
        String source,
        String version,
        Set<String> permissionTags,
        Set<String> allowedUserIds) {

    public DocumentMetadata {
        permissionTags = permissionTags == null ? Set.of() : Set.copyOf(permissionTags);
        allowedUserIds = allowedUserIds == null ? Set.of() : Set.copyOf(allowedUserIds);
    }
}
