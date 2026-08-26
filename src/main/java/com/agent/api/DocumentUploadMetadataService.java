package com.agent.api;

import com.agent.ingestion.DocumentRevisionStore;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/** 统一从上传文件名生成稳定文档标识，并根据当前最新版本计算下一个版本。 */
@Service
public class DocumentUploadMetadataService {
    private static final Pattern VERSION = Pattern.compile("[vV](\\d+)");
    private final DocumentRevisionStore revisions;

    public DocumentUploadMetadataService(DocumentRevisionStore revisions) {
        this.revisions = revisions;
    }

    public DocumentUploadMetadata resolve(String tenantId, String originalFileName) {
        String source = sourceFrom(originalFileName);
        String documentId = documentIdFrom(source);
        String previousVersion = revisions.find(tenantId, documentId).map(item -> item.metadata().version()).orElse(null);
        return new DocumentUploadMetadata(documentId, source, nextVersion(previousVersion), previousVersion);
    }

    private String sourceFrom(String originalFileName) {
        String normalized = originalFileName.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        int extension = name.lastIndexOf('.');
        return (extension > 0 ? name.substring(0, extension) : name).trim();
    }

    private String documentIdFrom(String source) {
        String value = source.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", "-")
                .replaceAll("^-+|-+$", "");
        return value.isBlank() ? "document" : value;
    }

    private String nextVersion(String previousVersion) {
        if (previousVersion == null || previousVersion.isBlank()) return "v1";
        Matcher matcher = VERSION.matcher(previousVersion.trim());
        if (!matcher.matches()) return "v2";
        try {
            return "v" + Math.addExact(Integer.parseInt(matcher.group(1)), 1);
        } catch (ArithmeticException | NumberFormatException ignored) {
            return "v2";
        }
    }
}
