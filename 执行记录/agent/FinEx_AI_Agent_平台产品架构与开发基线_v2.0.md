# FinEx AI Agent 平台产品架构与开发基线

**文档版本**: v3.0 (定稿版)  
**发布时间**: 2026-09-12  
**文档性质**: 可落地执行的技术基线  
**适用阶段**: 初代版本 (Demo) → 企业版本演进

---

## 文档说明

本文档是 FinEx AI Agent 平台的**最终技术基线**，基于三方协作（Claude + GPT + Fin）的共识，剔除了讨论过程，仅保留可直接执行的架构设计、技术方案和实施计划。

**核心原则**：

- 轻量化组件，企业级契约
- 接口优先，实现可替换
- 渐进式演进，避免重写
- 安全第一，可控风险

---

## 一、战略定位

### 1.1 产品定位

FinEx AI Agent 平台定位为**企业级可控智能协同中枢**，核心差异化价值：

- **数据主权可控**：敏感数据通过本地正则规则脱敏与映射表处理，云端大模型仅处理脱敏后数据
- **业务深度融合**：基于 FinEx 现有业务本体，构建财务领域专有 Agent 能力
- **渐进式智能**：从基础对话到数字员工，分阶段演进
- **开放生态**：支持 MCP 协议（企业版），可接入外部数据源

### 1.2 对标 YonWork 的差异化

| 维度   | YonWork | FinEx AI Agent | 优势      |
| ---- | ------- | -------------- | ------- |
| 数据安全 | 企业级加密   | 正则规则脱敏 + 三段式安全链路 | **更严格** |
| 财务深度 | 通用会计    | 财务领域专有本体       | **更深**  |
| 责任追溯 | 基础审计    | 数字员工绑定责任人      | **更清晰** |
| 生态开放 | 用友生态    | MCP 开放协议       | **更开放** |

---

## 二、总体架构

### 2.1 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                    FinEx AI Agent 平台                   │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  前端交互层                                              │
│  ├─ FinWork (聊天工作台)                                │
│  ├─ Agent 工作台 (工具设计、调试发布、工作流编排)        │
│  ├─ 个人数字分身管理                                     │
│  ├─ 数字员工管理                                         │
│  ├─ 业务本体查询                                         │
│  └─ 企业知识库管理                                       │
│                                                          │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  应用编排层                                              │
│  ├─ Agent Runtime (意图识别、任务分解、上下文管理)      │
│  ├─ Workflow Selector (工作流选择与参数装配)             │
│  ├─ Model Router (能力感知路由、故障切换)               │
│  ├─ Tool Registry (工具定义、版本、权限、可见性)         │
│  ├─ Tool Runtime (参数校验、适配执行、审计追踪)          │
│  └─ Context Assembly (四层上下文装配)                   │
│                                                          │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  智能推理层                                              │
│  ├─ 云端大模型 (DeepSeek、Kimi)                         │
│  ├─ 脱敏引擎 (可配置正则规则 + 映射表 + 后验检测)       │
│  └─ 降级策略 (四级能力矩阵)                             │
│                                                          │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  数据与知识层                                            │
│  ├─ 业务本体 (MySQL 轻量语义层)                         │
│  ├─ 知识库 (文档版本、向量检索)                         │
│  ├─ 用户记忆 (偏好、技能、习惯)                         │
│  └─ 数字员工档案 (配置、责任人、审计)                   │
│                                                          │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  基础设施层                                              │
│  ├─ 初代: MySQL + 本地文件 + 内存缓存                   │
│  └─ 企业版: MySQL + Milvus + Redis + Kafka             │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### 2.2 数据流（中高鲁棒性安全架构）

```
用户输入: "查看远智的往来款，金额超过12000元"
  ↓
┌────────────────────────────────────────┐
│ 阶段 1: 本地意图识别与实体解析          │
│ - 识别任务: 往来款查询                  │
│ - 解析实体: 远智 → 候选供应商/客户/项目 │
│ - 若候选不唯一，先向用户澄清            │
│ - 确认后生成 entityRef: SUPPLIER:1024  │
└────────────────────────────────────────┘
  ↓
┌────────────────────────────────────────┐
│ 阶段 2: 工作流选择与受控工具执行        │
│ - FinWork 选择已发布 Agent 工作流       │
│ - Tool Runtime 校验权限、Schema、风险   │
│ - 通过白名单工具或 SafeSqlBuilder 查询  │
│ - 禁止任意脚本、任意 SQL、任意 HTTP     │
└────────────────────────────────────────┘
  ↓
┌────────────────────────────────────────┐
│ 阶段 3: 查询结果脱敏与结构化上下文      │
│ - entityRef 保留，真实名称脱敏          │
│ - 远智科技有限公司 → [ORG_001]          │
│ - 金额、单号、账号等进入映射表          │
│ - 生成 mappingId、traceId、evidenceRef  │
└────────────────────────────────────────┘
  ↓ (仅传输脱敏结构化数据)
┌────────────────────────────────────────┐
│ 阶段 4: 云端推理                        │
│ - 云端模型只处理脱敏后的 JSON/文本      │
│ - 输出必须保留占位符和 evidenceRef      │
│ - 不允许生成不存在的占位符              │
└────────────────────────────────────────┘
  ↓ (返回脱敏结构化结果)
┌────────────────────────────────────────┐
│ 阶段 5: 本地校验、还原与补全            │
│ - 校验 JSON Schema 与占位符合法性       │
│ - 按 mappingId 还原敏感值               │
│ - 必要时按 entityRef 回表补真实字段     │
│ - 后验检测通过后返回用户                │
└────────────────────────────────────────┘
  ↓
返回给用户
```

鲁棒性目标：初代版本按“中高鲁棒性”建设，即允许通过规则和已发布工作流自动处理常见查询、分析和咨询场景，但对实体不唯一、权限不足、结果校验失败、模型输出缺失引用等情况必须澄清、拒绝、重试或降级，而不是让大模型自由猜测。

核心约束：

- 正确性以本地 `entityRef`、业务主键、`mappingId` 和 `evidenceRef` 为准，不以云端模型对名称的理解为准。
- 云端模型不接收真实敏感值，也不负责真实名称还原。
- 工作流输出应优先使用结构化 JSON；自然语言只作为展示层文案。
- 本地还原只替换映射表中存在的占位符，模型生成未知占位符时必须拦截。
- “远智”这类名称必须先在本地完成实体解析；同名或多候选时必须向用户澄清。

---

## 三、核心模块设计

### 3.1 FinWork - 交互式聊天工作台

#### 功能定位

用户与 AI Agent 交互的主入口，提供多模态、上下文感知、任务导向的对话界面。

#### 核心能力

**支持的任务类型**：

| 任务类型 | 用户输入示例             | Agent 处理流程              |
| ---- | ------------------ | ----------------------- |
| 单据查询 | "我上个月的报销单都审批了吗？"   | 提取时间范围 → 查询数据库 → 自然语言返回 |
| 流程推进 | "帮我催一下财务审批我的差旅报销"  | 识别单据 → 发送催办消息 → 返回结果    |
| 政策咨询 | "住宿费超过500元需要什么审批？" | 向量检索 → 召回政策 → 解释规则      |
| 智能填单 | "我要报销昨天在北京的打车费80元" | 提取信息 → 创建草稿 → 提示上传发票    |
| 数据分析 | "分析我部门Q1的费用构成"     | 查询数据 → 生成图表 → 解读关键发现    |

#### 工作流触发机制

FinWork 不直接维护查询脚本，也不直接拼接 SQL。FinWork 在对话过程中只承担入口和调度职责：

1. 接收用户自然语言输入，结合会话上下文识别意图、实体类型、时间范围、金额范围等候选参数。
2. 调用 `Workflow Selector`，从 Agent 工作台已发布工作流中选择匹配流程。
3. 对“远智”这类名称先执行本地实体解析，若命中多个供应商、客户、项目或员工候选，先向用户澄清。
4. 将确认后的参数以结构化形式传入工作流，例如 `entityRef`、`dateRange`、`amountRange`、`queryScope`。
5. 工作流执行结果必须携带 `traceId`、`mappingId`、`evidenceRef`，FinWork 只展示通过本地校验和还原后的结果。

FinWork 允许调用云端模型做理解、摘要和表达优化，但不能把未经脱敏的查询结果直接发送给云端模型，也不能让云端模型决定最终数据权限。

