package com.agent.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** 验证未知异常仍返回统一响应并保留可检索的错误堆栈。 */
@ExtendWith(OutputCaptureExtension.class)
class ApiExceptionHandlerTest {
    @AfterEach
    void 清理追踪标识() {
        TraceIdHolder.clear();
    }

    @Test
    void 兜底异常记录完整错误并返回内部错误(CapturedOutput output) {
        TraceIdHolder.set("trace-unhandled-test");

        ResponseEntity<ErrorResponse> response = new ApiExceptionHandler()
                .handleUnexpected(new IllegalStateException("unexpected failure"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().traceId()).isEqualTo("trace-unhandled-test");
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(output.getOut()).contains("unhandled_exception traceId=trace-unhandled-test")
                .contains("java.lang.IllegalStateException: unexpected failure");
    }
}
