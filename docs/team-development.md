# FinEx Team Development README

更新日期：2026-04-24

## 1. 目的

本文用于统一 FinEx 项目的团队开发、联调、提测和交付流程，避免继续依赖单人电脑作为唯一开发环境。

当前仓库地址：

- `git@github.com:funsir555/Reimbursement-System.git`

当前默认模块：

- `backend/auth-service`
- `backend/gateway`
- `frontend/admin-web`

## 2. 协作原则

- 代码以 GitHub 仓库为唯一准入源，禁止多人直接共用同一个本地项目目录。
- 每位开发和测试人员都在自己的电脑上拉取代码、运行服务和执行测试。
- 本地配置、数据库密码、JWT 密钥等敏感信息只能保存在个人环境，不提交到仓库。
- 所有功能改动通过分支和 Pull Request 合并，不直接改动 `main`。
- 提交前先自测，合并前至少完成代码评审和关键回归。

## 3. 成员角色建议

- 产品/负责人：确定需求范围、验收口径、提测窗口。
- 开发：按模块或需求分支开发，提交 PR，处理评审意见。
- 测试：基于测试环境或本地环境验证需求、回归核心链路。
- 仓库管理员：维护分支权限、协作者权限、版本标签、发布记录。

如果当前团队规模较小，以上角色可以由同一人兼任，但流程不要省略。

## 4. 新成员接入步骤

### 4.1 获取仓库权限

- 由仓库管理员在 GitHub 为新成员开通仓库访问权限。
- 新成员本地配置好 Git SSH Key 或 HTTPS 凭据。

### 4.2 克隆仓库

```bash
git clone git@github.com:funsir555/Reimbursement-System.git
cd Reimbursement-System
```

### 4.3 安装基础环境

- JDK 17
- Maven 3.9+
- Node.js 20+
- npm 10+
- MySQL 8.0

建议统一版本，避免因为运行时差异导致“我这里正常、你那里报错”。

### 4.4 配置本地环境变量

后端关键变量：

- `FINEX_DB_URL`
- `FINEX_DB_USERNAME`
- `FINEX_DB_PASSWORD`
- `FINEX_JWT_SECRET`

可参考以下文件：

- `backend/.env.example`
- `backend/.env.production.example`
- `frontend/admin-web/.env.production.example`

Windows 本地建议创建未提交文件 `backend/.env.local.cmd`：

```bat
set FINEX_DB_URL=jdbc:mysql://127.0.0.1:3306/finex?useUnicode=true^&characterEncoding=utf8^&serverTimezone=Asia/Shanghai
set FINEX_DB_USERNAME=root
set FINEX_DB_PASSWORD=your-real-db-password
set FINEX_JWT_SECRET=your-long-random-secret-at-least-32-chars
```

说明：

- `backend/.env.local.cmd` 已被 `.gitignore` 忽略，不应提交。
- 每位成员使用自己的数据库账号、密码和本地密钥。

### 4.5 初始化数据库

空库推荐按以下顺序执行：

1. `backend/sql/init.sql`
2. `backend/sql/init_custom_archive.sql`
3. `backend/sql/init_expense_type_tree.sql`
4. `backend/sql/init_async_task.sql`
5. `backend/sql/init_finance_gl.sql`
6. `backend/sql/init_process_flow_design.sql`

如果是升级已有库，优先执行对应的增量脚本，不要重复跑全量初始化脚本。

## 5. 本地启动方式

### 5.1 一键启动

```bat
start-finex.bat
```

默认会启动：

- `backend/auth-service`
- `backend/gateway`
- `frontend/admin-web`

默认端口：

- `8081`：auth-service
- `8080`：gateway
- `5173`：frontend/admin-web

### 5.2 手动启动

先编译公共模块：

```bash
cd backend/common
mvn compile
```

启动后端：

```bash
cd ../auth-service
mvn spring-boot:run

cd ../gateway
mvn spring-boot:run
```

启动前端：

```bash
cd ../../frontend/admin-web
npm install
npm run dev
```

## 6. 分支策略

推荐以下分支模型：

- `main`：可发布、可回溯的稳定分支
- `feature/*`：新功能开发
- `fix/*`：缺陷修复
- `hotfix/*`：线上紧急修复
- `refactor/*`：重构和治理类改动

命名示例：