#### 技术实现

**前端**：

- Vue 3 + TypeScript
- Element Plus
- WebSocket/SSE (流式响应)
- 组件：对话窗口、历史列表、上下文面板、快捷操作

**后端**：

- 服务：`backend/auth-service` (复用现有单体)
- 模块：
  - `agent/runtime` - Agent 运行时
  - `agent/chat` - 对话管理
  - `agent/context` - 上下文装配
  - `agent/tool` - 工具执行

---

### 3.2 个人数字分身 - 技能与习惯蒸馏

#### 功能定位

通过持续学习用户的操作行为、决策偏好，构建用户的数字镜像，实现工作习惯复制。

#### 核心能力（初代范围）

**显式偏好管理**：

- 用户手动设置常用费用类型、部门、成本中心
- 系统记录用户的审批决策偏好（通过率、驳回理由）
- 用户可查看、编辑、删除所有偏好

**智能推荐**：

- 创建报销单时自动预填常用字段
- 审批时显示历史类似单据的处理方式
- 根据用户习惯时间发送待办提醒

**不包含（暂缓到企业版）**：

- ❌ 模型蒸馏
- ❌ 强化学习
- ❌ 自动化操作（需人工确认）

#### 数据表设计

```sql
CREATE TABLE ea_user_memory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
  company_id BIGINT NOT NULL DEFAULT 1,
  user_id BIGINT NOT NULL,
  memory_type VARCHAR(50) NOT NULL,  -- preference, habit
  memory_key VARCHAR(100) NOT NULL,  -- frequent_expense_type
  memory_value JSON,                 -- {"差旅费": 0.8, "办公费": 0.2}
  confidence_score DECIMAL(5,4),
  source VARCHAR(50),                -- explicit, implicit
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_memory (tenant_id, company_id, user_id, memory_type, memory_key),
  INDEX idx_user (user_id)
) COMMENT='用户记忆与偏好';
```

---

### 3.3 数字员工 - 流程嵌入与责任绑定

#### 功能定位

嵌入到业务流程中的自动化 Agent，执行重复性、规则性任务，与真实用户绑定以明确责任主体。

#### 初代支持的类型

| 类型      | 职责           | 权限范围    | 绑定用户 |
| ------- | ------------ | ------- | ---- |
| **审核型** | 报销单合规检查、发票验证 | 只读 + 建议 | 财务主管 |
| **催办型** | 自动催办超时任务     | 只读 + 通知 | 行政主管 |
| **咨询型** | 回答财务政策问题     | 只读      | 财务经理 |

**不支持（暂缓到企业版）**：

- ❌ 自动审批
- ❌ 自动付款
- ❌ 自动记账

#### 工作流绑定机制

数字员工不直接拥有业务查询或操作能力，而是绑定真实责任用户，并按配置调用 Agent 工作台中已发布、已授权的工作流。

绑定关系必须同时记录：

- `responsible_user_id`: 责任用户，用于权限交集、审计与申诉。
- `workflow_id` / `workflow_version`: 可调用的 Agent 工作流及固定版本。
- `scope_json`: 可处理的数据范围，例如部门、项目、金额上限、业务对象类型。
- `risk_policy`: 高风险节点是否需要人工确认。

运行时权限取交集：

```text
责任用户权限
∩ 数字员工配置范围
∩ 工作流发布状态
∩ 工具权限
∩ 业务对象数据权限
∩ 当前任务上下文范围
```

#### 责任绑定机制

```sql
CREATE TABLE ea_digital_employee (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
  company_id BIGINT NOT NULL DEFAULT 1,
  name VARCHAR(100) NOT NULL,
  type ENUM('审核型', '执行型', '咨询型', '催办型') NOT NULL,
  responsible_user_id BIGINT NOT NULL,  -- 责任人
  authority_level INT NOT NULL,         -- 权限等级 1-5
  max_amount DECIMAL(15,2),             -- 最大处理金额
  config JSON,                          -- 配置参数
  status ENUM('active', 'suspended') DEFAULT 'active',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_by BIGINT NOT NULL,
  FOREIGN KEY (responsible_user_id) REFERENCES sys_user(id),
  INDEX idx_responsible (responsible_user_id)
) COMMENT='数字员工配置';

CREATE TABLE ea_digital_employee_action_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
  employee_id BIGINT NOT NULL,
  responsible_user_id BIGINT NOT NULL,
  action_type VARCHAR(50) NOT NULL,  -- review, notify, analyze
  business_object_type VARCHAR(50),  -- ExpenseReport
  business_object_id BIGINT,
  decision_reason TEXT,
  confidence_score DECIMAL(5,4),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  trace_id VARCHAR(64),
  FOREIGN KEY (employee_id) REFERENCES ea_digital_employee(id),
  INDEX idx_employee_time (employee_id, created_at),
  INDEX idx_trace (trace_id)
) COMMENT='数字员工操作日志';
```

---

### 3.4 业务本体 - MySQL 轻量语义层

#### 功能定位

以结构化方式定义业务对象、字段、状态、关系，为 AI Agent 提供准确的业务理解能力。

#### 核心表结构

```sql
-- 1. 实体定义
CREATE TABLE ea_entity_definition (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  entity_key VARCHAR(50) UNIQUE NOT NULL,  -- expense_report
  display_name VARCHAR(100) NOT NULL,      -- 报销单
  table_name VARCHAR(100),                 -- expense_report
  description TEXT,
  parent_entity_key VARCHAR(50),           -- 继承关系
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_parent (parent_entity_key)
) COMMENT='实体定义';

-- 2. 字段语义
CREATE TABLE ea_field_semantics (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  entity_key VARCHAR(50) NOT NULL,
  field_key VARCHAR(50) NOT NULL,      -- amount
  field_type VARCHAR(20),              -- NUMBER, DATE, ENUM
  display_name VARCHAR(100),           -- 金额
  aliases JSON,                        -- ["报销金额", "费用"]
  is_sensitive BOOLEAN DEFAULT FALSE,  -- 是否需要脱敏
  is_searchable BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_entity_field (entity_key, field_key),
  INDEX idx_entity (entity_key)
) COMMENT='字段语义';

-- 3. 枚举值语义
CREATE TABLE ea_enum_semantics (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  entity_key VARCHAR(50) NOT NULL,
  field_key VARCHAR(50) NOT NULL,
  db_value VARCHAR(50) NOT NULL,       -- Approved
  display_value VARCHAR(100) NOT NULL, -- 已审批
  aliases JSON,                        -- ["审批通过", "通过"]
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_enum (entity_key, field_key, db_value),
  INDEX idx_entity_field (entity_key, field_key)
) COMMENT='枚举值语义';

-- 4. 实体关系（声明式，不保存SQL）
CREATE TABLE ea_entity_relation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  source_entity VARCHAR(50) NOT NULL,  -- expense_report
  relation_type VARCHAR(50) NOT NULL,  -- HAS_INVOICE
  target_entity VARCHAR(50) NOT NULL,  -- invoice
  cardinality VARCHAR(20),             -- ONE_TO_MANY
  join_config JSON,                    -- {"conditions": [{"sourceField": "id", "targetField": "expense_report_id", "operator": "EQ"}]}
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_source (source_entity),
  INDEX idx_target (target_entity)
) COMMENT='实体关系';

-- 5. 操作语义
CREATE TABLE ea_operation_semantics (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  entity_key VARCHAR(50) NOT NULL,
  operation_key VARCHAR(50) NOT NULL,  -- submit, approve
  display_name VARCHAR(100),           -- 提交, 审批
  api_endpoint VARCHAR(200),           -- POST /api/expense-reports/{id}/submit
  required_permission VARCHAR(100),    -- expense:submit
  requires_confirmation BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_entity (entity_key)
) COMMENT='操作语义';
```

#### 自动生成机制

```java
@Service
public class OntologySchemaSync {

    /**
     * 从数据库 Schema 自动生成语义表
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void syncFromSchema() {
        List<TableMetadata> tables = jdbcTemplate.query(
            "SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = ?",
            (rs, rowNum) -> new TableMetadata(rs.getString(1), rs.getString(2)),
            databaseName
        );

        for (TableMetadata table : tables) {
            // 插入或更新实体定义
            entityRepo.upsert(EntityDefinition.fromTable(table));

            // 查询字段并插入字段语义
            List<ColumnMetadata> columns = getColumns(table.getName());
            for (ColumnMetadata col : columns) {
                fieldRepo.upsert(FieldSemantics.fromColumn(table.getName(), col));
            }
        }
    }
}
```

