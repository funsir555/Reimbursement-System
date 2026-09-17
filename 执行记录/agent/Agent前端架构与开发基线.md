# FinEx AI Agent 前端架构与开发基线

**文档版本**：v1.0（合并基线）  
**最后核验日期**：2026-09-17  
**适用范围**：`frontend/admin-web` 中的 FinWork、Agent 工作台、流程编排画布及 Agent 导航入口  
**文档状态**：当前有效  

## 1. 文档定位

本文档是以下三份 Phase 0 文档的合并替代版本：

- `Agent前端开发进度_Phase0.md`
- `Agent前端架构设计_Phase0.md`
- `Agent前端架构与开发基线_Phase0_Final.md`

合并时删除了大段示例代码、重复评审记录、主观工时、人员署名、过时方案和未经验证的质量结论。本文只记录当前代码可以证明的事实、明确的工程约束、可复核的验证结果和已知缺口。

事实优先级如下：

1. 当前仓库代码、自动化测试和实际接口行为。
2. 本文档记录的架构约束与状态。
3. 历史设计稿和阶段性讨论。

代码与本文不一致时，应先核实代码行为，再同步更新本文，不能用历史计划覆盖当前事实。

## 2. 当前结论

Agent 前端已经超出最初的 Phase 0 范围，当前形成了两条相互独立、共享基础设施的主链路：

- **FinWork 对话链路**：负责会话创建、消息展示、模型提供商与模型选择、消息发送、失败状态和重试入口。
- **Agent 工作台链路**：负责 Agent 列表、配置、发布、运行、日志查看，以及独立的流程编排画布。

两条链路共享 `MainLayout`、路由元数据、导航权限、`core.ts` 请求封装和后端认证体系，但不共享页面状态。当前没有引入 Agent 专用 Pinia Store，页面内状态和 Composable 足以支撑现有范围。

## 3. 技术栈与运行边界

以 `frontend/admin-web/package.json` 为准，当前主要技术栈为：

| 类别 | 当前实现 |
| --- | --- |
| 前端框架 | Vue 3.5 |
| 语言 | TypeScript 5.9 |
| UI 组件库 | Element Plus 2.13 |
| 路由 | Vue Router 5 |
| 构建工具 | Vite 7 |
| 测试 | Vitest 3 + Vue Test Utils |
| 状态管理 | 当前 Agent 主链路使用 Composable 和页面内 `ref`；项目具备 Pinia，但 Agent 未依赖 |
| API 基础路径 | `VITE_API_BASE_URL`，未配置时为 `/api` |

前端不承担真实数据权限、会话所有权、工具执行、工作流运行、脱敏或模型凭证管理。这些能力必须由后端完成，前端只负责采集配置、调用受控接口和展示结果。

## 4. 总体分层

```text
MainLayout / Router / Navigation
              |
              +-- FinWork: AgentChatView
              |       |
              |       +-- useAgentChatSession
              |       +-- ChatMessageList / ChatInputBox
              |       +-- agent-chat API / types
              |
              +-- Agent Workbench: ArchiveAgentView
              |       |
              |       +-- archive-agent API / types
              |       +-- agentRunDraft local draft helper
              |
              +-- Workflow Canvas: AgentWorkflowCanvasView
                      |
                      +-- archive-agent API / types
                      +-- node and edge editing state

All API modules -> api/modules/core.ts -> /api/auth/** -> backend
```

分层职责：

| 层级 | 职责 | 不应承担的职责 |
| --- | --- | --- |
| View | 页面布局、交互编排、抽屉和对话框状态 | 直接拼接后端地址、实现业务鉴权 |
| Component | 输入、消息列表等可复用展示与事件 | 管理会话生命周期 |
| Composable | FinWork 会话、消息、模型选择和错误状态 | 渲染页面结构 |
| API module | 路径、请求方法、DTO/VO 类型边界 | 页面状态和提示文案 |
| `core.ts` | Token、统一响应、401、超时、查询参数 | Agent 领域逻辑 |
| Backend | 权限、所有权、模型路由、工作流、工具和数据安全 | 依赖前端保证安全 |

