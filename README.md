# 企业级 AI 智能体平台

## P2 生产化与评测闭环

P2 在不改变既有 Markdown 入库和 `/api/chat` 请求字段的前提下，新增 `graphEvidenceUsed` 响应字段。当前普通聊天未调用图谱证据，该字段始终为 `false`；图谱独立查询成功不代表聊天已经使用 GraphRAG。

评测接口提供不可变的数据集版本、JSON 导入/导出、异步运行、结果查询、未开始运行取消、重跑与基线对比：`/api/evaluation-datasets`、`/api/evaluation-runs`、`/api/bad-cases`。写请求必须传递租户和用户；docker profile 还会校验 JWT 的 `tenant_id`、`sub` 与 `ROLE_APPROVER`，local profile 保持原有演示方式。

Ragas 默认关闭，结果会明确标记为 `NOT_COMPUTED`，不会生成模拟分数。Java 本地指标包含样例通过率、证据命中、拒答与平均耗时；外部 Ragas 服务仅在显式配置后才会被调用。

数据库结构唯一来源为 `src/main/resources/db/migration/`。Flyway 在 docker profile 使用迁移账户执行迁移，应用账户仅获得表级 DML 权限。待审批工作流、评测版本/运行/单例结果和 Bad Case 都以 `tenant_id` 与乐观锁字段隔离和保护。

Docker Compose 已加入命名卷、Redis/RabbitMQ 端口、固定 ParadeDB 镜像、Grafana 仪表盘和 Prometheus 告警。ParadeDB、RabbitMQ、Neo4j 与 Redis 的真实容器联调尚未执行；本轮只允许 Compose 静态校验。后续获得授权后可运行：

```powershell
docker compose --env-file .env up --build
```

容器启动后应验证健康检查、幂等入库、消息重试/死信、Neo4j 受限查询、ParadeDB 检索、重启后的审批恢复及评测指标；未完成这些步骤前不得宣称外部服务已真实集成。

基于 Java 17 与 Spring Boot 的企业知识问答最小闭环。所有检索均先执行租户与权限过滤；当证据不足时拒答，不生成无依据答案。

## 实现矩阵

| 能力 | local profile | docker profile |
| --- | --- | --- |
| P0 Markdown 切分、向量/全文 RRF、证据拒答、会话记忆与评测 | 已验证，内存实现 | Redis 会话记忆；其余适配器按配置启用 |
| 父子索引 | 已验证，子 Chunk 召回、父 Chunk 保存完整标题上下文 | 同步写入 PostgreSQL 全文索引与 Neo4j 图谱 |
| 关系图谱 | 已验证，确定性实体抽取的内存降级 | Neo4j：`Document-[:CONTAINS]->ParentChunk-[:CONTAINS]->Chunk-[:RELATES_TO]->Entity` |
| 全文检索 | 已验证，内存降级 | ParadeDB BM25，故障时仅返回空全文候选，不绕过权限或证据校验 |
| 增量入库 | 已验证，同步进程内事件 | RabbitMQ 发布/消费；处理器内部最多 3 次重试，最终失败进入死信队列 |
| 人工审批检查点 | 已验证，内存仓储 | 当前仍使用内存检查点；P2 可替换为数据库适配器 |

`RELATES_TO` 使用标题路径、反引号内容和大写标识符的确定性规则抽取，不代表语义实体识别。图谱查询始终限定 `tenantId`、`documentId`、`version`、用户权限、最大 3 跳和最多 50 条；没有将任意用户文本拼接进 Cypher。

## 启动与验证

前置条件：JDK 17、Maven 3.9+。