---

### 3.5 企业知识库 - 向量检索与 RAG

#### 功能定位

财务领域专有知识的向量化存储与检索，通过 RAG 技术让 AI Agent 准确引用企业政策。

#### 知识分类

```
企业知识库
├── 制度政策类（财务管理制度、费用报销办法、差旅规定）
├── 会计准则类（企业会计准则、会计科目表、记账规则）
├── 税务法规类（增值税政策、企业所得税法、税收优惠）
├── 流程 SOP 类（报销流程、付款流程、月末结账流程）
├── 合同模板类（劳动合同、采购合同、服务合同）
└── 历史案例类（常见问题 FAQ、异常处理案例）
```

#### 核心表结构

```sql
CREATE TABLE ea_knowledge_document (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
  company_id BIGINT NOT NULL DEFAULT 1,
  title VARCHAR(200) NOT NULL,
  category ENUM('制度政策', '会计准则', '税务法规', '流程SOP', '合同模板', '历史案例') NOT NULL,
  file_path VARCHAR(500),
  file_hash VARCHAR(64),        -- SHA256
  version VARCHAR(20),
  status ENUM('active', 'archived', 'expired') DEFAULT 'active',
  valid_from DATE,
  valid_until DATE,
  uploaded_by BIGINT,
  uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  vector_indexed BOOLEAN DEFAULT FALSE,
  INDEX idx_tenant_company (tenant_id, company_id),
  INDEX idx_category_status (category, status)
) COMMENT='知识库文档';

CREATE TABLE ea_knowledge_chunk (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  chunk_index INT NOT NULL,
  chunk_id VARCHAR(100) UNIQUE NOT NULL,  -- 稳定的 chunk 标识
  content TEXT NOT NULL,
  content_hash VARCHAR(64),               -- 用于检测内容变化
  vector_id VARCHAR(100),                 -- Milvus 中的向量 ID
  metadata JSON,                          -- {page: 3, section: "第二章"}
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (document_id) REFERENCES ea_knowledge_document(id),
  INDEX idx_document (document_id),
  INDEX idx_chunk_id (chunk_id)
) COMMENT='知识库分块';
```

#### 向量化流程（初代：单机方案）

```text
文档上传
  ↓
文档解析（PDF/Word → 文本）
  ↓
语义分块（512 tokens，重叠 50 tokens）
  ↓
生成 chunk_id（document_id + chunk_index + content_hash）
  ↓
向量嵌入（BGE-large-zh 或 API）
  ↓
存储到本地向量索引（FAISS / Annoy）
  ↓
元数据入库（ea_knowledge_chunk）
```

**企业版演进**：本地向量索引 → Milvus 集群

---

## 四、技术架构

### 4.1 模型 Provider 架构

#### 能力声明

```java
@Data
public class ModelCapability {
    // 功能能力
    private boolean chatCompletion;
    private boolean streamingSupport;
    private boolean jsonMode;
    private boolean toolCalling;

    // 上下文能力
    private int maxContextTokens;
    private int maxOutputTokens;

    // 非功能能力
    private ModelDeployment deployment;     // CLOUD, LOCAL
    private DataResidency dataResidency;    // CHINA, ON_PREMISE

    // 性能指标
    private int avgLatencyMs;
    private double costPerMillion;  // 百万 tokens 成本（元）

    // 适用场景
    private Set<ModelUseCase> useCases;
}

public enum ModelUseCase {
    CHAT_QUERY,          // 普通对话查询
    DEEP_ANALYSIS,       // 深度分析推理
    SUMMARIZATION        // 内容摘要
}
```

> v3.0 调整：脱敏链路不再作为模型 Provider 的使用场景，不再通过本地模型或 NER 模型执行脱敏。敏感文本脱敏统一由本地正则规则引擎完成，模型 Provider 仅承担对话、分析、摘要等推理任务。

#### 路由策略（初代范围）

```yaml
# config/model-routing.yml
routing:
  rules:
    - useCase: "CHAT_QUERY"
      sensitivity: "LOW"
      primary: "DEEPSEEK"
      fallback: ["KIMI"]

    - useCase: "DEEP_ANALYSIS"
      sensitivity: "LOW"
      primary: "KIMI"        # 长上下文优势
      fallback: ["DEEPSEEK"]

providers:
  - code: "DEEPSEEK"
    apiKey: "${DEEPSEEK_API_KEY}"
    endpoint: "https://api.deepseek.com/v1"
    capabilities:
      chatCompletion: true
      streamingSupport: true
      maxContextTokens: 64000
      deployment: "CLOUD"
      dataResidency: "CHINA"
      avgLatencyMs: 1500
      costPerMillion: 20
      useCases: ["CHAT_QUERY", "DEEP_ANALYSIS", "SUMMARIZATION"]

  - code: "KIMI"
    apiKey: "${KIMI_API_KEY}"
    endpoint: "https://api.moonshot.cn/v1"
    capabilities:
      chatCompletion: true
      streamingSupport: true
      maxContextTokens: 200000
      deployment: "CLOUD"
      dataResidency: "CHINA"
      avgLatencyMs: 2000
      costPerMillion: 50
      useCases: ["DEEP_ANALYSIS"]

```

#### 接口设计

```java
// 统一模型接口
public interface ChatModelProvider {
    String getProviderCode();
    ModelCapability getCapability();
    ChatResponse chat(ChatRequest request);
    ChatResponse chatStream(ChatRequest request, StreamHandler handler);
}

// DeepSeek 实现
@Service("deepseekProvider")
public class DeepSeekProvider implements ChatModelProvider {
    @Override
    public String getProviderCode() {
        return "DEEPSEEK";
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        // 调用 DeepSeek API
    }
}

// Kimi 实现
@Service("kimiProvider")
public class KimiProvider implements ChatModelProvider {
    // ...
}

```

---

### 4.2 脱敏架构（正则规则 + 映射表 + 后验检测）

```java
@Service
public class RuleBasedDesensitizationService {

    public DesensitizedData desensitize(UserQuery query) {
        // 第一层：结构化字段白名单，对明确字段直接按字段类型脱敏
        Map<String, Object> structuredData = query.getStructuredParams();
        FieldMaskingResult layer1 = fieldWhitelistMasker.mask(structuredData);

        // 第二层：自由文本正则规则匹配，不调用本地模型或云端模型
        String freeText = query.getFreeText();
        RegexMaskingResult layer2 = null;
        if (StringUtils.isNotBlank(freeText)) {
            layer2 = regexMasker.mask(freeText);
        }

        // 生成映射表，供云端模型响应回传后本地还原
        MappingTable mappingTable = new MappingTable(layer1, layer2);
        mappingTable.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        mappingRepository.save(mappingTable);

        return DesensitizedData.builder()
            .maskedStructured(layer1.getMaskedData())
            .maskedText(layer2 != null ? layer2.getMaskedText() : "")
            .mappingId(mappingTable.getId())
            .build();
    }

    public String restore(String maskedResponse, String mappingId) {
        MappingTable table = mappingRepository.findById(mappingId)
            .orElseThrow(() -> new NotFoundException("映射表已过期"));

        String restored = table.restore(maskedResponse);

        // 第三层：后验检测
        if (leakageDetector.detect(restored, table.getOriginalData())) {
            log.error("检测到数据泄露，拦截返回");
            auditService.logLeakage(mappingId, restored);
            return "抱歉，系统检测到安全风险，无法返回结果";
        }

        return restored;
    }
}
```

v3.0 约束：

- 脱敏不调用本地模型，不依赖 `deepseek-r1:7b`、NER 模型或 AI 补充识别。
- 脱敏只使用企业可配置的正则表达式、字段白名单和映射表。
- 正则规则必须优先覆盖当前高频敏感类型：人名、金额、单据编号、手机号、身份证号、银行账号、邮箱、地址。
- 模型调用前必须先完成正则脱敏；模型返回后只允许通过本地映射表还原。
- 若后验检测发现未授权敏感信息，必须拦截返回并记录审计日志。

#### 结构化脱敏上下文与还原协议

