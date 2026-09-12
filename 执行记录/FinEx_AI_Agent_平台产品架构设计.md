# FinEx AI Agent 平台产品架构设计

**文档版本**: v1.0  
**创建时间**: 2026-09-12  
**设计目标**: 对标用友 YonWork，构建企业级可控 AI Agent 协同平台  
**评审团队**: 三人协作小组（待定期讨论与迭代）

---

## 一、战略定位与产品愿景

### 1.1 产品定位

FinEx AI Agent 平台定位为**企业级可控智能协同中枢**，通过"数据不出域、智能可审计、能力可复用"的设计理念，将大模型能力安全、合规地融入企业财务与协同场景。

**核心差异化价值**：
- **数据主权可控**：敏感数据通过本地小模型脱敏，大模型仅处理脱敏后数据
- **业务深度融合**：基于 FinEx 现有业务本体，构建财务领域专有 Agent 能力
- **渐进式智能**：从个人数字分身到数字员工，从知识检索到流程自动化，分阶段演进
- **开放生态**：支持 MCP 协议，可接入外部数据源与工具链

### 1.2 对标分析：YonWork

**YonWork 核心能力**（2026年8月发布）：
- 企业级大模型工作台
- 智能助理与任务自动化
- 知识库问答与流程辅助
- 多模态交互（文档、图表、语音）

**FinEx AI Agent 平台的差异化优势**：
1. **数据安全架构更严格**：双层模型架构（本地脱敏+云端推理）
2. **财务领域深度更深**：内置财务本体、会计准则、税务政策知识图谱
3. **责任追溯更清晰**：数字员工与真实用户绑定，AI 决策全程可审计
4. **协议扩展性更强**：原生支持 MCP 协议，可接入 Slack、Notion、GitHub 等外部工具

---

## 二、总体架构设计

### 2.1 系统架构全景图

```
┌─────────────────────────────────────────────────────────────────┐
│                        FinEx AI Agent 平台                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────  前端交互层  ──────────────────┐           │
│  │                                                    │           │
│  │  Agent 导航页                                     │           │
│  │  ├─ FinWork (聊天工作台)                         │           │
│  │  ├─ 个人数字分身 (技能蒸馏)                      │           │
│  │  ├─ 数字员工 (流程嵌入)                          │           │
│  │  ├─ 业务本体 (语义解释)                          │           │
│  │  └─ 企业知识库向量 (RAG 检索)                    │           │
│  │                                                    │           │
│  └────────────────────────────────────────────────────┘           │
│                          ↕                                        │
│  ┌──────────────────  应用编排层  ──────────────────┐           │
│  │                                                    │           │
│  │  Agent 编排引擎 (Workflow Orchestration)          │           │
│  │  ├─ 意图识别与任务分解                            │           │
│  │  ├─ 多 Agent 协作调度                             │           │
│  │  ├─ 上下文管理 (对话历史、业务上下文)            │           │
│  │  └─ 结果聚合与追溯                                │           │
│  │                                                    │           │
│  │  MCP 协议网关 (Model Context Protocol)            │           │
│  │  ├─ 外部工具接入 (Slack, Notion, GitHub...)      │           │
│  │  ├─ 数据源连接器 (ERP, CRM, OA...)               │           │
│  │  └─ 标准化消息适配                                │           │
│  │                                                    │           │
│  └────────────────────────────────────────────────────┘           │
│                          ↕                                        │
│  ┌──────────────────  智能推理层  ──────────────────┐           │
│  │                                                    │           │
│  │  ┌──────────────────────────────────────┐        │           │
│  │  │  本地小模型集群 (边缘侧)            │        │           │
│  │  │  ├─ 数据脱敏模块 (PII Masking)      │        │           │
│  │  │  ├─ 数据恢复模块 (Data Restoration) │        │           │
│  │  │  ├─ 意图分类器 (Intent Classifier)   │        │           │
│  │  │  └─ 实体识别器 (NER)                 │        │           │
│  │  └──────────────────────────────────────┘        │           │
│  │                     ↕                              │           │
│  │  ┌──────────────────────────────────────┐        │           │
│  │  │  大模型 API 网关 (云端)              │        │           │
│  │  │  ├─ 通义千问 (Qwen)                  │        │           │
│  │  │  ├─ 文心一言 (ERNIE)                 │        │           │
│  │  │  ├─ 智谱 GLM (ChatGLM)               │        │           │
│  │  │  ├─ 豆包 (Doubao)                     │        │           │
│  │  │  └─ 讯飞星火 (SparkDesk)             │        │           │
│  │  └──────────────────────────────────────┘        │           │
│  │                                                    │           │
│  └────────────────────────────────────────────────────┘           │
│                          ↕                                        │
│  ┌──────────────────  数据与知识层  ─────────────────┐          │
│  │                                                    │           │
│  │  业务本体库 (Ontology)                            │           │
│  │  ├─ 业务对象模型 (Expense, Voucher...)           │           │
│  │  ├─ 字段语义定义                                  │           │
│  │  ├─ 状态机与关系图谱                              │           │
│  │  └─ 操作权限矩阵                                  │           │
│  │                                                    │           │
│  │  企业知识库向量存储 (Vector Store)                │           │
│  │  ├─ 财务政策文档                                  │           │
│  │  ├─ 会计准则与税法                                │           │
│  │  ├─ 流程 SOP                                      │           │
│  │  ├─ 合同模板                                      │           │
│  │  └─ 历史案例库                                    │           │
│  │                                                    │           │
│  │  用户画像与技能库                                 │           │
│  │  ├─ 个人数字分身 (行为蒸馏)                      │           │
│  │  ├─ 工作习惯模式                                  │           │
│  │  └─ 技能图谱                                      │           │
│  │                                                    │           │
│  │  数字员工档案库                                   │           │
│  │  ├─ Agent 配置与权限                             │           │
│  │  ├─ 责任人绑定                                    │           │
│  │  └─ 执行日志与审计                                │           │
│  │                                                    │           │
│  └────────────────────────────────────────────────────┘           │
│                          ↕                                        │
│  ┌──────────────────  基础设施层  ──────────────────┐           │
│  │                                                    │           │
│  │  ├─ FinEx 业务数据 (MySQL)                       │           │
│  │  ├─ 向量数据库 (Milvus / Weaviate)               │           │
│  │  ├─ 图数据库 (Neo4j - 本体与关系)                │           │
│  │  ├─ 缓存层 (Redis - 对话上下文)                  │           │
│  │  └─ 消息队列 (Kafka - 异步任务)                  │           │
│  │                                                    │           │
│  └────────────────────────────────────────────────────┘           │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 数据流与安全架构

#### 核心数据流（三段式安全架构）

```
用户输入
  ↓
┌────────────────────────────────────────────┐
│ 阶段 1: 本地脱敏 (Local Desensitization)  │
├────────────────────────────────────────────┤
│ 输入: "帮我查看张三的报销单，金额12000元"│
│                                            │
│ 本地小模型处理:                            │
│ 1. 实体识别 (NER)                          │
│    - 人名: 张三 → [PERSON_001]            │
│    - 金额: 12000 → [AMOUNT_001]           │
│                                            │
│ 2. 敏感字段映射                            │
│    - 报销单号: DOC202301010001            │
│      → [DOC_HASH_A3F2]                    │
│                                            │
│ 3. 生成脱敏映射表 (存储在本地)            │
│    {                                       │
│      "PERSON_001": "张三",                │
│      "AMOUNT_001": "12000",               │
│      "DOC_HASH_A3F2": "DOC202301010001"   │
│    }                                       │
│                                            │
│ 输出: "帮我查看[PERSON_001]的报销单，     │
│       金额[AMOUNT_001]元"                 │
└────────────────────────────────────────────┘
  ↓ (仅传输脱敏后数据)
