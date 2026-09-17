# FinWork 开发参考手册

**版本**：v1.0（现状重构版）  
**最后核验日期**：2026-09-17  
**适用范围**：FinWork 前端、聊天后端、模型路由、受控查询工作流、脱敏与还原链路  
**代码范围**：`frontend/admin-web`、`backend/auth-service`、`backend/sql`  
**文档性质**：开发基线、接口索引、进度与风险记录  

## 1. 文档说明

本文按当前代码重新核验和整理，不再保留历史故障流水、大段实现示例、主观工时、未经压测的性能数字、默认账号密码和已经失效的阶段计划。

本文中的状态含义：

- **已实现**：当前代码存在，且有测试或可定位的调用链依据。
- **部分实现**：主路径存在，但能力、权限、安全或测试尚未完全闭环。
- **未实现**：配置、类型或界面可能有预留，但没有可运行实现。
- **已知风险**：当前代码可以确认的缺口，不能当成已完成能力。

事实优先级为：当前代码和测试结果 > 本文 > 历史设计文档。代码发生变化时必须同步更新本文。

## 2. 当前架构结论

FinWork 当前包含两条消息处理路径：

1. **普通聊天路径**：会话消息直接交给模型路由服务。
2. **报销受控查询路径**：识别报销查询意图后，选择工作流、映射变量、运行受控工具、本地脱敏、调用模型、本地还原并校验证据引用。

当前受控查询链路已经可以由 FinWork 和 Agent 工作台共同调用，但仅支持报销单查询工具和固定的线性流程。它不是任意 SQL、任意脚本或通用 DAG 执行平台。

```text
Browser /agent/chat
  -> AgentChatView
  -> useAgentChatSession
  -> agentChatApi
  -> /api/auth/agent/chat/**
  -> AgentChatController
       |
       +-- chat
       |    -> ModelRoutingService
       |    -> DeepSeekProvider / LocalModelProvider
       |
       +-- query_expense
            -> WorkflowSelectorService
            -> QueryParameterExtractor
            -> QueryTemplateVariableMapper
            -> ToolRuntimeService
            -> ExpenseDocumentControlledQueryTool
            -> DesensitizationService
            -> ModelRoutingService
            -> restore(mappingId)
            -> evidenceRef 白名单校验
            -> FinWork 输出
```

Agent 工作台测试运行通过 `ArchiveAgentExecutionWorker` 进入同一个 `AgentWorkflowRuntimeService`，因此 FinWork 与工作台受控查询共享运行时，不应再维护第二套执行逻辑。

## 3. 前端架构

### 3.1 关键文件

| 文件 | 职责 |
| --- | --- |
| `frontend/admin-web/src/views/agent/AgentChatView.vue` | FinWork 页面、模型选择、空状态、自动滚动和交互编排 |
| `frontend/admin-web/src/components/agent/ChatMessageList.vue` | 消息列表、状态、失败提示和重试入口 |
| `frontend/admin-web/src/components/agent/ChatInputBox.vue` | 文本输入、长度限制、禁用和发送事件 |
| `frontend/admin-web/src/composables/useAgentChatSession.ts` | 会话生命周期、模型选择、消息发送、乐观更新和错误状态 |
| `frontend/admin-web/src/api/modules/agent-chat.ts` | FinWork API 封装 |
| `frontend/admin-web/src/api/modules/agent-chat-types.ts` | 会话、消息、模型元数据和请求响应类型 |
| `frontend/admin-web/src/api/modules/core.ts` | API 基础路径、Bearer Token、统一响应、401 和超时 |
| `frontend/admin-web/src/router/route-catalog.ts` | `/agent/chat` 路由 |
| `frontend/admin-web/src/router/route-meta.ts` | FinWork 标题与 `agent:chat:view` 权限 |
| `frontend/admin-web/src/router/navigation-config.ts` | Agent 导航树中的 FinWork 入口 |

### 3.2 页面初始化

```text
进入 /agent/chat
  -> GET /auth/agent/chat/meta
  -> 恢复 localStorage 中的 Provider/模型选择
  -> URL 有 sessionId：加载最近 50 条消息
  -> URL 无 sessionId：创建新会话
  -> 开放输入
```

模型选择存储键为 `finwork-chat-model-selection`。Provider 或模型切换后立即写入本地存储，发送消息时将 `providerCode` 和 `model` 传给后端。