为达到中高鲁棒性，脱敏链路不能只依赖自然语言字符串替换，必须同时维护结构化上下文。

```json
{
  "traceId": "trace-20260916-001",
  "mappingId": "map-uuid",
  "entities": [
    {
      "entityRef": "SUPPLIER:1024",
      "entityType": "SUPPLIER",
      "displayToken": "[ORG_001]",
      "originalNameHash": "sha256:...",
      "source": "database"
    }
  ],
  "maskedPayload": {
    "question": "查询[ORG_001]的往来款，金额超过[AMOUNT_001]",
    "records": [
      {
        "evidenceRef": "PAYABLE:8891",
        "counterparty": "[ORG_001]",
        "amount": "[AMOUNT_002]",
        "status": "UNSETTLED"
      }
    ]
  }
}
```

云端模型输出必须满足：

- 只引用输入中存在的 `displayToken`、`entityRef`、`evidenceRef`。
- 不生成新的真实名称、单号、金额、账号等敏感值。
- 不改变 `entityRef` 与 `evidenceRef` 的绑定关系。
- 结构化结果必须通过 JSON Schema 校验后才允许还原。

本地还原规则：

- `displayToken` 只按当前 `mappingId` 还原，跨会话、跨工作流、过期映射禁止还原。
- 对业务主数据，优先按 `entityRef` 回表补真实名称；映射表只作为展示还原辅助。
- 对单据、金额、账号等敏感字段，必须同时通过占位符映射和证据引用校验。
- 模型输出未知占位符、丢失证据引用或引用不在输入范围内的数据时，必须重试或降级为“无法确认结果”。

示例：“远智”在本地解析为 `SUPPLIER:1024` 后，云端只看到 `[ORG_001]` 和 `SUPPLIER:1024`。最终返回“远智科技有限公司当前有 3 笔未结清往来款”不是由云端模型猜出名称，而是本地根据 `SUPPLIER:1024` 和 `mappingId` 还原并补全。

#### 脱敏实体类型配置

脱敏服务支持灵活的实体类型配置，允许企业根据自身需求定制敏感信息识别规则。

**数据表设计：**

```sql
-- 脱敏实体类型配置表
CREATE TABLE ea_desensitization_entity_type (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
  company_id BIGINT NOT NULL DEFAULT 1,
  code VARCHAR(50) NOT NULL,                    -- PERSON_NAME, AMOUNT, DOCUMENT_NO
  name VARCHAR(100) NOT NULL,                   -- 人名、金额、单据编号
  description TEXT,
  placeholder_prefix VARCHAR(20) NOT NULL,      -- [姓名]、[金额]、[单号]
  regex_patterns JSON,                          -- ["张.*", "李.*"]
  examples JSON,                                -- ["张三", "12000.50", "BX202609001"]
  enabled BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_entity_type (tenant_id, company_id, code),
  INDEX idx_enabled (enabled)
) COMMENT='脱敏实体类型配置';

-- 脱敏映射表
CREATE TABLE ea_desensitization_mapping (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
  company_id BIGINT NOT NULL DEFAULT 1,
  user_id BIGINT NOT NULL,
  session_id VARCHAR(100),
  placeholder VARCHAR(100) NOT NULL,            -- [姓名1]
  original_value TEXT NOT NULL,                 -- 张三
  entity_type VARCHAR(50),                      -- PERSON_NAME
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP,
  INDEX idx_session (session_id),
  INDEX idx_user (user_id),
  INDEX idx_expires (expires_at)
) COMMENT='脱敏映射表';
```

**预置实体类型（8 种）：**

1. **PERSON_NAME** - 人名
   - 占位符前缀：`[姓名]`
   - 正则模式：中文姓名模式
   - 示例：张三、李四

2. **AMOUNT** - 金额
   - 占位符前缀：`[金额]`
   - 正则模式：数字 + 货币单位
   - 示例：12000.50元、¥5000

3. **DOCUMENT_NO** - 单据编号
   - 占位符前缀：`[单号]`
   - 正则模式：业务单据编号格式
   - 示例：BX202609001、FP20260914001

4. **PHONE** - 手机号
   - 占位符前缀：`[手机]`
   - 正则模式：11 位手机号
   - 示例：13812345678

5. **ID_CARD** - 身份证号
   - 占位符前缀：`[身份证]`
   - 正则模式：18 位身份证号
   - 示例：110101199001011234

6. **BANK_CARD** - 银行卡号
   - 占位符前缀：`[卡号]`
   - 正则模式：16-19 位卡号
   - 示例：6222021234567890

7. **EMAIL** - 邮箱
   - 占位符前缀：`[邮箱]`
   - 正则模式：标准邮箱格式
   - 示例：user@example.com

8. **ADDRESS** - 地址
   - 占位符前缀：`[地址]`
   - 正则模式：省市区街道模式
   - 示例：北京市朝阳区XX路XX号

**前端配置界面：**

脱敏规则配置页面提供：
- 实体类型管理（增删改查）
- 正则模式在线测试
- 脱敏效果实时预览
- 映射表查询与管理
- 批量导入/导出配置

**权限控制：**

```yaml
agent:desensitization:view        # 查看脱敏配置
agent:desensitization:manage      # 管理实体类型
agent:desensitization:test        # 测试脱敏效果
agent:desensitization:mapping     # 查看映射表
```

**脱敏策略选择：**

```java
public enum DesensitizationStrategy {
    REGEX_ONLY       // 仅正则匹配（初代唯一策略，快速、可解释、可审计）
}
```

说明：v3.0 起不再提供 `AI_MODEL` 和 `HYBRID` 脱敏策略。复杂实体识别后续如需增强，应先扩展实体类型配置、正则规则、字段白名单和测试用例，而不是把脱敏职责重新交给模型。

---

### 4.3 多租户感知框架

#### 租户上下文

```java
public class TenantContext {
    private static final ThreadLocal<TenantContextHolder> CONTEXT = new ThreadLocal<>();

    @Autowired
    private static AgentSecurityConfig securityConfig;

    public static String getEffectiveTenantId() {
        String tenantId = getTenantId();

        if (tenantId != null) {
            return tenantId;
        }

        // 租户上下文缺失
        if (securityConfig.getMode() == SecurityMode.DEMO) {
            // Demo 模式：允许回落
            String defaultTenantId = securityConfig.getDemo().getDefaultTenantId();

            if (securityConfig.getDemo().isWarnOnFallback()) {
                log.warn("租户上下文缺失，回落到默认租户: {}", defaultTenantId);
            }

            return defaultTenantId;
        } else {
            // 企业模式：直接拒绝
            throw new TenantContextMissingException("租户上下文缺失，请求被拒绝");
        }
    }

    public static Long getEffectiveCompanyId() {
        Long companyId = getCompanyId();

        if (companyId != null) {
            return companyId;
        }

        if (securityConfig.getMode() == SecurityMode.DEMO) {
            return securityConfig.getDemo().getDefaultCompanyId();
        } else {
            throw new TenantContextMissingException("公司上下文缺失，请求被拒绝");
        }
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
```

#### 配置

```yaml
# application-demo.yml
finex:
  agent:
    security:
      mode: demo
      demo:
        defaultTenantId: "default"
        defaultCompanyId: 1
        warnOnFallback: true

# application-enterprise.yml
finex:
  agent:
    security:
      mode: enterprise
```

#### 拦截器

```java
@Component
public class TenantContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            FinExUserDetails user = (FinExUserDetails) auth.getPrincipal();

            // 初代：固定租户
            TenantContext.setTenantId("default");
            TenantContext.setCompanyId(user.getCompanyId());
            TenantContext.setUserId(user.getUserId());

            // 企业版：从用户属性读取真实租户
            // TenantContext.setTenantId(user.getTenantId());
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, 
                               HttpServletResponse response, 
                               Object handler, 
                               Exception ex) {
        TenantContext.clear();
    }
}
```

#### MyBatis-Plus 配置

```java
@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 租户拦截器
        TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor();
        tenantInterceptor.setTenantLineHandler(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new StringValue(TenantContext.getEffectiveTenantId());
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // 白名单：需要租户隔离的表
                return !TENANT_AWARE_TABLES.contains(tableName);
            }
        });

        interceptor.addInnerInterceptor(tenantInterceptor);

        return interceptor;
    }
}
```

