package com.agent.infrastructure.evaluation;

import com.agent.evaluation.BadCase;
import com.agent.evaluation.BadCaseStatus;
import com.agent.evaluation.EvaluationCaseResult;
import com.agent.evaluation.EvaluationDatasetVersion;
import com.agent.evaluation.EvaluationRepository;
import com.agent.evaluation.EvaluationRun;
import com.agent.evaluation.EvaluationRunState;
import com.agent.evaluation.EvaluationSample;
import com.agent.evaluation.EvaluationSummary;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/** docker 评测仓储，所有读取与更新均携带租户边界。 */
@Component
@Profile({"docker", "local-docker"})
public class JdbcEvaluationRepository implements EvaluationRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcEvaluationRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public void saveDataset(EvaluationDatasetVersion item, List<EvaluationSample> samples) {
        jdbc.update("INSERT INTO evaluation_dataset_version (id,tenant_id,dataset_id,version,name,created_by,created_at,updated_at,row_version) VALUES (:id,:tenant,:dataset,:version,:name,:creator,:now,:now,:rowVersion)", Map.of("id",item.id(),"tenant",item.tenantId(),"dataset",item.datasetId(),"version",item.version(),"name",item.name(),"creator",item.createdBy(),"now",Timestamp.from(item.createdAt()),"rowVersion",item.rowVersion()));
        String sql="INSERT INTO evaluation_sample (id,tenant_id,dataset_version_id,question,expected_answer,sample_type,expected_evidence,expect_reject,tags,fingerprint,created_at,updated_at,row_version) VALUES (:id,:tenant,:versionId,:question,:answer,:type,:evidence,:reject,CAST(:tags AS text[]),:fingerprint,:now,:now,0)";
        jdbc.batchUpdate(sql, samples.stream().map(sample -> params("id",sample.id(),"tenant",item.tenantId(),"versionId",item.id(),"question",sample.question(),"answer",empty(sample.expectedAnswer()),"type",sample.type(),"evidence",empty(sample.expectedEvidence()),"reject",sample.expectReject(),"tags",array(sample.tags()),"fingerprint",sample.fingerprint(),"now",Timestamp.from(item.createdAt()))).toArray(Map[]::new));
    }
    @Override public Optional<EvaluationDatasetVersion> findDataset(String tenant, String dataset, int version) { return jdbc.query("SELECT * FROM evaluation_dataset_version WHERE tenant_id=:tenant AND dataset_id=:dataset AND version=:version",Map.of("tenant",tenant,"dataset",dataset,"version",version),(rs,row)->dataset(rs)).stream().findFirst(); }
    @Override public List<EvaluationDatasetVersion> listDatasets(String tenant) { return jdbc.query("SELECT * FROM evaluation_dataset_version WHERE tenant_id=:tenant ORDER BY created_at",Map.of("tenant",tenant),(rs,row)->dataset(rs)); }
    @Override public List<EvaluationSample> samples(String tenant, String versionId) { return jdbc.query("SELECT * FROM evaluation_sample WHERE tenant_id=:tenant AND dataset_version_id=:id ORDER BY created_at",Map.of("tenant",tenant,"id",versionId),(rs,row)->new EvaluationSample(rs.getString("id"),rs.getString("question"),rs.getString("expected_answer"),rs.getString("sample_type"),rs.getString("expected_evidence"),rs.getBoolean("expect_reject"),set(rs.getArray("tags")),rs.getString("fingerprint"))); }
    @Override public void saveRun(EvaluationRun run) { jdbc.update("INSERT INTO evaluation_run (id,tenant_id,dataset_version_id,state,requested_by,code_version,config_version,model_name,prompt_hash,created_at,updated_at,row_version) VALUES (:id,:tenant,:dataset,:state,:actor,:code,:config,:model,:prompt,:now,:now,:version)",params("id",run.id(),"tenant",run.tenantId(),"dataset",run.datasetVersionId(),"state",run.state().name(),"actor",run.requestedBy(),"code",run.codeVersion(),"config",run.configVersion(),"model",empty(run.modelName()),"prompt",empty(run.promptHash()),"now",Timestamp.from(run.createdAt()),"version",run.rowVersion())); }
    @Override public Optional<EvaluationRun> findRun(String tenant, String id) { return jdbc.query("SELECT * FROM evaluation_run WHERE tenant_id=:tenant AND id=:id",Map.of("tenant",tenant,"id",id),(rs,row)->run(rs)).stream().findFirst(); }
    @Override public boolean transitionRun(String tenant,String id,long expected,EvaluationRunState from,EvaluationRun next) {
        return jdbc.update("UPDATE evaluation_run SET state=:state,started_at=:started,finished_at=:finished,failure_category=:failure,summary_json=:summary,updated_at=:updated,row_version=:nextVersion WHERE tenant_id=:tenant AND id=:id AND state=:from AND row_version=:expected",params("state",next.state().name(),"started",timestamp(next.startedAt()),"finished",timestamp(next.finishedAt()),"failure",empty(next.failureCategory()),"summary",summary(next.summary()),"updated",Timestamp.from(Instant.now()),"nextVersion",next.rowVersion(),"tenant",tenant,"id",id,"from",from.name(),"expected",expected))==1;
    }
    @Override public List<EvaluationRun> recoverableRuns() {
        jdbc.update("UPDATE evaluation_run SET state='QUEUED', started_at=NULL, lease_until=NULL, updated_at=CURRENT_TIMESTAMP, row_version=row_version+1 WHERE state='RUNNING'", Map.of());
        return jdbc.query("SELECT * FROM evaluation_run WHERE state='QUEUED' ORDER BY created_at", Map.of(), (rs,row)->run(rs));
    }
    @Override public void saveCaseResult(EvaluationCaseResult item) { jdbc.update("INSERT INTO evaluation_case_result (id,tenant_id,run_id,sample_id,passed,failure_category,duration_ms,evidence_hit,refused,ragas_status,created_at,updated_at,row_version) VALUES (:id,:tenant,:run,:sample,:passed,:failure,:duration,:evidence,:refused,:ragas,:now,:now,0)",params("id",item.id(),"tenant",item.tenantId(),"run",item.runId(),"sample",item.sampleId(),"passed",item.passed(),"failure",empty(item.failureCategory()),"duration",item.durationMs(),"evidence",item.evidenceHit(),"refused",item.refused(),"ragas",item.ragasStatus(),"now",Timestamp.from(Instant.now()))); }
    @Override public List<EvaluationCaseResult> results(String tenant,String runId) { return jdbc.query("SELECT * FROM evaluation_case_result WHERE tenant_id=:tenant AND run_id=:run ORDER BY created_at",Map.of("tenant",tenant,"run",runId),(rs,row)->new EvaluationCaseResult(rs.getString("id"),tenant,runId,rs.getString("sample_id"),rs.getBoolean("passed"),rs.getString("failure_category"),rs.getDouble("duration_ms"),rs.getBoolean("evidence_hit"),rs.getBoolean("refused"),rs.getString("ragas_status"))); }
    @Override public BadCase saveBadCaseIfAbsent(BadCase item) { jdbc.update("INSERT INTO bad_case (id,tenant_id,source_run_id,source_sample_id,stable_key,failure_category,severity,status,owner_note,created_at,updated_at,row_version) VALUES (:id,:tenant,:run,:sample,:key,:failure,:severity,:status,:note,:created,:updated,:version) ON CONFLICT (tenant_id,stable_key) DO NOTHING",bad(item)); return jdbc.query("SELECT * FROM bad_case WHERE tenant_id=:tenant AND stable_key=:key",Map.of("tenant",item.tenantId(),"key",item.stableKey()),(rs,row)->bad(rs)).stream().findFirst().orElseThrow(); }
    @Override public Optional<BadCase> findBadCase(String tenant,String id) { return jdbc.query("SELECT * FROM bad_case WHERE tenant_id=:tenant AND id=:id",Map.of("tenant",tenant,"id",id),(rs,row)->bad(rs)).stream().findFirst(); }
    @Override public List<BadCase> listBadCases(String tenant,BadCaseStatus status) { String sql="SELECT * FROM bad_case WHERE tenant_id=:tenant"+(status==null?"":" AND status=:status")+" ORDER BY updated_at DESC"; Map<String,Object> params=status==null?Map.of("tenant",tenant):Map.of("tenant",tenant,"status",status.name()); return jdbc.query(sql,params,(rs,row)->bad(rs)); }
    @Override public boolean updateBadCase(BadCase item,long expected) { return jdbc.update("UPDATE bad_case SET status=:status,owner_note=:note,updated_at=:updated,row_version=:nextVersion WHERE tenant_id=:tenant AND id=:id AND row_version=:expected",Map.of("status",item.status().name(),"note",empty(item.ownerNote()),"updated",Timestamp.from(item.updatedAt()),"nextVersion",item.rowVersion(),"tenant",item.tenantId(),"id",item.id(),"expected",expected))==1; }
    private EvaluationDatasetVersion dataset(java.sql.ResultSet rs) throws java.sql.SQLException { return new EvaluationDatasetVersion(rs.getString("id"),rs.getString("tenant_id"),rs.getString("dataset_id"),rs.getInt("version"),rs.getString("name"),rs.getString("created_by"),rs.getTimestamp("created_at").toInstant(),rs.getLong("row_version")); }
    private EvaluationRun run(java.sql.ResultSet rs) throws java.sql.SQLException { return new EvaluationRun(rs.getString("id"),rs.getString("tenant_id"),rs.getString("dataset_version_id"),EvaluationRunState.valueOf(rs.getString("state")),rs.getString("requested_by"),rs.getString("code_version"),rs.getString("config_version"),rs.getString("model_name"),rs.getString("prompt_hash"),rs.getTimestamp("created_at").toInstant(),instant(rs,"started_at"),instant(rs,"finished_at"),rs.getString("failure_category"),null,rs.getLong("row_version")); }
    private BadCase bad(java.sql.ResultSet rs) throws java.sql.SQLException { return new BadCase(rs.getString("id"),rs.getString("tenant_id"),rs.getString("source_run_id"),rs.getString("source_sample_id"),rs.getString("stable_key"),rs.getString("failure_category"),rs.getString("severity"),BadCaseStatus.valueOf(rs.getString("status")),rs.getString("owner_note"),rs.getTimestamp("created_at").toInstant(),rs.getTimestamp("updated_at").toInstant(),rs.getLong("row_version")); }
    private Instant instant(java.sql.ResultSet rs,String field) throws java.sql.SQLException { Timestamp value=rs.getTimestamp(field); return value==null?null:value.toInstant(); }
    private Timestamp timestamp(Instant value) { return value==null?Timestamp.from(Instant.EPOCH):Timestamp.from(value); }
    private String summary(EvaluationSummary value) { return value==null?"":String.format("{\\\"total\\\":%d,\\\"failed\\\":%d,\\\"ragasStatus\\\":\\\"%s\\\"}",value.total(),value.failed(),value.ragasStatus()); }
    private String empty(String value) { return value==null?"":value; }
    private String array(java.util.Set<String> values) { return "{"+values.stream().map(item->"\\\""+item.replace("\\\"","\\\\\\\"")+"\\\"").collect(java.util.stream.Collectors.joining(","))+"}"; }
    private java.util.Set<String> set(java.sql.Array values) throws java.sql.SQLException { if(values==null)return java.util.Set.of(); Object raw=values.getArray(); return raw instanceof String[] items?java.util.Set.of(items):java.util.Set.of(); }
    private Map<String,Object> bad(BadCase item) { return params("id",item.id(),"tenant",item.tenantId(),"run",item.sourceRunId(),"sample",item.sourceSampleId(),"key",item.stableKey(),"failure",item.failureCategory(),"severity",item.severity(),"status",item.status().name(),"note",empty(item.ownerNote()),"created",Timestamp.from(item.createdAt()),"updated",Timestamp.from(item.updatedAt()),"version",item.rowVersion()); }
    private Map<String,Object> params(Object... pairs) { java.util.HashMap<String,Object> values = new java.util.HashMap<>(); for (int index = 0; index < pairs.length; index += 2) { values.put((String) pairs[index], pairs[index + 1]); } return values; }
}
