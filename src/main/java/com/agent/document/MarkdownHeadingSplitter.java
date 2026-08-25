/**
 * 本文件定义 {@code MarkdownHeadingSplitter}，负责文档解析、切分、索引及父子块模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.document;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class MarkdownHeadingSplitter implements DocumentSplitter {

    /** 单个检索子块的最大 Unicode 码点数，避免截断表情等代理对字符。 */
    private static final int MAX_CHARS = 1500;
    private static final Pattern HEADING = Pattern.compile("^(#{1,3})\\s+(.+?)\\s*$");

    /** 按一至三级标题维护层级路径，再把超长段落拆为可稳定定位的子块。 */
    @Override
    public List<Chunk> split(ParsedDocument document) {
        List<Section> sections = new ArrayList<>();
        List<String> path = new ArrayList<>();
        StringBuilder content = new StringBuilder();
        List<String> currentPath = List.of();
        for (String line : document.markdown().split("\\n", -1)) {
            Matcher matcher = HEADING.matcher(line);
            if (matcher.matches()) {
                addSection(sections, content, currentPath);
                int level = matcher.group(1).length();
                String heading = matcher.group(2).trim();
                while (path.size() >= level) {
                    path.remove(path.size() - 1);
                }
                while (path.size() < level - 1) {
                    path.add("");
                }
                path.add(heading);
                currentPath = List.copyOf(path);
            } else {
                content.append(line).append('\n');
            }
        }
        addSection(sections, content, currentPath);

        List<Chunk> chunks = new ArrayList<>();
        int index = 0;
        for (Section section : sections) {
            for (String part : splitLongText(section.content())) {
                if (!part.isBlank()) {
                    String chunkId = sha256(document.documentId() + ":" + index++ + ":" + part);
                    chunks.add(new Chunk(chunkId, document.documentId(), part, section.headingPath(), document.metadata()));
                }
            }
        }
        return chunks;
    }

    private void addSection(List<Section> sections, StringBuilder content, List<String> path) {
        String value = content.toString().trim();
        if (!value.isBlank()) {
            sections.add(new Section(value, path));
        }
        content.setLength(0);
    }

    /** 优先按自然段切分；单段超限时再按 Unicode 码点强制分段。 */
    private List<String> splitLongText(String text) {
        if (text.codePointCount(0, text.length()) <= MAX_CHARS) {
            return List.of(text);
        }
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : text.split("\\n\\s*\\n")) {
            if (paragraph.codePointCount(0, paragraph.length()) > MAX_CHARS) {
                flush(result, current);
                splitByCodePoint(paragraph, result);
            } else if (current.codePointCount(0, current.length()) + paragraph.codePointCount(0, paragraph.length()) + 2 > MAX_CHARS) {
                flush(result, current);
                current.append(paragraph);
            } else {
                if (!current.isEmpty()) {
                    current.append("\n\n");
                }
                current.append(paragraph);
            }
        }
        flush(result, current);
        return result;
    }

    private void splitByCodePoint(String text, List<String> result) {
        int offset = 0;
        while (offset < text.length()) {
            int end = text.offsetByCodePoints(offset, Math.min(MAX_CHARS, text.codePointCount(offset, text.length())));
            result.add(text.substring(offset, end));
            offset = end;
        }
    }

    private void flush(List<String> result, StringBuilder builder) {
        if (!builder.isEmpty()) {
            result.add(builder.toString());
            builder.setLength(0);
        }
    }

    /** 以 SHA-256 生成与文档顺序、内容绑定的稳定子块标识。 */
    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : digest) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256。", exception);
        }
    }

    private record Section(String content, List<String> headingPath) {
    }
}
