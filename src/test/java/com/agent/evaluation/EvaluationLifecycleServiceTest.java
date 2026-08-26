package com.agent.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agent.evaluation.EvaluationLifecycleService.SampleInput;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** 验证 local 评测闭环不会依赖外部 Ragas 或数据库。 */
@SpringBootTest
@ActiveProfiles("local")
class EvaluationLifecycleServiceTest {
    @Autowired
    private EvaluationLifecycleService service;

    @Test
    void 数据集版本不可变且Ragas未配置时不伪造评分() throws Exception {
        EvaluationDatasetVersion first = service.createDataset("eval-tenant", "owner", "回归集", "dataset-a",
                List.of(new SampleInput("不存在的事实", null, "reject", null, true, Set.of("拒答"))));
        EvaluationDatasetVersion second = service.createDataset("eval-tenant", "owner", "回归集", "dataset-a",
                List.of(new SampleInput("另一个不存在的事实", null, "reject", null, true, Set.of())));
        assertThat(first.version()).isEqualTo(1);
        assertThat(second.version()).isEqualTo(2);
        assertThat(service.exportSamples("eval-tenant", "dataset-a", 1)).hasSize(1)
                .extracting(EvaluationSample::question).containsExactly("不存在的事实");

        EvaluationRun run = service.start("eval-tenant", "owner", "dataset-a", 1);
        EvaluationRun completed = waitForCompletion("eval-tenant", run.id());
        assertThat(completed.state()).isEqualTo(EvaluationRunState.COMPLETED);
        assertThat(completed.summary().ragasStatus()).isEqualTo("NOT_COMPUTED");
        assertThat(service.results("eval-tenant", run.id())).allMatch(item -> item.ragasStatus().equals("NOT_COMPUTED"));
        assertThatThrownBy(() -> service.results("other-tenant", run.id())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 失败样例沉淀为BadCase且可关闭() throws Exception {
        EvaluationDatasetVersion dataset = service.createDataset("bad-tenant", "owner", "失败集", null,
                List.of(new SampleInput("不存在的事实", "必然不存在的答案", "single-hop", "需要证据", false, Set.of())));
        EvaluationRun run = service.start("bad-tenant", "owner", dataset.datasetId(), dataset.version());
        waitForCompletion("bad-tenant", run.id());
        BadCase item = service.badCases("bad-tenant", BadCaseStatus.OPEN).stream().filter(value -> value.sourceRunId().equals(run.id())).findFirst().orElseThrow();
        BadCase closed = service.changeBadCase("bad-tenant", item.id(), BadCaseStatus.CLOSED, "已确认并处理");
        assertThat(closed.status()).isEqualTo(BadCaseStatus.CLOSED);
    }

    private EvaluationRun waitForCompletion(String tenantId, String id) throws Exception {
        for (int attempt = 0; attempt < 50; attempt++) {
            EvaluationRun run = service.getRun(tenantId, id);
            if (run.state() == EvaluationRunState.COMPLETED || run.state() == EvaluationRunState.FAILED) { return run; }
            Thread.sleep(20);
        }
        throw new AssertionError("评测运行未在预期时间内完成。");
    }
}
