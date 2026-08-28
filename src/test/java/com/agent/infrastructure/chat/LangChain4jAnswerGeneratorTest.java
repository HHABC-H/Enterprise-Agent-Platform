package com.agent.infrastructure.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.agent.document.Chunk;
import com.agent.document.DocumentMetadata;
import com.agent.memory.MemoryEntry;
import com.agent.retrieval.RetrievalEvidence;
import com.agent.retrieval.SearchSource;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LangChain4jAnswerGeneratorTest {

    @Test
    void 提示词应约束自然回答并包含少样本示例和授权证据() {
        Chunk chunk = new Chunk("chunk-1", "doc-1", "当前最小版本支持 Markdown 文档。",
                List.of("平台能力说明"), new DocumentMetadata("tenant-a", "平台能力说明", "v1", Set.of("public"), Set.of()));

        String prompt = LangChain4jAnswerGenerator.buildPrompt("平台支持什么文档？",
                List.of(new RetrievalEvidence(chunk, 0.9, Set.of(SearchSource.VECTOR))),
                List.of(new MemoryEntry("user", "那它可以上传吗？", Instant.parse("2026-08-27T00:00:00Z"))));

        assertThat(prompt).contains("不要以“根据已授权文档检索到的证据：”开头")
                .contains("示例一")
                .contains("示例二")
                .contains("示例三")
                .contains("现有资料没有说明")
                .contains("最近会话（仅用于理解上下文，不是事实来源）")
                .contains("用户：那它可以上传吗？")
                .contains("[证据1] 来源：平台能力说明")
                .contains("平台支持什么文档？");
    }
}
