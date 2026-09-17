# Agent 工作台开发架构与实现基线

> 原文件名保留用于兼容已有引用。本文已由“可行性分析”更新为当前实现基线。
>
> 基线日期：2026-09-17  
> 代码根目录：`C:\Users\funsir\Desktop\报销系统`  
> 核验范围：`frontend/admin-web`、`backend/auth-service`、`backend/sql`  
> 信息口径：以当前代码、数据库脚本和可重复测试结果为准，不把规划能力写成已完成功能。

## 一、结论与当前定位

Agent 工作台已经具备 Phase 1 的完整骨架，可以完成 Agent 定义、模型选择、提示词与输入参数配置、工具绑定、触发器配置、低代码画布编排、保存、发布、手工测试、运行记录查看和删除。

当前系统同时存在两条执行路径：

1. **通用画布执行路径**：支持开始、模型、条件、工具、转换、通知、结束等节点，适合工作台内的通用流程测试。
2. **受控查询执行路径**：服务于“关键词提取 -> 查询模板变量映射 -> 本地受控查询 -> 脱敏 -> 云端模型 -> 本地还原”闭环，目前只支持严格的 `start -> tool -> llm -> end` 拓扑，并已接入报销单查询工具。

因此，工作台已达到“可设计、可保存、可发布、可运行、可追踪”的初步可用状态，但尚不能视为通用企业工具市场或任意节点组合都可稳定生产运行的平台。新增普通工具和新增节点已有清晰扩展点；新增受控查询工具仍需要后端代码、注册、权限与测试配套。

## 二、状态定义

本文使用以下状态，避免“页面上可选”等同于“后端可用”：

| 状态 | 含义 |
| --- | --- |
| 已完成 | 代码存在、主链路已接通，并有单元测试或可核验实现 |
| 部分完成 | 已有实现，但范围、健康检查、并发或测试覆盖仍有限 |
| 预留 | DTO、元数据或页面入口存在，运行实现未完成或明确禁用 |
| 已知缺陷 | 当前代码中已确认的问题，不在本文档更新任务中修改 |

## 三、总体架构

```text
浏览器
  |
  +-- Agent 工作台 /agent/workbench
  |     +-- Agent 列表、详情、模型、Prompt、工具、触发器、运行记录
  |
  +-- 流程画布 /agent/workbench/:id/workflow
        +-- 节点树、拖放、连线、节点配置抽屉、边右键菜单
  |
  v
ArchiveAgentController
  |
  v
ArchiveAgentServiceImpl（事务边界与门面）
  |
  +-- ArchiveAgentMetaSupport
  +-- ArchiveAgentDefinitionDomainSupport
  +-- ArchiveAgentRunDomainSupport
  +-- ArchiveAgentScheduleDomainSupport
  |
  v
ArchiveAgentExecutionWorker（异步执行）
  |
  +-- 通用画布执行
  |     +-- ArchiveAgentModelRuntime
  |     +-- ToolExecutor / CoreArchiveToolExecutor
  |
  +-- 严格受控查询执行
        +-- AgentWorkflowRuntimeService
        +-- ToolRuntimeService / ControlledTool
        +-- DesensitizationService
        +-- ChatModelRouter
        +-- 本地还原与 evidenceRef 校验
  |
  v
MySQL：ea_agent_* 定义、版本、绑定、运行、步骤、产物、计划
```

### 3.1 模块职责边界

| 层 | 当前职责 | 不应承担的职责 |
| --- | --- | --- |
| 前端工作台 | 编辑配置、画布交互、基础校验、调用 API、展示运行信息 | 直接执行 SQL、保存模型密钥、实现脱敏 |
| Controller | 鉴权、参数接收、统一响应 | 编排业务流程、拼装 SQL |
| Service 门面 | 事务边界、委派领域服务 | 堆叠节点类型分支 |
| 定义领域服务 | Agent/版本/绑定/触发器/计划的生命周期 | 执行工作流 |
| 执行 Worker | 选择执行路径、记录步骤与产物 | 暴露 HTTP 接口 |
| Tool Runtime | 工具注册、输入校验、受控执行 | 接收任意脚本文本直接运行 |
| 脱敏服务 | 本地脱敏、映射保存、结果还原 | 决策 Agent 或工具 |
| 模型路由 | 按 provider/model 调用模型 | 访问业务数据库 |

