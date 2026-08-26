/**
 * 本文件定义 {@code ParadeDbHealthIndicator}，负责外部基础设施和本地替代实现适配器。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.infrastructure.retrieval;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 在 docker profile 中公开 ParadeDB 扩展和索引是否可查询，不暴露连接信息。 */
@Component("paradeDb")
@Profile({"docker", "local-docker"})
public class ParadeDbHealthIndicator implements HealthIndicator {
    private final JdbcTemplate jdbc;
    public ParadeDbHealthIndicator(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public Health health() {
        try {
            Integer value = jdbc.queryForObject("SELECT 1 FROM pg_extension WHERE extname = 'paradedb'", Integer.class);
            return value == null ? Health.down().withDetail("reason", "ParadeDB 扩展未安装").build() : Health.up().build();
        } catch (RuntimeException exception) {
            return Health.down().withDetail("reason", "ParadeDB 数据库不可用").build();
        }
    }
}