- `feature/voucher-create`
- `fix/process-node-save`
- `refactor/finance-query-cleanup`

要求：

- 一个需求或一个缺陷使用一个独立分支。
- 分支从最新的 `main` 拉出。
- 不在他人的分支上继续开发。

## 7. 每日开发流程

### 7.1 开始开发前

```bash
git checkout main
git pull
git checkout -b feature/your-task-name
```

### 7.2 开发中

- 小步提交，避免一次提交包含过多无关改动。
- 尽量将后端、前端、SQL、文档变更保持在同一个任务上下文内。
- 不要提交本地临时文件、账号信息、导出文件、数据库备份。

### 7.3 提交前自检

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

最低要求：

- 本次改动涉及的模块必须自测通过。
- 核心链路至少人工走查一次。
- 不允许带着已知报错直接提交 PR。

### 7.4 提交与推送

```bash
git status
git add .
git commit -m "feat: add voucher create validation"
git push -u origin feature/your-task-name
```

提交信息建议使用统一前缀：

- `feat:`
- `fix:`
- `refactor:`
- `test:`
- `docs:`
- `chore:`

## 8. Pull Request 规范

每个 PR 应说明以下内容：

- 背景：为什么要改
- 范围：改了哪些模块
- 风险：可能影响哪些功能
- 验证：本地执行了哪些测试
- 截图或录屏：前端可见改动建议附上

建议评审重点：

- 是否存在业务回归
- 是否引入权限、金额、流程、凭证相关风险
- 是否缺失 SQL、接口、测试或文档同步
- 是否误提交了配置、缓存、导出文件

## 9. 测试协作建议

### 9.1 本地开发测试

- 每位开发本地维护自己的数据库和配置。
- 开发自测时不要直接使用他人数据库。

### 9.2 共享测试环境

建议尽快准备一套独立测试环境，用于：

- 集成联调
- 测试人员回归
- 产品验收
- 发版前冒烟

测试环境至少应与开发本地分离：

- 独立服务进程
- 独立数据库
- 独立测试账号
- 独立数据准备规则

### 9.3 提测要求

提测前建议满足：

- 相关功能分支已合并到测试分支或指定提测分支
- 数据库脚本已同步
- 关键功能已附自测说明
- 已告知测试影响范围和回归重点

## 10. 配置与安全要求

- 严禁提交真实数据库密码、生产地址、真实 JWT 密钥。
- 本地环境使用 `backend/.env.local.cmd` 等未提交文件维护。
- 如需共享测试账号，统一放在受控文档，不写入代码仓库。
- 数据库变更必须保留脚本，不允许只在本地手工改库。
- 涉及财务数据导入导出时，测试样本优先使用脱敏数据。

## 11. 建议尽快补齐的团队能力

当前仓库已经具备多人协作的基础，但建议继续补齐以下能力：

1. GitHub 协作者和分支保护规则
2. Pull Request 模板
3. GitHub Actions 自动测试
4. 独立测试环境
5. 版本发布记录和回滚说明

## 12. 常见问题

### Q1：能不能大家直接连负责人电脑开发？

不建议。这样会带来以下问题：

- 环境互相覆盖
- 进程端口冲突
- 无法追踪是谁改了什么
- 负责人电脑一旦关机，全员阻塞

### Q2：测试同事要不要自己拉代码？

如果测试需要本地验证、查看接口或复现问题，建议也拉一份仓库；如果只做页面和流程验收，可以优先使用共享测试环境。

### Q3：数据库脚本如何管理？

- 新增初始化或增量脚本时，必须提交到仓库
- 在 PR 说明里写明执行顺序和适用范围
- 禁止只在口头或聊天里通知“手工改一下表”

## 13. 推荐阅读

- `README.md`
- `docs/architecture/项目启动与初始化说明.md`
- `docs/architecture/系统架构设计.md`
- `docs/architecture/开发架构与线程分布.md`
- `docs/architecture/auth-service领域边界说明.md`
- `docs/learning-guide/README.md`

## 14. 当前执行口径

在项目进入正式多人协作前，默认按以下口径执行：

- 代码统一从 GitHub 拉取和提交
- `main` 只接受经过评审的合并
- 开发提交前至少执行一次对应模块测试
- 测试优先在独立测试环境完成，未具备条件前可先各自本地验证

如流程发生变化，请同步更新本文档。