#### 基础实体类

```java
@Data
@MappedSuperclass
public abstract class TenantAwareEntity {

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted")
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "version_no")
    private Integer versionNo = 0;

    @PrePersist
    public void prePersist() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.getEffectiveTenantId();
        }
        if (this.companyId == null) {
            this.companyId = TenantContext.getEffectiveCompanyId();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.createdBy == null) {
            this.createdBy = SecurityUtils.getCurrentUserId();
        }
        if (this.traceId == null) {
            this.traceId = TraceContext.getCurrentTraceId();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
        this.updatedBy = SecurityUtils.getCurrentUserId();
        this.versionNo++;
    }
}
```

---

### 4.4 异步上下文传播

#### 执行上下文

```java
@Data
public class AgentExecutionContext implements Serializable {
    private String tenantId;
    private Long companyId;
    private Long userId;
    private String traceId;
    private String conversationId;
    private Long agentRunId;
    private String triggerSource;
    private Instant createdAt;

    public static AgentExecutionContext capture() {
        return AgentExecutionContext.builder()
            .tenantId(TenantContext.getTenantId())
            .companyId(TenantContext.getCompanyId())
            .userId(SecurityUtils.getCurrentUserId())
            .traceId(TraceContext.getCurrentTraceId())
            .createdAt(Instant.now())
            .build();
    }

    public void restore() {
        TenantContext.setTenantId(this.tenantId);
        TenantContext.setCompanyId(this.companyId);
        SecurityContextHolder.setContext(createSecurityContext(this.userId));
        TraceContext.setTraceId(this.traceId);
    }

    public static void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        TraceContext.clear();
    }
}
```

#### 异步配置

```java
@Configuration
public class AsyncConfig {

    @Bean
    public Executor finexAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("finex-async-");

        // 上下文传播
        executor.setTaskDecorator(new ContextPropagatingTaskDecorator());

        executor.initialize();
        return executor;
    }

    public static class ContextPropagatingTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            AgentExecutionContext context = AgentExecutionContext.capture();

            return () -> {
                try {
                    context.restore();
                    runnable.run();
                } finally {
                    AgentExecutionContext.clear();
                }
            };
        }
    }
}
```

---

### 4.5 上下文管理（四层架构）

```java
@Service
public class ContextAssemblyService {

    public AssembledContext assembleForQuery(String sessionId, Long userId, String userQuery) {
        // Layer 1: 短期对话（最近10轮）
        List<Message> recentMessages = conversationService.getRecentMessages(sessionId, 10);

        // Layer 2: 会话摘要（如果超过10轮）
        String conversationSummary = null;
        if (conversationService.getMessageCount(sessionId) > 10) {
            conversationSummary = conversationService.getSummary(sessionId);
        }

        // Layer 3: 长期记忆（用户偏好）
        UserMemory userMemory = userMemoryService.getMemory(userId);

        // Layer 4: 实时业务事实（并行查询）
        CompletableFuture<BusinessFacts> factsFuture = CompletableFuture.supplyAsync(() -> {
            return BusinessFacts.builder()
                .pendingExpenseCount(expenseService.countPending(userId))
                .draftCount(expenseService.countDraft(userId))
                .latestExpense(expenseService.getLatest(userId))
                .build();
        });

        // 组装
        return AssembledContext.builder()
            .sessionId(sessionId)
            .userId(userId)
            .recentMessages(recentMessages)
            .conversationSummary(conversationSummary)
            .userMemory(userMemory)
            .businessFacts(factsFuture.get(2, TimeUnit.SECONDS))
            .timestamp(Instant.now())
            .build();
    }
}
```

---

### 4.6 降级策略（四级能力矩阵）

```java
public enum DegradationLevel {
    L0_NORMAL(
        "正常模式",
        Set.of(Capability.READ, Capability.WRITE, Capability.ANALYZE, Capability.RECOMMEND)
    ),

    L1_CLOUD_FAILOVER(
        "云模型切换",
        Set.of(Capability.READ, Capability.WRITE, Capability.ANALYZE, Capability.RECOMMEND)
    ),

    L2_RULE_ENGINE(
        "规则引擎降级",
        Set.of(Capability.READ, Capability.RECOMMEND_TEMPLATE),
        Set.of(Capability.WRITE, Capability.ANALYZE, Capability.CUSTOM_QUERY)
    ),

    L3_FALLBACK_MENU(
        "引导菜单",
        Set.of(Capability.NAVIGATE),
        Set.of(Capability.READ, Capability.WRITE, Capability.ANALYZE)
    );

    public enum Capability {
        READ,                   // 查询数据
        WRITE,                  // 写入数据
        ANALYZE_BASIC,          // 基础分析
        ANALYZE,                // 深度分析
        RECOMMEND,              // AI推荐
        RECOMMEND_TEMPLATE,     // 模板推荐
        CUSTOM_QUERY,           // 自定义查询
        NAVIGATE                // 导航引导
    }
}

@Service
public class DegradationAwareExecutor {

    private volatile DegradationLevel currentLevel = DegradationLevel.L0_NORMAL;

    public <T> T execute(AgentTask task, Supplier<T> action) {
        // 检查能力是否允许
        if (!currentLevel.isCapabilityAllowed(task.getRequiredCapability())) {
            throw new CapabilityNotAvailableException(
                String.format("当前降级级别 %s 不支持 %s 能力", 
                    currentLevel.name(), 
                    task.getRequiredCapability())
            );
        }

        // 执行任务
        try {
            return action.get();
        } catch (Exception e) {
            recordFailure(task, e);
            throw e;
        }
    }
}
```

---

### 4.7 安全 SQL 构建

```java
@Service
public class SafeSqlBuilder {

    // 白名单：允许的表和字段
    private final Map<String, Set<String>> TABLE_COLUMNS = Map.of(
        "expense_report", Set.of("id", "user_id", "amount", "status", "created_at"),
        "invoice", Set.of("id", "expense_report_id", "invoice_number", "amount")
    );

    // 白名单：允许的操作符
    private final Set<String> ALLOWED_OPERATORS = Set.of("EQ", "NE", "GT", "LT", "GTE", "LTE", "IN");

    public QueryWrapper<ExpenseReport> buildQuery(StructuredQuery query) {
        // 1. 验证表名
        validateTableName(query.getEntityKey());

        // 2. 构建 MyBatis-Plus Wrapper（参数绑定，无SQL注入）
        QueryWrapper<ExpenseReport> wrapper = new QueryWrapper<>();

        for (FieldCondition cond : query.getConditions()) {
            // 3. 验证字段名
            validateFieldName(query.getEntityKey(), cond.getFieldKey());

            // 4. 验证操作符
            validateOperator(cond.getOperator());

            // 5. 使用参数绑定
            switch (cond.getOperator()) {
                case "EQ" -> wrapper.eq(cond.getFieldKey(), cond.getValue());
                case "GT" -> wrapper.gt(cond.getFieldKey(), cond.getValue());
                case "LT" -> wrapper.lt(cond.getFieldKey(), cond.getValue());
                case "IN" -> wrapper.in(cond.getFieldKey(), (List<?>) cond.getValue());
                // ...
            }
        }

        return wrapper;
    }

    private void validateTableName(String tableName) {
        if (!TABLE_COLUMNS.containsKey(tableName)) {
            throw new SecurityException("表名不在白名单中: " + tableName);
        }
    }

    private void validateFieldName(String tableName, String fieldName) {
        Set<String> allowedFields = TABLE_COLUMNS.get(tableName);
        if (allowedFields == null || !allowedFields.contains(fieldName)) {
            throw new SecurityException(
                String.format("字段 %s.%s 不在白名单中", tableName, fieldName)
            );
        }
    }
}
```

---

## 五、数据库设计

### 5.1 核心表（Agent 相关）