元数据加载失败时，前端回退到 `LOCAL_7B / deepseek-r1:7b`。该回退只表示前端仍有选项，不代表 Ollama 服务或模型真实可用。

### 3.3 消息状态

- 发送前生成 `clientMessageId` 并追加 `pending` 用户消息。
- 成功后将该消息标记为 `success`，再追加助手消息。
- 失败后保留用户消息并标记为 `failed`。
- 服务端消息优先使用 `id`，本地消息使用 `clientMessageId` 作为 Vue key。
- 消息通过文本插值展示，当前不解析 Markdown，也不使用 `v-html`。

### 3.4 前端当前缺口

- 后端已有会话列表接口，前端 API 模块和页面尚未接入。
- 没有会话切换、重命名、删除、搜索和分页。
- 没有 SSE/WebSocket 流式输出。
- 重试当前会调用普通发送逻辑并创建新消息，原失败消息的状态闭环不完整。
- 本地模型回退没有健康检查。
- `agent:chat:send` 没有用于禁用发送按钮。

## 4. 后端关键接口

浏览器默认通过 `/api` 前缀访问。网关或开发代理去除 `/api` 后，`auth-service` 实际监听 `/auth/**`。

### 4.1 FinWork 聊天接口

控制器：`backend/auth-service/src/main/java/com/finex/auth/agent/controller/AgentChatController.java`

| 方法 | 后端路径 | 用途 | 前端状态 |
| --- | --- | --- | --- |
| GET | `/auth/agent/chat/meta` | Provider 和模型元数据 | 已使用 |
| POST | `/auth/agent/chat/conversations` | 创建会话 | 已使用 |
| GET | `/auth/agent/chat/conversations` | 最近 20 个会话 | 后端已实现，前端未接入 |
| GET | `/auth/agent/chat/conversations/{sessionId}/messages` | 最近 50 条消息 | 已使用 |
| POST | `/auth/agent/chat/conversations/{sessionId}/messages` | 发送消息 | 已使用 |

发送消息请求：

```json
{
  "message": "查询我待处理的报销单",
  "providerCode": "LOCAL_7B",
  "model": "deepseek-r1:7b"
}
```

响应结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "sessionId": "sess_xxx",
    "content": "处理结果",
    "intent": "query_expense",
    "timestamp": "2026-09-17T15:00:00Z"
  },
  "timestamp": 0
}
```

### 4.2 Agent 工作台接口

控制器：`backend/auth-service/src/main/java/com/finex/auth/controller/ArchiveAgentController.java`

接口同时兼容 `/auth/agent/workbench` 和旧路径 `/auth/archives/agents`。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/auth/agent/workbench` | Agent 列表 |
| GET | `/auth/agent/workbench/meta` | 模型、节点和工具元数据 |
| GET | `/auth/agent/workbench/{id}` | Agent 详情 |
| POST | `/auth/agent/workbench` | 新建 Agent |
| PUT | `/auth/agent/workbench/{id}` | 保存 Agent 和工作流 |
| DELETE | `/auth/agent/workbench/{id}` | 删除 Agent |
| POST | `/auth/agent/workbench/{id}/publish` | 发布版本 |
| POST | `/auth/agent/workbench/{id}/run` | 测试运行 |
| GET | `/auth/agent/workbench/{id}/runs` | 运行记录 |
| GET | `/auth/agent/workbench/runs/{runId}` | 步骤日志 |
| POST | `/auth/agent/workbench/{id}/toggle-status` | 启停或归档 |

### 4.3 脱敏管理接口

控制器：`backend/auth-service/src/main/java/com/finex/auth/agent/desensitization/DesensitizationController.java`

