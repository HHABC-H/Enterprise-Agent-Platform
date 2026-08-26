package com.agent.evaluation;

import com.agent.api.TraceIdHolder;
import com.agent.chat.ChatResult;
import com.agent.chat.ChatService;
import com.agent.config.AgentPlatformProperties;
import com.agent.metrics.PlatformMetrics;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/** 数据集版本、异步评测运行和 Bad Case 回归的应用服务。 */
@Service
public class EvaluationLifecycleService {
    private final EvaluationRepository repository;
    private final ChatService chatService;
    private final RagasEvaluationPort ragas;
    private final PlatformMetrics metrics;
    private final Executor executor;
    private final Clock clock;
    private final AgentPlatformProperties properties;
    public EvaluationLifecycleService(EvaluationRepository repository, ChatService chatService, RagasEvaluationPort ragas, PlatformMetrics metrics,
                                      @Qualifier("evaluationExecutor") Executor executor, Clock clock, AgentPlatformProperties properties) {
        this.repository = repository; this.chatService = chatService; this.ragas = ragas; this.metrics = metrics; this.executor = executor; this.clock = clock; this.properties = properties;
    }
    public EvaluationDatasetVersion createDataset(String tenantId, String actorId, String name, String datasetId, List<SampleInput> inputs) {
        if (name == null || name.isBlank() || inputs == null || inputs.isEmpty()) { throw new IllegalArgumentException("数据集名称和样例不能为空。"); }
        String effectiveId = datasetId == null || datasetId.isBlank() ? UUID.randomUUID().toString() : datasetId;
        int nextVersion = repository.listDatasets(tenantId).stream().filter(item -> item.datasetId().equals(effectiveId)).mapToInt(EvaluationDatasetVersion::version).max().orElse(0) + 1;
        List<EvaluationSample> samples = buildSamples(inputs);
        Instant now = Instant.now(clock);
        EvaluationDatasetVersion version = new EvaluationDatasetVersion(UUID.randomUUID().toString(), tenantId, effectiveId, nextVersion, name.trim(), actorId, now, 0);
        repository.saveDataset(version, samples);
        return version;
    }
    public List<EvaluationDatasetVersion> listDatasets(String tenantId) { return repository.listDatasets(tenantId); }
    public EvaluationDatasetVersion getDataset(String tenantId, String datasetId, int version) { return repository.findDataset(tenantId, datasetId, version).orElseThrow(() -> new IllegalArgumentException("未找到评测数据集版本。")); }
    public List<EvaluationSample> exportSamples(String tenantId, String datasetId, int version) { EvaluationDatasetVersion item = getDataset(tenantId, datasetId, version); return repository.samples(tenantId, item.id()); }
    public EvaluationRun start(String tenantId, String actorId, String datasetId, int version) {
        EvaluationDatasetVersion dataset = getDataset(tenantId, datasetId, version);
        EvaluationRun run = new EvaluationRun(UUID.randomUUID().toString(), tenantId, dataset.id(), EvaluationRunState.QUEUED, actorId, "unknown", "default", properties.getLlm().getModelName(), "template-v1", Instant.now(clock), null, null, null, null, 0);
        repository.saveRun(run); metrics.recordEvaluation(); executor.execute(() -> execute(run.id(), tenantId)); return run;
    }
    /** 重启后仅重新排队未完成评测，绝不把已完成记录重新执行。 */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverUnfinishedRuns() { repository.recoverableRuns().forEach(run -> executor.execute(() -> execute(run.id(), run.tenantId()))); }
    public EvaluationRun rerun(String tenantId, String actorId, String runId) {
        EvaluationRun previous = getRun(tenantId, runId); EvaluationDatasetVersion dataset = repository.listDatasets(tenantId).stream().filter(item -> item.id().equals(previous.datasetVersionId())).findFirst().orElseThrow(() -> new IllegalArgumentException("原评测数据集不存在。"));
        return start(tenantId, actorId, dataset.datasetId(), dataset.version());
    }
    public EvaluationRun getRun(String tenantId, String runId) { return repository.findRun(tenantId, runId).orElseThrow(() -> new IllegalArgumentException("未找到评测运行。")); }
    public List<EvaluationCaseResult> results(String tenantId, String runId) { getRun(tenantId, runId); return repository.results(tenantId, runId); }
    public EvaluationRun cancel(String tenantId, String runId) {
        EvaluationRun current = getRun(tenantId, runId);
        EvaluationRun cancelled = new EvaluationRun(current.id(), current.tenantId(), current.datasetVersionId(), EvaluationRunState.CANCELLED, current.requestedBy(), current.codeVersion(), current.configVersion(), current.modelName(), current.promptHash(), current.createdAt(), null, Instant.now(clock), null, current.summary(), current.rowVersion() + 1);
        if (!repository.transitionRun(tenantId, runId, current.rowVersion(), EvaluationRunState.QUEUED, cancelled)) { throw new IllegalStateException("仅未开始的评测可以取消。"); }
        return cancelled;
    }
    public List<BadCase> badCases(String tenantId, BadCaseStatus status) { return repository.listBadCases(tenantId, status); }
    public BadCase changeBadCase(String tenantId, String id, BadCaseStatus status, String note) {
        BadCase current = repository.findBadCase(tenantId, id).orElseThrow(() -> new IllegalArgumentException("未找到 Bad Case。"));
        BadCase changed = new BadCase(current.id(), current.tenantId(), current.sourceRunId(), current.sourceSampleId(), current.stableKey(), current.failureCategory(), current.severity(), status, summarize(note), current.createdAt(), Instant.now(clock), current.rowVersion() + 1);
        if (!repository.updateBadCase(changed, current.rowVersion())) { throw new IllegalStateException("Bad Case 已被并发修改，请刷新后重试。"); }
        if (status == BadCaseStatus.CLOSED) { metrics.recordBadCaseClosed(); }
        return changed;
    }
    public EvaluationDatasetVersion promote(String tenantId, String actorId, String badCaseId, String targetDatasetId) {
        BadCase bad = repository.findBadCase(tenantId, badCaseId).orElseThrow(() -> new IllegalArgumentException("未找到 Bad Case。"));
        EvaluationRun sourceRun = getRun(tenantId, bad.sourceRunId());
        EvaluationSample source = repository.samples(tenantId, sourceRun.datasetVersionId()).stream().filter(item -> item.id().equals(bad.sourceSampleId())).findFirst().orElseThrow(() -> new IllegalArgumentException("Bad Case 来源样例不存在。"));
        EvaluationDatasetVersion latest = repository.listDatasets(tenantId).stream().filter(item -> item.datasetId().equals(targetDatasetId)).max(java.util.Comparator.comparingInt(EvaluationDatasetVersion::version)).orElseThrow(() -> new IllegalArgumentException("目标数据集不存在。"));
        List<SampleInput> copy = repository.samples(tenantId, latest.id()).stream().map(item -> new SampleInput(item.question(), item.expectedAnswer(), item.type(), item.expectedEvidence(), item.expectReject(), item.tags())).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (copy.stream().noneMatch(item -> fingerprint(item.question(), item.expectedAnswer(), item.type(), item.expectedEvidence(), item.expectReject()).equals(source.fingerprint()))) { copy.add(new SampleInput(source.question(), source.expectedAnswer(), source.type(), source.expectedEvidence(), source.expectReject(), source.tags())); }
        return createDataset(tenantId, actorId, latest.name(), targetDatasetId, copy);
    }
    public RegressionComparison compare(String tenantId, String baselineId, String currentId) {
        List<EvaluationCaseResult> baseline = results(tenantId, baselineId); List<EvaluationCaseResult> current = results(tenantId, currentId);
        java.util.Map<String, Boolean> oldValues = baseline.stream().collect(java.util.stream.Collectors.toMap(EvaluationCaseResult::sampleId, EvaluationCaseResult::passed, (a,b)->a));
        int fixed = 0, regressed = 0, unchanged = 0;
        for (EvaluationCaseResult item : current) { Boolean old = oldValues.get(item.sampleId()); if (old == null || old == item.passed()) { unchanged++; } else if (!old && item.passed()) { fixed++; } else { regressed++; } }
        for (int index = 0; index < fixed; index++) { metrics.recordRegressionFixed(); } for (int index = 0; index < regressed; index++) { metrics.recordRegressionRegressed(); }
        return new RegressionComparison(fixed, regressed, unchanged);
    }
    private void execute(String runId, String tenantId) {
        EvaluationRun queued = getRun(tenantId, runId);
        EvaluationRun running = new EvaluationRun(queued.id(), queued.tenantId(), queued.datasetVersionId(), EvaluationRunState.RUNNING, queued.requestedBy(), queued.codeVersion(), queued.configVersion(), queued.modelName(), queued.promptHash(), queued.createdAt(), Instant.now(clock), null, null, null, queued.rowVersion() + 1);
        if (!repository.transitionRun(tenantId, runId, queued.rowVersion(), EvaluationRunState.QUEUED, running)) { return; }
        try {
            List<EvaluationCaseResult> values = new ArrayList<>();
            for (EvaluationSample sample : repository.samples(tenantId, running.datasetVersionId())) { values.add(metrics.recordEvaluationCase(() -> evaluateOne(running, sample))); }
            EvaluationSummary summary = summarize(values);
            EvaluationRun complete = new EvaluationRun(running.id(), running.tenantId(), running.datasetVersionId(), EvaluationRunState.COMPLETED, running.requestedBy(), running.codeVersion(), running.configVersion(), running.modelName(), running.promptHash(), running.createdAt(), running.startedAt(), Instant.now(clock), null, summary, running.rowVersion() + 1);
            repository.transitionRun(tenantId, runId, running.rowVersion(), EvaluationRunState.RUNNING, complete);
        } catch (RuntimeException exception) {
            metrics.recordEvaluationFailure();
            EvaluationRun failed = new EvaluationRun(running.id(), running.tenantId(), running.datasetVersionId(), EvaluationRunState.FAILED, running.requestedBy(), running.codeVersion(), running.configVersion(), running.modelName(), running.promptHash(), running.createdAt(), running.startedAt(), Instant.now(clock), exception.getClass().getSimpleName(), null, running.rowVersion() + 1);
            repository.transitionRun(tenantId, runId, running.rowVersion(), EvaluationRunState.RUNNING, failed);
        }
    }
    private EvaluationCaseResult evaluateOne(EvaluationRun run, EvaluationSample sample) {
        long started = System.nanoTime(); boolean passed; String category = null; ChatResult response;
        try {
            response = chatService.chat(run.tenantId(), "evaluator", "evaluation-" + run.id() + "-" + sample.id(), sample.question());
            boolean evidence = !response.evidence().isEmpty(); boolean answerMatches = sample.expectedAnswer() == null || (!response.refused() && response.answer() != null && response.answer().contains(sample.expectedAnswer()));
            boolean rejectMatches = !sample.expectReject() || response.refused(); boolean evidenceMatches = sample.expectedEvidence() == null || evidence;
            passed = answerMatches && rejectMatches && evidenceMatches; if (!passed) { category = response.refused() && !sample.expectReject() ? "UNEXPECTED_REJECT" : evidence ? "ANSWER_MISMATCH" : "LOW_EVIDENCE"; }
            double duration = (System.nanoTime() - started) / 1_000_000.0;
            EvaluationCaseResult result = new EvaluationCaseResult(UUID.randomUUID().toString(), run.tenantId(), run.id(), sample.id(), passed, category, duration, evidence, response.refused(), "NOT_COMPUTED");
            String ragasStatus = ragas.evaluate(sample, result); result = new EvaluationCaseResult(result.id(), result.tenantId(), result.runId(), result.sampleId(), result.passed(), result.failureCategory(), result.durationMs(), result.evidenceHit(), result.refused(), ragasStatus);
            repository.saveCaseResult(result); if (!passed) { persistBadCase(run, sample, result); } return result;
        } catch (RuntimeException exception) {
            double duration = (System.nanoTime() - started) / 1_000_000.0; EvaluationCaseResult result = new EvaluationCaseResult(UUID.randomUUID().toString(), run.tenantId(), run.id(), sample.id(), false, "SAMPLE_ERROR", duration, false, false, "NOT_COMPUTED"); repository.saveCaseResult(result); persistBadCase(run, sample, result); return result;
        }
    }
    private void persistBadCase(EvaluationRun run, EvaluationSample sample, EvaluationCaseResult result) { BadCase saved = repository.saveBadCaseIfAbsent(new BadCase(UUID.randomUUID().toString(), run.tenantId(), run.id(), sample.id(), fingerprint(sample.question(), "", run.datasetVersionId(), result.failureCategory(), false), result.failureCategory(), "MEDIUM", BadCaseStatus.OPEN, null, Instant.now(clock), Instant.now(clock), 0)); if (saved.sourceRunId().equals(run.id())) { metrics.recordBadCaseCreated(); } }
    private EvaluationSummary summarize(List<EvaluationCaseResult> values) { int total=values.size(), passed=(int) values.stream().filter(EvaluationCaseResult::passed).count(), evidence=(int) values.stream().filter(EvaluationCaseResult::evidenceHit).count(), rejects=(int) values.stream().filter(EvaluationCaseResult::refused).count(); double average=values.stream().mapToDouble(EvaluationCaseResult::durationMs).average().orElse(0); String ragas=values.stream().anyMatch(item -> item.ragasStatus().equals("COMPUTED")) ? "COMPUTED" : "NOT_COMPUTED"; return new EvaluationSummary(total, passed, total-passed, evidence, rejects, total == 0 ? 0 : (double)passed/total, average, ragas); }
    private List<EvaluationSample> buildSamples(List<SampleInput> inputs) { java.util.HashSet<String> seen = new java.util.HashSet<>(); List<EvaluationSample> result = new ArrayList<>(); for (SampleInput item : inputs) { if (item.question() == null || item.question().isBlank() || !Set.of("single-hop","multi-hop","task","reject").contains(item.type())) { throw new IllegalArgumentException("评测样例必须提供问题和合法 type。"); } String key=fingerprint(item.question(), item.expectedAnswer(), item.type(), item.expectedEvidence(), item.expectReject()); if (!seen.add(key)) { throw new IllegalArgumentException("数据集版本包含重复样例。"); } result.add(new EvaluationSample(UUID.randomUUID().toString(), item.question().trim(), item.expectedAnswer(), item.type(), item.expectedEvidence(), item.expectReject(), item.tags(), key)); } return result; }
    private String summarize(String value) { if (value == null) return null; return value.length() > 500 ? value.substring(0,500) : value; }
    private static String fingerprint(String a, String b, String c, String d, boolean e) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((normalize(a)+"|"+normalize(b)+"|"+normalize(c)+"|"+normalize(d)+"|"+e).getBytes(StandardCharsets.UTF_8))); } catch (Exception exception) { throw new IllegalStateException("无法计算样例指纹。", exception); } }
    private static String normalize(String value) { return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT); }
    public record SampleInput(String question, String expectedAnswer, String type, String expectedEvidence, boolean expectReject, Set<String> tags) { }
    public record RegressionComparison(int fixed, int regressed, int unchanged) { }
}
