/**
 * 本文件定义 {@code NoopDocumentIndexStore}，负责外部基础设施和本地替代实现适配器。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.infrastructure.ingestion;

import com.agent.document.DocumentIndex;
import com.agent.ingestion.DocumentIndexStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** local profile 仅使用内存检索适配器，因此不写外部全文索引。 */
@Component
@Profile("local")
public class NoopDocumentIndexStore implements DocumentIndexStore {
    @Override public void replaceDocument(String tenantId, String documentId, String version, DocumentIndex index) { }
    @Override public void deleteDocument(String tenantId, String documentId) { }
}
