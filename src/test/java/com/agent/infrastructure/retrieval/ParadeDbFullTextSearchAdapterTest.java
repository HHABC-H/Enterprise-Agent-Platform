package com.agent.infrastructure.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agent.document.Chunk;
import com.agent.document.DocumentMetadata;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class ParadeDbFullTextSearchAdapterTest {
    @Test
    void ParadeDb不可用时仅降级全文通道且不制造越权结果() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.query(any(String.class), any(java.util.Map.class), any(RowMapper.class))).thenThrow(new DataAccessResourceFailureException("不可用"));
        Chunk allowed = new Chunk("allowed", "doc", "公开手册", List.of(), new DocumentMetadata("tenant-a", "手册", "v1", Set.of("public"), Set.of()));
        assertThat(new ParadeDbFullTextSearchAdapter(jdbc).search("手册", List.of(allowed), 5)).isEmpty();
    }
}
