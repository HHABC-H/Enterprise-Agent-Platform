package com.agent.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class MarkdownHeadingSplitterTest {
    private final MarkdownHeadingSplitter splitter = new MarkdownHeadingSplitter();
    private final DocumentMetadata metadata = new DocumentMetadata("tenant", "测试", "v1", Set.of("public"), Set.of());

    @Test
    void shouldKeepHeadingPathAndSplitUntitledText() {
        var chunks = splitter.split(new ParsedDocument("doc", "# 一级\n内容一\n## 二级\n内容二", metadata));
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).headingPath()).containsExactly("一级");
        assertThat(chunks.get(1).headingPath()).containsExactly("一级", "二级");
        assertThat(splitter.split(new ParsedDocument("plain", "没有标题的文本", metadata)).get(0).headingPath()).isEmpty();
    }

    @Test
    void shouldSplitContentLongerThanLimit() {
        var chunks = splitter.split(new ParsedDocument("long", "甲".repeat(1601), metadata));
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).content().codePointCount(0, chunks.get(0).content().length())).isLessThanOrEqualTo(1500);
    }
}
