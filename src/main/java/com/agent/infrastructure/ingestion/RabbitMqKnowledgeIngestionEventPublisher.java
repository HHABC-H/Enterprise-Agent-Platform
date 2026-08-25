/**
 * 本文件定义 {@code RabbitMqKnowledgeIngestionEventPublisher}，负责外部基础设施和本地替代实现适配器。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.infrastructure.ingestion;

import com.agent.extension.KnowledgeIngestionEventPublisher;
import com.agent.ingestion.KnowledgeDocumentChangedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** docker profile 的 RabbitMQ 发布适配器，消息头和正文都携带 traceId。 */
@Component
@Profile("docker")
public class RabbitMqKnowledgeIngestionEventPublisher implements KnowledgeIngestionEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    public RabbitMqKnowledgeIngestionEventPublisher(RabbitTemplate rabbitTemplate) { this.rabbitTemplate = rabbitTemplate; }
    @Override public void publish(KnowledgeDocumentChangedEvent event) {
        rabbitTemplate.convertAndSend("knowledge.ingestion", "knowledge.document.changed", event, message -> {
            message.getMessageProperties().setHeader("X-Trace-Id", event.traceId());
            message.getMessageProperties().setMessageId(event.eventId());
            return message;
        });
    }
}
