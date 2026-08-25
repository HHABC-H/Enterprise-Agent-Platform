/**
 * 本文件定义 {@code RabbitMqIngestionConfiguration}，负责外部基础设施和本地替代实现适配器。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.infrastructure.ingestion;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** RabbitMQ 队列拓扑：消费失败且本地三次重试耗尽后进入死信队列，由人工重试。 */
@Configuration
@Profile("docker")
public class RabbitMqIngestionConfiguration {
    @Bean public DirectExchange knowledgeExchange() { return new DirectExchange("knowledge.ingestion", true, false); }
    @Bean public DirectExchange knowledgeDeadLetterExchange() { return new DirectExchange("knowledge.ingestion.dlx", true, false); }
    @Bean public Queue knowledgeQueue() {
        return QueueBuilder.durable("knowledge.ingestion.queue").deadLetterExchange("knowledge.ingestion.dlx")
                .deadLetterRoutingKey("knowledge.ingestion.dead").build();
    }
    @Bean public Queue knowledgeDeadLetterQueue() { return QueueBuilder.durable("knowledge.ingestion.dead").build(); }
    @Bean public Binding knowledgeBinding() { return BindingBuilder.bind(knowledgeQueue()).to(knowledgeExchange()).with("knowledge.document.changed"); }
    @Bean public Binding knowledgeDeadLetterBinding() { return BindingBuilder.bind(knowledgeDeadLetterQueue()).to(knowledgeDeadLetterExchange()).with("knowledge.ingestion.dead"); }
    @Bean public Jackson2JsonMessageConverter ingestionMessageConverter() { return new Jackson2JsonMessageConverter(); }
    @Bean(name = "ingestionRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory ingestionRabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                                         Jackson2JsonMessageConverter ingestionMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(ingestionMessageConverter);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