## 四、前端实现

### 4.1 路由与入口

| 路由 | 页面 | 权限 |
| --- | --- | --- |
| `/agent/workbench` | Agent 工作台主页面 | `agent:workbench:view` 或 `agents:view` |
| `/agent/workbench/:id/workflow` | 流程编排画布 | 同上 |
| `/archives/agents` | 兼容旧入口 | 跳转/复用工作台能力 |

路由定义位于：

- `frontend/admin-web/src/router/index.ts`
- `frontend/admin-web/src/router/navigationConfig.ts`

### 4.2 主工作台

主组件：`frontend/admin-web/src/views/archives/ArchiveAgentView.vue`

已完成能力：

- Agent 列表、筛选、选择、新建、复制、保存、发布、启用/停用、归档和删除。
- 删除前二次确认，确认后调用后端删除接口，同时清理关联的 Agent 数据。
- 配置模型提供商、模型、Prompt、工作流输入结构、工具和触发器。
- 工作台测试运行使用可编辑 JSON 入参。
- 测试入参按“当前用户 + Agent ID”存入 `sessionStorage`，重新进入页面不会立即被默认模板覆盖。
- 最近运行区域仅展示最近 5 条；单击运行记录打开右侧详情抽屉。
- 流程编排卡片进入独立画布页面。
- 根据新旧两套权限码控制按钮显示和操作能力。

运行草稿辅助模块：

- `frontend/admin-web/src/views/archives/agentRunDraft.ts`
- 存储键：`agent-workbench:run-draft:{user}:{agentId}`
- 保存内容：`triggerSource`、`inputPayloadText`

### 4.3 流程画布

主组件：`frontend/admin-web/src/views/agent/AgentWorkflowCanvasView.vue`

已完成能力：

- 节点库按类型分组为树形结构。
- 节点拖入画布、节点移动、端口连线和删除连接。
- 右击连线显示上下文菜单，可删除连接。
- 双击节点打开右侧配置抽屉；单击画布其他位置关闭抽屉。
- 保存前执行前端工作流校验。
- 保存时通过 Agent 更新接口提交完整 Agent 配置，而不是单独保存一份游离的画布数据。
- 画布背景、顶部区域与工作台主体视觉保持一致，节点继续使用类型色区分。

工作流类型与 DTO：

- `frontend/admin-web/src/api/modules/archive-agent-types.ts`
- 当前节点类型：`start`、`llm`、`condition`、`tool`、`transform`、`notify`、`end`

### 4.4 前端 API 封装

文件：`frontend/admin-web/src/api/modules/archive-agent.ts`

统一前缀：`/auth/agent/workbench`

方法包括：

- `list`
- `meta`
- `detail`
- `create`
- `update`
- `publish`
- `remove`
- `run`
- `listRuns`
- `getRunDetail`
- `toggleStatus`

## 五、后端实现

### 5.1 接口层

Controller：

`backend/auth-service/src/main/java/com/finex/auth/controller/ArchiveAgentController.java`

同时兼容两个前缀：

- `/auth/agent/workbench`
- `/auth/archives/agents`

| 方法 | 路径 | 用途 | 关键权限 |
| --- | --- | --- | --- |
| GET | 根路径 | 查询当前用户 Agent 列表 | view |
| GET | `/meta` | 查询模型、工具、节点、触发器元数据 | view |
| GET | `/{id}` | 查询 Agent 详情 | view |
| POST | 根路径 | 新建 Agent | create |
| PUT | `/{id}` | 保存并生成新草稿版本 | edit |
| POST | `/{id}/publish` | 发布最新版本 | publish |
| POST | `/{id}/run` | 提交手工测试运行 | run |
| GET | `/{id}/runs` | 查询最近运行记录，后端最多返回 20 条 | view_logs |
| GET | `/runs/{runId}` | 查询步骤与产物详情 | view_logs |
| POST | `/{id}/toggle-status` | 切换状态 | edit；归档要求 delete |
| DELETE | `/{id}` | 删除 Agent 及其专属关联数据 | delete |

