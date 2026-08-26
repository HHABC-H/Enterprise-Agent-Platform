package com.agent.infrastructure.evaluation;

import com.agent.config.AgentPlatformProperties;
import com.agent.evaluation.EvaluationCaseResult;
import com.agent.evaluation.EvaluationSample;
import com.agent.evaluation.RagasEvaluationPort;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** 仅在显式启用时调用外部 Ragas 服务，失败不伪造评分。 */
@Component
@ConditionalOnProperty(prefix = "ai-platform.evaluation.ragas", name = "enabled", havingValue = "true")
public class HttpRagasEvaluationAdapter implements RagasEvaluationPort {
    private final RestClient client;
    public HttpRagasEvaluationAdapter(AgentPlatformProperties properties) { this.client = RestClient.builder().baseUrl(properties.getEvaluation().getRagas().getBaseUrl()).build(); }
    @Override public String evaluate(EvaluationSample sample, EvaluationCaseResult result) {
        try {
            client.post().contentType(MediaType.APPLICATION_JSON).body(Map.of("question", sample.question(), "expectedAnswer", safe(sample.expectedAnswer()), "passed", result.passed()))
                    .retrieve().toBodilessEntity();
            return "COMPUTED";
        } catch (RuntimeException exception) { return "ERROR"; }
    }
    private String safe(String value) { return value == null ? "" : value; }
}
