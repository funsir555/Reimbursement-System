# Agent 模块上线执行清单

## 当前状态说明

- 本文属于 `archive-agent` 专项上线清单，当前仍适用于 archive-agent residual 已收口后的部署与验收
- 当前项目进度请以 `C:\Users\funsir\Desktop\报销系统\执行记录\报销系统治理落地方案.md` 为准
- 若需整体部署步骤，请同时参考 `docs/阿里云轻量服务器部署-finexgz.xyz.md` 与 `docs/architecture/项目启动与初始化说明.md`

## 1. 数据库增量初始化

按下面顺序执行，全部针对现有 `finex_db` 做增量迁移，不清库：

1. 校验本机连接配置  
   - 文件：`backend/.env.local.cmd`
   - 默认值：
     - `FINEX_DB_URL=jdbc:mysql://localhost:3306/finex_db?...`
     - `FINEX_DB_USERNAME=root`
     - `FINEX_DB_PASSWORD=replace-with-your-db-password`

2. 执行 Agent 表迁移  
   - `backend/sql/migrate_archive_agent.sql`

3. 执行系统设置与菜单权限迁移  
   - `backend/sql/migrate_system_settings.sql`

4. 刷新权限字典  
   - `backend/sql/refresh_system_settings_permissions.sql`

5. 给超级管理员补齐权限  
   - `backend/sql/grant_super_admin_all_permissions.sql`

## 2. 数据库核查项

执行完 SQL 后，至少检查以下对象是否存在：

- Agent 表
  - `ea_agent_definition`
  - `ea_agent_version`
  - `ea_agent_trigger`
  - `ea_agent_tool_binding`
  - `ea_agent_run`
  - `ea_agent_run_step`

- Agent 权限
  - `agents:menu`
  - `agents:view`
  - `agents:create`
  - `agents:edit`
  - `agents:delete`
  - `agents:run`
  - `agents:publish`
  - `agents:view_logs`

- 超管授权
  - 确认 `SUPER_ADMIN` 已拿到全部 `agents:*` 权限
  - 确认 `admin` 已绑定 `SUPER_ADMIN`

## 3. 后端编译与启动

1. 编译校验  
   - 在 `backend` 目录执行：`mvn -q -DskipTests compile`

2. 聚焦测试  
   - `mvn -q -pl auth-service "-Dtest=ArchiveAgentControllerTest" test`
   - `mvn -q -pl auth-service "-Dtest=GlobalExceptionHandlerTest" test`

3. 启动 `auth-service`  
   - 使用本机环境变量：
     - `FINEX_DB_URL`
     - `FINEX_DB_USERNAME`
     - `FINEX_DB_PASSWORD`
     - `FINEX_JWT_SECRET`

## 4. 登录与权限刷新

1. 如果前端已登录，先退出
2. 重新登录超管账号
3. 重新拉取菜单和权限
4. 确认左侧出现一级菜单 `Agent`
5. 确认菜单位置在 `电子档案` 下方、`系统设置` 上方

## 5. 前端功能验收

1. 打开页面：`/archives/agents`
2. 确认页面无白屏、无接口 401/403
3. 确认 `meta` 接口成功
4. 确认列表接口成功
5. 当前账号具备 `agents:create` 时可创建 Agent
6. 当前账号具备 `agents:publish` 时可发布版本
7. 当前账号具备 `agents:run` 时可手动试跑
8. 当前账号具备 `agents:view_logs` 时可查看运行记录
9. 无对应权限时，按钮应隐藏

## 6. 最小闭环 smoke

建议按下面顺序做一遍最小闭环：

1. `GET /auth/archives/agents/meta`
2. `GET /auth/archives/agents`
3. `POST /auth/archives/agents`
4. `POST /auth/archives/agents/{id}/publish`
5. `POST /auth/archives/agents/{id}/run`
6. `GET /auth/archives/agents/{id}/runs`
7. `GET /auth/archives/agents/runs/{runId}`

通过标准：

- 创建成功
- 发布成功
- 运行记录生成成功
- 运行状态最终进入 `SUCCESS`
- `ea_agent_run_step` 至少生成 `start`、`end` 两步

## 7. 已知补充说明

- `backend/sql/migrate_system_settings.sql` 已修复一处损坏的插入语句，可正常重复执行
- Agent 运行调度已改为通过 `finexAsyncExecutor` 显式投递，避免本机联调时运行记录长期停留在 `PENDING`

