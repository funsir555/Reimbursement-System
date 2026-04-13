# FinEx 报销与财务协同系统

## 当前状态

FinEx 当前以 `C:\Users\funsir\Desktop\报销系统\backend\auth-service` 为单体主业务服务，覆盖 `auth / profile / process / async-task / voucher / settings / mvp / finance / expense / fixed-asset / archive-agent` 等子域；`gateway` 提供统一入口，`frontend/admin-web` 提供管理端界面。

当前已验证基线：

- backend：`mvn test` = `303/303`
- frontend：`npm run test:unit` = `211/211`

当前治理阶段：4.4 主链路与 residual 第一轮已完成，仓库进入 `backend residual hotspot second-wave`。

## 环境要求

- JDK 17
- Maven 3.9+
- Node.js 20+
- npm 10+
- MySQL 8.0

## 启动前准备

后端运行依赖以下关键环境变量：

- `FINEX_DB_URL`
- `FINEX_DB_USERNAME`
- `FINEX_DB_PASSWORD`
- `FINEX_JWT_SECRET`

建议在本地创建未提交的 `C:\Users\funsir\Desktop\报销系统\backend\.env.local.cmd`：

```bat
set FINEX_DB_PASSWORD=your-real-db-password
set FINEX_JWT_SECRET=your-long-random-secret-at-least-32-chars
```

说明：

- `start-finex.bat` 会自动加载 `backend/.env.local.cmd`
- `backend/auth-service` 直接执行 `mvn spring-boot:run` 时，现在也会自动补读该文件
- 真实系统环境变量或命令行传参优先级更高，不会被本地文件覆盖

样例配置文件：

- `backend/.env.example`
- `backend/.env.production.example`
- `frontend/admin-web/.env.production.example`

## 快速启动

### 一键启动

```bat
start-finex.bat
```

默认启动：

- `backend/auth-service`
- `backend/gateway`
- `frontend/admin-web`

### 分模块启动

```bash
cd backend/common
mvn compile

cd ../auth-service
mvn spring-boot:run

cd ../gateway
mvn spring-boot:run

cd ../../frontend/admin-web
npm install
npm run dev
```

## 常用验证命令

```bash
cd backend
mvn test
```

```bash
cd frontend/admin-web
npm run test:unit
```

```bash
cd frontend/admin-web
npm run build
```

## 下一批 residual 优先级

1. `ProcessFlowDesignServiceImpl`
2. `ExpensePaymentDomainSupport`
3. `ExpenseRelationWriteOffService`
4. `ExpenseSummaryAssembler`

## 相关文档

- `C:\Users\funsir\Desktop\报销系统\当前实现架构图与演进路线图.md`
- `C:\Users\funsir\Desktop\报销系统\docs\architecture\项目启动与初始化说明.md`
- `C:\Users\funsir\Desktop\报销系统\docs\architecture\auth-service领域边界说明.md`
- `C:\Users\funsir\Desktop\报销系统\执行记录\报销系统治理落地方案.md`