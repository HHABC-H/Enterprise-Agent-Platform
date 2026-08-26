package com.agent.evaluation;

import java.util.List;
import java.util.Optional;

/** 评测领域只依赖本端口，具体存储由 local/docker 适配器实现。 */
public interface EvaluationRepository {
    void saveDataset(EvaluationDatasetVersion version, List<EvaluationSample> samples);
    Optional<EvaluationDatasetVersion> findDataset(String tenantId, String datasetId, int version);
    List<EvaluationDatasetVersion> listDatasets(String tenantId);
    List<EvaluationSample> samples(String tenantId, String datasetVersionId);
    void saveRun(EvaluationRun run);
    Optional<EvaluationRun> findRun(String tenantId, String runId);
    boolean transitionRun(String tenantId, String runId, long expectedVersion, EvaluationRunState from, EvaluationRun replacement);
    default List<EvaluationRun> recoverableRuns() { return List.of(); }
    void saveCaseResult(EvaluationCaseResult result);
    List<EvaluationCaseResult> results(String tenantId, String runId);
    BadCase saveBadCaseIfAbsent(BadCase item);
    Optional<BadCase> findBadCase(String tenantId, String id);
    List<BadCase> listBadCases(String tenantId, BadCaseStatus status);
    boolean updateBadCase(BadCase item, long expectedVersion);
}