```sql
-- 1. 会话表
CREATE TABLE ea_conversation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
  company_id BIGINT NOT NULL DEFAULT 1,
  session_id VARCHAR(64) UNIQUE NOT NULL,
  user_id BIGINT NOT NULL,
  context_json JSON,
  summary_text TEXT,
  message_count INT DEFAULT 0,
  last_message_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  expires_at TIMESTAMP,
  trace_id VARCHAR(64),
  INDEX idx_tenant_user (tenant_id, company_id, user_id),
  INDEX idx_session (session_id)
) COMMENT='Agent 会话';

-- 2. 消息表
CREATE TABLE ea_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
  session_id VARCHAR(64) NOT NULL,
  role ENUM('user', 'assistant', 'system') NOT NULL,
  content TEXT NOT NULL,
  masked_content TEXT,
  intent VARCHAR(50),
  tokens INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  trace_id VARCHAR(64),
  INDEX idx_session (session_id),
  INDEX idx_trace (trace_id)
) COMMENT='Agent 消息';

-- 3. Agent 运行记录（复用现有 agent_run）
ALTER TABLE agent_run ADD COLUMN execution_context JSON COMMENT '执行上下文快照';

-- 4. 工具执行日志
CREATE TABLE ea_tool_execution_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
  company_id BIGINT NOT NULL DEFAULT 1,
  session_id VARCHAR(64),
  run_id BIGINT,
  tool_name VARCHAR(100) NOT NULL,
  tool_version VARCHAR(20),
  input_params JSON,
  output_result JSON,
  status ENUM('success', 'failure', 'timeout') NOT NULL,
  error_message TEXT,
  execution_time_ms INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_by BIGINT,
  trace_id VARCHAR(64),
  INDEX idx_tenant_company (tenant_id, company_id),
  INDEX idx_tool_time (tool_name, created_at),
  INDEX idx_trace (trace_id)
) COMMENT='工具执行日志';

-- 5. LLM 调用日志
CREATE TABLE ea_llm_api_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
  session_id VARCHAR(64),
  provider VARCHAR(50) NOT NULL,
  model VARCHAR(50),
  prompt_tokens INT,
  completion_tokens INT,
  total_cost DECIMAL(10,6),
  latency_ms INT,
  status ENUM('success', 'failure', 'timeout') NOT NULL,
  error_message TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  trace_id VARCHAR(64),
  INDEX idx_tenant_session (tenant_id, session_id),
  INDEX idx_provider_time (provider, created_at),
  INDEX idx_trace (trace_id)
) COMMENT='LLM API 调用日志';

-- 6. 脱敏映射表（24小时自动清理）
CREATE TABLE ea_desensitization_mapping (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
  company_id BIGINT NOT NULL DEFAULT 1,
  user_id BIGINT,
  session_id VARCHAR(64) NOT NULL,
  trace_id VARCHAR(64),
  mapping_id VARCHAR(64) UNIQUE NOT NULL,
  workflow_run_id BIGINT,
  mappings JSON NOT NULL,  -- {"ORG_001": "远智科技有限公司", "AMOUNT_001": "12000"}
  entity_refs JSON,        -- [{"token":"ORG_001","entityRef":"SUPPLIER:1024"}]
  evidence_refs JSON,      -- ["PAYABLE:8891", "EXPENSE:DOC001"]
  scope_hash VARCHAR(128), -- 当前权限/数据范围摘要，防止跨范围还原
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP NOT NULL,
  INDEX idx_session (session_id),
  INDEX idx_trace (trace_id),
  INDEX idx_expires (expires_at),
  INDEX idx_mapping_id (mapping_id)
) COMMENT='脱敏映射表';

-- 定时清理任务
CREATE EVENT clean_expired_mappings
ON SCHEDULE EVERY 1 HOUR
DO
  DELETE FROM ea_desensitization_mapping WHERE expires_at < NOW();
```

### 5.2 租户迁移脚本

```sql
-- 阶段 1: 初代上线（单租户）
-- 所有数据 tenant_id = 'default', company_id = 1

-- 阶段 2: 企业版上线（多租户准备）
-- 2.1 创建租户表
CREATE TABLE ea_tenant (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_code VARCHAR(50) UNIQUE NOT NULL,
  tenant_name VARCHAR(200) NOT NULL,
  status VARCHAR(20) DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) COMMENT='租户表';

CREATE TABLE ea_company (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_code VARCHAR(50) NOT NULL,
  company_code VARCHAR(50) NOT NULL,
  company_name VARCHAR(200) NOT NULL,
  UNIQUE KEY uk_company (tenant_code, company_code),
  FOREIGN KEY (tenant_code) REFERENCES ea_tenant(tenant_code)
) COMMENT='公司表';

-- 2.2 数据迁移（如需要拆分租户）
-- 示例：将不同公司的数据拆分到不同租户
INSERT INTO ea_tenant (tenant_code, tenant_name, status)
SELECT DISTINCT 
  CONCAT('tenant_', company_id) AS tenant_code,
  company_name AS tenant_name,
  'ACTIVE' AS status
FROM sys_company;

UPDATE ea_conversation c
JOIN sys_user u ON c.user_id = u.id
JOIN ea_tenant t ON CONCAT('tenant_', u.company_id) = t.tenant_code
SET c.tenant_id = t.tenant_code
WHERE c.tenant_id = 'default';

-- 2.3 添加外键约束（可选，视性能影响决定）
-- ALTER TABLE ea_conversation 
-- ADD CONSTRAINT fk_tenant 
-- FOREIGN KEY (tenant_id) REFERENCES ea_tenant(tenant_code);
```

---

## 六、实施路线图

### 6.1 阶段 0：最小可用内核（3周）

**目标**：跑通端到端链路，验证架构可行性

#### Week 1: 基础设施

**Day 1-2: 模型接入**

- [x] 注册 DeepSeek API Key
- [ ] 注册 Kimi API Key，暂时先使用deepseek做测试，后续再考虑自动切换的问题
- [x] 实现 `ChatModelProvider` 接口
- [x] 实现 `DeepSeekProvider` 与 `LocalModelProvider`
- [ ] 实现 `KimiProvider`
- [x] 实现模型注册与能力声明（`ModelRoutingService` + Provider Capability）
- [ ] 单元测试：Provider 调用与云模型故障切换

**Day 3-4: 数据库设计**

- [x] 创建所有 `ea_*` 表
- [ ] 实现 `TenantAwareEntity` 基类
- [ ] 配置 MyBatis-Plus 租户拦截器
- [ ] 编写数据库迁移脚本
- [ ] 测试租户自动填充

**Day 5-7: 核心服务**

- [ ] 实现 `ConversationService`
- [ ] 实现 `MessageService`
- [ ] 实现 `ContextAssemblyService` (四层上下文)
- [ ] 实现 `RuleBasedDesensitizationService` (正则规则脱敏 + 映射表 + 后验检测)
- [ ] 实现 `ToolRegistry`
- [ ] 实现 `AgentExecutionContext` + `TaskDecorator`
- [ ] 单元测试覆盖率 >70%

#### Week 2: 业务功能

**Day 8-10: 第一个真实工具**

- [ ] 实现 `ExpenseQueryTool`
- [ ] 实现 `SemanticMappingService`
- [ ] 实现 `SafeSqlBuilder`
- [ ] 权限校验：不能查询他人数据
- [ ] 集成测试：端到端查询

**Day 11-12: 前端集成**

- [x] FinWork 聊天界面 (Vue 3 + TypeScript)
- [x] FinWork 模型选择功能（云端模型 + 本地模型）
- [ ] SSE 流式响应
- [x] 消息列表渲染
- [x] 快捷操作按钮
- [x] 错误提示组件

**Day 13-14: 端到端测试**

- [ ] 执行 30 个边界用例测试
- [ ] 性能压测 (10 QPS, 10分钟)
- [ ] 故障演练 (关闭 DeepSeek，验证降级)
- [ ] 越权测试
- [ ] Bug 修复

#### Week 3: 优化与上线

**Day 15-17: 优化**

- [ ] 性能优化 (SQL 索引、查询优化)
- [ ] Token 消耗优化
- [ ] 错误处理优化
- [ ] 日志优化
- [ ] 监控埋点

**Day 18-19: 文档**

- [ ] API 接口文档
- [ ] 数据库设计文档
- [ ] 部署运维文档
- [ ] 用户使用手册

**Day 20-21: 试运行**

- [ ] 邀请 5 位内部用户试用
- [ ] 收集反馈
- [ ] 修复紧急问题
- [ ] 准备正式发布

#### 验收标准

**功能验收**：

- [x] 用户可以通过聊天查询自己的报销单
- [x] 模型故障时可以切换备用模型
- [x] 不能通过聊天越权查询其他用户数据
- [x] 每次工具调用都有可追溯的运行记录

**技术验收**：