┌────────────────────────────────────────────┐
│ 阶段 2: 云端推理 (Cloud Inference)        │
├────────────────────────────────────────────┤
│ 大模型 API (通义千问/文心一言等)          │
│                                            │
│ 接收: 脱敏后的问题                        │
│ 处理: 基于业务本体与知识库理解意图        │
│ 输出: 脱敏格式的结果                      │
│                                            │
│ 示例输出:                                  │
│ "[PERSON_001] 的报销单 [DOC_HASH_A3F2]    │
│  当前状态为已审批，金额 [AMOUNT_001] 元， │
│  预计 3 个工作日内到账。"                 │
└────────────────────────────────────────────┘
  ↓ (返回脱敏结果)
┌────────────────────────────────────────────┐
│ 阶段 3: 本地还原 (Local Restoration)      │
├────────────────────────────────────────────┤
│ 本地小模型处理:                            │
│ 1. 读取脱敏映射表                          │
│ 2. 替换占位符                              │
│    [PERSON_001] → 张三                    │
│    [AMOUNT_001] → 12000                   │
│    [DOC_HASH_A3F2] → DOC202301010001      │
│                                            │
│ 最终输出:                                  │
│ "张三的报销单 DOC202301010001 当前状态为  │
│  已审批，金额 12000 元，预计 3 个工作日内 │
│  到账。"                                   │
│                                            │
│ 清理: 删除本次会话的脱敏映射表            │
└────────────────────────────────────────────┘
  ↓
返回给用户
```

#### 安全保障机制

1. **数据隔离**：
   - 原始业务数据永不离开本地环境
   - 大模型仅接触符号化、抽象化的数据
   - 脱敏映射表存储在会话级内存/Redis，生命周期≤24小时

2. **传输加密**：
   - 本地 ↔ 云端：TLS 1.3 加密
   - API 密钥轮换机制（30天）
   - 请求签名与时间戳防重放

3. **审计追溯**：
   - 每次大模型调用记录：用户ID、时间戳、脱敏前后数据摘要
   - 数字员工操作日志：责任人绑定、操作类型、影响范围
   - 异常检测：敏感信息泄露告警（正则匹配+NLP检测）

---

## 三、五大核心模块详细设计

### 3.1 FinWork - 交互式聊天工作台

#### 功能定位
FinWork 是用户与 AI Agent 平台交互的主入口，提供**多模态、上下文感知、任务导向**的对话式工作界面。

#### 核心功能

##### 3.1.1 智能对话引擎
- **多轮对话管理**：
  - 上下文窗口：保留最近 10 轮对话
  - 业务上下文注入：自动关联当前用户的待办、草稿、最近访问的单据
  - 意图切换检测：识别话题转换，自动清理无关上下文

- **多模态交互**：
  - 文本输入/输出（主要）
  - 语音转文本（集成科大讯飞 ASR）
  - 图片识别（OCR 发票、合同）
  - 表格生成（Markdown → Excel 导出）

##### 3.1.2 任务型对话能力
支持财务领域常见任务，示例：

| 任务类型 | 用户输入示例 | Agent 处理流程 |
|---------|------------|--------------|
| **单据查询** | "我上个月的报销单都审批了吗？" | 1. 提取时间范围（上个月）<br>2. 查询数据库（当前用户的报销单）<br>3. 筛选状态（已审批/未审批）<br>4. 自然语言返回结果 |
| **流程推进** | "帮我催一下财务审批我的差旅报销" | 1. 识别单据类型（差旅报销）<br>2. 查找当前流程节点<br>3. 通过数字员工发送催办消息<br>4. 返回催办结果 |
| **政策咨询** | "住宿费超过500元需要什么审批？" | 1. 向量检索企业政策库<br>2. 召回相关制度条款<br>3. 结合业务本体解释审批规则<br>4. 给出操作建议 |
| **智能填单** | "我要报销昨天在北京的打车费80元" | 1. 提取关键信息（日期、地点、类型、金额）<br>2. 调用报销单创建 API<br>3. 自动填充表单字段<br>4. 提示用户上传发票 |
| **数据分析** | "分析我部门Q1的费用构成" | 1. 查询部门费用数据<br>2. 调用数据分析模块<br>3. 生成图表（饼图、趋势图）<br>4. 自然语言解读关键发现 |

##### 3.1.3 UI/UX 设计要点
- **沉浸式对话界面**：
  - 左侧：会话历史列表（可分组：今天/本周/本月）
  - 中间：对话主窗口（气泡式消息流）
  - 右侧：上下文面板（显示当前关联的业务对象）

- **快捷操作**：
  - Slash 命令：`/新建报销单`、`/查询待办`、`/分析费用`
  - 建议卡片：根据用户角色推荐常用任务
  - 快速引用：@提及业务对象（如 `@报销单DOC001`）

- **响应式反馈**：
  - 流式输出（Streaming Response）：边推理边显示
  - 进度提示：数据库查询、API 调用显示加载状态
  - 错误友好：无法理解时提供澄清选项

##### 3.1.4 技术实现
- **前端**：Vue 3 + TypeScript
  - 组件库：Element Plus（保持与现有前端一致）
  - 状态管理：Pinia（对话历史、上下文）
  - 实时通信：WebSocket（流式响应）

- **后端**：
  - 新增服务：`backend/agent-service`（Spring Boot）
  - 核心模块：
    - `DialogueController`：WebSocket 接口
    - `IntentRecognitionService`：意图识别
    - `ContextManager`：上下文管理
    - `TaskExecutor`：任务执行编排

---

### 3.2 个人数字分身 - 技能与习惯蒸馏

#### 功能定位
通过持续学习用户的操作行为、决策偏好、工作模式，构建**用户的数字镜像**，实现工作习惯复制与辅助决策。

#### 核心能力

##### 3.2.1 行为采集与蒸馏
- **数据采集维度**：
  1. **操作行为**：
     - 页面访问路径（高频访问的功能模块）
     - 表单填写模式（字段填写顺序、常用值）
     - 审批决策（通过/驳回的判断依据）
  
  2. **时间模式**：
     - 工作时段分析（高峰期、低峰期）
     - 任务处理时长分布（快速审批 vs 深度审查）
     - 延迟模式（拖延症检测）

  3. **偏好学习**：
     - 报销类目偏好（差旅 > 餐饮 > 办公）
     - 审批严格度（对金额、单据类型的容忍阈值）
     - 沟通风格（正式/随意）

- **蒸馏算法**：
  - **知识蒸馏**（Knowledge Distillation）：
    - 教师模型：大模型分析用户历史行为
    - 学生模型：本地小模型学习用户特征
    - 目标：压缩用户画像到 <10MB 的个性化模型
  
  - **强化学习**（Reinforcement Learning）：
    - 奖励信号：用户对 Agent 建议的采纳率
    - 策略优化：动态调整建议权重

##### 3.2.2 应用场景
1. **智能预填**：
   - 创建报销单时，自动填充常用项目类别、部门、成本中心
   - 根据历史报销金额，推荐合理金额范围

2. **决策辅助**：
   - 审批时，显示用户历史类似单据的处理方式
   - 风险提示：金额异常、单据异常频次

3. **工作提醒**：
   - 根据用户习惯时间发送待办提醒
   - 识别拖延任务，主动催办

4. **代理操作**：
   - 用户授权后，数字分身可自动处理低风险任务（如 <500 元报销单的初审）

##### 3.2.3 隐私与控制
- **透明化**：
  - 用户可查看数字分身学习的所有行为特征
  - 可视化用户画像（技能雷达图、时间热力图）

- **可控性**：
  - 用户可删除特定行为记录
  - 可关闭特定维度的学习（如不希望学习审批偏好）

- **授权机制**：
  - 数字分身的操作权限需用户显式授权
  - 高风险操作（如审批、付款）禁止自动化

##### 3.2.4 技术实现
- **数据层**：
  - 表：`user_behavior_log`（操作日志）
  - 表：`user_digital_twin`（蒸馏后的用户特征）
  - 表：`user_preference`（显式偏好设置）

- **算法层**：
  - 行为分析引擎：Python + Scikit-learn
  - 特征工程：时序特征、统计特征、文本特征
  - 模型：LightGBM（预测用户行为）+ 小型 Transformer（文本生成）

---

### 3.3 数字员工 - 流程嵌入与责任绑定

#### 功能定位
数字员工是**嵌入到业务流程中的自动化 Agent**，具备特定岗位的专业能力，可执行重复性、规则性任务，同时与真实用户绑定以明确责任主体。

#### 核心设计

##### 3.3.1 数字员工类型体系
| 类型 | 岗位角色 | 职责范围 | 绑定用户 |
|-----|---------|---------|---------|
| **审核型** | 初审员 | - 报销单合规性检查<br>- 发票真伪验证<br>- 金额计算复核 | 财务初审主管 |
| **执行型** | 付款专员 | - 自动发起付款申请<br>- 跟踪付款状态<br>- 异常支付告警 | 出纳主管 |
| **咨询型** | 政策顾问 | - 回答财务政策问题<br>- 推荐审批流程<br>- 提供填单指导 | 财务经理 |
| **分析型** | 数据分析师 | - 生成费用分析报告<br>- 异常模式检测<br>- 预算执行监控 | 财务总监 |
| **催办型** | 流程协调员 | - 自动催办超时任务<br>- 跨部门协调<br>- 会议日程安排 | 行政主管 |

##### 3.3.2 工作流嵌入机制
数字员工可嵌入到流程的**节点**或**边**：

```
报销流程示例：
┌──────────┐
│ 提交申请 │
└────┬─────┘
     │
     ↓ [嵌入点 1: 自动初审]