## 5. 路由与导航现状

### 5.1 已接入路由

| 路径 | 路由名 | 页面 | 权限 | 状态 |
| --- | --- | --- | --- | --- |
| `/agent/chat` | `agent-chat` | `AgentChatView.vue` | `agent:chat:view` | 已实现 |
| `/agent/workbench` | `agent-workbench` | `ArchiveAgentView.vue` | `agent:workbench:view` 或 `agents:view` | 已实现 |
| `/agent/workbench/:id/workflow` | `agent-workflow-canvas` | `AgentWorkflowCanvasView.vue` | `agent:workbench:view` 或 `agents:view` | 已实现 |
| `/archives/agents` | `archives-agents` | `ArchiveAgentView.vue` | `agents:view` | 兼容旧入口 |
| `/agent/personal-avatar` | `agent-personal-avatar` | `PersonalAvatarView.vue` | `agent:personal_avatar:view` | 路由和页面存在 |
| `/agent/digital-employee` | `agent-digital-employee` | `DigitalEmployeeView.vue` | `agent:digital_employee:view` | 路由和页面存在 |
| `/agent/business-ontology` | `agent-business-ontology` | `BusinessOntologyView.vue` | `agent:business_ontology:view` | 路由和页面存在 |
| `/agent/knowledge-base` | `agent-knowledge-base` | `KnowledgeBaseView.vue` | `agent:knowledge_base:view` | 路由和页面存在 |
| `/agent/desensitization` | `agent-desensitization` | `DesensitizationConfigView.vue` | `agent:desensitization:view` | 页面存在，路由定义有重复问题 |

### 5.2 路由维护规则

新增或调整 Agent 页面时，必须同步检查：

1. `router/route-catalog.ts`：路径、名称、组件和 `metaKey`。
2. `router/route-meta.ts`：标题、描述和权限码。
3. `router/navigation-config.ts`：是否应出现在 Agent 导航树中。
4. 路由、导航和布局测试：权限可见性、兼容入口和回退路径。

不得只修改其中一个文件。权限别名兼容必须显式记录，不能散落在组件模板中。

## 6. FinWork 对话架构

### 6.1 文件边界

| 文件 | 当前职责 |
| --- | --- |
| `views/agent/AgentChatView.vue` | 页面结构、模型选择控件、空状态、自动滚动和交互事件 |
| `components/agent/ChatMessageList.vue` | 用户/助手消息、时间、失败提示和重试事件 |
| `components/agent/ChatInputBox.vue` | 文本输入、长度限制、禁用状态和发送事件 |
| `composables/useAgentChatSession.ts` | 会话初始化、消息加载、乐观更新、模型选择和本地持久化 |
| `api/modules/agent-chat.ts` | FinWork API 封装 |
| `api/modules/agent-chat-types.ts` | 会话、消息、模型元数据和请求响应类型 |

### 6.2 初始化流程

```text
进入 /agent/chat
  -> GET /auth/agent/chat/meta
  -> 恢复 localStorage 中的模型选择
  -> URL 有 sessionId：加载该会话最近消息
  -> URL 无 sessionId：创建新会话
  -> 页面进入可发送状态
```

当前模型选择保存在 `localStorage` 的 `finwork-chat-model-selection` 中。Provider 变化时会选择该 Provider 下第一个可用模型；模型和 Provider 会随消息请求传给后端。

元数据接口失败时，前端会回退到 `LOCAL_7B / deepseek-r1:7b` 的本地模型元数据。该回退只保证界面仍可操作，不代表本机 Ollama 或对应模型一定可用。

### 6.3 发送流程

```text
用户输入
  -> 生成 clientMessageId
  -> 本地追加 pending 用户消息
  -> POST /auth/agent/chat/conversations/{sessionId}/messages
     携带 message、providerCode、model
  -> 成功：用户消息变为 success，追加 assistant 消息
  -> 失败：用户消息变为 failed，展示错误和重试入口
```