- [x] 平均首字节响应时间: <2秒 (P95)
- [x] 端到端响应时间: <5秒 (P95)
- [x] 并发支持: >=10 QPS
- [x] DeepSeek 失败切换到 Kimi: <500ms
- [x] 模型调用成功率: >99% (含重试)
- [x] 权限校验拦截率: 100%
- [x] 运行日志完整率: 100%

**测试验收**：

- [x] 30 个边界用例 100% 通过
- [x] 正常查询类 (10个): 100% 正确结果
- [x] 越权测试类 (5个): 100% 正确拦截
- [x] 模糊意图类 (5个): 80% 正确澄清
- [x] 知识咨询类 (5个): 80% 命中知识库
- [x] 异常容错类 (5个): 100% 不崩溃

---

### 6.2 阶段 1：企业知识库问答（1-2个月）

**目标**：让 AI 能够引用企业政策，准确理解业务对象

#### 主要任务

1. **知识库管理**
   
   - [ ] 文档上传页面
   - [ ] 文档解析 (PDF/Word → 文本)
   - [ ] 文档版本管理
   - [ ] 有效期与权限管理

2. **向量化与检索**
   
   - [ ] 语义分块 (512 tokens, 重叠 50)
   - [ ] 向量嵌入 (BGE-large-zh)
   - [ ] 本地向量索引 (FAISS)
   - [ ] 混合检索 (关键词 + 向量)

3. **RAG 集成**
   
   - [ ] 查询改写
   - [ ] Top-K 检索
   - [ ] 上下文注入
   - [ ] 回答引用来源

4. **反馈机制**
   
   - [ ] 知识命中率统计
   - [ ] 用户反馈 (有帮助/无帮助)
   - [ ] 知识质量评估

#### 验收标准

- [x] 知识库能够上传并解析主流格式文档
- [x] 向量检索召回准确率 >80%
- [x] 回答能够引用文档、章节和版本
- [x] 知识库更新后立即生效

---

### 6.3 阶段 2：业务本体完善（1个月）

**目标**：让 AI 准确理解业务对象和操作

#### 主要任务

1. **本体自动生成**
   
   - [ ] 从数据库 Schema 自动生成实体定义
   - [ ] 从数据库 Schema 自动生成字段语义
   - [ ] 从枚举表自动生成枚举值语义

2. **别名管理**
   
   - [ ] 业务人员添加实体别名
   - [ ] 业务人员添加字段别名
   - [ ] 业务人员添加枚举值别名

3. **关系定义**
   
   - [ ] 定义核心实体关系 (报销单-发票-凭证)
   - [ ] 声明式 JOIN 配置
   - [ ] 关系查询测试

4. **操作映射**
   
   - [ ] 定义核心操作 (提交、审批、驳回)
   - [ ] 映射到 API 端点
   - [ ] 权限校验

#### 验收标准

- [x] 自然语言能够正确映射到业务对象
- [x] 别名识别准确率 >90%
- [x] 关系查询能够正确 JOIN
- [x] 操作能够正确调用 API

---

### 6.4 阶段 3：数字员工（1-2个月）

**目标**：AI 从助手升级为执行者

#### 主要任务

1. **数字员工管理**
   
   - [ ] 创建/配置/监控界面
   - [ ] 责任人绑定
   - [ ] 权限配置

2. **审核型数字员工**
   
   - [ ] 发票真伪验证
   - [ ] 金额计算复核
   - [ ] 合规性检查

3. **催办型数字员工**
   
   - [ ] 定时扫描超时任务
   - [ ] 自动发送催办消息
   - [ ] 升级机制

4. **审计与追溯**
   
   - [ ] 操作日志记录
   - [ ] 责任追溯界面
   - [ ] 申诉机制

#### 验收标准

- [x] 数字员工可自动处理 <1000元 报销单初审
- [x] 误审率 <5%
- [x] 人工干预率 <20%
- [x] 所有操作可审计追溯

---

### 6.5 阶段 4：企业版加固（2-3个月）

**目标**：具备企业级部署能力

#### 主要任务

1. **多租户迁移**
   
   - [ ] 租户表创建
   - [ ] 数据迁移脚本
   - [ ] 多租户测试

2. **基础设施升级**
   
   - [ ] Milvus 集群部署
   - [ ] Redis 集群部署
   - [ ] Kafka 部署
   - [ ] 监控系统 (Prometheus + Grafana)

3. **模型替换**
   
   - [ ] 私有化模型接入
   - [ ] 企业级小模型替换
   - [ ] 模型性能测试

4. **安全加固**
   
   - [ ] WAF 部署
   - [ ] 密钥管理 (Vault)
   - [ ] 审计日志完善
   - [ ] 红队测试

5. **性能优化**
   
   - [ ] 性能压测 (1000+ QPS)
   - [ ] 缓存优化
   - [ ] 数据库优化
   - [ ] 灾备演练

#### 验收标准

- [x] 多租户数据完全隔离
- [x] 并发支持 >1000 QPS
- [x] 可用性 >99.9%
- [x] 通过安全审查

---

## 七、成功指标

### 7.1 安全目标

**目标**：通过测试将已知风险控制在可接受范围内

**验收标准**：

- SQL 注入测试：30 个注入用例 100% 拦截
- 越权测试：10 个越权场景 100% 拒绝
- 脱敏测试：敏感数据泄露检测 100% 告警

**残留风险**：复杂场景（多表 JOIN、子查询）需在企业版上线前专项测试

### 7.2 性能目标

**初代版本**：

- 平均首字节响应时间: <2秒 (P95)
- 端到端响应时间: <5秒 (P95)
- 并发支持: >=10 QPS
- 单次对话成本: <0.05元

**企业版本**：

- 平均首字节响应时间: <1秒 (P95)
- 端到端响应时间: <3秒 (P95)
- 并发支持: >=1000 QPS
- 单次对话成本: <0.03元

### 7.3 用户体验指标

- FinWork 使用率：月活用户 >60%
- 对话满意度：评分 >4.0/5.0
- 知识检索命中率：>80%
- 意图识别准确率：>90%

### 7.4 效率提升指标

- 报销单处理时效：缩短 30%
- 政策咨询响应时间：从 2小时 → 5分钟
- 重复性任务自动化率：>50%

---

## 八、风险与应对

### 8.1 技术风险

| 风险          | 影响  | 应对措施              |
| ----------- | --- | ----------------- |
| 大模型幻觉       | 高   | 强制引用来源、人工审核高风险决策  |
| 脱敏算法失效      | 高   | 多层检测、定期红队测试、异常告警  |
| 脱敏后实体错配     | 高   | 本地实体解析、entityRef、evidenceRef、未知占位符拦截 |
| 工作台脚本失控     | 高   | 禁止任意脚本，改用白名单工具、查询模板和 SafeSqlBuilder |
| 工作流误选         | 中   | 工作流选择评分、阈值控制、低置信度澄清、命中审计 |
| 模型推理延迟      | 中   | 缓存常见问题、流式输出、降级策略  |
| 向量检索召回率低    | 中   | 多路召回（向量+关键词）、定期优化 |
| 第三方 API 不稳定 | 中   | 多模型容灾、本地缓存、降级方案   |

### 8.2 合规风险

| 风险        | 应对措施                     |
| --------- | ------------------------ |
| 数据泄露      | 数据不出域架构、脱敏映射表24小时清理、定期审计 |
| AI 决策责任不清 | 数字员工强制绑定责任人、操作全程可审计、申诉机制 |
| 知识产权侵权    | 知识库仅使用企业自有或授权文档          |

### 8.3 业务风险

| 风险      | 应对措施             |
| ------- | ---------------- |
| 用户接受度低  | 分阶段推出、充分培训、快速迭代  |
| 与现有流程冲突 | 数字员工作为辅助、灰度上线    |
| 投资回报周期长 | 优先实现高频任务自动化、量化指标 |

---

## 九、企业版上线清单

### 9.1 模型替换

- [x] 评估企业级模型需求（私有化/云端）
- [x] 接入企业级模型 Provider
- [ ] 性能测试与对比
- [x] 切换配置并验证

本轮完成记录（2026-09-16）：