┌──────────────────────────┐
│ 数字员工: 初审机器人      │
│ - 检查发票真伪            │
│ - 验证金额计算            │
│ - 匹配政策规则            │
│ 结果: 通过 → 下一节点     │
│      驳回 → 退回修改      │
│      存疑 → 转人工审核    │
└────┬─────────────────────┘
     │
     ↓
┌──────────┐
│ 财务审批 │ ← [嵌入点 2: 辅助决策]
└────┬─────┘   数字员工: 政策顾问
     │          - 显示类似历史案例
     │          - 标注风险点
     │          - 建议审批意见
     ↓
┌──────────┐
│ 付款执行 │ ← [嵌入点 3: 自动付款]
└──────────┘   数字员工: 付款专员
               - 自动发起付款指令
               - 更新单据状态
```

##### 3.3.3 责任绑定机制
- **绑定关系**：
  - 每个数字员工必须绑定一个真实用户作为**责任人**
  - 绑定关系存储在 `digital_employee` 表：
    ```sql
    CREATE TABLE digital_employee (
      id BIGINT PRIMARY KEY,
      name VARCHAR(100),            -- 数字员工名称
      type ENUM('审核型', '执行型', ...),
      responsible_user_id BIGINT,   -- 绑定的责任人
      authority_level INT,          -- 权限等级（1-5）
      max_amount DECIMAL(15,2),     -- 最大处理金额
      status ENUM('active', 'suspended'),
      created_at TIMESTAMP
    );
    ```

- **审计日志**：
  - 表：`digital_employee_action_log`
  - 记录内容：
    - 数字员工 ID
    - 责任人 ID
    - 操作类型（审批/驳回/付款/催办）
    - 业务对象（报销单号、凭证号）
    - 操作时间
    - 决策依据（引用的规则、模型输出）

- **责任追溯**：
  - 如数字员工误审，可追溯到责任人
  - 责任人可申诉，提交人工复核
  - 系统记录申诉结果，用于优化数字员工模型

##### 3.3.4 配置与管理界面
- **数字员工管理页面**：
  - 列表视图：显示所有数字员工、状态、责任人
  - 创建向导：
    1. 选择员工类型（审核型/执行型...）
    2. 配置权限（处理金额上限、可操作流程）
    3. 绑定责任人
    4. 设置触发条件（如自动处理 <1000 元报销单）
  - 性能监控：
    - 处理量统计（日/周/月）
    - 准确率（误审率、召回率）
    - 人工干预率

- **工作流设计器增强**：
  - 在现有流程设计器中，新增"数字员工"节点类型
  - 可拖拽配置嵌入点
  - 设置触发规则（金额范围、业务类型）

##### 3.3.5 技术实现
- **后端服务**：
  - `DigitalEmployeeService`：数字员工管理
  - `WorkflowIntegrationService`：流程嵌入适配
  - `AuditService`：审计日志

- **规则引擎**：
  - Drools（规则驱动的决策引擎）
  - 规则示例：
    ```java
    rule "初审-发票金额匹配"
    when
        $expense : ExpenseReport(invoiceAmount != claimedAmount)
    then
        reject($expense, "发票金额与报销金额不符");
    end
    ```

---

### 3.4 业务本体 - 语义解释与知识图谱

#### 功能定位
业务本体是**FinEx 系统的语义大脑**，以结构化的方式定义业务对象、字段、状态、关系和操作，为 AI Agent 提供准确的业务理解能力。

#### 核心组成

##### 3.4.1 本体模型设计
采用 **OWL（Web Ontology Language）** 标准，构建三层本体：

```
┌─────────────────────────────────────────┐
│         领域本体 (Domain Ontology)       │
├─────────────────────────────────────────┤
│ - 财务会计本体 (会计科目、凭证、账簿)   │
│ - 税务本体 (税种、税率、申报规则)       │
│ - 报销本体 (费用类型、审批规则)         │
└────────────┬────────────────────────────┘
             │
             ↓
┌─────────────────────────────────────────┐
│        应用本体 (Application Ontology)   │
├─────────────────────────────────────────┤
│ - FinEx 报销系统业务对象                │
│   - 类: ExpenseReport, Invoice, Voucher │
│   - 属性: amount, status, applicant     │
│   - 关系: hasInvoice, belongsToDept     │
└────────────┬────────────────────────────┘
             │
             ↓
┌─────────────────────────────────────────┐
│         任务本体 (Task Ontology)        │
├─────────────────────────────────────────┤
│ - 用户意图分类 (查询/创建/修改/分析)   │
│ - 操作映射 (意图 → API 调用)           │
└─────────────────────────────────────────┘
```

##### 3.4.2 核心实体定义示例

**报销单实体 (ExpenseReport)**
```turtle
@prefix finex: <http://finex.com/ontology#> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .

finex:ExpenseReport rdf:type owl:Class ;
    rdfs:label "报销单"@zh, "Expense Report"@en ;
    rdfs:comment "员工提交的费用报销申请单据" ;
    finex:hasProperty finex:documentNumber ;     # 单据号
    finex:hasProperty finex:amount ;             # 报销金额
    finex:hasProperty finex:expenseType ;        # 费用类型
    finex:hasProperty finex:status ;             # 单据状态
    finex:hasProperty finex:applicant ;          # 申请人
    finex:hasRelation finex:hasInvoice ;         # 关联发票
    finex:hasRelation finex:belongsToDepartment ; # 所属部门
    finex:hasOperation finex:submit ;            # 操作: 提交
    finex:hasOperation finex:approve ;           # 操作: 审批
    finex:hasOperation finex:reject .            # 操作: 驳回