消息列表优先使用服务端 `id`，其次使用 `clientMessageId` 作为稳定 key。消息内容通过文本插值展示，当前未使用 `v-html`，因此没有引入 Markdown HTML 渲染面。

### 6.4 后端协作事实

当前后端 `AgentChatController` 已提供会话创建、会话列表、模型元数据、发送消息和历史消息接口。发送消息时：

- 校验登录和会话所有权。
- 保存用户消息和助手消息。
- 使用关键词做基础意图识别。
- 报销查询意图进入 `AgentWorkflowRuntimeService`。
- 普通聊天进入模型路由服务。

这些是当前后端行为，不是前端保证。前端不应复制意图识别、数据查询或模型路由逻辑。

## 7. Agent 工作台与流程画布

### 7.1 工作台职责

`ArchiveAgentView.vue` 当前负责：

- Agent 列表、关键词和状态筛选。
- Agent 详情编辑、新建、保存、删除、发布、启停和归档。
- Provider 与模型配置。
- 工具定义、触发器和工作流摘要。
- 手工测试运行、最近五条运行记录和运行步骤抽屉。
- 跳转独立流程画布。

页面根据新旧两套权限码计算操作能力，后端仍必须进行真实权限校验。

### 7.2 流程画布职责

`AgentWorkflowCanvasView.vue` 当前负责：

- 从 Agent 详情加载节点和连线。
- 分类节点库、节点拖入画布、节点位置和配置编辑。
- 节点双击打开配置抽屉，点击画布其他位置关闭。
- 节点连线、右键连线菜单和删除连接。
- 基础流程校验和保存。
- 通过工作台更新接口持久化整个 Agent 配置。

画布没有单独的后端资源模型，仍使用 `ArchiveAgentSavePayload` 整体保存。因此修改保存协议时，工作台和画布必须同步验证。

### 7.3 工作台 API

