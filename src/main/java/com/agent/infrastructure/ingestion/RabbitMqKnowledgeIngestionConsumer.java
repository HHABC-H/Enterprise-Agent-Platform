/**
 * 本文件定义 {@code RabbitMqKnowledgeIngestionConsumer}，负责外部基础设施和本地替代实现适配器。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.infrastructure.ingestion;

import com.agent.ingestion.KnowledgeDocumentChangedEvent;
import com.agent.ingestion.KnowledgeIngestionProcessor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** 消费端只委托给幂等处理器；处理器内部有限重试后抛错，容器将消息送入死信队列。 */
@Component
@Profile({"docker", "local-docker"})
public class RabbitMqKnowledgeIngestionConsumer {
    private final KnowledgeIngestionProcessor processor;
    public RabbitMqKnowledgeIngestionConsumer(KnowledgeIngestionProcessor processor) { this.processor = processor; }
    @RabbitListener(queues = "${ai-platform.ingestion.rabbit.queue}", containerFactory = "ingestionRabbitListenerContainerFactory")
    public void consume(KnowledgeDocumentChangedEvent event) { processor.process(event); }
}
