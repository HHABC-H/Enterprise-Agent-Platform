package com.agent.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 对外部或确定性抽取结果执行严格 JSON 校验。 */
@Component
public class StructuredMemoryExtractor {
    private static final Logger log = LoggerFactory.getLogger(StructuredMemoryExtractor.class);
    private static final Set<String> ROLES = Set.of("开发者", "产品", "管理者", "测试工程师");
    private final StructuredProfileExtractionPort port;
    private final ObjectMapper mapper;
    public StructuredMemoryExtractor(StructuredProfileExtractionPort port, ObjectMapper mapper) { this.port = port; this.mapper = mapper; }
    public UserProfile extract(List<MemoryEntry> entries) {
        try {
            Candidate candidate = mapper.readValue(port.extractJson(entries), Candidate.class);
            String role = text(candidate.role()); if (!role.isEmpty() && !ROLES.contains(role)) role = "";
            List<String> stack = candidate.techStack() == null ? List.of() : candidate.techStack().stream().map(this::text).filter(item -> !item.isEmpty()).limit(10).toList();
            return new UserProfile(text(candidate.preferredLanguage()), text(candidate.codingStyle()), stack, text(candidate.projectBudget()), role, Instant.now());
        } catch (RuntimeException | java.io.IOException exception) { log.warn("结构化画像抽取失败，已降级为空画像：{}", exception.getClass().getSimpleName()); return UserProfile.empty(); }
    }
    private String text(String value) { if (value == null) return ""; String normalized = value.trim(); return normalized.length() > 100 ? "" : normalized; }
    private record Candidate(String preferredLanguage, String codingStyle, List<String> techStack, String projectBudget, String role) { }
}
