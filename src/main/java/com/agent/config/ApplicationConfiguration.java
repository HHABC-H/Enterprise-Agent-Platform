/**
 * 本文件定义 {@code ApplicationConfiguration}，负责应用属性与运行时 Bean 装配。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.config;

import java.time.Clock;
import java.util.concurrent.Executor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableConfigurationProperties(AgentPlatformProperties.class)
public class ApplicationConfiguration {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public Executor retrievalExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setThreadNamePrefix("retrieval-");
        executor.initialize();
        return executor;
    }

    @Bean
    public Executor evaluationExecutor(AgentPlatformProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Math.max(1, properties.getEvaluation().getConcurrency()));
        executor.setMaxPoolSize(Math.max(1, properties.getEvaluation().getConcurrency()));
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("evaluation-");
        executor.initialize();
        return executor;
    }
}