权限同时兼容 `agent:workbench:*` 与旧版 `agents:*` 代码。

### 5.2 服务门面与领域服务

服务接口：

`backend/auth-service/src/main/java/com/finex/auth/service/ArchiveAgentService.java`

事务门面：

`backend/auth-service/src/main/java/com/finex/auth/service/impl/ArchiveAgentServiceImpl.java`

创建、更新、发布、状态切换、删除、运行创建和计划执行等写操作使用 `@Transactional(rollbackFor = Exception.class)`。

领域支持类：

| 类 | 主要职责 |
| --- | --- |
| `ArchiveAgentMetaSupport` | 返回模型、工具、节点和触发器元数据 |
| `ArchiveAgentDefinitionDomainSupport` | Agent CRUD、版本、发布、绑定同步、配置校验 |
| `ArchiveAgentRunDomainSupport` | 手工运行、运行列表、运行详情、归属校验 |
| `ArchiveAgentScheduleDomainSupport` | 查找到期计划、创建计划运行、推进下次执行时间 |

目录：

`backend/auth-service/src/main/java/com/finex/auth/service/impl/archiveagent/`

### 5.3 异步执行

关键文件：

- `backend/auth-service/src/main/java/com/finex/auth/support/archiveagent/ArchiveAgentTriggerDispatcher.java`
- `backend/auth-service/src/main/java/com/finex/auth/support/archiveagent/ArchiveAgentExecutionWorker.java`
- `backend/auth-service/src/main/java/com/finex/auth/service/impl/ArchiveAgentScheduler.java`

行为：

- 手工或计划运行先创建 `ea_agent_run`，再交给 `finexAsyncExecutor` 异步执行。
- Worker 加载定义与版本，判断是否为严格受控查询工作流。
- 执行过程写入 `ea_agent_run_step`。
- 最终输出写入 `ea_agent_run_artifact`，运行状态更新为成功或失败。
- 调度器每 60 秒扫描到期计划。

## 六、版本与生命周期

### 6.1 Agent 状态

当前支持：

- `DRAFT`
- `READY`
- `DISABLED`
- `ARCHIVED`

### 6.2 保存与发布

- 新建 Agent 时创建版本 1，状态为草稿。
- 每次保存都会新增一个版本，不覆盖旧版本。
- 发布时取消旧版本的发布标记，发布最新版本，并更新定义表的 `published_version_id`。
- 发布后，非禁用状态的 Agent 会进入 `READY`。
- 工具绑定、触发器和计划采用“按 Agent 删除后重建”的同步方式。

### 6.3 不同入口使用的版本

| 入口 | 使用版本 | 说明 |
| --- | --- | --- |
| 工作台手工测试 | 最新已保存版本 | 便于发布前调试 |
| 定时计划 | 已发布可运行版本 | 避免草稿进入自动任务 |
| FinWork 工作流选择 | 已发布版本 | Selector 只应匹配可发布运行定义 |

该差异是有意设计，但前端目前提示不够明显。排查“测试成功、FinWork 仍执行旧逻辑”时，应先确认是否重新发布。

## 七、数据库模型

初始化与迁移脚本：

- `backend/sql/init_archive_agent.sql`
- `backend/sql/migrate_archive_agent.sql`
- `backend/sql/init_agent_platform_permissions_v2.sql`
- `backend/sql/fix_agent_permissions.sql`

核心表：

| 表 | 用途 |
| --- | --- |
| `ea_agent_definition` | Agent 主定义、状态、所有者、当前发布版本 |
| `ea_agent_version` | 不可变版本快照、模型和工作流 JSON |
| `ea_agent_trigger` | 手工、计划等触发器 |
| `ea_agent_tool_binding` | Agent 与工具的绑定及配置 |
| `ea_agent_credential_ref` | 凭证引用预留 |
| `ea_agent_run` | 运行实例 |
| `ea_agent_run_step` | 节点执行步骤 |
| `ea_agent_run_artifact` | 输出、证据或中间产物 |
| `ea_agent_schedule` | 计划任务与下次执行时间 |