统一前缀：`/auth/agent/desensitization`

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/entity-types` | 查询脱敏实体类型 |
| POST | `/entity-types` | 保存实体类型 |
| DELETE | `/entity-types/{code}` | 删除实体类型 |
| PUT | `/entity-types/{code}/enabled` | 启停实体类型 |
| POST | `/test` | 测试脱敏规则 |
| GET | `/mappings` | 分页查看映射 |
| POST | `/mappings` | 按条件查看映射 |
| GET | `/mappings/stats` | 映射统计 |
| POST | `/mappings/cleanup` | 清理过期映射 |

这些是管理接口，当前控制器未显式调用 `AccessControlService`，属于权限风险，不能仅依赖前端隐藏菜单。

## 5. 后端关键服务

| 服务 | 文件 | 当前职责 |
| --- | --- | --- |
| `ConversationService` | `agent/service/ConversationService.java` | 会话创建、会话查询、消息持久化和统计 |
| `ModelRoutingService` | `agent/model/ModelRoutingService.java` | Provider 注册、首选 Provider、规则路由和失败降级 |
| `WorkflowSelectorService` | `agent/workflow/WorkflowSelectorService.java` | 查找可运行的已发布 Agent，校验工作流和工具绑定，提供内置回退 |
| `WorkflowGraphPlan` | `agent/workflow/WorkflowGraphPlan.java` | 校验节点、连线、连通性、循环和单链路约束 |
| `QueryParameterExtractor` | `agent/workflow/QueryParameterExtractor.java` | 从中文输入抽取范围、状态、日期、金额、关键词和 limit |
| `QueryTemplateVariableMapper` | `agent/workflow/QueryTemplateVariableMapper.java` | 将 `{{user.*}}`、`{{input.*}}` 映射为受控变量 |
| `ToolRuntimeService` | `agent/workflow/ToolRuntimeService.java` | 注册 `ControlledTool` Bean，校验工具编码和查询模板 |
| `ExpenseDocumentControlledQueryTool` | `agent/workflow/ExpenseDocumentControlledQueryTool.java` | 查询当前用户可访问的报销单和待办 |
| `AgentWorkflowRuntimeService` | `agent/workflow/AgentWorkflowRuntimeService.java` | 串联工作流、查询、脱敏、模型、还原、证据校验和运行日志 |
| `DesensitizationService` | `agent/desensitization/DesensitizationService.java` | JSON 字段脱敏、正则脱敏、映射持久化和还原 |
| `ArchiveAgentExecutionWorker` | `support/archiveagent/ArchiveAgentExecutionWorker.java` | 工作台运行入口；受控工具流程转交统一运行时 |
| `AgentPlatformInitializer` | `agent/config/AgentPlatformInitializer.java` | 应用启动时注册模型 Provider 并报告安全模式 |

## 6. 受控查询运行链路

### 6.1 意图识别

当前意图识别位于 `AgentChatController.detectIntent`，是关键词规则，不是模型分类器：

- 输入包含“报销单”“报销”或“费用”；
- 同时包含“查”“看”“显示”或“有哪些”；
- 才进入 `query_expense`。

其他输入进入普通聊天。该规则覆盖有限，容易出现漏判和误判。

### 6.2 工作流选择

`WorkflowSelectorService` 当前按以下条件查询候选：

- `owner_user_id` 为当前用户或 `0`（全局）。
- Agent 状态为 `READY`。
- 存在已发布版本。
- 工作流结构和工具绑定通过运行时校验。

候选按用户归属、更新时间和 ID 排序，返回第一个可执行工作流。当前 `message` 参数没有参与关键词或语义打分，所以尚未实现“根据关键词在多个已发布工作流中精确选择”。

未找到候选时会使用代码内置的报销查询工作流，而不是直接失败。内置回退提高可用性，但会掩盖工作台发布配置错误。

### 6.3 当前可执行图约束

受控查询运行时只支持：

```text
start -> tool -> llm -> end
```

具体限制：

- 必须只有一个开始节点和一个结束节点。
- 必须且只能有一个工具节点、一个 LLM 节点。
- 工具必须先于 LLM。
- 仅支持 `start`、`tool`、`llm`、`end`。
- 每个中间节点必须各有一条入线和出线。
- 不支持分支、并行、循环、条件和多个工具。

画布可以展示更多节点类型，不代表后端当前能够执行。新增节点时必须同步扩展运行时。

### 6.4 查询变量

规范变量由 `QueryTemplateVariables` 承载：

- `rawKeyword`
- `normalizedKeyword`
- `scope`
- `status`
- `startDate`、`endDate`
- `minAmount`、`maxAmount`
- `limit`，最终限制在 1 到 50

抽取器当前支持：

- 本月、上个月。
- 大于、超过、不少于、小于、低于、不超过等金额表达。
- 草稿、待审批、已审批、驳回、待支付、已支付、已完成和异常状态。
- “待我处理”“待我审批”等范围表达。
- `DOC`、`EXP`、`BX`、`FY` 开头的单据编码。

画布变量映射只允许写入规范变量，未知目标字段会被忽略，避免通过映射扩大查询权限。

### 6.5 工具注册和查询模板

工具接口：`ControlledTool`。`ToolRuntimeService` 在 Spring 启动时收集全部 `ControlledTool` Bean，并按 `toolCode` 建立注册表。

当前唯一受控查询工具：

```text
toolCode: expense.query_my_expenses
templates:
  expense.my_expenses.v1
  expense.accessible_expenses.v2
