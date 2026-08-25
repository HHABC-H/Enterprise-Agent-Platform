/**
 * 本文件定义 {@code PlatformMetrics}，负责平台运行指标采集。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/** 平台关键链路指标，不记录正文或密钥。 */
@Component
public class PlatformMetrics {
    private final Timer searchTimer;
    private final Counter refusalCounter;
    private final Counter workflowCounter;
    private final DistributionSummary hitSummary;
    private final Counter ingestionCounter;
    private final Counter ingestionSuccessCounter;
    private final Counter ingestionFailureCounter;
    private final Counter ingestionRetryCounter;
    private final Counter ingestionSkippedCounter;
    private final Timer ingestionTimer;
    private final Timer graphTimer;
    private final Counter approvalWaitingCounter;
    private final Counter approvalApprovedCounter;
    private final Counter approvalRejectedCounter;
    public PlatformMetrics(MeterRegistry registry) {
        searchTimer = Timer.builder("agent.retrieval.duration").register(registry);
        refusalCounter = Counter.builder("agent.retrieval.refusals").register(registry);
        workflowCounter = Counter.builder("agent.workflow.executions").register(registry);
        hitSummary = DistributionSummary.builder("agent.retrieval.hits").register(registry);
        ingestionCounter = Counter.builder("agent.ingestion.tasks").register(registry);
        ingestionSuccessCounter = Counter.builder("agent.ingestion.success").register(registry);
        ingestionFailureCounter = Counter.builder("agent.ingestion.failure").register(registry);
        ingestionRetryCounter = Counter.builder("agent.ingestion.retry").register(registry);
        ingestionSkippedCounter = Counter.builder("agent.ingestion.skipped").register(registry);
        ingestionTimer = Timer.builder("agent.ingestion.duration").register(registry);
        graphTimer = Timer.builder("agent.graph.duration").register(registry);
        approvalWaitingCounter = Counter.builder("agent.approval.waiting").register(registry);
        approvalApprovedCounter = Counter.builder("agent.approval.approved").register(registry);
        approvalRejectedCounter = Counter.builder("agent.approval.rejected").register(registry);
    }
    public <T> T recordSearch(Supplier<T> action) { return searchTimer.record(action); }
    public <T> T recordIngestionDuration(Supplier<T> action) { return ingestionTimer.record(action); }
    public <T> T recordGraph(Supplier<T> action) { return graphTimer.record(action); }
    public void recordRefusal() { refusalCounter.increment(); }
    public void recordWorkflow() { workflowCounter.increment(); }
    public void recordHitCount(int count) { hitSummary.record(count); }
    public void recordIngestion() { ingestionCounter.increment(); }
    public void recordIngestionSuccess() { ingestionSuccessCounter.increment(); }
    public void recordIngestionFailure() { ingestionFailureCounter.increment(); }
    public void recordIngestionRetry() { ingestionRetryCounter.increment(); }
    public void recordIngestionSkipped() { ingestionSkippedCounter.increment(); }
    public void recordApprovalWaiting() { approvalWaitingCounter.increment(); }
    public void recordApprovalApproved() { approvalApprovedCounter.increment(); }
    public void recordApprovalRejected() { approvalRejectedCounter.increment(); }
}