删除 Agent 时，服务会拒绝删除仍有 `PENDING` 或 `RUNNING` 运行的 Agent；通过后在事务内删除产物、步骤、计划、触发器、工具绑定、运行、版本和定义。凭证引用不随 Agent 删除，因为设计上可能被其他对象复用。

当前没有实现旧方案中设想的独立 `ea_tool_definition`、`ea_tool_version` 等工具资产表。工具注册仍以 Spring Bean + 工作台静态元数据为主，`ea_agent_tool_binding` 只保存 Agent 绑定，不等同于完整工具市场。

## 八、工作流执行

### 8.1 通用画布执行

入口：

`backend/auth-service/src/main/java/com/finex/auth/support/archiveagent/ArchiveAgentExecutionWorker.java`

执行前使用 `WorkflowGraphPlan` 校验图并生成有序节点。通用节点行为：

| 节点 | 当前行为 |
| --- | --- |
| `start` | 初始化运行上下文 |
| `llm` | 通过 `ArchiveAgentModelRuntime` 调用模型 |
| `condition` | 对运行上下文中的字段做条件判断 |
| `tool` | 从 `ToolExecutor` 集合按工具编码执行 |
| `transform` | 按配置进行轻量数据转换 |
| `notify` | 通过通知工具发送消息 |
| `end` | 汇总最终输出 |

通用路径适合工作台流程验证，但条件与转换能力仍较轻，不应当作复杂表达式引擎或 ETL 平台使用。

### 8.2 受控查询执行

关键文件：

- `backend/auth-service/src/main/java/com/finex/auth/agent/workflow/AgentWorkflowRuntimeService.java`
- `backend/auth-service/src/main/java/com/finex/auth/agent/workflow/WorkflowSelectorService.java`
- `backend/auth-service/src/main/java/com/finex/auth/agent/workflow/ToolRuntimeService.java`
- `backend/auth-service/src/main/java/com/finex/auth/agent/workflow/ControlledTool.java`
- `backend/auth-service/src/main/java/com/finex/auth/agent/workflow/ExpenseDocumentControlledQueryTool.java`

真实链路：

```text
FinWork 提取意图和关键词
  -> WorkflowSelectorService 匹配已发布工作流
  -> QueryParameterExtractor 提取输入参数
  -> QueryTemplateVariableMapper 映射模板变量
  -> ToolRuntimeService 查找并执行 ControlledTool
  -> 本地数据库执行参数化、受控查询
  -> 查询结果本地脱敏并生成 mappingId/entityRef/evidenceRef
  -> 脱敏数据交给 ChatModelRouter
  -> 云端返回结构化结果
  -> 本地按映射还原，并校验 evidenceRef 白名单
  -> 返回 FinWork
```

严格路径当前只接受：

- 恰好一个开始节点。
- 恰好一个工具节点。
- 恰好一个模型节点。
- 恰好一个结束节点。
- 拓扑顺序为 `start -> tool -> llm -> end`。
- 工具必须已注册为 `ControlledTool`。

因此，画布能绘制条件、转换、通知等节点，不代表这些节点可以插入受控查询链。若加入额外节点，流程会回到通用执行或因严格校验失败。

### 8.3 当前工具

| 工具编码 | 路径 | 状态 | 说明 |
| --- | --- | --- | --- |
| `expense.query_my_expenses` | 通用 + 受控查询 | 已完成 | 通用路径返回报销摘要；严格路径执行报销单受控查询 |
| `invoice.query_my_invoices` | 通用 | 已完成 | 查询当前用户发票摘要 |
| `notify.send_message` | 通用 | 已完成 | 提交异步通知 |
| `http.mock_request` | 通用 | 部分完成 | 仅模拟 HTTP，不会调用任意外部地址 |
| `json.extract` | 无运行实现 | 预留 | 元数据中不可用 |
| `async_task.submit` | 无运行实现 | 预留 | 元数据中不可用 |