```

当前不是在工作台编写任意脚本或 SQL。所谓查询模板是后端 Java 工具允许的固定模板编码，工具内部使用 MyBatis 条件构造器执行查询。

查询范围：

- `expense.my_expenses.v1`：当前用户提交的报销单。
- `expense.accessible_expenses.v2`：当前用户提交的报销单，或分配给当前用户且状态为 `PENDING` 的任务对应单据。
- 仅查询 `templateType = report` 且未删除的数据。
- 支持状态、日期、金额、关键词和数量限制。

结果为结构化记录，每条包含 `entityRef` 和 `evidenceRef`，当前格式为 `EXPENSE_DOCUMENT:{id}`。

### 6.6 脱敏、模型与还原

受控查询结果不会直接传给云端模型。运行时执行：

1. 根据固定敏感字段表对 JSON 字段整体替换。
2. 将相同关键词传播为同一占位符。
3. 使用数据库“脱敏规则配置”中的启用规则做正则脱敏。
4. 数据库规则不可用或尚未初始化时，回退到 `desensitization-rules.yml`。
5. 生成 `mappingId`，将原值映射保存到数据库并缓存。
6. 只把脱敏 JSON 交给分析模型。
7. 使用同一 `mappingId` 本地还原模型输出。
8. 将模型返回的 `evidenceRefs` 与工具真实返回的引用做白名单交集。
9. 本地拼接查询记录和证据引用，输出给 FinWork。

结构化字段表当前在 `AgentWorkflowRuntimeService.CLOUD_SENSITIVE_FIELDS` 中维护，覆盖关键词、单据 ID/编号、标题、事由和金额。通用人名、手机号、身份证号、银行卡号、邮箱、地址等由脱敏规则配置识别。

映射默认有效期为 24 小时。过期后还原返回仍带占位符的文本。`DesensitizationCleanupTask` 每小时清理缓存和数据库过期映射。

需要特别注意：**普通聊天路径当前不经过上述脱敏链路**。普通聊天会把用户输入和最近会话消息直接交给模型路由服务，因此不能把 FinWork 整体描述为“所有内容上云前都会脱敏”。

## 7. 模型路由与配置

### 7.1 配置文件

- `backend/auth-service/src/main/resources/application.yml`
- `backend/auth-service/src/main/resources/application-agent.yml`
- `backend/auth-service/src/main/resources/desensitization-rules.yml`

`application.yml` 激活 `agent` Profile，因此 `application-agent.yml` 生效。

### 7.2 当前 Provider

| Provider | 实现 | 注册状态 | 凭证/依赖 |
| --- | --- | --- | --- |
| `DEEPSEEK` | `DeepSeekProvider` | 已实现并注册 | `DEEPSEEK_API_KEY`，实际调用通过 `deepSeekModelProviderAdapter` |
| `LOCAL_7B` | `LocalModelProvider` | 已实现；开关默认开启 | Ollama，默认 `http://localhost:11434/api/chat` 和 `deepseek-r1:7b` |
| `KIMI` | 只有配置项 | 未实现 Provider Bean，因此元数据中应为不可用 | `KIMI_API_KEY` 当前不足以让其运行 |

用户显式选择 Provider 时，`ModelRoutingService` 优先调用该 Provider；未注册、不支持场景或调用失败时，再按规则降级。模型名称通过 `extraParams.model` 传给 Provider。

DeepSeek 和本地 Provider 的 `chatStream` 当前都回退到同步调用，前端也没有流式客户端。

### 7.3 凭证原则

- 云端凭证只允许由后端环境变量或受控凭证服务读取。
- 前端、Agent 配置 JSON、运行日志和数据库脚本中不得保存明文 API Key。
- “当前模型提供商缺少凭证配置”通常表示 Provider 已选中但对应后端适配器没有可用凭证，不是前端模型下拉框问题。

