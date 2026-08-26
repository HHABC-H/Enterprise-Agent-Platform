package com.agent.infrastructure.evaluation;

import com.agent.evaluation.EvaluationCaseResult;
import com.agent.evaluation.EvaluationSample;
import com.agent.evaluation.RagasEvaluationPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 未配置 Ragas 时不产生模拟评分。 */
@Component
@ConditionalOnProperty(prefix = "ai-platform.evaluation.ragas", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopRagasEvaluationAdapter implements RagasEvaluationPort {
    @Override public String evaluate(EvaluationSample sample, EvaluationCaseResult result) { return "NOT_COMPUTED"; }
}
