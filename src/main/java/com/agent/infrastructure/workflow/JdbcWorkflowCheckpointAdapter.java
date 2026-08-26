package com.agent.infrastructure.workflow;

import com.agent.workflow.ApprovalDecision;
import com.agent.workflow.WorkflowCheckpoint;
import com.agent.workflow.WorkflowCheckpointPort;
import com.agent.workflow.WorkflowState;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/** PostgreSQL 审批检查点仓储；审批更新以状态和版本作为并发条件。 */
@Component
@Profile({"docker", "local-docker"})
public class JdbcWorkflowCheckpointAdapter implements WorkflowCheckpointPort {
    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;
    public JdbcWorkflowCheckpointAdapter(NamedParameterJdbcTemplate jdbc, Clock clock) { this.jdbc = jdbc; this.clock = clock; }
    @Override public void save(String sessionId, List<WorkflowState> states) { }
    @Override public void create(WorkflowCheckpoint item) {
        jdbc.update("INSERT INTO workflow_checkpoint (workflow_id,tenant_id,owner_user_id,session_id,input_summary,pending_action,state,created_at,updated_at,expires_at,trace_id,row_version,approver_id,decision,approval_comment,state_trace) VALUES (:id,:tenant,:owner,:session,:summary,:action,:state,:created,:updated,:expires,:trace,:version,NULL,NULL,NULL,:states)",
                params("id", item.workflowId(), "tenant", item.tenantId(), "owner", item.ownerUserId(), "session", item.sessionId(), "summary", item.inputSummary(), "action", item.pendingAction(), "state", item.state().name(), "created", Timestamp.from(item.createdAt()), "updated", Timestamp.from(item.createdAt()), "expires", Timestamp.from(item.expiresAt()), "trace", nullSafe(item.traceId()), "version", item.version(), "states", trace(item.trace())));
    }
    @Override public Optional<WorkflowCheckpoint> find(String workflowId) {
        return jdbc.query("SELECT * FROM workflow_checkpoint WHERE workflow_id=:id", Map.of("id", workflowId), (rs, row) -> map(rs)).stream().findFirst();
    }
    @Override public List<WorkflowCheckpoint> findPending(String tenantId, String userId, boolean approver) {
        String sql = "SELECT * FROM workflow_checkpoint WHERE tenant_id=:tenant AND state='WAITING_APPROVAL' AND expires_at > :now" + (approver ? "" : " AND owner_user_id=:user") + " ORDER BY created_at";
        Map<String, Object> params = approver ? Map.of("tenant", tenantId, "now", Timestamp.from(Instant.now(clock))) : Map.of("tenant", tenantId, "user", userId, "now", Timestamp.from(Instant.now(clock)));
        return jdbc.query(sql, params, (rs, row) -> map(rs));
    }
    @Override public WorkflowCheckpoint decide(String workflowId, long version, String approverId, ApprovalDecision decision, String comment) {
        WorkflowCheckpoint before = find(workflowId).orElseThrow(() -> new IllegalArgumentException("未找到审批工作流。"));
        if (before.expiresAt().isBefore(Instant.now(clock))) { throw new IllegalStateException("审批检查点已过期。"); }
        WorkflowState state = decision == ApprovalDecision.APPROVE ? WorkflowState.EXECUTING : WorkflowState.REFUSED;
        int changed = jdbc.update("UPDATE workflow_checkpoint SET state=:state,approver_id=:approver,decision=:decision,approval_comment=:comment,updated_at=:updated,row_version=row_version+1 WHERE workflow_id=:id AND state='WAITING_APPROVAL' AND row_version=:version AND expires_at > :now",
                Map.of("state", state.name(), "approver", approverId, "decision", decision.name(), "comment", redact(comment), "updated", Timestamp.from(Instant.now(clock)), "id", workflowId, "version", version, "now", Timestamp.from(Instant.now(clock))));
        if (changed != 1) { throw new IllegalStateException("审批版本已变化、已处理或已过期，请刷新后重试。"); }
        return find(workflowId).orElseThrow();
    }
    private WorkflowCheckpoint map(java.sql.ResultSet rs) throws java.sql.SQLException {
        String comment = rs.getString("approval_comment");
        String decision = rs.getString("decision");
        return new WorkflowCheckpoint(rs.getString("workflow_id"), rs.getString("tenant_id"), rs.getString("owner_user_id"), rs.getString("session_id"), rs.getString("input_summary"), rs.getString("pending_action"), WorkflowState.valueOf(rs.getString("state")), rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("expires_at").toInstant(), rs.getString("trace_id"), rs.getLong("row_version"), rs.getString("approver_id"), decision == null ? null : ApprovalDecision.valueOf(decision), comment, parseTrace(rs.getString("state_trace")));
    }
    private String trace(List<WorkflowState> values) { return values.stream().map(Enum::name).collect(java.util.stream.Collectors.joining(",")); }
    private List<WorkflowState> parseTrace(String value) { return value == null || value.isBlank() ? List.of() : Arrays.stream(value.split(",")).map(WorkflowState::valueOf).toList(); }
    private String nullSafe(String value) { return value == null ? "" : value; }
    private String redact(String value) { return value.replaceAll("(?i)(sk-[a-z0-9_-]{8,}|password\\s*[:=]\\s*\\S+)", "[已脱敏]"); }
    private Map<String,Object> params(Object... pairs) { java.util.HashMap<String,Object> values = new java.util.HashMap<>(); for (int index = 0; index < pairs.length; index += 2) { values.put((String) pairs[index], pairs[index + 1]); } return values; }
}
