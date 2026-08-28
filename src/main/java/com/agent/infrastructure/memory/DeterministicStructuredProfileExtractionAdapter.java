package com.agent.infrastructure.memory;

import com.agent.memory.MemoryEntry;
import com.agent.memory.StructuredProfileExtractionPort;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** local 环境确定性画像降级，不伪造未知字段。 */
@Component
@ConditionalOnProperty(prefix = "ai-platform.memory.profile-extraction", name = "llm-enabled", havingValue = "false", matchIfMissing = true)
public class DeterministicStructuredProfileExtractionAdapter implements StructuredProfileExtractionPort {
    @Override public String extractJson(List<MemoryEntry> entries) {
        String text = entries.stream().filter(item -> "user".equals(item.role())).map(MemoryEntry::content).reduce("", (left, right) -> left + "\n" + right);
        String language = match(text, "(?i)(Java|Python|JavaScript|TypeScript|Go|Rust)");
        String role = match(text, "(开发者|产品|管理者|测试工程师)");
        String style = text.contains("简洁") ? "简洁" : text.contains("注释") ? "重视注释" : "";
        return "{\"preferredLanguage\":\"" + language + "\",\"codingStyle\":\"" + style + "\",\"techStack\":[],\"projectBudget\":\"\",\"role\":\"" + role + "\"}";
    }
    private String match(String text, String pattern) { java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(text); return matcher.find() ? matcher.group(1) : ""; }
}
