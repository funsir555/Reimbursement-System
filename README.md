# FinEx 报销与财务协同系统

更新时间：2026-04-24

## 当前状态

FinEx 当前仓库的真实形态是：

- `backend/auth-service`：单体主服务，承载 `auth / profile / process / async-task / voucher / settings / mvp / finance / expense / fixed-asset / archive-agent` 等子域
- `backend/gateway`：统一网关入口
- `frontend/admin-web`：管理端前端
- `backend/common`：公共基础能力
- `backend/sql`：初始化、迁移与刷新脚本

当前已验证基线：

- backend：`mvn test` 通过，`556/556`
- frontend：`npm run test:unit` 通过，`211/211`

当前治理阶段判断：

- `4.4` 主链路与 residual 第一轮已完成
- `backend residual hotspot second-wave` 既定四个热点已全部完成收口
- 下一步不再沿用旧的固定顺位，而是回到全项目 hotspot 扫描与 residual re-rank

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

推荐在本地创建未提交的 `backend/.env.local.cmd`：

```bat
set FINEX_DB_PASSWORD=your-real-db-password
set FINEX_JWT_SECRET=your-long-random-secret-at-least-32-chars
```

说明：

- `start-finex.bat` 会自动加载 `backend/.env.local.cmd`
- 直接在 `backend/auth-service` 执行 `mvn spring-boot:run` 时，也会自动补读 `backend/.env.local.cmd`
- 如果当前 shell 或 JVM 参数里已经显式设置了同名变量，则以显式传入值为准，不会被本地文件覆盖

样例文件：

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

### 手动启动

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

默认端口：

- `8081`：`auth-service`
- `8080`：`gateway`
- `5173`：`frontend/admin-web`

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

## 当前治理结论

当前 second-wave 已完成的后端热点：

- `ProcessFlowDesignServiceImpl`
- `ExpensePaymentDomainSupport`
- `ExpenseRelationWriteOffService`
- `ExpenseSummaryAssembler`

当前下一步：

1. 回到全项目 hotspot 扫描与 residual re-rank
2. 按《项目长期架构治理工作流》决定下一批主目标
3. 持续同步 README、架构文档、执行记录与启动说明，避免文档再次漂移或乱码回流

## 相关文档

- `当前实现架构图与演进路线图.md`
- `docs/architecture/系统架构设计.md`
- `docs/architecture/开发架构与线程分布.md`
- `docs/architecture/项目启动与初始化说明.md`
- `docs/architecture/auth-service领域边界说明.md`
- `执行记录/报销系统治理落地方案.md`
- `执行记录/2026-04-24_项目长期架构治理工作流.md`