# 状态定义
finex:ExpenseReportStatus rdf:type owl:Class ;
    owl:oneOf (finex:Draft finex:Pending finex:Approved finex:Rejected finex:Paid) .

finex:Draft rdfs:label "草稿"@zh .
finex:Pending rdfs:label "待审批"@zh .
finex:Approved rdfs:label "已审批"@zh .
finex:Rejected rdfs:label "已驳回"@zh .
finex:Paid rdfs:label "已支付"@zh .

# 状态转换规则
finex:submitTransition rdf:type finex:StateTransition ;
    finex:fromState finex:Draft ;
    finex:toState finex:Pending ;
    finex:requiredCondition "所有必填字段已填写且有至少一张发票" ;
    finex:requiredPermission "employee" .

# 业务规则
finex:AmountValidationRule rdf:type finex:BusinessRule ;
    finex:appliesTo finex:ExpenseReport ;
    finex:ruleType "validation" ;
    finex:condition "amount > 0 AND amount <= 50000" ;
    finex:errorMessage "报销金额必须在0-50000元之间" .
```

##### 3.4.3 知识图谱构建
使用 **Neo4j** 存储业务实体关系：

```cypher
// 实体节点
CREATE (e:ExpenseReport {
  id: 'DOC202301010001',
  amount: 12000,
  status: 'Approved'
})

CREATE (u:User {
  id: 'U001',
  name: '张三',
  department: '研发部'
})

CREATE (inv:Invoice {
  id: 'INV20230101001',
  amount: 12000,
  type: '增值税专用发票'
})

CREATE (dept:Department {
  id: 'D001',
  name: '研发部'
})

// 关系
CREATE (e)-[:SUBMITTED_BY]->(u)
CREATE (e)-[:HAS_INVOICE]->(inv)
CREATE (u)-[:BELONGS_TO]->(dept)
CREATE (e)-[:ALLOCATED_TO]->(dept)
```

##### 3.4.4 应用场景

**场景 1: 自然语言查询 → 结构化查询**
```
用户输入: "显示我部门上个月超过5000元的已审批报销单"

Agent 处理:
1. 意图识别: 查询报销单
2. 实体抽取:
   - 部门: 当前用户所属部门 (dept = '研发部')
   - 时间: 上个月 (date BETWEEN '2026-08-01' AND '2026-08-31')
   - 金额: >5000 (amount > 5000)
   - 状态: 已审批 (status = 'Approved')
3. 本体映射:
   - 查询 ExpenseReport 实体
   - 关联 Department 关系
4. 生成 SQL:
   SELECT * FROM expense_report
   WHERE department_id = (SELECT id FROM department WHERE name = '研发部')
   AND created_at BETWEEN '2026-08-01' AND '2026-08-31'
   AND amount > 5000
   AND status = 'Approved'
```

**场景 2: 业务规则解释**
```
用户输入: "为什么我的报销单不能提交？"

Agent 处理:
1. 查询报销单当前状态 (Draft)
2. 从本体中读取 submitTransition 规则
3. 检查前置条件:
   - 必填字段: ✓ 已填写
   - 发票: ✗ 未上传
4. 自然语言解释:
   "您的报销单缺少发票附件。根据《费用报销管理办法》第3.2条，
    所有报销申请必须上传对应发票才能提交审批。"
```

**场景 3: 跨系统语义对齐**
```
外部 ERP 系统调用 FinEx API，发送字段名为 "reimbursement_form"

FinEx 本体层:
1. 检测到外部术语 "reimbursement_form"
2. 通过本体映射发现:
   reimbursement_form (外部) ≡ ExpenseReport (FinEx)
3. 自动转换字段名并处理请求
```

##### 3.4.5 技术实现
- **本体编辑器**：
  - 基于 Protégé 或自研 Web 编辑器
  - 可视化编辑类、属性、关系

- **推理引擎**：
  - Apache Jena（OWL 推理）
  - 支持 RDFS/OWL 2 推理

- **查询接口**：
  - SPARQL 端点（标准语义查询）
  - RESTful API（业务层封装）

---

### 3.5 企业知识库向量 - RAG 检索与知识管理

#### 功能定位
企业知识库向量是**财务领域专有知识的向量化存储与检索系统**，通过 RAG（Retrieval-Augmented Generation）技术，让 AI Agent 能够准确引用企业内部政策、流程、案例。

#### 核心架构

##### 3.5.1 知识分类体系
```
企业知识库
├── 制度政策类
│   ├── 财务管理制度
│   ├── 费用报销管理办法
│   ├── 差旅管理规定
│   ├── 采购管理制度
│   └── 资产管理办法
│
├── 会计准则类
│   ├── 企业会计准则（中国）
│   ├── 国际财务报告准则 (IFRS)
│   ├── 会计科目表
│   └── 记账规则手册
│
├── 税务法规类
│   ├── 增值税政策
│   ├── 企业所得税法
│   ├── 个人所得税法
│   └── 税收优惠政策汇编
│
├── 流程 SOP 类
│   ├── 报销流程操作手册
│   ├── 付款流程 SOP
│   ├── 月末结账流程
│   └── 审计配合指南
│
├── 合同模板类
│   ├── 劳动合同模板
│   ├── 采购合同模板
│   ├── 服务合同模板
│   └── 保密协议模板
│
└── 历史案例类
    ├── 常见问题 FAQ
    ├── 异常处理案例
    ├── 审计发现与整改
    └── 最佳实践汇编
```

##### 3.5.2 向量化流程
```
文档输入
  ↓
┌────────────────────────────────┐
│ 1. 文档解析                    │
│ - PDF/Word/Excel 提取文本     │
│ - 保留结构信息（标题、章节）   │
│ - OCR 处理扫描件              │
└────────────┬───────────────────┘
             ↓
┌────────────────────────────────┐
│ 2. 分块 (Chunking)             │
│ - 策略: 语义分块               │
│ - 块大小: 512 tokens          │
│ - 重叠: 50 tokens (保持上下文) │
│ - 元数据: 标题、页码、章节     │
└────────────┬───────────────────┘
             ↓
┌────────────────────────────────┐
│ 3. 向量嵌入 (Embedding)        │
│ - 模型: BGE-large-zh (中文)   │
│ - 维度: 1024                   │
│ - 批处理: 32 chunks/batch     │
└────────────┬───────────────────┘
             ↓
┌────────────────────────────────┐
│ 4. 向量存储                    │
│ - 数据库: Milvus / Weaviate   │
│ - 索引: HNSW (高效检索)        │
│ - 元数据过滤: 支持             │
└────────────────────────────────┘
```

##### 3.5.3 RAG 检索增强生成
```
用户查询: "住宿费超过500元需要什么审批？"
  ↓
┌────────────────────────────────────┐
│ 阶段 1: 查询改写 (Query Rewriting) │
├────────────────────────────────────┤
│ 原查询: "住宿费超过500元需要什么   │
│          审批？"                   │
│                                    │
│ LLM 改写 (HyDE):                   │
│ "根据《差旅管理规定》，单次住宿费  │
│  超过500元的，需要部门经理审批，   │
│  超过1000元的需要总经理审批。"     │
│                                    │
│ 提取关键词:                        │
│ ["住宿费", "500元", "审批", "差旅"]│
└────────────┬───────────────────────┘
             ↓
