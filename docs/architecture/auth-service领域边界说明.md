# auth-service 领域边界说明

当前基线更新时间：2026-04-11

本文用于约束 `auth-service` 的领域边界、冻结入口和 owner 归属。当前 `auth-service` 的正确定位是：单体主服务 + 多子域 owner，而不是继续把新真相直接堆回历史 mega service。

## 1. 总体原则

- 不激进重命名，不先拆微服务
- 已冻结 façade 只保留兼容委派或极薄桥接
- 新真相优先进入清晰 owner，不得回灌旧入口
- 已形成的 `Abstract*Support` 大基座继续受控，但当前不作为下一批首要治理目标

## 2. 当前领域边界

### `auth`

职责：登录、JWT、当前用户身份识别、角色与权限读取。

稳定 owner：

- `AuthLoginDomainSupport`
- `AuthAuthorizationDomainSupport`

冻结入口：

- `UserServiceImpl`：frozen facade
- `AccessControlServiceImpl`：companion facade

### `profile`

职责：个人中心、银行账户、下载中心、密码修改。

稳定 owner：

- `ProfileCenterDomainSupport`
- `ProfileBankAccountDomainSupport`
- `ProfileDownloadDomainSupport`

冻结入口：

- `UserCenterServiceImpl`：frozen facade

### `process`

职责：流程中心、模板、自定义档案、费用类型、表单设计、费用明细设计、流程设计。

稳定 owner：

- `ProcessCenterDomainSupport`
- `ProcessTemplateDomainSupport`
- `ProcessCustomArchiveDomainSupport`
- `ProcessExpenseTypeDomainSupport`
- `ProcessFormDesignServiceImpl`
- `ProcessExpenseDetailDesignServiceImpl`
- `ProcessFlowDesignServiceImpl`

冻结入口：

- `ProcessManagementServiceImpl`：frozen facade

说明：`ProcessFlowDesignServiceImpl` 仍是当前 residual second-wave 的第一优先级，因为它依然是 process 域里最大的 live owner 之一。

### `async-task`

职责：导出、发票 OCR/验真任务提交，通知读侧与执行态协作。

稳定 owner：

- `AsyncTaskSubmissionDomainSupport`
- `AsyncTaskNotificationDomainSupport`
- `AsyncTaskWorker`
- `NotificationServiceImpl`

冻结入口：

- `AsyncTaskServiceImpl`：frozen facade

### `voucher`

职责：凭证元数据、查询、录入与 finance context。

稳定 owner：

- `VoucherMetaSupport`
- `VoucherQueryDomainSupport`
- `VoucherMutationDomainSupport`
- `VoucherContextSupport`

冻结入口：

- `FinanceVoucherServiceImpl`：frozen facade
- `FinanceContextServiceImpl`：frozen facade

### `settings`

职责：系统 bootstrap、组织、角色、公司、同步连接器与同步任务。

稳定 owner：

- `SettingsBootstrapSupport`
- `SettingsOrganizationDomainSupport`
- `SettingsRoleDomainSupport`
- `SettingsCompanyDomainSupport`
- `SettingsSyncDomainSupport`

冻结入口：

- `SystemSettingsServiceImpl`：frozen facade

### `mvp-dashboard`

职责：首页概览、报销列表、发票列表、当前用户摘要读取。

稳定 owner：

- `MvpCurrentUserDomainSupport`
- `MvpDashboardDomainSupport`
- `MvpInvoiceDomainSupport`

冻结入口：

- `MvpDataServiceImpl`：frozen facade

## 3. finance 补充说明

### `financesystem`

职责：账套元数据、账套查询、账套任务编排。

稳定 owner：

- `FinanceAccountSetMetaSupport`
- `FinanceAccountSetQueryDomainSupport`
- `FinanceAccountSetTaskDomainSupport`

冻结入口：

- `FinanceSystemManagementServiceImpl`：frozen facade

### `financearchive`

职责：会计科目、客户、项目分类/项目、供应商档案与 expense-create option。

稳定 owner：

- account-subject：`FinanceAccountSubjectMetaSupport` / `FinanceAccountSubjectQueryDomainSupport` / `FinanceAccountSubjectMutationDomainSupport`
- customer：`FinanceCustomerQueryDomainSupport` / `FinanceCustomerMutationDomainSupport`
- project：`FinanceProjectArchiveMetaSupport` / `FinanceProjectClassDomainSupport` / `FinanceProjectQueryDomainSupport` / `FinanceProjectMutationDomainSupport`
- vendor：`FinanceVendorQueryDomainSupport` / `FinanceVendorMutationDomainSupport` / `FinanceVendorOptionDomainSupport`

