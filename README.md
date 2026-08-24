# 企业级 AI 智能体平台（P0）

这是一个可启动、可验证的 Java 17 / Spring Boot 企业级 AI 智能体平台最小闭环。默认 `local` profile 完全使用确定性内存实现，因此不需要模型密钥、PostgreSQL 或 Redis 即可完成 Markdown 入库、权限检索和聊天拒答验证。

## 已实现范围

- Markdown 解析与 H1-H3 标题路径切分；单块超过 1500 个字符时按段落和字符边界继续拆分。
- 以 `tenantId`、用户标签和显式用户授权进行前置权限过滤的混合检索；向量与全文结果并行召回，采用 RRF（k=60）融合，最多返回 5 条证据。
- 默认关闭查询改写与重排。无授权证据或相关性不足时返回中文拒答，绝不生成无依据答案。
- 会话记忆键为 `session:{sessionId}`，最多 20 条，滑动 TTL 为一小时。`docker` profile 使用 Redis；长期画像仅保留端口，P0 不写入长期数据。
- 可测试状态机：`PLANNING → EXECUTING → REFLECTING → REPLANNING`，最多反思 3 次，随后进入明确拒答状态。
- `/actuator/prometheus` 暴露检索耗时、命中数、拒答次数及状态机执行次数。

## 启动

前置条件：JDK 17 与 Maven 3.9+。

```powershell
mvn test
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

可选模型适配器使用 LangChain4j OpenAI-compatible 客户端。只有设置下列变量并显式启用后才会调用外部模型：

```powershell
$env:AI_PLATFORM_LLM_ENABLED = 'true'
$env:AI_PLATFORM_LLM_API_KEY = '本地未提交的密钥'
$env:AI_PLATFORM_LLM_MODEL_NAME = '模型名称'
$env:AI_PLATFORM_LLM_BASE_URL = 'https://兼容接口地址/v1'
```

## API 示例

### 提交 Markdown 文档

```powershell
curl.exe -X POST http://localhost:8080/api/documents/markdown -H "Content-Type: application/json" -H "X-Trace-Id: demo-doc-001" -d '{"documentId":"handbook-v1","tenantId":"tenant-a","markdown":"# 平台手册\n\n系统支持 Markdown 文档与权限感知检索。\n\n## 安全\n\n无权限文档不会参与检索。","source":"产品手册","version":"v1","permissionTags":["public"],"allowedUserIds":[]}'
```

### 有权限聊天

```powershell
curl.exe -X POST http://localhost:8080/api/chat -H "Content-Type: application/json" -d '{"tenantId":"tenant-a","userId":"user-1","sessionId":"session-001","question":"系统支持什么文档？"}'
```

响应中的 `evidence` 包含文档 ID、Chunk ID、来源、融合分数和命中通道；`trace` 返回状态机轨迹。

### 运行内置评测

```powershell
curl.exe -X POST http://localhost:8080/api/evaluations/run
```

评测返回确定性答案匹配率、证据命中数、正确拒答数与平均耗时；它不计算或声称计算 Ragas 指标。

## Docker 与可观测性

```powershell
docker compose up --build
```

- 应用：`http://localhost:8080/actuator/health`
- Prometheus：`http://localhost:9090`
- Grafana：`http://localhost:3000`（默认用户名 `admin`；密码由 `GRAFANA_ADMIN_PASSWORD` 提供，默认仅限本地开发）

Compose 包含 PostgreSQL + pgvector、Redis、Prometheus 和 Grafana。P0 仅让 Redis 承担会话记忆；PostgreSQL/pgvector 容器用于验证运行环境，尚未承担向量持久化，避免把未实现适配器表述为生产能力。

## 配置

`application.yml` 的 `local` profile 默认使用内存存储。`docker` profile 自动设置 `ai-platform.memory.type=redis`，Redis 地址来自 `REDIS_HOST` 与 `REDIS_PORT`。所有模型配置均来自环境变量，仓库不保存密钥或私有地址。

## 验证命令

```powershell
mvn test
mvn package
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## P1 / P2 边界

P1 只保留 `GraphRelationSearchPort`、`KnowledgeIngestionEventPublisher` 与工作流检查点端口：Neo4j 父子索引/多跳查询、RabbitMQ 增量入库（内容 SHA-256、版本与幂等）、人工审批和暂停恢复均未实现。ParadeDB 全文索引也未接入，当前全文检索为内存降级实现。

P2 的 Ragas 指标、Bad Case 回归、版本对比仪表盘和生产数据库迁移均未实现；内置评测仅提供 P0 可重复执行的最小样例。