```powershell
mvn -q test
mvn -q package
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

`local` 是默认 profile，不连接 PostgreSQL、Redis、RabbitMQ 或 Neo4j。它使用同步事件发布器，因此文档提交成功时即可查询处理状态；没有外部依赖也可跑测试。

前后端已拆分：后端仅提供 `/api/**` 与 `/actuator/**`，前端位于 `frontend/`。另开一个 PowerShell 窗口启动前端：

```powershell
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173/` 使用工作台。Vite 会将 `/api`、`/actuator` 请求代理到本地 `http://localhost:8080`，不需要额外配置跨域。页面包含 Markdown 入库、证据问答、入库状态、图谱关系、审批和最小评测入口。

## API 示例

提交文档会创建或更新当前版本；内容 SHA-256 未变时标记为 `SKIPPED`，不会重复切分、建图或写索引。

```powershell
curl.exe -X POST http://localhost:8080/api/documents/markdown `
  -H "Content-Type: application/json" -H "X-Trace-Id: demo-doc-001" `
  -d '{"documentId":"handbook-v1","tenantId":"tenant-a","markdown":"# 平台手册\n\n## 安全\n\n系统支持 Markdown 文档与权限感知检索。","source":"产品手册","version":"v1","permissionTags":["public"],"allowedUserIds":[]}'

curl.exe "http://localhost:8080/api/documents/handbook-v1/ingestion-status?tenantId=tenant-a"
curl.exe "http://localhost:8080/api/graph/relations?tenantId=tenant-a&userId=user-1&documentId=handbook-v1&version=v1&maxHops=3&limit=20"
```

正常聊天保持 P0 契约不变。设置 `requireApproval=true` 后，检索充分时会返回 `waitingApproval=true` 与 `workflowId`，不会先生成答案。

```powershell
curl.exe -X POST http://localhost:8080/api/chat -H "Content-Type: application/json" `
  -d '{"tenantId":"tenant-a","userId":"user-1","sessionId":"s-001","question":"系统支持什么文档？","requireApproval":true}'

curl.exe -X POST http://localhost:8080/api/workflows/{workflowId}/approval -H "Content-Type: application/json" `
  -d '{"approverId":"reviewer","decision":"APPROVE","comment":"已核对","version":0}'
```

审批人由 `AI_PLATFORM_WORKFLOW_APPROVER_IDS` 配置（逗号分隔；local 默认 `reviewer`）。审批意见必填且最长 500 字。越权、过期、重复审批与版本不一致均返回 400；检查点只保存输入摘要、待审批动作、traceId 和版本，不记录完整敏感对话或密钥。

## docker profile 与环境变量

复制 `.env.example` 为本机 `.env` 并改为本地开发密码，然后执行：

```powershell
docker compose up --build
```

Compose 配置了 ParadeDB、Redis、Neo4j、RabbitMQ、Prometheus、Grafana 与应用。它通过 `db/init/01-paradedb.sql` 创建：

- `knowledge_document_revision`：受控消费端读取的当前文档版本。
- `knowledge_ingestion_task`：唯一 `idempotency_key`、尝试次数与失败原因。
- `knowledge_chunk` 和 ParadeDB BM25 索引：使用参数绑定查询；SQL 中同时约束租户和已通过授权的 Chunk ID。

RabbitMQ 交换机为 `knowledge.ingestion`，路由键为 `knowledge.document.changed`，队列为 `knowledge.ingestion.queue`。本地三次处理重试仍失败后，消息以拒绝不重入队的方式进入 `knowledge.ingestion.dlx` / `knowledge.ingestion.dead`，由人工核对任务状态、修复原因后以新的事件重新触发。消息确认与数据库/Neo4j 写入之间没有分布式事务：唯一幂等键及“仅处理当前内容哈希和版本”规则使重复投递无副作用；较旧事件不会覆盖较新版本。

常用变量：

| 变量 | 用途 |
| --- | --- |
| `POSTGRES_URL`、`POSTGRES_USER`、`POSTGRES_PASSWORD` | ParadeDB/PostgreSQL 连接 |
| `NEO4J_URI`、`NEO4J_USER`、`NEO4J_PASSWORD` | Neo4j Bolt 连接 |
| `RABBITMQ_HOST`、`RABBITMQ_PORT`、`RABBITMQ_USER`、`RABBITMQ_PASSWORD` | RabbitMQ 连接 |
| `REDIS_HOST`、`REDIS_PORT` | docker profile 会话记忆 |
| `AI_PLATFORM_WORKFLOW_APPROVER_IDS` | 可提交审批的用户 ID 列表 |
| `AI_PLATFORM_LLM_ENABLED`、`AI_PLATFORM_LLM_API_KEY`、`AI_PLATFORM_LLM_MODEL_NAME`、`AI_PLATFORM_LLM_BASE_URL` | 可选 OpenAI-compatible 模型；默认关闭 |

`/actuator/prometheus` 暴露检索、入库任务/成功/失败/重试/幂等跳过、图谱耗时和审批等待/通过/拒绝指标。日志只记录状态、数量和异常类型，不记录文档正文、模型密钥或审批内容。

## 验证边界与后续工作

本仓库已实际验证 local profile 的 Maven 测试与打包。Compose 文件、ParadeDB SQL、Neo4j 与 RabbitMQ 适配器已完成代码和配置，但当前没有启动 Docker 服务，因此不能声称这些外部服务已真实联调。启动后应验证：BM25 建索引与权限过滤、RabbitMQ 死信、Neo4j 多跳超时，以及 PostgreSQL/Neo4j 写入失败的恢复流程。

P2 范围仍包括 Ragas 真实指标、Bad Case 回归平台、版本仪表盘、持久化审批检查点和生产级迁移治理。
