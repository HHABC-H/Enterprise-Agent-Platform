/**
 * 本文件定义 {@code DocumentMetadata}，负责文档解析、切分、索引及父子块模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
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
