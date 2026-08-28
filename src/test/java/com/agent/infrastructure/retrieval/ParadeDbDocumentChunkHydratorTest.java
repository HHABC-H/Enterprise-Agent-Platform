package com.agent.infrastructure.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agent.document.Chunk;
import com.agent.infrastructure.document.InMemoryDocumentChunkStore;
import com.agent.ingestion.LocalDocumentIndexMirror;
import java.sql.Array;
import java.sql.ResultSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class ParadeDbDocumentChunkHydratorTest {

    @Test
    void 应在应用就绪时将持久化分块回灌到运行时检索索引() throws Exception {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        ResultSet resultSet = mock(ResultSet.class);
        Array tags = mock(Array.class);
        Array users = mock(Array.class);
        when(tags.getArray()).thenReturn(new String[] {"public"});
        when(users.getArray()).thenReturn(new String[0]);
        when(resultSet.getString("chunk_id")).thenReturn("chunk-1");
        when(resultSet.getString("parent_chunk_id")).thenReturn("parent-1");
        when(resultSet.getString("tenant_id")).thenReturn("tenant-a");
        when(resultSet.getString("document_id")).thenReturn("document-a");
        when(resultSet.getString("version")).thenReturn("v1");
        when(resultSet.getString("content")).thenReturn("证据不足时应明确拒答");
        when(resultSet.getString("source")).thenReturn("平台说明.md");
        when(resultSet.getArray("permission_tags")).thenReturn(tags);
        when(resultSet.getArray("allowed_user_ids")).thenReturn(users);
        when(jdbc.query(any(String.class), any(RowMapper.class))).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            RowMapper<Chunk> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(resultSet, 0));
        });

        InMemoryDocumentChunkStore store = new InMemoryDocumentChunkStore();
        LocalDocumentIndexMirror mirror = new LocalDocumentIndexMirror();
        new ParadeDbDocumentChunkHydrator(jdbc, store, mirror).hydrate();

        assertThat(store.findAll()).extracting(Chunk::chunkId).contains("chunk-1");
        assertThat(mirror.findAll()).extracting(Chunk::chunkId).contains("chunk-1");
        assertThat(store.findAll().get(0).metadata().permissionTags()).containsExactly("public");
    }
}
