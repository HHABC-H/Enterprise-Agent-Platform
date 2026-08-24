package com.agent.api;

import java.util.Map;

public record ErrorResponse(String traceId, String code, String message, Map<String, String> fieldErrors) {
}
