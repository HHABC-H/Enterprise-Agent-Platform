package com.agent.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

/** 验证访问日志和关键业务日志的字段边界。 */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LoggingIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void 成功请求记录访问和业务日志且不泄露密码(CapturedOutput output) throws Exception {
        String traceId = "trace-access-" + UUID.randomUUID();
        String password = "private-password-123";
        String username = "log-user-" + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Trace-Id", traceId)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"tenantId\":\"logging-tenant\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Trace-Id", traceId));

        assertThat(output.getOut()).contains("business_operation traceId=" + traceId)
                .contains("operation=USER_REGISTER outcome=SUCCESS")
                .contains("http_access traceId=" + traceId)
                .contains("method=POST path=/api/auth/register status=201")
                .doesNotContain(password);
    }

    @Test
    void 未认证请求同样记录访问日志(CapturedOutput output) throws Exception {
        String traceId = "trace-unauthorized-" + UUID.randomUUID();

        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Trace-Id", traceId)
                        .content("{\"sessionId\":\"s1\",\"question\":\"test\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Trace-Id", traceId));

        assertThat(output.getOut()).contains("http_access traceId=" + traceId)
                .contains("method=POST path=/api/chat status=401");
    }

    @Test
    void 关键业务失败只记录异常类型而不记录请求参数(CapturedOutput output) throws Exception {
        String traceId = "trace-business-failure-" + UUID.randomUUID();
        String username = "duplicate-log-" + UUID.randomUUID().toString().substring(0, 8);
        String password = "duplicate-private-password";
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"tenantId\":\"logging-tenant\"}";

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Trace-Id", traceId).content(body))
                .andExpect(status().isConflict());

        assertThat(output.getOut()).contains("business_operation traceId=" + traceId)
                .contains("operation=USER_REGISTER outcome=FAILED")
                .contains("exception=UsernameAlreadyExistsException")
                .doesNotContain(password);
    }
}
