package com.agent.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AgentPlatformIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldIngestAndAnswerWithAuthorizedEvidence() throws Exception {
        mockMvc.perform(post("/api/documents/markdown").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentId\":\"integration-public\",\"tenantId\":\"integration\",\"markdown\":\"# 手册\\n\\n平台支持 Markdown 文档切分。\",\"source\":\"测试\",\"version\":\"v1\",\"permissionTags\":[\"public\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.chunkCount").value(1));
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).header("X-Trace-Id", "trace-integration")
                        .content("{\"tenantId\":\"integration\",\"userId\":\"bob\",\"sessionId\":\"s1\",\"question\":\"平台支持什么文档？\"}"))
                .andExpect(status().isOk()).andExpect(header().string("X-Trace-Id", "trace-integration"))
                .andExpect(jsonPath("$.data.refused").value(false)).andExpect(jsonPath("$.data.evidence[0].documentId").value("integration-public"));
    }

    @Test
    void shouldNotExposeUnauthorizedOrUnsupportedEvidence() throws Exception {
        mockMvc.perform(post("/api/documents/markdown").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentId\":\"integration-secret\",\"tenantId\":\"security\",\"markdown\":\"机密词为海王星。\",\"source\":\"机密\",\"version\":\"v1\",\"permissionTags\":[\"user:alice\"]}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"security\",\"userId\":\"bob\",\"sessionId\":\"s2\",\"question\":\"机密词是什么？\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.refused").value(true)).andExpect(jsonPath("$.data.evidence").isEmpty());
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"security\",\"userId\":\"alice\",\"sessionId\":\"s3\",\"question\":\"火星基地管理员是谁？\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.refused").value(true));
    }
}