冻结入口：

- `FinanceAccountSubjectArchiveServiceImpl`
- `FinanceCustomerServiceImpl`
- `FinanceProjectArchiveServiceImpl`
- `FinanceVendorServiceImpl`

说明：finance archive 主链路已收口完成，不再作为当前 residual first-priority。

## 4. residual 子域补充说明

### `expense-residual`

职责：工作流运行时、文档读侧、模板/编辑上下文、日志、提交流程兼容层。

稳定 owner：

- `AbstractExpenseWorkflowSupport`
- `ExpenseWorkflowContextSupport`
- `ExpenseWorkflowExecutionSupport`
- `ExpenseWorkflowRepairSupport`
- `AbstractExpenseDocumentSupport`
- `ExpenseDocumentReadSupport`
- `ExpenseDocumentActionLogSupport`
- `ExpenseDocumentTemplateDomainSupport`
- `ExpenseDocumentMutationDomainSupport`

冻结入口：

- `ExpenseWorkflowRuntimeSupport`：runtime facade
- `ExpenseDocumentMutationSupport`：compatibility facade

当前 residual second-wave 仍重点关注：

- `ExpensePaymentDomainSupport`
- `ExpenseRelationWriteOffService`
- `ExpenseSummaryAssembler`

### `fixedasset`

职责：固定资产元数据/类别、卡片与期初、变动与处置、折旧与期间关闭、凭证联查。

稳定 owner：

- `FixedAssetMetaCategorySupport`
- `FixedAssetCardOpeningSupport`
- `FixedAssetChangeDisposalSupport`
- `FixedAssetDepreciationPeriodSupport`
- `FixedAssetVoucherQuerySupport`

冻结入口：

- `FixedAssetServiceImpl`：thin facade

### `expensevoucher`

职责：报销凭证生成元数据、映射策略、推送执行、已生成记录查询。

稳定 owner：

- `ExpenseVoucherMetaSupport`
- `ExpenseVoucherMappingDomainSupport`
- `ExpenseVoucherPushDomainSupport`
- `ExpenseVoucherRecordQuerySupport`

冻结入口：

- `ExpenseVoucherGenerationServiceImpl`：thin facade

### `archiveagent`

职责：agent 元数据、定义与版本、手动运行、调度运行、运行记录读侧。

稳定 owner：

- `ArchiveAgentMetaSupport`
- `ArchiveAgentDefinitionDomainSupport`
- `ArchiveAgentRunDomainSupport`
- `ArchiveAgentScheduleDomainSupport`

执行态 owner 继续在 `com.finex.auth.support.archiveagent`。

冻结入口：

- `ArchiveAgentServiceImpl`：thin facade

## 5. 新增功能归属规则

- 登录、权限、当前用户身份相关能力优先进入 `auth`
- 个人中心、下载中心、个人账户相关能力优先进入 `profile`
- 模板、流程、表单、费用类型相关能力优先进入 `process`
- 导出、通知、异步提交相关能力优先进入 `async-task`
- 凭证与 finance context 相关能力优先进入 `voucher`
- 组织、角色、公司、同步相关能力优先进入 `settings`
- 首页聚合只读能力进入 `mvp-dashboard`
- finance archive、expense residual、fixedasset、expensevoucher、archiveagent 按既有子域 owner 继续下沉，不回流 façade

## 6. 代码治理红线

- 不要把新业务继续堆进已冻结入口
- Controller 只负责协议层
- 共享逻辑放到明确 owner 或 `common`，不要反向侵入多个 service
- 已进入 frozen facade 阶段的类只保留兼容委派，不再承接 live business truth

## 7. 当前阶段与下一批

当前 `auth-service` 的主域边界已经基本成型，阶段判断为：`backend residual hotspot second-wave`。

当前建议顺位：

1. `ProcessFlowDesignServiceImpl`
2. `ExpensePaymentDomainSupport`
3. `ExpenseRelationWriteOffService`
4. `ExpenseSummaryAssembler`

`ExpenseVoucherGenerationServiceImpl` 和 `ArchiveAgentServiceImpl` 已完成收口，不再作为“下一优先级”保留在文档中。
