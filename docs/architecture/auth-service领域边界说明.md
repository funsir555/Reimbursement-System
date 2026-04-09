# auth-service 领域边界说明

## 1. 目标

当前不建议立刻拆微服务，但需要先把 `auth-service` 内部边界收紧，避免继续横向堆功能。

## 2. 当前建议领域

### `auth`

职责：

- 登录
- JWT 认证
- 当前用户身份识别
- 权限校验

典型入口：

- `AuthController`
- `AuthInterceptor`
- `UserService`
- `AccessControlService`

### `profile`

职责：

- 个人中心
- 下载中心
- 密码修改
- 银行账户等个人资料展示

典型入口：

- `UserCenterController`
- `UserCenterService`

### `process`

职责：

- 流程模板管理
- 流程设计器
- 费用类型树
- 自定义档案

典型入口：

- `ProcessManagementController`
- `ProcessManagementService`
- `ProcessFlowDesignService`

### `async-task`

职责：

- 下载导出任务提交
- 发票验真任务提交
- OCR 任务提交
- 通知发送与通知摘要

典型入口：

- `AsyncTaskController`
- `AsyncTaskService`
- `AsyncTaskWorker`
- `NotificationService`

### `voucher`

职责：

- 财务凭证录入
- 凭证明细与元数据查询

典型入口：

- `FinanceVoucherController`
- `FinanceVoucherService`

### `settings`

职责：

- 组织、员工、角色、公司主体
- 同步连接器与同步任务
- 系统设置相关权限树

典型入口：

- `SystemSettingsController`
- 对应 settings 服务与 mapper

### `mvp-dashboard`

职责：

- 首页概览
- 报销列表
- 发票列表
- 当前用户简档读取

典型入口：

- `MvpController`
- `MvpDataService`

## 3. 后续新增功能的归属规则

- 新增认证、登录、权限相关能力，优先进入 `auth`
- 新增流程模板、流程图、流程元数据，优先进入 `process`
- 新增导出、通知、外部耗时处理，优先进入 `async-task`
- 新增凭证、总账、会计档案相关能力，优先进入 `voucher`
- 新增组织与主数据治理能力，优先进入 `settings`
- 仅用于首页聚合展示的数据读取，才进入 `mvp-dashboard`

## 4. 代码治理规则

- 不要把新业务继续直接堆进 `AuthController` 或 `MvpController`
- Controller 只负责协议层，不负责复杂编排
- 耗时任务优先走异步执行器，不阻塞请求线程
- 跨领域共享逻辑放在 `common` 或 `support`，不要反向侵入多个 controller

## 5. 当前阶段建议

这一阶段先做“领域收口”，不做“大拆分”：

1. 新代码按领域归位
2. 旧代码逐步顺手搬迁
3. 等边界稳定后，再评估是否拆成独立服务

## 6. Phase 1 治理红线

- `ExpenseDocumentServiceImpl` 进入公开方法冻结期，不再新增新的 public 业务入口
- 新的报销创建、查询、审批、选择器能力先落到拆分后的领域 service，再由旧入口逐步收口
- `frontend/admin-web/src/api/index.ts` 不再新增新的接口定义或业务 API 分组
- 新的前端接口契约先进入按领域拆分的新模块，再视迁移节奏从 `index.ts` 做兼容导出