┌────────────────────────────────────┐
│ 阶段 2: 向量检索                   │
├────────────────────────────────────┤
│ 1. 查询向量化 (BGE-large-zh)       │
│ 2. 向量相似度搜索 (Top-K=5)        │
│ 3. 元数据过滤:                     │
│    - 文档类型: 制度政策类          │
│    - 关键词匹配: "住宿费" OR "差旅"│
│                                    │
│ 检索结果:                          │
│ [1] 《差旅管理规定》第4.2条        │
│     相似度: 0.89                   │
│     内容: "住宿费标准：一线城市    │
│            ≤800元/天，二线城市     │
│            ≤500元/天。超标需部门   │
│            经理审批。"             │
│                                    │
│ [2] 《费用报销管理办法》第3.5条    │
│     相似度: 0.82                   │
│     内容: "单笔费用>500元需上传    │
│            发票原件并附说明。"     │
│                                    │
│ [3] 历史案例 #2023-Q2-018          │
│     相似度: 0.78                   │
│     内容: "张三出差上海，住宿费    │
│            650元，已获部门经理     │
│            审批通过。"             │
└────────────┬───────────────────────┘
             ↓
┌────────────────────────────────────┐
│ 阶段 3: 上下文注入与生成           │
├────────────────────────────────────┤
│ Prompt 构建:                       │
│ """                                │
│ 你是 FinEx 财务助手，基于以下企业  │
│ 政策回答用户问题。                 │
│                                    │
│ 【相关政策】                       │
│ 1. 《差旅管理规定》第4.2条：...   │
│ 2. 《费用报销管理办法》第3.5条：...│
│                                    │
│ 【用户问题】                       │
│ 住宿费超过500元需要什么审批？      │
│                                    │
│ 请基于上述政策给出准确、具体的回答，│
│ 并引用条款来源。                   │
│ """                                │
│                                    │
│ 大模型生成:                        │
│ "根据《差旅管理规定》第4.2条，住宿 │
│  费标准为一线城市≤800元/天，二线   │
│  城市≤500元/天。如果您的住宿费超过 │
│  500元但在标准内，无需额外审批；   │
│  如果超过标准，需要提交说明并获得  │
│  部门经理审批。                    │
│                                    │
│  同时，根据《费用报销管理办法》第  │
│  3.5条，单笔费用超过500元需上传发票│
│  原件并附费用说明。"               │
│                                    │
│ 来源标注:                          │
│ - 《差旅管理规定》第4.2条          │
│ - 《费用报销管理办法》第3.5条      │
└────────────────────────────────────┘
```

##### 3.5.4 知识管理功能

**知识库管理界面**：
- **上传与解析**：
  - 批量上传 PDF/Word/Excel
  - 自动解析并预览分块结果
  - 手动编辑分块（修正分块边界）

- **知识审核**：
  - 审核员确认解析准确性
  - 标注关键段落（高权重）
  - 设置知识有效期（过期自动下线）

- **版本管理**：
  - 文档版本追踪（如《差旅规定》v1.0 → v2.0）
  - 自动标记过期版本
  - 保留历史版本用于审计

- **知识评估**：
  - 统计知识引用频次
  - 用户反馈（回答有帮助/无帮助）
  - 召回率评估（检索到的知识是否被采纳）

**知识热更新**：
- 新文档上传后，立即向量化并加入检索库
- 旧文档更新后，自动替换对应向量
- 支持增量更新（仅重新处理修改部分）

##### 3.5.5 技术实现
- **向量数据库**：
  - **Milvus**（推荐）：
    - 优势：开源、高性能、支持混合检索
    - 索引：HNSW（精度高）+ IVF（速度快）
  - **Weaviate**（备选）：
    - 优势：内置 NLP 模块、易用性好

- **嵌入模型**：
  - **BGE-large-zh**（智源研究院）：
    - 中文效果优秀，维度 1024
    - 支持本地部署（<2GB 显存）
  - **M3E**（Moka AI）：
    - 备选方案，轻量级

- **文档解析**：
  - **Unstructured**（Python 库）：支持多格式解析
  - **PyMuPDF**：PDF 提取
  - **python-docx**：Word 提取

- **分块策略**：
  - **LangChain RecursiveCharacterTextSplitter**
  - 按语义边界分块（句号、段落）

---

## 四、技术架构与实现路径

### 4.1 本地小模型技术选型

#### 4.1.1 数据脱敏模块
**选型方案**：
| 任务 | 模型 | 规模 | 推理速度 | 部署方式 |
|-----|------|------|---------|---------|
| **实体识别 (NER)** | UIE (Universal Information Extraction) | 500MB | ~10ms/句 | ONNX Runtime |
| **敏感信息检测** | StructBERT-NER | 400MB | ~8ms/句 | ONNX Runtime |
| **文本分类** | TinyBERT-6L | 60MB | ~5ms/句 | ONNX Runtime |

**脱敏策略**：
1. **命名实体脱敏**：
   - 人名 → `[PERSON_001]`
   - 组织名 → `[ORG_001]`
   - 地点 → `[LOC_001]`

2. **数值脱敏**：
   - 金额 → `[AMOUNT_001]`（保留数量级信息，如"万元级"）
   - 日期 → `[DATE_001]`（保留相对时间，如"上个月"）

3. **业务标识脱敏**：
   - 单据号 → `[DOC_HASH_xxx]`（哈希后前8位）
   - 发票号 → `[INV_HASH_xxx]`

#### 4.1.2 意图分类与槽位提取
**选型方案**：
- **模型**：DistilBERT-6L (中文)
- **训练数据**：FinEx 历史用户查询日志（脱敏后）
- **意图类别**（18类）：
  - 查询类：`query_expense`, `query_status`, `query_balance`
  - 操作类：`create_expense`, `approve`, `reject`, `cancel`
  - 分析类：`analyze_trend`, `compare_data`, `generate_report`
  - 咨询类：`ask_policy`, `ask_procedure`, `ask_field`
  - 其他：`greeting`, `thanks`, `complaint`, `unclear`

#### 4.1.3 模型部署架构
```
┌─────────────────────────────────────────┐
│  本地模型服务 (Local Model Service)     │
│  部署方式: Docker + ONNX Runtime        │
├─────────────────────────────────────────┤
│  端口: 9000 (内网不对外暴露)            │
│                                         │
│  API 端点:                              │
│  POST /desensitize                      │
│    输入: {text: "原始文本"}             │
│    输出: {                              │
│      masked_text: "脱敏文本",          │
│      mapping_id: "sess_xxx"  # 映射表ID│
│    }                                    │
│                                         │
│  POST /restore                          │
│    输入: {                              │
│      masked_text: "脱敏文本",          │
│      mapping_id: "sess_xxx"            │
│    }                                    │
│    输出: {restored_text: "还原文本"}   │
│                                         │
│  POST /classify                         │
│    输入: {text: "用户输入"}             │
│    输出: {                              │
│      intent: "query_expense",          │
│      confidence: 0.95,                 │
│      slots: {time: "上个月", ...}      │
│    }                                    │
└─────────────────────────────────────────┘
```

**性能要求**：
- 延迟：<50ms/请求
- 吞吐：>100 QPS
- 显存：<4GB（支持 CPU 推理）

---

### 4.2 大模型 API 接入架构

#### 4.2.1 支持的国内主流大模型
| 厂商 | 模型 | 能力 | API 成本 | 接入优先级 |
|-----|------|------|---------|-----------|
| **阿里云** | 通义千问 (Qwen-Max) | 通用对话、代码、多模态 | ¥0.02/千tokens | ⭐⭐⭐⭐⭐ |
| **百度** | 文心一言 (ERNIE-4.0) | 通用对话、文档理解 | ¥0.012/千tokens | ⭐⭐⭐⭐⭐ |
| **智谱AI** | GLM-4 | 长文本、知识问答 | ¥0.05/千tokens | ⭐⭐⭐⭐ |
| **字节跳动** | 豆包 (Doubao-pro) | 对话、内容生成 | ¥0.008/千tokens | ⭐⭐⭐⭐ |
| **讯飞** | 星火 (SparkDesk-3.5) | 专业领域、语音 | ¥0.018/千tokens | ⭐⭐⭐ |

#### 4.2.2 大模型网关设计
```java
// backend/agent-service/src/main/java/com/finex/agent/gateway/

