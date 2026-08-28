package com.agent.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** 覆盖本地注册、登录、JWT 鉴权及原有审批、评测接口。 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AgentPlatformIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 注册登录并携带JWT访问原有业务接口() throws Exception {
        String username = unique("api-user");
        String token = registerAndLogin(username, "integration");

        mockMvc.perform(post("/api/documents/markdown").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("{\"documentId\":\"integration-public-" + username + "\",\"tenantId\":\"integration\",\"markdown\":\"# 手册\\n\\n平台支持 Markdown 文档切分。\",\"source\":\"测试\",\"version\":\"v1\",\"permissionTags\":[\"public\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.chunkCount").value(1));
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
                        .header("X-Trace-Id", "trace-integration")
                        .content("{\"tenantId\":\"integration\",\"userId\":\"" + username + "\",\"sessionId\":\"s1\",\"question\":\"平台支持什么文档？\"}"))
                .andExpect(status().isOk()).andExpect(header().string("X-Trace-Id", "trace-integration"))
                .andExpect(jsonPath("$.data.refused").value(false));
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"integration\",\"userId\":\"" + username + "\",\"sessionId\":\"s2\",\"question\":\"平台支持什么文档？\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 重复注册和错误密码有明确响应() throws Exception {
        String username = unique("duplicate-user");
        register(username, "tenant-duplicate").andExpect(status().isCreated());
        register(username, "tenant-duplicate").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("USERNAME_EXISTS"));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void 主动记忆和关闭会话均从JWT取得当前用户身份() throws Exception {
        String username = unique("memory-user");
        String token = registerAndLogin(username, "memory-tenant");
        mockMvc.perform(post("/api/memories").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
                        .content("{\"content\":\"语言偏好 Java\",\"type\":\"SEMANTIC\",\"importance\":0.9}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
                        .content("{\"tenantId\":\"memory-tenant\",\"userId\":\"" + username + "\",\"sessionId\":\"memory-session\",\"question\":\"没有证据的问题\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/chat/sessions/memory-session/close").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void 审批和评测接口在JWT保护下保持可用() throws Exception {
        String owner = unique("workflow-owner");
        String token = registerAndLogin(owner, "workflow-tenant");
        String documentId = "workflow-doc-" + owner;
        mockMvc.perform(post("/api/documents/markdown").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
                        .content("{\"documentId\":\"" + documentId + "\",\"tenantId\":\"workflow-tenant\",\"markdown\":\"审批手册说明流程。\",\"source\":\"测试\",\"version\":\"v1\",\"permissionTags\":[\"public\"]}"))
                .andExpect(status().isOk());
        MvcResult workflow = mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
                        .content("{\"tenantId\":\"workflow-tenant\",\"userId\":\"" + owner + "\",\"sessionId\":\"approval-session\",\"question\":\"审批手册说明什么？\",\"requireApproval\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.waitingApproval").value(true)).andReturn();
        String workflowId = objectMapper.readTree(workflow.getResponse().getContentAsString()).path("data").path("workflowId").asText();
        String approverToken = registerAndLogin("reviewer", "workflow-tenant");
        mockMvc.perform(post("/api/workflows/{workflowId}/approval", workflowId).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + approverToken)
                        .content("{\"approverId\":\"ignored\",\"version\":0,\"decision\":\"APPROVE\",\"comment\":\"已核验\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/evaluation-datasets").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
                        .content("{\"tenantId\":\"workflow-tenant\",\"userId\":\"" + owner + "\",\"name\":\"回归集\",\"samples\":[{\"question\":\"不存在的事实\",\"type\":\"reject\",\"expectReject\":true,\"tags\":[]}]}") )
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(1));
    }

    @Test
    void 上传文件名会自动生成文档标识来源并递增版本() throws Exception {
        String username = unique("upload-user");
        String token = registerAndLogin(username, "upload-tenant");
        String first = "{\"tenantId\":\"upload-tenant\",\"originalFileName\":\"security-policy.md\","
                + "\"markdown\":\"# First version\",\"permissionTags\":[\"public\"]}";
        mockMvc.perform(post("/api/documents/markdown").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
                        .content(first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentId").value("security-policy"))
                .andExpect(jsonPath("$.data.source").value("security-policy"))
                .andExpect(jsonPath("$.data.version").value("v1"));
        String second = first.replace("First version", "Second version");
        mockMvc.perform(post("/api/documents/markdown").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
                        .content(second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value("v2"));
    }

    @Test
    void 图谱文档和版本选择器只返回当前用户可访问的数据() throws Exception {
        String username = unique("catalog-user");
        String token = registerAndLogin(username, "catalog-tenant");
        String documentId = "catalog-doc-" + username;
        String first = "{\"documentId\":\"" + documentId + "\",\"tenantId\":\"catalog-tenant\","
                + "\"markdown\":\"# v1\\n\\n图谱版本一正文。\",\"source\":\"图谱测试文档\",\"version\":\"v1\",\"permissionTags\":[\"public\"]}";
        mockMvc.perform(post("/api/documents/markdown").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token).content(first))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/documents/markdown").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
                        .content(first.replace("# v1", "# v2").replace("版本一", "版本二").replace("\"version\":\"v1\"", "\"version\":\"v2\"")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/documents/markdown").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
                        .content("{\"documentId\":\"private-doc-" + username + "\",\"tenantId\":\"catalog-tenant\",\"markdown\":\"# 私有\",\"source\":\"私有文档\",\"version\":\"v1\",\"permissionTags\":[\"user:another-user\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/documents").header("Authorization", "Bearer " + token)
                        .param("tenantId", "catalog-tenant").param("userId", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].documentId").value(documentId))
                .andExpect(jsonPath("$.data[0].source").value("图谱测试文档"));
        mockMvc.perform(get("/api/documents/{documentId}/versions", documentId).header("Authorization", "Bearer " + token)
                        .param("tenantId", "catalog-tenant").param("userId", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].version").value("v1"))
                .andExpect(jsonPath("$.data[1].version").value("v2"));
        mockMvc.perform(get("/api/graph/relations").header("Authorization", "Bearer " + token)
                        .param("tenantId", "catalog-tenant").param("userId", username).param("documentId", documentId)
                        .param("version", "v1").param("maxHops", "1").param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.relations[0].type").value("CONTAINS"));
        mockMvc.perform(get("/api/graph/relations").header("Authorization", "Bearer " + token)
                        .param("tenantId", "catalog-tenant").param("userId", username).param("documentId", documentId)
                        .param("version", "v2").param("maxHops", "1").param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.relations[0].type").value("CONTAINS"));
    }

    private String registerAndLogin(String username, String tenantId) throws Exception {
        register(username, tenantId).andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"password-123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.tokenType").value("Bearer")).andReturn();
        JsonNode json = objectMapper.readTree(login.getResponse().getContentAsString());
        return json.path("data").path("accessToken").asText();
    }

    private org.springframework.test.web.servlet.ResultActions register(String username, String tenantId) throws Exception {
        return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"password-123\",\"tenantId\":\"" + tenantId + "\"}"));
    }

    private String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