通用工具执行器：

`backend/auth-service/src/main/java/com/finex/auth/support/archiveagent/CoreArchiveToolExecutor.java`

### 8.4 新增工具的正确方式

新增普通工具：

1. 实现 `ToolExecutor`，声明稳定的工具编码。
2. 实现输入校验、权限/租户边界和结构化输出。
3. 在 `ArchiveAgentMetaSupport` 暴露元数据。
4. 增加工具单元测试、Worker 集成测试和失败场景测试。

新增受控查询工具：

1. 实现 `ControlledTool`，禁止接收可执行 SQL 或脚本文本。
2. 使用固定查询模板、参数白名单、类型转换和行数上限。
3. 在 `ToolRuntimeService` 的 Spring Bean 集合中完成注册。
4. 输出结构化 records、entityRef 和 evidenceRef。
5. 接入脱敏、还原、证据引用校验和审计。
6. 更新工作台元数据，并增加完整闭环测试。

禁止把“前端录入任意 SQL、后端直接执行”作为新增工具方案。

## 九、模型与脱敏

### 9.1 模型

模型路由相关文件：

- `backend/auth-service/src/main/java/com/finex/auth/agent/model/ChatModelRouter.java`
- `backend/auth-service/src/main/java/com/finex/auth/agent/model/impl/DeepSeekProvider.java`
- `backend/auth-service/src/main/java/com/finex/auth/agent/model/impl/LocalModelProvider.java`
- `backend/auth-service/src/main/java/com/finex/auth/support/archiveagent/DeepSeekModelProviderAdapter.java`
- `backend/auth-service/src/main/java/com/finex/auth/support/archiveagent/ArchiveAgentModelRuntime.java`

当前元数据声明：

| Provider | 状态 | 备注 |
| --- | --- | --- |
| `MOCK` | 可用 | 测试用途 |
| `DEEPSEEK` | 可用但依赖环境 | 需要平台环境变量 `DEEPSEEK_API_KEY` |
| `LOCAL_7B` | 可用但依赖本地服务 | 是否真实可调用取决于本地模型服务与配置 |
| `KIMI` | 不可用 | 预留 |

`ArchiveAgentMetaSupport` 返回的是静态声明，不会实时检查 Spring Bean、密钥、网络或本地模型健康状态。因此“下拉框可选”不能作为模型一定可用的证据。

DeepSeek 还有一处装配耦合：Adapter 只有存在 `DEEPSEEK_API_KEY` 时才创建，而 `DeepSeekProvider` 直接依赖该 Adapter。部署配置必须保证两者一致，后续应改为按 Provider 条件装配并由元数据读取真实健康状态。

### 9.2 脱敏与还原

通用模型节点：

- `ArchiveAgentModelRuntime` 在调用非 MOCK 模型前，将运行上下文序列化后交给 `DesensitizationService`。
- 服务返回 `mappingId`，模型响应回来后在本地执行还原。

受控查询路径：

- 先对结构化查询结果执行字段级脱敏。
- 再执行系统“脱敏规则配置”中的适用正则规则。
- 云端只接收脱敏后的数据。
- 返回结果按 `mappingId`、`entityRef`、`evidenceRef` 在本地还原和校验。
- 不允许模型自行生成未在本次查询证据集合中的 `evidenceRef`。

这意味着受控查询不是只依赖一套写死规则：它使用结构化字段脱敏保证底线，同时叠加已有脱敏规则配置。

## 十、权限与数据边界

当前权限：

- `agent:workbench:view`
- `agent:workbench:create`
- `agent:workbench:edit`
- `agent:workbench:publish`
- `agent:workbench:run`
- `agent:workbench:view_logs`
- `agent:workbench:delete`

后端同时兼容 `agents:*` 旧权限，且不能只依赖前端按钮隐藏。

当前数据边界：

