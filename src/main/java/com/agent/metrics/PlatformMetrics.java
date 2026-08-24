package com.agent.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class PlatformMetrics {
    private final Timer searchTimer;
    private final Counter refusalCounter;
    private final Counter workflowCounter;
    private final io.micrometer.core.instrument.DistributionSummary hitSummary;

    public PlatformMetrics(MeterRegistry registry) {
        searchTimer = Timer.builder("agent.retrieval.duration").description("检索链路耗时").register(registry);
        refusalCounter = Counter.builder("agent.retrieval.refusals").description("证据不足拒答次数").register(registry);
        workflowCounter = Counter.builder("agent.workflow.executions").description("状态机执行次数").register(registry);
        hitSummary = io.micrometer.core.instrument.DistributionSummary.builder("agent.retrieval.hits").description("检索命中数").register(registry);
    }

    public <T> T recordSearch(Supplier<T> action) {
        return searchTimer.record(action);
    }

    public void recordRefusal() { refusalCounter.increment(); }
    public void recordWorkflow() { workflowCounter.increment(); }
    public void recordHitCount(int count) { hitSummary.record(count); }
}
