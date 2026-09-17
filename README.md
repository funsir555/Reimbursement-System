# FinEx 报销与财务协同系统

**更新时间**: 2026-09-17  
**系统版本**: v1.2.x  
**架构阶段**: 模块化单体 + 持续增量迭代

---

## 📋 目录

- [系统概述](#系统概述)
- [当前状态](#当前状态)
- [功能范围](#功能范围)
- [技术栈](#技术栈)
- [环境要求](#环境要求)
- [快速启动](#快速启动)
- [开发指南](#开发指南)
- [架构文档](#架构文档)
- [贡献指南](#贡献指南)

---

## 系统概述

**FinEx** 是一个面向中小企业的**报销与财务一体化管理平台**，提供从报销申请、审批、付款到财务凭证生成的全流程自动化能力。

### 核心价值

1. **报销自动化**: 报销申请 → 审批流转 → 付款执行 → 凭证生成全流程自动化
2. **财务一体化**: 报销与总账、固定资产无缝集成，数据实时同步
3. **智能化协助**: AI Agent 提供报销审核、财务分析、合规检查等智能能力
4. **灵活可配**: 流程、表单、规则可视化配置，适配不同企业需求

### 目标用户

- **企业规模**: 50-500 人的中小企业
- **日活用户**: 50-200 人
- **数据量级**: 单账套 10 万张凭证/年

---

## 当前状态

### 系统架构

FinEx 采用**模块化单体架构**，清晰的领域边界，为未来微服务拆分预留空间：

```
frontend/admin-web/         # Vue 3 + TypeScript 管理端前端
backend/
├─ gateway/                 # Spring Cloud Gateway 统一网关
├─ auth-service/            # 单体主服务，承载多个业务域
│  ├─ auth/                 # 认证授权域
│  ├─ expense/              # 报销管理域
│  ├─ finance/              # 财务管理域 (voucher/ledger/asset)
│  ├─ process/              # 流程引擎域
│  ├─ agent/                # 智能 Agent 域
│  └─ system/               # 系统管理域 (settings/profile)
├─ common/                  # 公共基础能力
└─ sql/                     # 数据库脚本与迁移
```

### 测试基线

| 模块 | 测试用例数 | 通过率 | 说明 |
|------|-----------|--------|------|
| Backend | 707 | 98.3% (695/707) | 12个失败用例待修复（财务模块测试配置） |
| Frontend | 648 | 99.5% (645/648) | 3个失败用例待修复（报销单验证、财务工作区） |

### 代码规模

| 指标 | 数量 | 变化（vs 2026-04） |
|------|------|-------------------|
| Backend Java 文件 | 1001 | +54% |
| Frontend 源文件 | 327 | 持平 |
| DTO/VO 对象 | 300+ | - |
| 最大单文件 (后端) | 5196 行 | AbstractExpenseWorkflowSupport.java |
| 最大单文件 (前端) | 2648 行 | SystemSettingsView.vue |

### 架构治理阶段

✅ **已完成**:
- 4.4 主链路与第一轮 residual 收口
- second-wave 四个后端热点治理
- 财务模块功能增强（凭证作废、冲销、期末结转）
- 系统设置模块重构与权限体系完善

⚠️ **待处理**:
- 上下文管理体系统一（3套独立实现）
- Agent 平台边界明确（技术栈融合）
- 超大 Support 类重构（7个文件 > 2000行）
- Expense-Finance 领域边界清晰化

📋 **治理原则**: 
- 所有新功能开发必须遵循 `docs/architecture/系统最终目标与开发架构规范.md`
- 禁止新增 2000+ 行的类或 1500+ 行的组件
- 禁止跨域直接访问 Mapper
- 禁止循环依赖

---

## 功能范围

### 核心功能模块

#### 📝 报销管理 (Expense)
- 报销单全生命周期管理（创建、提交、审批、付款）
- 多种报销类型（差旅、日常、借款、还款、预付核销）
- 附件管理（上传、OCR 识别、发票验真）
- 付款单管理与银行对接

#### 💰 财务管理 (Finance)
- **总账管理**: 凭证录入、审核、记账、结账
- **账簿查询**: 明细账、总账、序时账、多维度辅助账
- **固定资产**: 资产卡片、折旧计提
- **期末处理**: 期末结转（损益、费用）、期初余额
- **财务档案**: 科目、客户、供应商、员工、部门、项目

#### 🔄 流程引擎 (Process)
- 可视化流程设计器（节点、连线、条件分支）
- 动态表单设计器（字段拖拽、验证规则）
- 报销明细设计器（明细项字段配置）
- 流程模板管理与版本控制

#### 🤖 智能 Agent (Agent)
- Agent 工作台（Prompt、模型、工具、流程编排）
- Agent 对话界面（多轮对话、上下文管理）
- 业务工具集成（查询报销单、发送通知等）
- 数据脱敏管理（敏感字段映射、还原）

#### ⚙️ 系统管理 (System)
- 组织架构管理（公司、部门、员工）
- 角色权限管理（角色、权限码、菜单）
- 系统配置管理（凭证字、OCR、接口对接）
- 异步任务管理与通知中心

### 明确不做的功能

- ❌ 多租户 SaaS 化（当前仅支持单租户私有部署）
- ❌ 移动端原生 App（仅保证 H5 响应式）
- ❌ 供应链管理（采购、库存、销售）
- ❌ 人力资源管理（招聘、考勤、薪酬）
- ❌ 合同管理
- ❌ 第三方深度集成（钉钉、企微深度集成）

---

## 技术栈

### 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.x | 核心框架 |
| MyBatis-Plus | 3.5+ | ORM 框架 |
| MySQL | 8.0+ | 主数据库 |
| Spring Cloud Gateway | 4.x | API 网关 |
| Jackson | 2.x | JSON 处理 |
| JUnit 5 + Mockito | - | 单元测试 |
| SpringDoc (OpenAPI 3) | - | API 文档 |

### 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.x | 核心框架 |
| TypeScript | 5.x | 类型系统 |
| Pinia | 2.x | 状态管理 |
| Vue Router | 4.x | 路由管理 |
| Element Plus | 2.x | UI 组件库 |
| Axios | 1.x | HTTP 客户端 |
| Vite | 5.x | 构建工具 |
| Vitest | - | 单元测试 |

### 基础设施（规划中）

| 技术 | 状态 | 优先级 |
|------|------|--------|
| Redis | 规划中 | P0 |
| RabbitMQ / Kafka | 规划中 | P1 |
| SkyWalking / Zipkin | 规划中 | P2 |
| ELK / Loki | 规划中 | P2 |
| Prometheus + Grafana | 规划中 | P2 |

---

## 环境要求

### 开发环境

| 工具 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 17+ | 推荐使用 OpenJDK 或 Oracle JDK |
| Maven | 3.9+ | 构建工具 |
| Node.js | 20+ | 前端运行环境 |
| npm | 10+ | 包管理器 |
| MySQL | 8.0+ | 主数据库 |
| Git | 2.x | 版本控制 |

### 推荐 IDE

- **后端**: IntelliJ IDEA Ultimate / VS Code + Java 扩展
- **前端**: VS Code + Volar 扩展

---

## 启动前准备

### 1. 数据库初始化

```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE finex_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 导入初始化脚本
mysql -u root -p finex_db < backend/sql/init.sql
```

### 2. 环境变量配置

后端运行依赖以下关键环境变量：

- `FINEX_DB_URL` - 数据库连接地址
- `FINEX_DB_USERNAME` - 数据库用户名
- `FINEX_DB_PASSWORD` - 数据库密码
- `FINEX_JWT_SECRET` - JWT 签名密钥（至少 32 字符）

**推荐方式**：在本地创建 `backend/.env.local.cmd`（不提交到 Git）：

```bat
set FINEX_DB_URL=jdbc:mysql://localhost:3306/finex_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
set FINEX_DB_USERNAME=root
set FINEX_DB_PASSWORD=your-real-db-password
set FINEX_JWT_SECRET=your-long-random-secret-at-least-32-chars
```

**加载说明**：
- `start-finex.bat` 会自动加载 `backend/.env.local.cmd`
- `mvn spring-boot:run` 在 `backend/auth-service` 目录下执行时也会自动加载
- 显式传入的环境变量优先级更高

**样例文件**：
- `backend/.env.example` - 开发环境样例
- `backend/.env.production.example` - 生产环境样例
- `frontend/admin-web/.env.production.example` - 前端生产环境样例

---

## 快速启动

### 方式一：一键启动（推荐）

```bat
start-finex.bat
```

启动后访问：
- 前端管理台：http://localhost:5173
- 后端网关：http://localhost:8080
- 后端服务：http://localhost:8081

默认测试账号：
- 用户名：`admin`
- 密码：`admin123`

### 方式二：手动启动

**1. 编译公共模块**
```bash
cd backend/common
mvn clean compile install
```

**2. 启动后端服务**
```bash
# Terminal 1: 启动主服务
cd backend/auth-service
mvn spring-boot:run

# Terminal 2: 启动网关
cd backend/gateway
mvn spring-boot:run
```

**3. 启动前端**
```bash
# Terminal 3: 启动前端
cd frontend/admin-web
npm install
npm run dev
```

### 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| frontend/admin-web | 5173 | Vue 3 前端（开发模式） |
| backend/gateway | 8080 | Spring Cloud Gateway 网关 |
| backend/auth-service | 8081 | 主业务服务 |

---

## 开发指南

### 常用命令

**后端**：
```bash
# 编译
cd backend
mvn clean compile

# 运行测试
mvn test

# 打包
mvn clean package -DskipTests

# 运行指定测试类
mvn test -Dtest=ExpenseDocumentServiceTest
```

**前端**：
```bash
cd frontend/admin-web

# 安装依赖
npm install

# 开发模式
npm run dev

# 运行单元测试
npm run test:unit

# 运行单元测试（监听模式）
npm run test:unit:watch

# 类型检查
npm run type-check

# 代码格式化
npm run format

# 构建生产包
npm run build
```

### 开发规范

**必须遵循的规范**（详见 `docs/architecture/系统最终目标与开发架构规范.md`）：

1. **文件大小约束**
   - 🚫 禁止新增 2000+ 行的 Java 类
   - 🚫 禁止新增 1500+ 行的 Vue 组件
   - ✅ 推荐单个类/组件不超过 500 行

2. **架构约束**
   - 🚫 禁止跨域直接访问 Mapper
   - 🚫 禁止 Controller 直接调用 Mapper
   - 🚫 禁止循环依赖
   - ✅ 只能通过 Service 接口跨域调用

3. **命名规范**
   - ✅ 类名：大驼峰，语义明确
   - ✅ 方法名：小驼峰，动词开头
   - ❌ 避免使用泛化命名（`*Support`、`*Helper`、`*Util`）

4. **测试要求**
   - ✅ 新功能必须添加单元测试
   - ✅ 核心业务逻辑覆盖率 > 70%
   - ✅ 提交前必须执行 `mvn test` / `npm run test:unit`

### 分支管理

```
main                    # 生产环境代码（受保护）
├─ develop              # 开发主分支
│  ├─ feature/xxx       # 功能分支
│  ├─ bugfix/xxx        # Bug 修复分支
│  └─ refactor/xxx      # 重构分支
└─ hotfix/xxx          # 紧急修复分支（从 main 拉取）
```

**分支命名示例**：
- `feature/expense-ocr-integration`
- `bugfix/finance-voucher-validation`
- `refactor/agent-context-unification`

**合并流程**：
```
feature/xxx → develop (Code Review) → main (Release)
```

### Code Review 检查清单

提交 Pull Request 前请确认：

- [ ] 功能完整实现，边界条件已考虑
- [ ] 符合架构约束（无跨域 Mapper 访问）
- [ ] 文件大小符合规范（< 2000行 Java / < 1500行 Vue）
- [ ] 命名清晰，无泛化命名
- [ ] 添加了单元测试，测试通过
- [ ] 添加了必要的注释
- [ ] 代码格式化完成

---

## 架构文档

### 核心文档（必读）

| 文档 | 位置 | 说明 |
|------|------|------|
| **系统最终目标与开发架构规范** | `docs/architecture/系统最终目标与开发架构规范.md` | 🌟 最高指导文件，所有开发必须遵循 |
| 当前实现架构图与演进路线图 | `当前实现架构图与演进路线图.md` | 当前架构状态与治理路线 |
| 项目长期架构治理工作流 | `执行记录/2026-04-24_项目长期架构治理工作流.md` | 治理方法论与节奏 |

### 专项文档

| 文档 | 位置 | 说明 |
|------|------|------|
| 系统架构设计 | `docs/architecture/系统架构设计.md` | 整体架构设计 |
| auth-service 领域边界说明 | `docs/architecture/auth-service领域边界说明.md` | 领域职责划分 |
| 开发架构与线程分布 | `docs/architecture/开发架构与线程分布.md` | 技术架构详解 |
| 项目启动与初始化说明 | `docs/architecture/项目启动与初始化说明.md` | 启动流程说明 |
| 重复造轮子分析报告 | `执行记录/2026-09-17_系统重复造轮子与架构各自为政分析报告.md` | 当前架构问题分析 |
| 报销系统治理落地方案 | `执行记录/报销系统治理落地方案.md` | 治理进度基线 |

### 文档层级关系

```
系统最终目标与开发架构规范.md (Tier 1 - 战略文档)
  ├─ 指导 → 当前实现架构图与演进路线图.md (Tier 2)
  ├─ 指导 → 项目长期架构治理工作流.md (Tier 2)
  ├─ 指导 → 系统架构设计.md (Tier 2)
  └─ 指导 → 专项治理方案 (Tier 3)
```

---

## 贡献指南

### 如何贡献

1. Fork 本仓库
2. 创建功能分支（`git checkout -b feature/your-feature`）
3. 提交更改（`git commit -m 'Add some feature'`）
4. 推送到分支（`git push origin feature/your-feature`）
5. 创建 Pull Request

### 提交信息规范

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type 类型**：
- `feat`: 新功能
- `fix`: Bug 修复
- `refactor`: 重构（不改变功能）
- `docs`: 文档更新
- `style`: 代码格式调整
- `test`: 测试相关
- `chore`: 构建/工具链调整

**示例**：
```
feat(expense): 添加报销单 OCR 识别功能

- 集成百度 OCR API
- 支持发票、火车票、出租车票识别
- 自动填充报销明细

Closes #123
```

### 问题反馈

- 通过 GitHub Issues 提交 Bug 报告或功能建议
- 提供详细的复现步骤和环境信息
- 附上相关日志和截图

---

## 常见问题

### 启动问题

**Q: 启动时提示数据库连接失败？**

A: 检查以下几点：
1. MySQL 服务是否启动
2. 数据库是否已创建（`finex_db`）
3. `backend/.env.local.cmd` 中的数据库配置是否正确
4. 数据库用户是否有权限

**Q: 前端启动后页面空白？**

A: 检查：
1. 后端服务是否正常启动（8080、8081 端口）
2. 浏览器控制台是否有错误信息
3. `frontend/admin-web/.env` 配置是否正确

### 测试问题

**Q: 运行 `mvn test` 时部分测试失败？**

A: 当前已知问题（正在修复）：
- 12 个后端测试失败（财务模块测试环境配置问题）
- 3 个前端测试失败（报销单验证、财务工作区交互）
- 不影响正常开发，可使用 `-DskipTests` 跳过

**Q: 如何运行单个测试类？**

A: 
```bash
# 后端
mvn test -Dtest=ExpenseDocumentServiceTest

# 前端
npm run test:unit -- ExpenseCreateView.test.ts
```

### 开发问题

**Q: 如何新增一个领域模块？**

A: 参考 `docs/architecture/系统最终目标与开发架构规范.md` 第二部分"领域边界与职责划分"

**Q: 跨域调用如何实现？**

A: 只能通过 Service 接口调用，禁止直接访问 Mapper。详见架构规范文档。

---

## 许可证

[待补充]

---

## 联系方式

- 项目负责人：[待补充]
- 技术支持：[待补充]
- 邮箱：[待补充]

---

**最后更新**: 2026-09-17  
**文档维护**: Architecture Team