- 工作台列表只返回当前 owner 的 Agent。
- 详情、更新、删除、运行和日志查询均校验 owner。
- 删除正在运行的 Agent 会被拒绝。
- FinWork 可以选择发布工作流；全局 Agent 与个人工作台 Agent 的共享和可见性规则尚未形成完整产品能力。

## 十一、开发进度

| 能力 | 状态 | 备注 |
| --- | --- | --- |
| 工作台路由与菜单 | 已完成 | 新旧权限兼容 |
| Agent CRUD | 已完成 | 删除含后端关联清理 |
| 模型选择 | 已完成 | 可用性仍依赖真实运行环境 |
| Prompt、输入结构、工具、触发器配置 | 已完成 | 以版本快照保存 |
| 独立低代码画布 | 已完成 | 树形节点库、拖放、连线、右键边菜单、抽屉 |
| 工作流基础校验 | 已完成 | 前后端均有校验 |
| 发布与版本管理 | 已完成 | 每次保存生成新版本 |
| 手工异步运行 | 已完成 | 使用最新保存版本 |
| 运行步骤与产物 | 已完成 | 前端最近 5 条，后端最多 20 条 |
| 定时触发 | 部分完成 | 单实例可用，多实例并发领取需加强 |
| 通用模型节点脱敏/还原 | 已完成 | 非 MOCK 路径 |
| 报销单受控查询闭环 | 已完成 | 严格四节点拓扑 |
| FinWork 选择已发布工作流 | 已完成 | 依赖发布状态和选择规则 |
| 独立工具资产管理 | 未完成 | 当前无工具定义/版本管理页面与表 |
| EVENT 触发器 | 预留 | 元数据标记不可用 |
| KIMI Provider | 预留 | 元数据标记不可用 |
| 画布组件级交互测试 | 未完成 | 当前主要覆盖路由和辅助逻辑 |
| 工作流审批、回滚、灰度 | 未完成 | 不属于当前 Phase 1 |

## 十二、验证结果

验证日期：2026-09-17

### 12.1 后端

针对工作台、领域服务、模型运行时和受控查询链执行 Maven 测试：

- 测试类：14 个
- 测试项：36 个
- 结果：36 通过，0 失败，0 错误，0 跳过

覆盖范围包括：

- Controller 权限与接口行为。
- Agent 定义、元数据、运行、计划和门面服务。
- 通用模型运行时。
- 工作流选择、图校验、变量映射、参数提取。
- Tool Runtime、报销单受控工具和严格工作流运行时。

### 12.2 前端

针对工作台相关辅助逻辑、路由、菜单和主布局执行 Vitest：

- 测试文件：4 个
- 测试项：36 个
- 结果：36 通过

当前未覆盖：

- 画布拖放与节点移动。
- 连线和右键删除。
- 节点双击抽屉。
- Agent 新建、保存、发布、删除的浏览器端到端流程。
- 模型服务和真实数据库的联调健康检查。

## 十三、风险与技术债

### 13.1 高优先级

1. **元数据与真实可用性可能不一致**  
   `ArchiveAgentMetaSupport` 静态声明 Provider/工具可用，未读取 Bean 注册、密钥、网络或健康检查状态。这会直接导致“前端可选、运行时报缺少凭证/未注册”。

2. **通用工具与受控工具存在两套注册体系**  
   `ToolExecutor` 和 `ControlledTool` 分别服务通用与严格路径。边界目前可解释，但继续无规则扩展会造成相同工具编码在两条路径行为不一致。应形成统一描述符和能力标记，执行器仍可分层。

3. **严格受控查询只支持四节点拓扑**  
   画布展示七种节点，但严格路径不支持条件、转换和通知节点。发布前应给出明确的“执行模式”和拓扑校验提示。

4. **计划任务多实例重复领取风险**  
   当前按 `IDLE` 和到期时间查询后逐条提交，没有数据库原子抢占或分布式锁。多实例部署时可能重复触发。

### 13.2 中优先级

1. **草稿测试与发布运行差异不够显性**  
   工作台测试最新保存版本，FinWork/计划任务执行已发布版本。用户保存但未发布时可能误判为系统未更新。

