# FinEx 报销与财务协同系统

## 当前定位

当前仓库已经不是早期“认证骨架 + 前端原型”，而是一个以 `backend/auth-service` 为单体主服务、持续做内部边界治理的业务系统。

当前真实形态：

- `frontend/admin-web`：Vue 3 管理台，承载登录、首页、报销、流程、财务、系统设置等页面
- `backend/gateway`：统一 API 入口，当前主要负责 `/api/auth/**` 路由转发
- `backend/auth-service`：单体主服务，已承载 `auth / profile / process / async-task / voucher / settings / mvp / finance / expense / fixed-asset / archive-agent` 等多个子域
- `backend/common`：统一返回、JWT、公共基础能力
- `backend/sql`：初始化、迁移、刷新脚本

当前治理阶段：`4.4` 主链路与第一轮 residual 收口已完成，仓库已进入 `backend residual hotspot second-wave`。

## 当前验证基线

- backend：`mvn test` 通过，`297/297`
- frontend：`npm run test:unit` 通过，`211/211`

当前完成的重点治理批次包括：

- `process + settings`
- `voucher + finance-context`
- `async-task`
- `profile`
- `auth + mvp-dashboard`
- `finance-system`
- `finance archive` 四个 service
- `expense residual hotspot`
- `fixed-asset residual`
- `expense-voucher-generation residual`
- `archive-agent residual`

## 当前 residual 优先级

根据当前仓库热点复盘，下一批 backend residual 建议顺位为：

1. `ProcessFlowDesignServiceImpl`
2. `ExpensePaymentDomainSupport`
3. `ExpenseRelationWriteOffService`
4. `ExpenseSummaryAssembler`

说明：当前不回头重打已形成 owner 的 `Abstract*Support` 大基座；它们仍需约束，但不作为下一批首要目标。

## 运行环境

- JDK 17
- Maven 3.9+
- Node.js 20+
- npm 10+
- MySQL 8.0

## 仓库结构

```text
.
├─ backend/
│  ├─ auth-service/
│  ├─ common/
│  ├─ gateway/
│  └─ sql/
├─ docs/
│  └─ architecture/
├─ frontend/
│  └─ admin-web/
├─ 执行记录/
├─ 当前实现架构图与演进路线图.md
└─ start-finex.bat
```

## 配置说明

后端优先从环境变量读取数据库、JWT、端口和异步线程池配置；仓库内只保留样例，不保存真实密码或固定密钥。

- 本地开发样例：`backend/.env.example`
- 生产部署样例：`backend/.env.production.example`
- 前端生产样例：`frontend/admin-web/.env.production.example`

Windows 本地开发建议新建未提交的 `backend/.env.local.cmd`：

```bat
set FINEX_DB_PASSWORD=replace-with-your-db-password
set FINEX_JWT_SECRET=replace-with-a-long-random-secret-at-least-32-chars
```

`start-finex.bat` 会优先加载该文件，并在数据库密码缺失、JWT 密钥仍为占位值、或 JWT 长度少于 32 个字符时直接拒绝启动。

## 数据库初始化

新库建议按下面顺序执行：

1. `backend/sql/init.sql`
2. `backend/sql/init_custom_archive.sql`
3. `backend/sql/init_expense_type_tree.sql`
4. `backend/sql/init_async_task.sql`
5. `backend/sql/init_finance_gl.sql`
6. `backend/sql/init_process_flow_design.sql`

旧库升级则按实际差异补执行：

- `backend/sql/migrate_*.sql`
- `backend/sql/refresh_*.sql`

更详细说明见 `docs/architecture/项目启动与初始化说明.md`。

## 启动方式

### 一键启动

```bat
start-finex.bat
```

默认会启动：

- `backend/auth-service`
- `backend/gateway`
- `frontend/admin-web`

### 分模块启动

后端：

```bash
cd backend/common
mvn compile

cd ../auth-service
mvn spring-boot:run

cd ../gateway
mvn spring-boot:run
```

前端：

```bash
cd frontend/admin-web
npm install
npm run dev
```

## 质量校验

后端：

```bash
cd backend
mvn test
```

前端单测：

```bash
cd frontend/admin-web
npm run test:unit
```

前端构建：

```bash
cd frontend/admin-web
npm run build
```

## 相关文档

- `当前实现架构图与演进路线图.md`
- `docs/architecture/系统架构设计.md`
- `docs/architecture/开发架构与线程分布.md`
- `docs/architecture/项目启动与初始化说明.md`
- `docs/architecture/auth-service领域边界说明.md`
- `执行记录/报销系统治理落地方案.md`
