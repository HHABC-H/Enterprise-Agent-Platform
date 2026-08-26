/**
 * 本文件定义 {@code ParadeDbFullTextSearchAdapter}，负责外部基础设施和本地替代实现适配器。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.infrastructure.retrieval;

import com.agent.document.Chunk;
import com.agent.retrieval.FullTextSearchPort;
import com.agent.retrieval.SearchCandidate;
import com.agent.retrieval.SearchSource;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/** ParadeDB BM25 查询适配器；故障时仅返回空全文结果，向量链路和权限过滤继续生效。 */
@Component
@Primary
@Profile({"docker", "local-docker"})
public class ParadeDbFullTextSearchAdapter implements FullTextSearchPort {
    private static final Logger log = LoggerFactory.getLogger(ParadeDbFullTextSearchAdapter.class);
    private final NamedParameterJdbcTemplate jdbc;
    public ParadeDbFullTextSearchAdapter(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public List<SearchCandidate> search(String question, List<Chunk> authorizedChunks, int limit) {
        if (authorizedChunks.isEmpty()) { return List.of(); }
        Map<String, Chunk> byId = authorizedChunks.stream().collect(java.util.stream.Collectors.toMap(Chunk::chunkId, Function.identity()));
        String tenantId = authorizedChunks.get(0).metadata().tenantId();
        String sql = "SELECT chunk_id, pdb.score(chunk_id) AS score FROM knowledge_chunk "
                + "WHERE tenant_id = :tenantId AND chunk_id IN (:chunkIds) AND content ||| :query "
                + "ORDER BY pdb.score(chunk_id) DESC LIMIT :limit";
        try {
            return jdbc.query(sql, Map.of("tenantId", tenantId, "chunkIds", byId.keySet(), "query", question, "limit", limit),
                    (resultSet, row) -> new SearchCandidate(byId.get(resultSet.getString("chunk_id")), resultSet.getDouble("score"), SearchSource.FULL_TEXT));
        } catch (DataAccessException exception) {
            log.warn("ParadeDB 全文检索不可用，已降级为仅向量召回: {}", exception.getClass().getSimpleName());
            return List.of();
        }
    }
}