2. **整包保存存在并发覆盖风险**  
   画布保存提交完整 Agent，绑定、触发器和计划会删除后重建；目前没有乐观锁或版本冲突提示。多窗口同时编辑可能后保存覆盖先保存。

3. **版本增长无清理策略**  
   每次保存都新增版本，尚无草稿合并、保留期限或历史版本清理策略。

4. **工具资产治理未完成**  
   没有独立工具定义、版本、上线状态、健康状态和权限作用域管理。新增工具仍需要开发改代码。

5. **凭证引用未形成完整闭环**  
   `ea_agent_credential_ref` 已建表，但 DeepSeek 当前主要使用平台环境变量，工作台没有安全的凭证配置与轮换闭环。

6. **工作台共享模型有限**  
   工作台按 owner 隔离，尚无团队共享、只读、协作编辑或租户级模板机制。

### 13.3 已知缺陷

1. `ArchiveAgentScheduler` 的异常日志文本存在历史乱码。
2. `ArchiveAgentScheduleDomainSupport` 的定时触发运行摘要存在历史乱码。
3. `backend/sql/init_agent_controlled_query_workflow.sql` 包含转换节点，与当前严格四节点运行时不兼容；部署受控查询示例时应使用当前兼容迁移脚本，例如 `migrate_agent_20260406131739_accessible_query_v2.sql`，并在执行前核对目标 Agent。
4. `http.mock_request` 名称容易让人误以为会真实请求外部系统，实际仅为模拟实现。

以上缺陷在本次文档更新中未修改代码。

## 十四、关键文件索引

### 14.1 前端

| 用途 | 路径 |
| --- | --- |
| 工作台主页面 | `frontend/admin-web/src/views/archives/ArchiveAgentView.vue` |
| 画布页面 | `frontend/admin-web/src/views/agent/AgentWorkflowCanvasView.vue` |
| API 封装 | `frontend/admin-web/src/api/modules/archive-agent.ts` |
| API 类型 | `frontend/admin-web/src/api/modules/archive-agent-types.ts` |
| 运行入参草稿 | `frontend/admin-web/src/views/archives/agentRunDraft.ts` |
| 路由 | `frontend/admin-web/src/router/index.ts` |
| 导航配置 | `frontend/admin-web/src/router/navigationConfig.ts` |
| 草稿测试 | `frontend/admin-web/src/views/archives/__tests__/agentRunDraft.test.ts` |

### 14.2 后端