@Service
public class LLMGatewayService {
    
    @Autowired
    private List<LLMProvider> providers; // 通义、文心、GLM 等
    
    /**
     * 统一调用接口
     */
    public LLMResponse chat(LLMRequest request) {
        // 1. 负载均衡 + 故障切换
        LLMProvider provider = selectProvider(request);
        
        // 2. 构建 Prompt
        String prompt = buildPrompt(request);
        
        // 3. 调用大模型 API
        try {
            String response = provider.complete(prompt);
            return LLMResponse.success(response);
        } catch (RateLimitException e) {
            // 切换到备用模型
            return retryWithFallback(request);
        }
    }
    
    /**
     * 负载均衡策略
     */
    private LLMProvider selectProvider(LLMRequest request) {
        // 策略 1: 按成本优先（默认）
        if (request.getPriority() == Priority.COST) {
            return providers.stream()
                .min(Comparator.comparing(LLMProvider::getCostPerToken))
                .orElseThrow();
        }
        
        // 策略 2: 按性能优先
        if (request.getPriority() == Priority.LATENCY) {
            return providers.stream()
                .filter(p -> p.getAvgLatency() < 2000) // <2s
                .findFirst()
                .orElse(providers.get(0));
        }
        
        // 策略 3: 轮询
        return roundRobinSelector.next();
    }
}

// 接口抽象
public interface LLMProvider {
    String complete(String prompt);
    double getCostPerToken();
    int getAvgLatency();
    boolean isAvailable();
}

// 通义千问实现
@Component
public class QwenProvider implements LLMProvider {
    @Value("${llm.qwen.api-key}")
    private String apiKey;
    
    @Override
    public String complete(String prompt) {
        // 调用通义千问 API
        // https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation
    }
}
```

#### 4.2.3 Prompt 工程策略
**系统 Prompt 模板**：
```python
SYSTEM_PROMPT = """你是 FinEx 企业财务助手，专注于报销、费用管理、财务政策咨询。

【核心规则】
1. 回答必须基于提供的企业知识库，不得编造政策
2. 涉及金额、日期、人名的信息已脱敏为占位符（如 [PERSON_001]），你应保持占位符不变
3. 回答需引用来源（如《差旅管理规定》第X条）
4. 如果知识库中没有相关信息，明确告知用户"暂无相关政策，建议咨询财务部门"

【当前用户上下文】
- 用户角色: {user_role}
- 所属部门: {department}
- 权限等级: {permission_level}

【业务本体】
{ontology_context}

【企业知识库（相关片段）】
{retrieved_knowledge}

【用户问题】
{user_query}

请基于上述信息回答用户问题。
"""
```

---

### 4.3 MCP 协议接入设计

#### 4.3.1 MCP 协议简介
Model Context Protocol (MCP) 是一种标准化的协议，用于 AI 模型与外部工具、数据源交互。

**核心概念**：
- **Tools**：模型可调用的外部功能（如查询数据库、发送邮件）
- **Resources**：模型可访问的数据源（如文件、API）
- **Prompts**：预定义的提示词模板

#### 4.3.2 FinEx MCP Server 设计
```typescript
// backend/mcp-server/src/index.ts

import { MCPServer, Tool, Resource } from '@modelcontextprotocol/sdk';

const server = new MCPServer({
  name: 'finex-mcp-server',
  version: '1.0.0'
});

// 工具 1: 查询报销单
server.addTool({
  name: 'query_expense_report',
  description: '根据条件查询报销单',
  inputSchema: {
    type: 'object',
    properties: {
      userId: { type: 'string' },
      startDate: { type: 'string', format: 'date' },
      endDate: { type: 'string', format: 'date' },
      status: { type: 'string', enum: ['Draft', 'Pending', 'Approved'] }
    }
  },
  async execute(params) {
    // 调用 FinEx API
    const response = await fetch('http://localhost:8080/api/expense-reports', {
      method: 'POST',
      body: JSON.stringify(params)
    });
    return await response.json();
  }
});

// 工具 2: 创建报销单
server.addTool({
  name: 'create_expense_report',
  description: '创建新报销单',
  inputSchema: {
    type: 'object',
    properties: {
      amount: { type: 'number' },
      expenseType: { type: 'string' },
      description: { type: 'string' }
    },
    required: ['amount', 'expenseType']
  },
  async execute(params) {
    // 调用创建 API
  }
});

// 资源 1: 企业政策文档
server.addResource({
  uri: 'finex://policies/travel-policy',
  name: '差旅管理规定',
  mimeType: 'text/markdown',
  async read() {
    // 从知识库读取
    return await knowledgeBase.get('travel-policy');
  }
});

server.listen(3000);
```

#### 4.3.3 外部工具接入示例
**接入 Slack（消息通知）**：
```javascript
server.addTool({
  name: 'send_slack_notification',
  description: '向 Slack 频道发送通知',
  async execute({ channel, message }) {
    await slackClient.chat.postMessage({
      channel: channel,
      text: message
    });
  }
});

// 使用场景：数字员工催办
// Agent: "检测到报销单 DOC001 超时3天未审批"
// → 调用 send_slack_notification
// → 消息发送到财务审批频道
```

**接入 Notion（知识管理）**：
```javascript
server.addResource({
  uri: 'notion://databases/finance-wiki',
  name: '财务知识库（Notion）',
  async read() {
    const pages = await notionClient.databases.query({
      database_id: 'xxx'
    });
    return pages.results.map(page => ({
      title: page.properties.Title.title[0].plain_text,
      content: page.properties.Content.rich_text[0].plain_text
    }));
  }
});
```

---

### 4.4 数据库设计

#### 4.4.1 新增表结构
```sql
-- Agent 会话表
CREATE TABLE agent_conversation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  session_id VARCHAR(64) UNIQUE NOT NULL,
  context JSON,  -- 对话上下文（最近10轮）
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  expires_at TIMESTAMP,  -- 会话过期时间（24小时）
  INDEX idx_user_session (user_id, session_id)
);

-- Agent 消息表
CREATE TABLE agent_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id VARCHAR(64) NOT NULL,
  role ENUM('user', 'assistant', 'system') NOT NULL,
  content TEXT NOT NULL,
  masked_content TEXT,  -- 脱敏后的内容（用于审计）
  intent VARCHAR(50),   -- 意图分类
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_session (session_id)
);

-- 数字分身表
CREATE TABLE user_digital_twin (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT UNIQUE NOT NULL,
  behavior_profile JSON,  -- 行为特征
  skill_matrix JSON,      -- 技能图谱
  work_pattern JSON,      -- 工作模式
  last_trained_at TIMESTAMP,
  model_version VARCHAR(20),
  FOREIGN KEY (user_id) REFERENCES user(id)
);

-- 数字员工表
CREATE TABLE digital_employee (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  type ENUM('审核型', '执行型', '咨询型', '分析型', '催办型') NOT NULL,
  responsible_user_id BIGINT NOT NULL,  -- 责任人
  authority_level INT NOT NULL,         -- 权限等级 1-5
  max_amount DECIMAL(15,2),             -- 最大处理金额
  workflow_nodes JSON,                  -- 嵌入的流程节点
  config JSON,                          -- 配置参数
  status ENUM('active', 'suspended', 'archived') DEFAULT 'active',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (responsible_user_id) REFERENCES user(id),
  INDEX idx_responsible_user (responsible_user_id)
);

