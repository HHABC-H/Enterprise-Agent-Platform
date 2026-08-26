package com.agent.infrastructure.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agent.ingestion.DocumentOperation;
import com.agent.ingestion.KnowledgeDocumentChangedEvent;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** 验证 PostgreSQL 入库任务使用 JDBC 可识别的时间戳类型。 */
class JdbcIngestionTaskStoreTest {
    @Test
    void 入队和启动任务均绑定Timestamp() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.update(anyString(), org.mockito.ArgumentMatchers.<Map<String, ?>>any())).thenReturn(1);
        JdbcIngestionTaskStore store = new JdbcIngestionTaskStore(jdbc, Clock.fixed(Instant.parse("2026-08-26T00:00:00Z"), ZoneOffset.UTC));
        KnowledgeDocumentChangedEvent event = new KnowledgeDocumentChangedEvent("event", "tenant-a", "doc-a", "v1", "hash",
                DocumentOperation.UPSERT, Instant.parse("2026-08-26T00:00:00Z"), "trace", "key");

        store.enqueue(event);
        store.tryStart(event);

        ArgumentCaptor<Map<String, ?>> parameters = ArgumentCaptor.forClass(Map.class);
        verify(jdbc, org.mockito.Mockito.times(2)).update(anyString(), parameters.capture());
        assertThat(parameters.getAllValues()).allSatisfy(value -> assertThat(value.get("updatedAt")).isInstanceOf(Timestamp.class));
    }
}
