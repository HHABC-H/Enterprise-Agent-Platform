package com.agent.evaluation;

import com.agent.chat.ChatResult;
import com.agent.chat.ChatService;
import com.agent.document.DocumentMetadata;
import com.agent.document.DocumentService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class EvaluationService {
    private final DocumentService documentService;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    public EvaluationService(DocumentService documentService, ChatService chatService, ObjectMapper objectMapper) {
        this.documentService = documentService;
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }
    public EvaluationResult run() {
        documentService.ingest("builtin-evaluation-document", "# 平台能力\n\n本平台 P0 阶段支持 Markdown 文档切分、权限感知混合检索和会话记忆。\n\n## 安全边界\n\n无权限文档不会参与检索，证据不足时系统会拒答。",
                new DocumentMetadata("evaluation", "内置评测文档", "v1", java.util.Set.of("public"), java.util.Set.of()));
        List<EvaluationCase> cases = loadCases();
        int evidenceHits = 0;
        int correctRejects = 0;
        int answerMatches = 0;
        long started = System.nanoTime();
        for (int index = 0; index < cases.size(); index++) {
            EvaluationCase item = cases.get(index);
            ChatResult result = chatService.chat("evaluation", "evaluator", "evaluation-" + index, item.question());
            if (!result.evidence().isEmpty()) { evidenceHits++; }
            if ("reject".equals(item.type()) && result.refused()) { correctRejects++; }
            if (!result.refused() && result.answer() != null && result.answer().contains(item.expectedAnswer())) { answerMatches++; }
        }
        double duration = (System.nanoTime() - started) / 1_000_000.0;
        return new EvaluationResult(cases.size(), evidenceHits, correctRejects, (double) answerMatches / cases.size(), duration / cases.size());
    }
    private List<EvaluationCase> loadCases() {
        try (InputStream input = new ClassPathResource("eval/cases.json").getInputStream()) {
            return objectMapper.readValue(input, new TypeReference<List<EvaluationCase>>() { });
        } catch (Exception exception) {
            throw new IllegalStateException("内置评测集加载失败。", exception);
        }
    }
}