## 8. 数据与数据库

### 8.1 关键表

| 范围 | 表 |
| --- | --- |
| 会话 | `ea_agent_conversation` |
| 消息 | `ea_agent_message` |
| Agent 定义与版本 | `ea_agent_definition`、`ea_agent_version` |
| 工具与触发器 | `ea_agent_tool_binding`、`ea_agent_trigger` |
| 运行审计 | `ea_agent_run`、`ea_agent_run_step`、`ea_agent_run_artifact` |
| 脱敏规则 | `ea_desensitization_entity_type` |
| 脱敏映射 | `ea_desensitization_mapping` |
| 报销查询 | `process_document_instance`、`process_document_task` 对应实体表 |

`AgentConversation` 当前要求自增 `id` 和 `TenantAwareEntity` 字段；`AgentMessage` 要求 `id`、`tenant_id`、`masked_content`、`tokens`、`trace_id` 等字段。数据库结构必须与实体保持一致。

### 8.2 SQL 脚本现状

| 脚本 | 用途 | 当前注意事项 |
| --- | --- | --- |
| `backend/sql/init_archive_agent.sql` | 工作台定义、版本、工具和运行表 | 工作台基础表 |
| `backend/sql/migrate_archive_agent.sql` | 工作台迁移 | 使用前检查现有版本 |
| `backend/sql/migrate_desensitization.sql` | 脱敏规则和映射表 | 创建并重建数据库清理事件，需要事件权限 |
| `backend/sql/fix_agent_tables_idempotent.sql` | 补齐会话和消息字段 | 执行前仍需核对现有主键和字段，不能仅凭文件名判断安全 |
| `backend/sql/init_agent_platform_permissions_v2.sql` | Agent 平台权限 | 使用 `NOT EXISTS` 控制重复 |
| `backend/sql/migrate_agent_20260406131739_accessible_query_v2.sql` | 升级指定 Agent 到 v2 查询模板 | 只适用于目标 Agent 编码 |
| `backend/sql/init_agent_controlled_query_workflow.sql` | 历史默认工作流初始化 | 当前包含 `transform` 节点，与现运行时约束不兼容，不应直接作为当前标准脚本 |

仓库中存在多份早期建表和修复脚本。`init_agent_chat_tables.sql` 的初始会话表以 `session_id` 为主键，与当前 `AgentConversation.id` 映射不一致；部分早期脚本会备份后重建表。生产环境执行任何 Agent SQL 前必须先对照当前实体和实际表结构，并完成备份、预演和回滚方案。

## 9. 权限、租户与安全边界

### 9.1 当前权限

| 能力 | 权限码 | 当前执行情况 |
| --- | --- | --- |
| 查看 FinWork | `agent:chat:view` | 路由和菜单使用 |
| 发送消息 | `agent:chat:send` | SQL 中存在，但前端发送按钮和 `AgentChatController` 未强制校验 |
| 查看工作台 | `agent:workbench:view`，兼容 `agents:view` | 前后端使用 |
| 工作台操作 | `agent:workbench:create/edit/delete/run/publish/view_logs`，兼容 `agents:*` | 工作台按钮和后端控制器校验 |
| 脱敏管理 | `agent:desensitization:*` | 菜单有查看权限；当前管理控制器缺少显式操作权限校验 |

`AgentChatController` 当前校验登录状态和会话所属用户，但没有调用 `AccessControlService` 校验 `agent:chat:view` 或 `agent:chat:send`。

### 9.2 租户现状

会话和脱敏规则服务会读取 `TenantContext`。但当前存在以下事实：

- `AgentWebConfig` 只将 `TenantContextInterceptor` 注册到 `/api/agent/**`。
- 实际 Agent 控制器路径为 `/auth/agent/**`。
- 因此该拦截器可能不会覆盖实际请求。
- Demo 模式下 `TenantContext` 会回退到 `default` 租户，问题可能被隐藏。
- Enterprise 模式下上下文缺失会直接抛错。
- 报销受控查询工具没有显式增加 tenant/company 条件，主要依赖当前用户 ID 和平台数据隔离。

在修正拦截器路径并验证企业模式前，不能宣称多租户链路已经完整闭环。

### 9.3 脱敏映射安全

映射表保存可还原的原值，属于高敏感数据：

