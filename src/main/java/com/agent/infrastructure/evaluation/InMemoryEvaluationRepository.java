package com.agent.infrastructure.evaluation;

import com.agent.evaluation.BadCase;
import com.agent.evaluation.BadCaseStatus;
import com.agent.evaluation.EvaluationCaseResult;
import com.agent.evaluation.EvaluationDatasetVersion;
import com.agent.evaluation.EvaluationRepository;
import com.agent.evaluation.EvaluationRun;
import com.agent.evaluation.EvaluationRunState;
import com.agent.evaluation.EvaluationSample;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** local 评测仓储，保持重启外的确定性行为以支持演示和测试。 */
@Component
@Profile("local")
public class InMemoryEvaluationRepository implements EvaluationRepository {
    private final ConcurrentHashMap<String, EvaluationDatasetVersion> datasets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<EvaluationSample>> samples = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, EvaluationRun> runs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<EvaluationCaseResult>> results = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BadCase> badCases = new ConcurrentHashMap<>();
    @Override public void saveDataset(EvaluationDatasetVersion version, List<EvaluationSample> values) { datasets.put(key(version.tenantId(), version.id()), version); samples.put(key(version.tenantId(), version.id()), List.copyOf(values)); }
    @Override public Optional<EvaluationDatasetVersion> findDataset(String tenant, String datasetId, int version) { return datasets.values().stream().filter(item -> item.tenantId().equals(tenant) && item.datasetId().equals(datasetId) && item.version() == version).findFirst(); }
    @Override public List<EvaluationDatasetVersion> listDatasets(String tenant) { return datasets.values().stream().filter(item -> item.tenantId().equals(tenant)).sorted(java.util.Comparator.comparing(EvaluationDatasetVersion::createdAt)).toList(); }
    @Override public List<EvaluationSample> samples(String tenant, String versionId) { return List.copyOf(samples.getOrDefault(key(tenant, versionId), List.of())); }
    @Override public void saveRun(EvaluationRun run) { runs.put(key(run.tenantId(), run.id()), run); }
    @Override public Optional<EvaluationRun> findRun(String tenant, String id) { return Optional.ofNullable(runs.get(key(tenant, id))); }
    @Override public boolean transitionRun(String tenant, String id, long expected, EvaluationRunState from, EvaluationRun next) {
        return runs.computeIfPresent(key(tenant, id), (ignored, current) -> current.rowVersion() == expected && current.state() == from ? next : current) == next;
    }
    @Override public List<EvaluationRun> recoverableRuns() { return runs.values().stream().filter(item -> item.state() == EvaluationRunState.QUEUED).toList(); }
    @Override public void saveCaseResult(EvaluationCaseResult result) { results.computeIfAbsent(key(result.tenantId(), result.runId()), ignored -> new ArrayList<>()).add(result); }
    @Override public List<EvaluationCaseResult> results(String tenant, String runId) { return List.copyOf(results.getOrDefault(key(tenant, runId), List.of())); }
    @Override public BadCase saveBadCaseIfAbsent(BadCase item) { return badCases.computeIfAbsent(key(item.tenantId(), item.stableKey()), ignored -> item); }
    @Override public Optional<BadCase> findBadCase(String tenant, String id) { return badCases.values().stream().filter(item -> item.tenantId().equals(tenant) && item.id().equals(id)).findFirst(); }
    @Override public List<BadCase> listBadCases(String tenant, BadCaseStatus status) { return badCases.values().stream().filter(item -> item.tenantId().equals(tenant) && (status == null || item.status() == status)).toList(); }
    @Override public boolean updateBadCase(BadCase item, long expected) { return badCases.replace(key(item.tenantId(), item.stableKey()), badCases.get(key(item.tenantId(), item.stableKey())), item) && item.rowVersion() == expected + 1; }
    private String key(String tenant, String id) { return tenant + ":" + id; }
}
