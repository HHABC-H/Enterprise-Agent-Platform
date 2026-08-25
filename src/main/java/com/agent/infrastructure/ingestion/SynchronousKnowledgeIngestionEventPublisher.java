/**
 * 本文件定义 {@code SynchronousKnowledgeIngestionEventPublisher}，负责外部基础设施和本地替代实现适配器。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.infrastructure.ingestion;

import com.agent.extension.KnowledgeIngestionEventPublisher;
import com.agent.ingestion.KnowledgeDocumentChangedEvent;
import com.agent.ingestion.KnowledgeIngestionProcessor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** local profile 的确定性进程内事件发布器。 */
@Component
@Profile("local")
public class SynchronousKnowledgeIngestionEventPublisher implements KnowledgeIngestionEventPublisher {
    private final KnowledgeIngestionProcessor processor;
    public SynchronousKnowledgeIngestionEventPublisher(KnowledgeIngestionProcessor processor) { this.processor = processor; }
    @Override public void publish(KnowledgeDocumentChangedEvent event) { processor.process(event); }
}