- `findByMappingId` 和 `findBySessionId` 当前 Mapper SQL 没有租户条件。
- 映射 JSON 当前未见字段级加密实现。
- 清理任务按过期时间全局删除，适合作业，但管理查询必须受权限和租户限制。
- `postValidation` 和泄漏规则存在配置，但当前没有代码消费，尚未形成真正的上云前后验阻断。

## 10. 开发进度

| 能力 | 状态 | 代码依据 |
| --- | --- | --- |
| FinWork 页面和独立路由 | 已实现 | `AgentChatView.vue`、路由配置 |
| 会话创建和历史加载 | 已实现 | `ConversationService`、聊天接口 |
| Provider/模型选择 | 已实现 | `/meta`、Composable、模型下拉框 |
| 本地模型选择 | 已实现 | `LocalModelProvider`、前端回退元数据 |
| 普通模型聊天 | 已实现 | `AgentChatController.callLLM` |
| 报销查询意图识别 | 部分实现 | 关键词规则，覆盖有限 |
| 已发布工作流选择 | 部分实现 | 能筛选可运行版本，但不按关键词或语义匹配 |
| 画布流程真实执行 | 部分实现 | 仅支持 `start -> tool -> llm -> end` |
| 查询变量抽取和映射 | 已实现 | 抽取器、变量映射器及测试 |
| 受控工具注册 | 已实现 | Spring `ControlledTool` Bean 注册表 |
| 报销单受控查询 | 已实现 | `ExpenseDocumentControlledQueryTool` |
| 任意查询脚本 | 未实现 | 当前明确禁止任意脚本和 SQL |
| 数据库脱敏规则接入 | 已实现 | 数据库优先、YAML 回退 |
| 结构化字段脱敏 | 已实现 | `desensitizeJsonFields` |
| `mappingId` 本地还原 | 已实现 | 映射持久化、缓存和恢复 |
| `entityRef` / `evidenceRef` | 已实现 | 工具生成、运行时白名单校验 |
| 运行步骤审计 | 已实现 | `ea_agent_run`、`ea_agent_run_step` |
| 普通聊天脱敏 | 未实现 | 普通聊天直接调用模型 |
| Kimi Provider | 未实现 | 只有配置，无 Provider Bean |
| 流式输出 | 未实现 | Provider 回退同步，前端无流式客户端 |
| 会话管理界面 | 未实现 | 后端列表已存在，前端未接入 |
| 多租户企业模式 | 部分实现 | 上下文和 Mapper 仍有缺口 |

## 11. 测试与验证

### 11.1 2026-09-17 定向测试

后端运行以下测试集合：

```bash
mvn -pl auth-service \
  '-Dtest=WorkflowSelectorServiceTest,WorkflowGraphPlanTest,ToolRuntimeServiceTest,QueryTemplateVariableMapperTest,QueryParameterExtractorTest,ExpenseDocumentControlledQueryToolTest,AgentWorkflowRuntimeServiceTest,DeepSeekProviderTest,DesensitizationServiceTest' \
  test
```

结果：9 个测试类、20 个测试全部通过。

前端运行：

```bash
npm run test:unit -- src/composables/__tests__/useAgentChatSession.test.ts
```

结果：1 个测试文件、2 个测试全部通过。

### 11.2 当前覆盖范围

已覆盖：

- 默认模型选择和消息携带 Provider/模型。
- 工作流图校验和选择器基本行为。
- 变量抽取和画布变量映射。
- 工具注册、模板校验和报销范围查询。
- 工作流脱敏到还原的主流程。
- DeepSeek 模型参数传递。
- 正则与结构化 JSON 脱敏。

未覆盖或证据不足：

- 真实数据库、网关、模型凭证和 Ollama 的完整端到端自动化测试。
- 多个发布工作流之间的关键词选择。
- Enterprise 模式租户隔离。
- 脱敏管理接口权限。
- 普通聊天敏感数据保护。
- 前端重试完整状态流、会话列表和组件端到端交互。
- 负载、延迟、QPS 和成本；本文不再记录未经压测的性能数字。

最近一次前端生产构建已通过。当前全量前端类型检查仍被 `PersonalBankAccountDialog.vue` 两处既有 `accountId` 类型错误阻断，与 FinWork 模块无关，但在修复前不能宣称仓库全量类型检查通过。

## 12. 风险清单