| 用途 | 路径 |
| --- | --- |
| Controller | `backend/auth-service/src/main/java/com/finex/auth/controller/ArchiveAgentController.java` |
| Service 接口 | `backend/auth-service/src/main/java/com/finex/auth/service/ArchiveAgentService.java` |
| 事务门面 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/ArchiveAgentServiceImpl.java` |
| 定义领域服务 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/archiveagent/ArchiveAgentDefinitionDomainSupport.java` |
| 运行领域服务 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/archiveagent/ArchiveAgentRunDomainSupport.java` |
| 调度领域服务 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/archiveagent/ArchiveAgentScheduleDomainSupport.java` |
| 元数据 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/archiveagent/ArchiveAgentMetaSupport.java` |
| 异步 Worker | `backend/auth-service/src/main/java/com/finex/auth/support/archiveagent/ArchiveAgentExecutionWorker.java` |
| 通用模型运行时 | `backend/auth-service/src/main/java/com/finex/auth/support/archiveagent/ArchiveAgentModelRuntime.java` |
| 通用工具 | `backend/auth-service/src/main/java/com/finex/auth/support/archiveagent/CoreArchiveToolExecutor.java` |
| 受控工作流运行时 | `backend/auth-service/src/main/java/com/finex/auth/agent/workflow/AgentWorkflowRuntimeService.java` |
| 工作流选择器 | `backend/auth-service/src/main/java/com/finex/auth/agent/workflow/WorkflowSelectorService.java` |
| Tool Runtime | `backend/auth-service/src/main/java/com/finex/auth/agent/workflow/ToolRuntimeService.java` |
| 报销单受控工具 | `backend/auth-service/src/main/java/com/finex/auth/agent/workflow/ExpenseDocumentControlledQueryTool.java` |
| 模型路由 | `backend/auth-service/src/main/java/com/finex/auth/agent/model/ChatModelRouter.java` |
| 脱敏服务 | `backend/auth-service/src/main/java/com/finex/auth/agent/desensitization/DesensitizationService.java` |

### 14.3 SQL

| 用途 | 路径 |
| --- | --- |
| 工作台全量初始化 | `backend/sql/init_archive_agent.sql` |
| 工作台增量迁移 | `backend/sql/migrate_archive_agent.sql` |
| 权限初始化 | `backend/sql/init_agent_platform_permissions_v2.sql` |
| 权限修复 | `backend/sql/fix_agent_permissions.sql` |
| 受控查询旧示例 | `backend/sql/init_agent_controlled_query_workflow.sql` |
| 当前四节点迁移示例 | `backend/sql/migrate_agent_20260406131739_accessible_query_v2.sql` |

## 十五、排障顺序

### 15.1 页面提示无权限

1. 检查用户是否拥有 `agent:workbench:view` 或兼容的 `agents:view`。
2. 检查前端权限缓存是否刷新。
3. 检查后端 Controller 是否返回 403，而不是只看前端按钮。

### 15.2 工具未注册

1. 确认画布工具编码与 Java Bean 声明完全一致。
2. 判断当前流程走 `ToolExecutor` 还是 `ControlledTool`。
3. 检查 Bean 是否被 Spring 扫描和成功装配。
4. 检查 `ArchiveAgentMetaSupport` 是否只展示了工具，但后端没有执行实现。

### 15.3 模型缺少凭证

1. 确认运行环境存在 `DEEPSEEK_API_KEY`。
2. 确认 `DeepSeekModelProviderAdapter` 已创建。
3. 确认 Provider 元数据不是仅静态显示为可用。
4. 本地模型需要同时检查服务地址、模型名称和服务健康状态。

### 15.4 保存后 FinWork 仍执行旧流程

1. 工作台保存后必须再次发布。
2. 检查 `published_version_id` 是否指向最新版本。
3. 检查 Workflow Selector 是否匹配了目标 Agent，而不是其他同类工作流。
4. 检查 Agent 状态是否为 `READY`。

### 15.5 查询结果为 0

1. 查看运行步骤，确认是否进入受控查询工具。
2. 检查输入 JSON 是否包含模板所需字段。
3. 检查变量映射后的类型和值。
4. 检查当前用户、租户和业务数据权限。
5. 检查查询模板状态、行数上限和过滤条件。
6. 不要通过放开任意 SQL 来绕过参数或权限问题。

## 十六、后续演进建议

建议按以下顺序增强：

1. 让元数据来自真实 Provider/Tool 注册表与健康状态，消除“可选但不可用”。
2. 为工作流声明执行模式：通用编排或严格受控查询，并在保存/发布前按模式校验。
3. 为计划任务增加数据库原子抢占、幂等键和多实例测试。
4. 为整包保存增加版本号或 ETag，发现并发修改时拒绝覆盖。
5. 建立独立工具资产、版本和权限作用域，但继续坚持受控模板，不开放任意脚本执行。
6. 补齐画布组件测试和完整浏览器端到端测试。
7. 建立版本保留、回滚和审计策略。
8. 完成凭证引用、加密存储、轮换和按租户授权闭环。

## 十七、维护规则

以后更新本文档时必须遵守：

- 新能力只有在代码存在并经过验证后才能标记“已完成”。
- 前端下拉项、静态元数据和数据库预留字段不能单独作为完成依据。
- 新增接口、表、节点、工具、Provider 或执行路径时，同步更新文件索引、进度表和风险。
- SQL 变更必须提供幂等脚本，并注明适用版本和回滚方式。
- 涉及受控查询时，必须同时核对权限、参数化查询、行数限制、脱敏、还原、证据引用和审计。
- 保留测试命令与测试结果日期，避免把历史通过结果当作当前状态。
