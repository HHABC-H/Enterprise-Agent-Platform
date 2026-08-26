package com.agent.api;

import com.agent.evaluation.BadCase;
import com.agent.evaluation.BadCaseStatus;
import com.agent.evaluation.EvaluationCaseResult;
import com.agent.evaluation.EvaluationDatasetVersion;
import com.agent.evaluation.EvaluationLifecycleService;
import com.agent.evaluation.EvaluationLifecycleService.RegressionComparison;
import com.agent.evaluation.EvaluationLifecycleService.SampleInput;
import com.agent.evaluation.EvaluationRun;
import com.agent.evaluation.EvaluationSample;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** P2 评测数据集、运行和 Bad Case HTTP 接口。 */
@RestController
public class EvaluationLifecycleController {
    private final EvaluationLifecycleService service;
    private final IdentityGuard identity;
    public EvaluationLifecycleController(EvaluationLifecycleService service, IdentityGuard identity) { this.service = service; this.identity = identity; }

    @PostMapping("/api/evaluation-datasets")
    public ApiResponse<EvaluationDatasetVersion> createDataset(@Valid @RequestBody DatasetRequest request) {
        verify(request.tenantId(), request.userId());
        return ApiResponse.of(service.createDataset(request.tenantId(), request.userId(), request.name(), request.datasetId(), request.samples().stream().map(SampleRequest::toInput).toList()));
    }
    @PostMapping("/api/evaluation-datasets/import")
    public ApiResponse<EvaluationDatasetVersion> importDataset(@Valid @RequestBody DatasetRequest request) { return createDataset(request); }
    @GetMapping("/api/evaluation-datasets")
    public ApiResponse<List<EvaluationDatasetVersion>> datasets(@RequestParam String tenantId, @RequestParam String userId) { verify(tenantId, userId); return ApiResponse.of(service.listDatasets(tenantId)); }
    @GetMapping("/api/evaluation-datasets/{datasetId}/versions/{version}")
    public ApiResponse<EvaluationDatasetVersion> dataset(@PathVariable String datasetId, @PathVariable int version, @RequestParam String tenantId, @RequestParam String userId) { verify(tenantId, userId); return ApiResponse.of(service.getDataset(tenantId, datasetId, version)); }
    @GetMapping("/api/evaluation-datasets/{datasetId}/versions/{version}/export")
    public ApiResponse<List<EvaluationSample>> export(@PathVariable String datasetId, @PathVariable int version, @RequestParam String tenantId, @RequestParam String userId) { verify(tenantId, userId); return ApiResponse.of(service.exportSamples(tenantId, datasetId, version)); }
    @PostMapping("/api/evaluation-datasets/{datasetId}/versions")
    public ApiResponse<EvaluationDatasetVersion> newVersion(@PathVariable String datasetId, @Valid @RequestBody DatasetRequest request) { verify(request.tenantId(), request.userId()); return ApiResponse.of(service.createDataset(request.tenantId(), request.userId(), request.name(), datasetId, request.samples().stream().map(SampleRequest::toInput).toList())); }

    @PostMapping("/api/evaluation-runs")
    public ResponseEntity<ApiResponse<EvaluationRun>> start(@Valid @RequestBody RunRequest request) { verify(request.tenantId(), request.userId()); return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.of(service.start(request.tenantId(), request.userId(), request.datasetId(), request.version()))); }
    @GetMapping("/api/evaluation-runs/{runId}")
    public ApiResponse<EvaluationRun> run(@PathVariable String runId, @RequestParam String tenantId, @RequestParam String userId) { verify(tenantId, userId); return ApiResponse.of(service.getRun(tenantId, runId)); }
    @GetMapping("/api/evaluation-runs/{runId}/results")
    public ApiResponse<List<EvaluationCaseResult>> results(@PathVariable String runId, @RequestParam String tenantId, @RequestParam String userId) { verify(tenantId, userId); return ApiResponse.of(service.results(tenantId, runId)); }
    @PostMapping("/api/evaluation-runs/{runId}/cancel")
    public ApiResponse<EvaluationRun> cancel(@PathVariable String runId, @RequestParam String tenantId, @RequestParam String userId) { verify(tenantId, userId); return ApiResponse.of(service.cancel(tenantId, runId)); }
    @PostMapping("/api/evaluation-runs/{runId}/rerun")
    public ResponseEntity<ApiResponse<EvaluationRun>> rerun(@PathVariable String runId, @RequestParam String tenantId, @RequestParam String userId) { verify(tenantId, userId); return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.of(service.rerun(tenantId, userId, runId))); }
    @GetMapping("/api/evaluation-runs/{runId}/comparison")
    public ApiResponse<RegressionComparison> compare(@PathVariable String runId, @RequestParam String baselineRunId, @RequestParam String tenantId, @RequestParam String userId) { verify(tenantId, userId); return ApiResponse.of(service.compare(tenantId, baselineRunId, runId)); }

    @GetMapping("/api/bad-cases")
    public ApiResponse<List<BadCase>> badCases(@RequestParam String tenantId, @RequestParam String userId, @RequestParam(required = false) BadCaseStatus status) { verify(tenantId, userId); return ApiResponse.of(service.badCases(tenantId, status)); }
    @PostMapping("/api/bad-cases/{id}/promote")
    public ApiResponse<EvaluationDatasetVersion> promote(@PathVariable String id, @Valid @RequestBody PromoteRequest request) { verify(request.tenantId(), request.userId()); return ApiResponse.of(service.promote(request.tenantId(), request.userId(), id, request.targetDatasetId())); }
    @PutMapping("/api/bad-cases/{id}/status")
    public ApiResponse<BadCase> status(@PathVariable String id, @Valid @RequestBody BadCaseStatusRequest request) { verify(request.tenantId(), request.userId()); return ApiResponse.of(service.changeBadCase(request.tenantId(), id, request.status(), request.note())); }
    private void verify(String tenantId, String userId) { identity.assertRequestIdentity(tenantId, userId); }

    public record DatasetRequest(@NotBlank String tenantId, @NotBlank String userId, @NotBlank String name, String datasetId, @NotEmpty List<@Valid SampleRequest> samples) { }
    public record SampleRequest(@NotBlank String question, String expectedAnswer, @NotBlank String type, String expectedEvidence, boolean expectReject, Set<String> tags) { SampleInput toInput() { return new SampleInput(question, expectedAnswer, type, expectedEvidence, expectReject, tags); } }
    public record RunRequest(@NotBlank String tenantId, @NotBlank String userId, @NotBlank String datasetId, @NotNull Integer version) { }
    public record PromoteRequest(@NotBlank String tenantId, @NotBlank String userId, @NotBlank String targetDatasetId) { }
    public record BadCaseStatusRequest(@NotBlank String tenantId, @NotBlank String userId, @NotNull BadCaseStatus status, String note) { }
}
