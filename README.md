# FinEx 报销与财务协同系统

## 当前定位

当前仓库已经是一个可运行、可演示、可继续扩展的业务项目，不再是纯原型。

当前部署形态是：

- `frontend/admin-web`：Vue 3 管理台
- `backend/gateway`：统一 API 入口
- `backend/auth-service`：当前主业务服务
- `backend/common`：公共响应、JWT 等基础能力
- `backend/sql`：数据库初始化与升级脚本

当前已经覆盖的主要功能包括：

- 登录与当前用户信息
- 首页、报销列表、发票列表
- 个人中心与下载中心
- 流程模板管理、流程设计、费用类型与自定义档案
- 异步任务：下载导出、发票验真、OCR、通知
- 系统设置
- 财务凭证录入

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
└─ start-finex.bat
```

## 配置说明

后端已改为优先从环境变量读取数据库、JWT 和异步线程池配置，不再在仓库中保存真实密码或固定密钥。

示例变量见：

- `backend/.env.example`

Windows 本地开发建议新建一个未提交的 `backend/.env.local.cmd`，内容示例：

```bat
set FINEX_DB_PASSWORD=replace-with-your-db-password
set FINEX_JWT_SECRET=replace-with-a-long-random-secret-at-least-32-chars
```

`start-finex.bat` 会优先加载这个文件，并在以下情况直接拒绝启动：

- `FINEX_DB_PASSWORD` 未设置或仍是示例占位值
- `FINEX_JWT_SECRET` 未设置、仍是示例占位值，或长度少于 32 个字符

开发环境也应提供固定 JWT 密钥，避免临时内存密钥导致登录态在重启后全部失效；生产环境必须通过环境变量注入真实密钥。

## 数据库初始化

建议在全新数据库中按下面顺序执行脚本：

1. `backend/sql/init.sql`
2. `backend/sql/init_custom_archive.sql`
3. `backend/sql/init_expense_type_tree.sql`
4. `backend/sql/init_async_task.sql`
5. `backend/sql/init_finance_gl.sql`
6. `backend/sql/init_process_flow_design.sql`

如果是在旧库上升级，请根据实际版本额外检查并执行：

- `backend/sql/migrate_*.sql`
- `backend/sql/refresh_*.sql`

更详细的初始化与启动说明见：

- `docs/architecture/项目启动与初始化说明.md`

## 启动方式

### 一键启动

```bat
start-finex.bat
```

`start-finex.bat` 现在只负责一键启动入口：它会按顺序加载 `backend/.env.local.cmd` 与可选的 `backend/.env.shadow.cmd`、补齐默认端口、校验 `FINEX_DB_PASSWORD` / `FINEX_JWT_SECRET`、先编译 `backend/common`，然后再委派 `start-finex.ps1` 处理端口冲突、旧进程回收和三个模块的实际启动。

默认启动模块固定为：

- `backend/auth-service`
- `backend/gateway`
- `frontend/admin-web`

若启动失败，优先检查：

- `backend/.env.local.cmd` 里的数据库密码和 JWT 密钥是否为真实值
- JWT 密钥是否至少 32 个字符
- `8081`、`8080`、`5173` 是否被外部进程占用

### 分模块启动

后端服务：

```bash
cd backend/common
mvn compile
```

```bash
cd backend/auth-service
mvn spring-boot:run
```

```bash
cd backend/gateway
mvn spring-boot:run
```

前端管理台：

```bash
cd frontend/admin-web
npm install
npm run dev
```

## 质量校验

后端测试：

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

## 当前治理重点

这轮仓库已经补上的基础护栏包括：

- 数据库连接与 JWT 密钥外置
- 默认日志级别下调到 `info`
- 异步线程池参数支持配置化
- 后端最小单元/控制器测试基线
- 前端 Vitest 基线与权限/登录测试
- README 与初始化说明同步到当前仓库状态

## 相关文档

- `docs/architecture/系统架构设计.md`
- `docs/architecture/开发架构与线程分布.md`
- `docs/architecture/项目启动与初始化说明.md`
- `docs/architecture/auth-service领域边界说明.md`