| 等级 | 风险 | 当前影响 | 建议 |
| --- | --- | --- | --- |
| 高 | 普通聊天不脱敏 | 用户在普通对话输入敏感信息时可能直接传给云端 | 上云前统一做敏感度分类和脱敏，或高敏内容强制本地模型 |
| 高 | `TenantContextInterceptor` 路径与实际接口不一致 | Enterprise 模式可能缺少租户上下文，Demo 模式掩盖问题 | 覆盖 `/auth/agent/**` 并增加企业模式集成测试 |
| 高 | 脱敏映射按 ID/会话查询缺少租户条件 | 映射表包含可还原原值，存在越租户读取风险 | Mapper 强制 tenant/company 条件并限制管理接口 |
| 高 | 聊天和脱敏管理接口缺少操作权限校验 | 已登录用户可能调用未授权操作 | 后端接入 `AccessControlService`，前端同步按钮权限 |
| 高 | 上云后验泄漏检测仅有配置、无实现 | 未识别敏感字段可能进入云端或模型输出 | 在模型调用前后实施可阻断的泄漏扫描 |
| 中 | 工作流选择不使用关键词 | 多个已发布工作流时可能选错第一个候选 | 引入显式触发条件、优先级和可解释评分 |
| 中 | 内置工作流自动回退 | 发布配置错误时仍有结果，降低问题可见性 | 在运行记录标明回退，生产环境可配置是否允许 |
| 中 | 画布能力大于运行时能力 | 用户能设计运行时不支持的节点 | 前端按运行模式限制节点，发布前调用后端校验 |
| 中 | 旧初始化脚本含 `transform` 节点 | 已发布工作流可能被运行时跳过 | 更新或废弃旧脚本，统一标准模板 |
| 中 | SQL 脚本多版本并存 | 误执行可能造成结构冲突或数据重建 | 收敛为版本化迁移并记录校验值 |
| 中 | Kimi 仅有配置 | 路由规则列出 Kimi，但运行时不会注册 | 实现 Provider 或从规则中移除 |
| 中 | 映射原值未见加密 | 数据库泄露时可直接恢复敏感信息 | 使用受控密钥加密映射内容并缩短 TTL |
| 中 | 模型调用位于事务流程内 | 慢调用可能拉长数据库事务和锁持有时间 | 拆分运行状态事务与外部模型调用 |
| 低 | 前端本地回退固定标记可用 | Ollama 未启动时界面仍显示可选 | 元数据返回真实健康状态 |
| 低 | 消息重试会新增消息 | 可能出现原消息 pending 和重复消息 | 抽取内部发送方法，复用原 `clientMessageId` |

## 13. 扩展开发规则

### 13.1 新增受控工具

1. 实现 `ControlledTool` 并注册为 Spring Bean。
2. 使用唯一 `toolCode`，显式列出允许的 `queryTemplateCode`。
3. 工具内部绑定当前用户、租户和公司范围，不能信任前端传入身份。
4. 只使用参数化查询、MyBatis 条件构造器或受审查的固定查询。
5. 返回结构化 `records`、`entityRef` 和 `evidenceRef`。
6. 在工作台元数据中暴露工具选项，并增加工具绑定配置。
7. 添加权限、范围、空结果、非法状态、limit 和越权测试。

禁止将工作台文本直接当 SQL、Shell、JavaScript 或本地脚本执行。

### 13.2 新增查询变量

必须同步修改：

- `QueryTemplateVariables`
- `QueryParameterExtractor`
- `QueryTemplateVariableMapper`
- 工具白名单和查询实现
- 工作台输入 Schema 与节点配置界面
- 前后端类型和单元测试

未知映射目标继续保持忽略或拒绝，不能动态扩展查询权限。

### 13.3 新增节点

画布新增节点不等于运行时支持。需要同时完成：

- 工作台节点元数据和前端配置抽屉。
- `WorkflowGraphPlan` 的图约束。
- `WorkflowSelectorService` 的发布校验。
- `AgentWorkflowRuntimeService` 的真实执行器。
- 输入输出 Schema、错误语义和步骤日志。
- 兼容旧版本配置的迁移策略。
- 单元测试和工作台真实运行测试。

### 13.4 新增模型 Provider