统一前缀为 `/auth/agent/workbench`：

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/auth/agent/workbench` | 查询 Agent 列表 |
| GET | `/auth/agent/workbench/meta` | 获取工作台元数据 |
| GET | `/auth/agent/workbench/{id}` | 获取详情 |
| POST | `/auth/agent/workbench` | 新建 Agent |
| PUT | `/auth/agent/workbench/{id}` | 保存 Agent 和工作流 |
| DELETE | `/auth/agent/workbench/{id}` | 删除 Agent |
| POST | `/auth/agent/workbench/{id}/publish` | 发布 |
| POST | `/auth/agent/workbench/{id}/toggle-status` | 切换状态 |
| POST | `/auth/agent/workbench/{id}/run` | 测试运行 |
| GET | `/auth/agent/workbench/{id}/runs` | 查询运行记录 |
| GET | `/auth/agent/workbench/runs/{runId}` | 查询运行步骤 |

## 8. FinWork API 契约

统一前缀为 `/auth/agent/chat`，经 `core.ts` 和默认 `/api` 基础路径后，浏览器实际请求通常为 `/api/auth/agent/chat/**`。

| 方法 | 路径 | 前端使用情况 |
| --- | --- | --- |
| GET | `/auth/agent/chat/meta` | 已使用，加载 Provider 和模型 |
| POST | `/auth/agent/chat/conversations` | 已使用，创建新会话 |
| GET | `/auth/agent/chat/conversations` | 后端存在，前端 API 模块当前未暴露 |
| GET | `/auth/agent/chat/conversations/{sessionId}/messages` | 已使用，加载历史消息 |
| POST | `/auth/agent/chat/conversations/{sessionId}/messages` | 已使用，发送消息 |

统一响应应符合：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

`core.ts` 对 HTTP 401 或业务码 401 清理登录状态并跳转 `/login`；其他非 200 业务码抛出 `Error`，由 Composable 或页面展示业务提示。

## 9. 权限与安全基线

### 9.1 权限现状

| 能力 | 权限码 | 当前状态 |
| --- | --- | --- |
| 查看 FinWork | `agent:chat:view` | 路由和菜单使用 |
| 发送消息 | `agent:chat:send` | SQL 中存在；当前前端按钮和 `AgentChatController` 未按此权限强校验 |
| 查看工作台 | `agent:workbench:view`、兼容 `agents:view` | 前后端使用 |
| 新建/编辑/删除/运行/发布/日志 | `agent:workbench:*`、兼容 `agents:*` | 工作台按权限控制按钮，后端执行校验 |

前端权限控制只用于导航和交互可见性，不能作为安全边界。

### 9.2 安全要求

- 所有请求通过 `core.ts` 注入 Bearer Token。
- 后端必须校验当前用户、租户、会话所有权和 Agent 操作权限。
- 前端不得接收或保存模型 API Key、数据库凭证和脱敏映射表。
- 聊天消息默认按纯文本展示；引入 Markdown 时必须先完成 XSS 策略和安全测试。
- 工具查询、脱敏、还原和证据引用由后端受控链路完成，前端只展示结果。
- 错误信息不得直接透传堆栈、SQL、密钥或内部网络地址。

## 10. 当前功能状态

### 10.1 已实现并有代码依据

- FinWork 独立路由和 Agent 导航入口。
- 新建会话、按 `sessionId` 加载历史消息、发送消息。
- Provider/模型选择及选择持久化。
- 本地模型元数据回退。
- 消息乐观更新、失败状态和重试入口。
- Agent 工作台的列表、详情、保存、删除、发布、运行和日志查看。
- 独立流程画布、节点和连线编辑、配置抽屉及保存。
- 新旧工作台权限码兼容。
- Agent 相关辅助页面的路由和导航入口。

### 10.2 部分实现或尚未闭环

- 后端提供会话列表，但前端没有会话侧边栏，也没有调用该列表接口。
- `agent:chat:send` 已有权限脚本，但当前发送链路未完成前后端强制校验。
- 失败消息有重试入口，但当前实现会新增一条消息，原失败消息的状态流转仍需专项测试和修正。
- 模型元数据失败时回退到本地模型，但没有探测 Ollama 服务和模型是否真实可用。
- Agent 辅助页面存在，不在本次合并审计范围内，不能据此认定其业务链路全部完成。

### 10.3 当前未实现

- 会话列表、切换、重命名、删除、搜索和分页。
- SSE/WebSocket 流式输出。
- Markdown、代码高亮、附件和工具调用过程可视化。
- 跨页面 Agent 会话共享状态。
- FinWork 组件级端到端测试和主流浏览器兼容性测试。

## 11. 已知问题与技术风险

| 编号 | 问题 | 影响 | 建议 |
| --- | --- | --- | --- |
| R1 | `route-catalog.ts` 中 `agent-desensitization` 路由重复定义 | 可能产生重复路由名警告或维护歧义 | 保留一处定义并补路由测试 |
| R2 | `agent:chat:send` 未在当前发送接口强制校验 | 仅有查看权限的用户可能仍能发送 | 前后端同时补按钮状态和后端鉴权 |
| R3 | 重试流程会调用常规发送并生成新消息 | 原失败消息可能保留错误或 pending 状态 | 将“重发已有消息”和“新消息发送”拆成共享私有方法 |
| R4 | 本地回退元数据固定标记可用 | 服务未启动时界面状态可能误导 | 增加后端健康状态或可用性字段 |
| R5 | 工作台与画布整体保存同一 Agent Payload | 并发编辑时可能覆盖另一页面修改 | 后端增加版本号或乐观锁，前端处理冲突 |
| R6 | Agent 路由同时保留新旧工作台入口和权限 | 长期维护成本上升 | 迁移完成后制定旧入口和 `agents:*` 权限退役计划 |

## 12. 测试与验证记录

2026-09-17 运行以下定向测试：

```bash
npm run test:unit -- \
  src/composables/__tests__/useAgentChatSession.test.ts \
  src/router/__tests__/index.test.ts \
  src/router/__tests__/navigationConfig.test.ts \
  src/layouts/__tests__/MainLayout.test.ts \
  src/views/archives/__tests__/agentRunDraft.test.ts
```

结果：5 个测试文件、38 个测试全部通过。

覆盖内容包括：

- FinWork 默认模型加载和发送时携带模型参数。
- Agent 工作台新旧路由与权限元数据。
- 导航回退和 MainLayout 菜单可见性。
- 工作台运行入参草稿持久化。

当前测试没有覆盖：

- FinWork 组件交互、重试完整状态流和后端联调。
- 工作台 CRUD、删除确认、发布和运行的组件测试。
- 画布拖拽、连线、抽屉和保存冲突。
- 浏览器兼容性与端到端业务链路。

最近一次生产构建验证已通过。当前全量类型检查仍被 `PersonalBankAccountDialog.vue` 两处既有 `accountId` 类型错误阻断，该问题与 Agent 模块无关，但在修复前不能宣称仓库全量类型检查通过。

## 13. 开发约束

### 13.1 新增 FinWork 能力

- API 类型放在 `agent-chat-types.ts`，请求封装放在 `agent-chat.ts`。
- 会话与消息状态优先放在 `useAgentChatSession.ts`，页面只编排交互。
- 只有出现跨页面共享、多会话并行或复杂缓存需求时才引入 Pinia。
- 新消息状态必须可追踪，不能使用数组 `pop()` 作为失败回滚。
- 新的模型参数必须通过类型、API、Composable、后端 DTO 全链路同步。

### 13.2 新增工作台工具或画布节点

- 先扩展 `ArchiveAgentMeta` 和后端元数据，再由页面消费，避免前端硬编码两套目录。
- 节点配置必须保持结构化对象，禁止把配置序列化成不可校验的大字符串。
- 保存前执行节点、连线和必填配置校验。
- 工具执行能力只在后端注册，前端节点只能引用受控 `toolCode`。
- 工作台详情页与画布复用同一 DTO 时，必须添加回归测试防止字段丢失。

### 13.3 质量要求

- 文件统一使用 UTF-8，禁止通过错误编码转换修复中文。
- 路由、元数据、导航和权限测试必须同步更新。
- 页面不得直接调用 `fetch`，统一经过 API 模块和 `core.ts`。
- 不修改无关模块，不以大规模重构替代局部修复。
- 文档中的“已完成”必须能指向代码、测试或可复现验证记录。

## 14. 变更验收清单

Agent 前端变更合入前至少确认：

- [ ] 路由可访问，菜单权限和直接访问权限一致。
- [ ] API 路径使用 `/auth/**`，由 `/api` 基础路径统一代理。
- [ ] 请求和响应类型与后端 DTO 一致。
- [ ] 401、业务错误、网络失败和加载状态有明确表现。
- [ ] 消息和列表使用稳定 key，不依赖时间戳碰撞。
- [ ] 中文显示和文件编码正常。
- [ ] 相关单元测试通过。
- [ ] 生产构建通过。
- [ ] 全量类型检查通过，或明确记录与本次无关的既有阻断项。
- [ ] 没有把凭证、敏感数据、脱敏映射或权限判断下沉到前端。

## 15. 维护说明

本文是 Agent 前端架构和开发状态的唯一基线文档。后续更新应直接修改本文，不再按开发人员、评审轮次或临时 Phase 拆分出重复文档。

建议在以下情况更新本文：

- Agent 路由、权限码或导航结构变化。
- FinWork API、消息协议或模型选择策略变化。
- 工作台保存协议、节点类型或工具元数据变化。
- 已知问题关闭或出现新的架构风险。
- 测试范围、构建状态或部署边界发生变化。

历史讨论如需保留，应进入版本控制历史或独立决策记录，不应再次复制完整实现代码到本基线中。