- FinWork 前端已支持模型 Provider / 模型名称选择，包含本地 Ollama 模型入口。
- FinWork 后端已提供 `/auth/agent/chat/meta` 元数据接口，并在发送消息时支持 `providerCode` 与 `model` 透传。
- 模型路由支持用户指定 Provider 优先调用，失败后继续按路由规则降级，避免单一模型不可用导致聊天链路中断。
- 本轮未完成模型性能测试与对比，9.1 中“性能测试与对比”保持未勾选。

### 9.2 基础设施

- [ ] Milvus 集群部署与迁移
- [ ] Redis 集群部署
- [ ] Kafka 部署
- [ ] 监控系统（Prometheus、Grafana、链路追踪）

### 9.3 数据迁移

- [ ] 多租户数据迁移脚本
- [ ] 数据完整性验证
- [ ] 回滚方案准备
- [ ] 灰度迁移

### 9.4 安全加固

- [ ] WAF 部署
- [ ] 密钥管理（Vault/KMS）
- [ ] 审计日志完善
- [ ] 红队渗透测试
- [ ] 安全合规认证

### 9.5 性能优化

- [ ] 性能压测（1000+ QPS）
- [ ] 缓存策略优化
- [ ] 数据库索引优化
- [ ] CDN 接入
- [ ] 灾备演练

### 9.6 运维准备

- [ ] 运维手册编写
- [ ] 故障演练
- [ ] 值班机制建立
- [ ] 应急预案准备

### 9.7 受控查询工作流闭环

- [x] Workflow Selector：FinWork 报销查询意图可匹配用户或全局已发布工作流
- [x] 受控查询工具：实现 `expense.query_my_expenses` 白名单查询工具，只查询当前用户自己的报销单
- [x] 查询模板变量映射：支持从用户输入抽取关键词、状态、日期范围、金额范围和 limit
- [x] Tool Runtime：实现工具注册表与受控执行入口，禁止未注册工具执行
- [x] 本地脱敏与映射：查询结果进入云端模型前先通过本地正则脱敏，生成 `mappingId`
- [x] 云端模型分析：脱敏 JSON 交给模型分析，并要求输出 `answer` 与 `evidenceRefs`
- [x] 本地还原输出：模型结果按 `mappingId` 还原，并保留 `evidenceRef`
- [x] 运行审计：工作流运行写入 `ea_agent_run`，各步骤写入 `ea_agent_run_step`
- [x] 工作台配置增强：工具节点支持保存查询模板编码和变量映射
- [x] 幂等初始化脚本：新增 `backend/sql/init_agent_controlled_query_workflow.sql`
- [ ] 多工具通用查询模板管理页面
- [ ] 复杂实体消歧与用户澄清
- [ ] 工作流 DAG 通用执行器
- [ ] 端到端数据库环境回归测试

本轮完成记录（2026-09-16）：

- 新增 `AgentWorkflowRuntimeService`，串联 Workflow Selector、参数抽取、Tool Runtime、本地脱敏、模型分析、本地还原和运行审计。
- 新增 `ExpenseDocumentControlledQueryTool`，作为“受控工具 + 查询模板”的首个样板工具。
- FinWork 查询报销单时不再直接硬编码调用旧查询方法，而是进入受控工作流运行链路。
- Agent 工作台工具节点可配置 `queryTemplateCode` 和 `variableMapping`，用于表达查询模板与变量映射。
- 新增幂等 SQL 脚本 `backend/sql/init_agent_controlled_query_workflow.sql`，用于初始化全局默认报销单受控查询工作流。
- 本轮通过后端编译与前端既有单测；未完成数据库实库执行、复杂实体消歧、通用 DAG 引擎和企业级压测。

---

## 十、开发规范

### 10.1 代码检查点

在代码审查时，必须检查以下内容：

**租户上下文处理**：

- ✅ 所有 Agent 表继承 `TenantAwareEntity`
- ✅ 租户缺失时符合 Fail Closed 原则
- ✅ 异步任务传播完整上下文
- ❌ 禁止硬编码 `tenant_id = 'default'`

**SQL 安全**：

- ✅ 使用 MyBatis-Plus Wrapper 参数绑定
- ✅ 表名和字段名必须在白名单中
- ✅ 操作符必须在白名单中
- ❌ 禁止字符串拼接 SQL
- ❌ 禁止保存可执行 SQL 片段

**模型路由**：

- ✅ 使用配置化路由
- ✅ 模型顺序由配置决定
- ❌ 禁止硬编码模型顺序
- ❌ 禁止跳过路由策略直接调用模型

**降级策略**：

- ✅ 检查降级级别允许的能力
- ✅ 高风险操作禁止在降级状态执行
- ❌ 禁止降级后绕过权限校验

### 10.2 命名规范

**表名**：

- Agent 相关表：`ea_*`（Enterprise Agent）
- 示例：`ea_conversation`, `ea_message`, `ea_tool_execution_log`

**字段名**：

- 租户字段：`tenant_id`, `company_id`
- 审计字段：`created_by`, `updated_by`, `created_at`, `updated_at`
- 追踪字段：`trace_id`
- 软删除：`deleted`, `deleted_at`, `deleted_by`

**Java 类名**：

- Provider：`*Provider`（如 `DeepSeekProvider`）
- Service：`*Service`（如 `ConversationService`）
- Repository：`*Repository`（如 `ConversationRepository`）
- Entity：与表名对应（如 `AgentConversation`）

### 10.3 日志规范

**日志级别**：

- `ERROR`: 系统错误、异常、安全事件
- `WARN`: 降级、回落、配置缺失
- `INFO`: 关键业务事件（会话创建、工具调用）
- `DEBUG`: 详细调试信息

**关键日志**：

```java
// 租户上下文缺失
log.warn("租户上下文缺失，回落到默认租户: {}", defaultTenantId);

// 模型切换
log.warn("Provider {} 失败，切换到 {}", fromProvider, toProvider);

// 数据泄露检测
log.error("检测到数据泄露，拦截返回，mappingId: {}", mappingId);

// 降级
log.warn("降级至 {}", degradationLevel);
```

### 10.4 测试规范

**单元测试**：

- 覆盖率目标：>70%
- 必测场景：核心业务逻辑、权限校验、异常处理

**集成测试**：

- 端到端测试：用户输入 → 模型调用 → 工具执行 → 结果返回
- 权限测试：越权场景必须被拒绝
- 降级测试：模型失败后的降级行为

**边界测试**：

- 30 个测试用例（正常、越权、模糊、知识、异常）
- 100% 通过率

---

## 十一、附录

### 11.1 环境变量

```bash
# DeepSeek
DEEPSEEK_API_KEY=sk-xxx

# Kimi
KIMI_API_KEY=sk-xxx

# 数据库
FINEX_DB_URL=jdbc:mysql://localhost:3306/finex
FINEX_DB_USERNAME=finex
FINEX_DB_PASSWORD=your-password

# JWT
FINEX_JWT_SECRET=your-secret-at-least-32-chars

# 安全模式
FINEX_AGENT_SECURITY_MODE=demo  # demo | enterprise
```

### 11.2 依赖版本

```xml
<!-- Spring Boot -->
<spring-boot.version>3.2.0</spring-boot.version>

<!-- MyBatis-Plus -->
<mybatis-plus.version>3.5.5</mybatis-plus.version>

<!-- LLM SDK -->
<langchain4j.version>0.28.0</langchain4j.version>

<!-- 向量检索 -->
<faiss.version>1.7.4</faiss.version>

<!-- NLP -->
<hanlp.version>1.8.4</hanlp.version>
```

### 11.3 端口分配

| 服务           | 端口   | 说明                     |
| ------------ | ---- | ---------------------- |
| auth-service | 8081 | Agent Runtime + 业务 API |
| gateway      | 8080 | 统一网关                   |
| admin-web    | 5173 | 管理前端                   |

### 11.4 术语表

- **RAG**: Retrieval-Augmented Generation，检索增强生成
- **OWL**: Web Ontology Language，网络本体语言
- **MCP**: Model Context Protocol，模型上下文协议
- **SSE**: Server-Sent Events，服务器推送事件
- **Chunk**: 知识库文档分块
- **Provider**: 模型提供商抽象
- **Tenant**: 租户
- **Degradation**: 降级

---

**文档状态**: v3.0 定稿版  
**适用范围**: FinEx AI Agent 平台初代版本及企业版演进  
**维护责任**: 开发团队  
**更新频率**: 每阶段完成后更新实施进展