-- 数字员工操作日志表
CREATE TABLE digital_employee_action_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_id BIGINT NOT NULL,
  responsible_user_id BIGINT NOT NULL,
  action_type VARCHAR(50) NOT NULL,  -- approve, reject, notify, analyze
  business_object_type VARCHAR(50),  -- ExpenseReport, Invoice, Voucher
  business_object_id BIGINT,
  decision_reason TEXT,              -- 决策依据
  confidence_score DECIMAL(5,4),     -- 置信度
  human_review_required BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (employee_id) REFERENCES digital_employee(id),
  INDEX idx_employee_time (employee_id, created_at),
  INDEX idx_business_object (business_object_type, business_object_id)
);

-- 知识库文档表
CREATE TABLE knowledge_document (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  category ENUM('制度政策', '会计准则', '税务法规', '流程SOP', '合同模板', '历史案例') NOT NULL,
  file_path VARCHAR(500),
  file_hash VARCHAR(64),  -- SHA256
  version VARCHAR(20),
  status ENUM('active', 'archived', 'expired') DEFAULT 'active',
  valid_from DATE,
  valid_until DATE,
  uploaded_by BIGINT,
  uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  vector_indexed BOOLEAN DEFAULT FALSE,
  INDEX idx_category_status (category, status)
);

-- 知识库向量块表
CREATE TABLE knowledge_chunk (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  chunk_index INT NOT NULL,
  content TEXT NOT NULL,
  vector_id VARCHAR(100),  -- Milvus 中的向量 ID
  metadata JSON,           -- {page: 3, section: "第二章"}
  FOREIGN KEY (document_id) REFERENCES knowledge_document(id),
  INDEX idx_document (document_id)
);

-- LLM 调用日志表
CREATE TABLE llm_api_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id VARCHAR(64),
  provider VARCHAR(50),   -- qwen, ernie, glm
  model VARCHAR(50),
  prompt_tokens INT,
  completion_tokens INT,
  total_cost DECIMAL(10,6),
  latency_ms INT,
  status ENUM('success', 'failure', 'timeout'),
  error_message TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_session (session_id),
  INDEX idx_provider_time (provider, created_at)
);

-- 脱敏映射表（临时表，24小时自动清理）
CREATE TABLE desensitization_mapping (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id VARCHAR(64) NOT NULL,
  placeholder VARCHAR(50) NOT NULL,  -- [PERSON_001]
  original_value TEXT NOT NULL,      -- 张三
  value_type VARCHAR(20),            -- person, amount, date, doc_id
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP NOT NULL,     -- created_at + 24小时
  INDEX idx_session_placeholder (session_id, placeholder),
  INDEX idx_expires (expires_at)
);
```

#### 4.4.2 向量数据库 Schema (Milvus)
```python
from pymilvus import Collection, FieldSchema, CollectionSchema, DataType