1. 实现 `ChatModelProvider` Bean。
2. 定义 Provider code、能力、数据驻留和场景。
3. 在 `application-agent.yml` 增加配置。
4. 凭证只从后端安全配置读取。
5. 验证首选 Provider、失败降级、模型参数和元数据可用状态。
6. 未实现流式协议前，不得将 `streamingSupport` 当成前端已支持。

## 14. 常见问题定位

### 14.1 “当前模型提供商缺少凭证配置”

检查：

- 选择的 Provider 是否真正注册。
- DeepSeek 对应的后端适配器是否读取到 `DEEPSEEK_API_KEY`。
- Agent 已发布版本是否仍引用不可用 Provider。
- 工作台运行是否覆盖了 Provider 或模型配置。

### 14.2 “工具 xxx 未注册”

检查：

- 是否存在实现 `ControlledTool` 的 Spring Bean。
- `toolCode()` 是否与工作流节点完全一致。
- 查询模板是否被该工具允许。
- 工具是否在 Agent 配置或工具绑定表中启用。

### 14.3 “未匹配到可执行工作流”或自动走内置回退

检查：

- Agent 是否为 `READY`。
- 是否存在 `published_version_id` 且版本 `published = 1`。
- 图是否严格符合 `start -> tool -> llm -> end`。
- 工具是否已注册并启用。
- 运行日志中的 `builtInFallback` 是否为 `true`。

### 14.4 查询到 0 条报销单

检查：

- `scope` 是 `SUBMITTED_BY_ME` 还是 `ASSIGNED_TO_ME`。
- 当前用户是否为提交人，或是否有 `PENDING` 待办任务。
- 单据 `templateType` 是否为 `report`。
- 状态、日期、金额和关键词是否过度过滤。
- 工作台输入映射是否使用 `{{input.*}}`，FinWork 自动抽取是否使用 `{{user.*}}`。
- `limit` 是否在 1 到 50。

### 14.5 脱敏后没有还原

检查：

- 运行步骤是否生成 `mappingId`。
- `ea_desensitization_mapping` 是否存在该记录且未过期。
- 模型是否保留了形如 `[PREFIX_001]` 的占位符。
- 是否在 24 小时后才尝试还原。
- 当前租户上下文是否正确。

## 15. 本地验证建议

### 15.1 必需依赖

- MySQL 及与当前实体一致的 Agent 表结构。
- `auth-service`，默认端口由 `FINEX_AUTH_SERVICE_PORT` 控制。
- 前端 `admin-web`。
- 至少一个真实可用的模型 Provider：DeepSeek 凭证或本地 Ollama。

不要在文档或脚本中保存真实数据库密码、JWT Secret、模型 Key 或默认管理员密码。

### 15.2 最小验证顺序

1. 启动数据库并检查会话、工作台和脱敏表。
2. 启动 `auth-service`，确认 Provider 注册日志。
3. 访问 `/api/auth/agent/chat/meta`，确认 `available` 与真实 Provider 一致。
4. 登录后创建会话并发送普通消息。
5. 发送“查询我待处理的报销单”，确认进入 `query_expense`。
6. 检查 `ea_agent_run` 和 `ea_agent_run_step` 中的 selector、tool、mask、llm、restore、end 步骤。
7. 检查云端请求使用脱敏 JSON，最终输出使用本地还原值和真实 `evidenceRef`。
8. 分别验证本人提交、分配给本人、非法状态、空结果和越权数据。

## 16. 维护要求

以下变化必须同步更新本文：

- FinWork 接口、DTO 或路由变化。
- 模型 Provider、路由规则或凭证方式变化。
- 工作流节点、工具、查询模板或变量变化。
- 脱敏字段、规则来源、映射 TTL 或还原策略变化。
- 数据库迁移入口变化。
- 已知风险关闭或出现新的安全缺口。
- 测试数量、构建状态或端到端验证结论变化。

文档中的“完成”必须能够定位到代码、测试或可复现运行记录。历史修复经过不再重复堆入本文，应由版本控制和独立故障记录保存。

## 17. 更新记录

### v1.0 现状重构版（2026-09-17）

- 按当前前后端代码重写架构、接口、服务和文件索引。
- 补充受控查询、变量映射、工具注册、脱敏、还原和证据校验链路。
- 区分普通聊天与受控查询的安全边界。
- 更新模型 Provider、数据库脚本和开发进度的真实状态。
- 新增测试证据和风险清单。
- 删除历史故障流水、虚构性能数据、默认账号密码和过时计划。
