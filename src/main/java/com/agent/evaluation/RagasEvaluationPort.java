package com.agent.evaluation;

/** 可选 Ragas 服务端口；未配置时必须明确返回未计算。 */
public interface RagasEvaluationPort {
    String evaluate(EvaluationSample sample, EvaluationCaseResult result);
}