# 知识库向量集合
knowledge_collection = Collection(
    name="finex_knowledge",
    schema=CollectionSchema([
        FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=True),
        FieldSchema(name="document_id", dtype=DataType.INT64),
        FieldSchema(name="chunk_index", dtype=DataType.INT64),
        FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=1024),
        FieldSchema(name="category", dtype=DataType.VARCHAR, max_length=50),
        FieldSchema(name="content", dtype=DataType.VARCHAR, max_length=10000)
    ]),
    index_params={
        "index_type": "HNSW",
        "metric_type": "IP",  # Inner Product (余弦相似度)
        "params": {"M": 16, "efConstruction": 200}
    }
)
```

---

## 五、实施路线图

### 5.1 四阶段演进策略

#### 阶段 1: 基础设施与核心能力（M1-M2，2个月）
**目标**：建立 AI Agent 平台的技术底座

| 任务 | 交付物 | 责任人 | 工期 |
|-----|--------|--------|------|
| 1.1 本地小模型服务搭建 | - ONNX 模型推理服务<br>- 脱敏/还原 API<br>- 性能基准测试报告 | 算法工程师 | 2周 |
| 1.2 大模型网关开发 | - 统一 LLM Gateway<br>- 接入通义、文心<br>- 负载均衡与故障切换 | 后端工程师 | 2周 |
| 1.3 业务本体建模 | - 核心实体 OWL 定义<br>- Neo4j 知识图谱<br>- SPARQL 查询接口 | 架构师 + 业务分析师 | 3周 |
| 1.4 知识库向量化 | - Milvus 部署<br>- 文档解析流水线<br>- 向量化工具开发 | 算法工程师 + 后端 | 2周 |
| 1.5 数据库设计与迁移 | - 新增表创建<br>- 测试数据初始化 | DBA + 后端 | 1周 |

**里程碑**：
- ✅ 脱敏→大模型→还原的完整链路可用
- ✅ 向量检索召回准确率 >80%
- ✅ 业务本体覆盖核心实体（报销单、发票、凭证）

---

#### 阶段 2: FinWork 与个人数字分身（M3-M4，2个月）
**目标**：上线第一个用户可见的 AI 功能

| 任务 | 交付物 | 责任人 | 工期 |
|-----|--------|--------|------|
| 2.1 FinWork 前端开发 | - 对话界面组件<br>- WebSocket 实时通信<br>- 多模态输入（文本/语音） | 前端工程师 | 3周 |
| 2.2 对话引擎后端 | - 意图识别服务<br>- 上下文管理<br>- 任务编排引擎 | 后端工程师 | 3周 |
| 2.3 RAG 检索集成 | - 向量检索 + 知识注入<br>- Prompt 工程优化 | 算法工程师 | 2周 |
| 2.4 个人数字分身 MVP | - 行为采集埋点<br>- 简单特征提取<br>- 智能预填功能 | 全栈工程师 | 2周 |
| 2.5 内测与优化 | - 20人内测<br>- Bug 修复<br>- 用户反馈迭代 | QA + 产品经理 | 2周 |

**里程碑**：
- ✅ 用户可通过 FinWork 查询报销单、咨询政策
- ✅ 个人数字分身可智能预填常用字段
- ✅ 内测用户满意度 >70%

---

#### 阶段 3: 数字员工与流程自动化（M5-M6，2个月）
**目标**：AI 从助手升级为执行者

| 任务 | 交付物 | 责任人 | 工期 |
|-----|--------|--------|------|
| 3.1 数字员工管理后台 | - 创建/配置/监控界面<br>- 责任人绑定功能 | 前端 + 后端 | 3周 |
| 3.2 流程嵌入开发 | - 工作流引擎集成<br>- 数字员工节点类型<br>- 规则引擎 (Drools) | 后端工程师 | 3周 |
| 3.3 审核型数字员工 | - 发票真伪验证<br>- 金额计算复核<br>- 合规性检查 | 算法 + 后端 | 2周 |
| 3.4 审计日志与追溯 | - 操作日志记录<br>- 责任追溯界面<br>- 申诉机制 | 后端 + 前端 | 2周 |
| 3.5 试点部署 | - 选择1-2个部门试点<br>- 效果评估报告 | 产品 + 运营 | 2周 |

**里程碑**：
- ✅ 数字员工可自动处理 <1000元 的报销单初审
- ✅ 误审率 <5%
- ✅ 人工干预率 <20%

---

#### 阶段 4: MCP 生态与持续进化（M7+，持续）
**目标**：开放平台能力，构建生态

| 任务 | 交付物 | 责任人 | 工期 |
|-----|--------|--------|------|
| 4.1 MCP Server 开发 | - MCP 协议实现<br>- 标准化 API 文档 | 后端工程师 | 3周 |
| 4.2 外部工具接入 | - Slack 通知<br>- 钉钉集成<br>- 飞书集成 | 后端工程师 | 2周 |
| 4.3 知识库持续更新机制 | - 自动监测政策变更<br>- 增量向量化<br>- 版本管理 | 算法 + 后端 | 2周 |
| 4.4 模型微调与优化 | - 基于用户反馈微调<br>- A/B 测试框架 | 算法工程师 | 持续 |
| 4.5 开放 API 与文档 | - 开发者文档<br>- SDK (Python/JS)<br>- 示例代码 | 技术写作 + 后端 | 3周 |

**里程碑**：
- ✅ 外部系统可通过 MCP 协议调用 FinEx AI 能力
- ✅ 知识库自动更新，无需人工干预
- ✅ 开发者社区初步建立

---

### 5.2 资源估算

#### 5.2.1 团队配置
| 角色 | 人数 | 投入时间 | 职责 |
|-----|------|---------|------|
| **产品经理** | 1 | 100% | 需求定义、用户调研、roadmap |
| **架构师** | 1 | 50% | 技术架构、本体建模、技术选型 |
| **后端工程师** | 3 | 100% | Agent 服务、网关、MCP Server |
| **前端工程师** | 2 | 100% | FinWork、数字员工管理界面 |
| **算法工程师** | 2 | 100% | 模型训练、Prompt 工程、RAG 优化 |
| **QA 工程师** | 1 | 100% | 测试、质量保障 |
| **DBA** | 1 | 30% | 数据库设计、性能优化 |
| **运维工程师** | 1 | 50% | 部署、监控、日志 |

**总人力**：~10人 FTE（全职等效）

#### 5.2.2 成本估算
| 项目 | 成本（万元/年） | 说明 |
|-----|--------------|------|
| **人力成本** | 200-300 | 10人团队，均薪2-3万/月 |
| **大模型 API** | 10-20 | 按100万次调用/月，平均0.02元/千tokens |
| **服务器** | 15 | GPU 服务器（本地模型推理）+ 云服务器 |
| **向量数据库** | 5 | Milvus 集群（3节点） |
| **第三方服务** | 3 | OCR、语音识别等 |
| **合计** | **233-343万** | 首年总投入 |

---

## 六、风险与应对

### 6.1 技术风险

| 风险 | 影响 | 概率 | 应对措施 |
|-----|------|------|---------|
| **大模型幻觉（Hallucination）** | 高 | 中 | - 强制引用来源<br>- 知识库覆盖核心政策<br>- 人工审核高风险决策 |
| **脱敏算法被绕过** | 高 | 低 | - 多层检测（正则+NLP）<br>- 定期红队测试<br>- 异常告警 |
| **模型推理延迟** | 中 | 中 | - 缓存常见问题<br>- 流式输出<br>- 边缘计算 |
| **向量检索召回率低** | 中 | 中 | - 多路召回（向量+关键词）<br>- 定期评估与优化 |
| **第三方 API 不稳定** | 中 | 中 | - 多模型容灾<br>- 本地缓存<br>- 降级方案 |

### 6.2 合规风险

| 风险 | 应对措施 |
|-----|---------|
| **数据泄露** | - 数据不出域架构<br>- 脱敏映射表24小时自动清理<br>- 定期安全审计 |
| **AI 决策责任不清** | - 数字员工强制绑定责任人<br>- 操作全程可审计<br>- 申诉机制 |
| **知识产权侵权** | - 知识库内容仅使用企业自有或授权文档<br>- 不使用互联网爬取数据 |

### 6.3 业务风险

| 风险 | 应对措施 |
|-----|---------|
| **用户接受度低** | - 分阶段推出，先解决痛点<br>- 充分培训与宣导<br>- 收集反馈快速迭代 |
| **与现有流程冲突** | - 数字员工作为辅助，不强制替代人工<br>- 灰度上线，逐步扩大范围 |
| **投资回报周期长** | - 优先实现高频、重复性任务自动化<br>- 量化降本增效指标 |

---

## 七、成功指标 (KPI)

### 7.1 用户体验指标
- **FinWork 使用率**：月活用户 >60%（目标）
- **对话满意度**：评分 >4.0/5.0
- **平均响应时间**：<3秒（端到端）

### 7.2 效率提升指标
- **报销单处理时效**：缩短 30%
- **政策咨询响应时间**：从 2小时 → 5分钟
- **重复性任务自动化率**：>50%

### 7.3 准确性指标
- **数字员工误审率**：<5%
- **知识检索召回率**：>80%
- **意图识别准确率**：>90%

### 7.4 成本指标
- **人力成本节省**：相当于 2-3个 FTE
- **大模型 API 成本**：<2万元/月

---

## 八、与 YonWork 对比分析

### 8.1 功能对比矩阵
| 功能模块 | YonWork | FinEx AI Agent | 差异化优势 |
|---------|---------|---------------|-----------|
| **对话工作台** | ✅ | ✅ | FinEx 深度集成财务场景 |
| **知识库问答** | ✅ | ✅ | FinEx 支持向量检索+业务本体 |
| **流程自动化** | ✅ | ✅ | FinEx 数字员工责任可追溯 |
| **数据安全** | 企业级加密 | 三段式脱敏架构 | **FinEx 更严格** |
| **多模态交互** | ✅（文档/图表） | ✅（文档/语音/OCR） | 基本持平 |
| **外部集成** | 用友生态 | MCP 开放协议 | **FinEx 更开放** |
| **个人数字分身** | ❌ | ✅ | **FinEx 独有** |
| **业务本体** | 内置会计模型 | 可定制 OWL 本体 | **FinEx 更灵活** |

### 8.2 竞争策略
1. **技术差异化**：主打"数据主权可控"，吸引对数据安全敏感的企业
2. **垂直深度**：聚焦财务领域，不求大而全，但求专而精
3. **开放生态**：通过 MCP 协议，降低客户锁定风险
4. **成本优势**：国产大模型 + 自研本体，成本比 YonWork 低 30%

---

## 九、后续讨论议题（三人小组）

### 待决策问题
1. **本地小模型部署位置**：
   - 方案 A：客户本地部署（私有化）
   - 方案 B：FinEx 云端部署（SaaS）
   - 方案 C：混合部署（小模型本地，大模型云端）

2. **大模型选型优先级**：
   - 成本优先 vs 性能优先？
   - 单一模型 vs 多模型组合？

3. **知识库初始化策略**：
   - 由 FinEx 团队预置通用财务知识？
   - 还是要求客户自行上传企业政策？

4. **数字员工权限边界**：
   - 哪些任务可以完全自动化？
   - 哪些必须保留人工审核？

5. **商业模式**：
   - 按用户数收费？
   - 按 API 调用量收费？
   - 按模块收费（FinWork、数字员工分开售卖）？

### 需补充的技术细节
- [ ] 本地小模型的硬件配置清单
- [ ] 各大模型 API 的详细对比测试
- [ ] 向量数据库容量规划（100万文档需要多少存储？）
- [ ] 高并发场景下的性能压测方案

---

## 十、附录

### A. 参考资料
- 用友 YonWork 产品白皮书（2026年8月）
- Model Context Protocol (MCP) 官方文档
- 《企业级 RAG 系统设计最佳实践》
- 《财务领域知识图谱构建指南》

### B. 术语表
- **RAG**：Retrieval-Augmented Generation，检索增强生成
- **NER**：Named Entity Recognition，命名实体识别
- **OWL**：Web Ontology Language，网络本体语言
- **SPARQL**：SPARQL Protocol and RDF Query Language，语义查询语言
- **HNSW**：Hierarchical Navigable Small World，向量索引算法

---

**文档状态**：待三人小组评审  
**下一步**：组织讨论会议，针对"待决策问题"逐项表决  
**更新计划**：根据讨论结果，发布 v1.1 版本
