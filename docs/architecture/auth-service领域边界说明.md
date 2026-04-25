# auth-service 领域边界说明

更新时间：2026-04-24

## 1. 总体原则

- `auth-service` 的目标形态是模块化单体，不是当前阶段的微服务拆分
- 对外 controller URL、service 接口、DTO / VO 协议默认保持稳定
- 已冻结 façade 不再回灌 live business truth
- 新 owner 必须边界清晰，不能把 support / assembler 再养成新的 mega file
- 文档统一使用 UTF-8，文档链本身也是治理对象

## 2. 当前领域边界

### `auth`

职责：登录、鉴权、令牌、用户认证相关能力。
稳定 owner：

- `AuthLoginDomainSupport`
- `AuthAuthorizationDomainSupport`

冻结入口：

- `UserServiceImpl`：frozen facade
- `AccessControlServiceImpl`：companion facade

### `profile`

职责：个人中心、银行卡、下载等用户侧能力。
稳定 owner：

- `ProfileCenterDomainSupport`
- `ProfileBankAccountDomainSupport`
- `ProfileDownloadDomainSupport`

冻结入口：

- `UserCenterServiceImpl`：frozen facade

### `process`

职责：流程中心、模板、表单、费用明细设计、流程设计与审批人解析。
稳定 owner：

- `ProcessCenterDomainSupport`
- `ProcessTemplateDomainSupport`
- `ProcessCustomArchiveDomainSupport`
- `ProcessExpenseTypeDomainSupport`
- `ProcessFormDesignServiceImpl`
- `ProcessExpenseDetailDesignServiceImpl`
- `ProcessFlowMetaSupport`
- `ProcessFlowMutationDomainSupport`
- `ProcessFlowStructureSupport`
- `ProcessFlowQuerySupport`
- `ProcessFlowApproverResolveSupport`

冻结入口：

- `ProcessManagementServiceImpl`：frozen facade
- `ProcessFlowDesignServiceImpl`：thin facade

说明：`ProcessFlowDesignServiceImpl` 已完成本轮收口，当前仅保留 `ProcessFlowDesignService` 的薄委派职责；process flow 真相已下沉到 `com.finex.auth.service.impl.process`。

### `async-task`

职责：异步任务提交、通知、OCR、导出、后台 worker 协调。
稳定 owner：

- `AsyncTaskSubmissionDomainSupport`
- `AsyncTaskNotificationDomainSupport`
- `AsyncTaskWorker`
- `NotificationServiceImpl`

冻结入口：

- `AsyncTaskServiceImpl`：frozen facade

### `voucher`

职责：凭证元数据、查询、写入与 finance context 协同。
稳定 owner：

- `VoucherMetaSupport`
- `VoucherQueryDomainSupport`
- `VoucherMutationDomainSupport`
- `VoucherContextSupport`

冻结入口：

- `FinanceVoucherServiceImpl`：frozen facade
- `FinanceContextServiceImpl`：frozen facade

### `settings`

职责：系统设置 bootstrap、组织、角色、公司与同步能力。
稳定 owner：

- `SettingsBootstrapSupport`
- `SettingsOrganizationDomainSupport`
- `SettingsRoleDomainSupport`
- `SettingsCompanyDomainSupport`
- `SettingsSyncDomainSupport`

冻结入口：

- `SystemSettingsServiceImpl`：frozen facade

### `mvp-dashboard`

职责：当前用户、仪表盘、发票等 MVP 页面残留能力。
稳定 owner：

- `MvpCurrentUserDomainSupport`
- `MvpDashboardDomainSupport`
- `MvpInvoiceDomainSupport`

冻结入口：

- `MvpDataServiceImpl`：frozen facade

## 3. finance 补充说明

### `financesystem`

职责：账套元数据、账套查询、账套任务。
稳定 owner：

- `FinanceAccountSetMetaSupport`
- `FinanceAccountSetQueryDomainSupport`
- `FinanceAccountSetTaskDomainSupport`

冻结入口：

- `FinanceSystemManagementServiceImpl`：frozen facade

### `financearchive`

职责：会计科目、客户、项目、供应商等财务档案能力。
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

## 4. residual 子域补充说明

### `expense-residual`

职责：费用单据运行态、读写协同、支付、核销、summary read-model 与 residual owner 的边界收口。
稳定 owner：

- `ExpenseWorkflowContextSupport`
- `ExpenseWorkflowRuntimeSupport`
- `ExpenseWorkflowRepairSupport`
- `ExpenseDocumentReadSupport`
- `ExpenseDocumentActionLogSupport`
- `ExpenseDocumentTemplateSupport`
- `ExpenseDocumentMutationSupport`
- `ExpensePaymentOrderQuerySupport`
- `ExpenseBankLinkDomainSupport`
- `ExpensePaymentExecutionSupport`
- `ExpensePaymentReceiptSupport`
- `ExpensePaymentRecordSupport`
- `ExpenseWriteOffAmountSupport`
- `ExpenseWriteOffRelationQuerySupport`
- `ExpenseWriteOffRelationMutationSupport`
- `ExpenseDashboardWriteOffSupport`
- `ExpenseSummarySnapshotSupport`
- `ExpenseSummaryLookupSupport`
- `ExpenseSummaryEnrichmentSupport`
- `ExpenseSummaryViewSupport`

冻结入口：

- `ExpenseDocumentServiceImpl`：facade
- `ExpenseWorkflowRuntimeSupport`：runtime facade
- `ExpenseDocumentMutationSupport`：compatibility facade
- `ExpensePaymentDomainSupport`：thin domain coordinator
- `ExpenseRelationWriteOffService`：thin domain coordinator
- `ExpenseSummaryAssembler`：thin summary coordinator

second-wave 既定四个热点已全部完成：

- `ProcessFlowDesignServiceImpl`
- `ExpensePaymentDomainSupport`
- `ExpenseRelationWriteOffService`
- `ExpenseSummaryAssembler`

说明：

- `ExpensePaymentDomainSupport` 已完成收口，payment 真相已下沉到 `order-query / bank-link / execution / receipt / record`
- `ExpenseRelationWriteOffService` 已完成收口，write-off 真相已下沉到 `amount / relation-query / relation-mutation / dashboard-writeoff`
- `ExpenseSummaryAssembler` 已完成收口，summary read-model 真相已下沉到 `snapshot / lookup / enrichment / view`

### `fixedasset`

职责：固定资产分类、卡片建账、变动处置、折旧期间、凭证查询。
稳定 owner：

- `FixedAssetMetaCategorySupport`
- `FixedAssetCardOpeningSupport`
- `FixedAssetChangeDisposalSupport`
- `FixedAssetDepreciationPeriodSupport`
- `FixedAssetVoucherQuerySupport`

冻结入口：

- `FixedAssetServiceImpl`：thin facade

### `expensevoucher`

职责：费用单据生成凭证链路。
稳定 owner：

- `ExpenseVoucherMetaSupport`
- `ExpenseVoucherMappingDomainSupport`
- `ExpenseVoucherPushDomainSupport`
- `ExpenseVoucherRecordQuerySupport`

冻结入口：

- `ExpenseVoucherGenerationServiceImpl`：thin facade

### `archiveagent`

职责：档案 agent 元数据、定义、手动运行、调度运行。
稳定 owner：

- `ArchiveAgentMetaSupport`
- `ArchiveAgentDefinitionDomainSupport`
- `ArchiveAgentRunDomainSupport`
- `ArchiveAgentScheduleDomainSupport`

冻结入口：

- `ArchiveAgentServiceImpl`：thin facade

## 5. 新增功能归属规则

- 新增认证能力优先归入 `auth`
- 个人中心、下载、个人银行卡归入 `profile`
- 流程模板、流程设计、表单设计归入 `process`
- 异步、通知、OCR、导出归入 `async-task`
- 凭证与 finance context 归入 `voucher`
- 设置、组织、角色、公司归入 `settings`
- finance archive 与 financesystem 继续按现有 owner 归属
- expense、fixedasset、expensevoucher、archiveagent 继续在对应残余 owner 中演进

## 6. 代码治理红线

- Controller 不直连 Mapper
- frozen facade 不回灌新逻辑
- 新增 support 只能承接清晰 owner，不能继续长成新大基座
- 新批次没有 compile / focused tests / full test / 文档同步，就不能宣告收口

## 7. 当前阶段与下一步

当前 `auth-service` 的主域边界已经基本成型，阶段判断为：`backend residual hotspot second-wave（既定四批已完成，待进入下一轮 hotspot re-rank）`。

当前下一步：

1. 重新做全项目 hotspot 扫描与 residual re-rank
2. 根据复盘结果决定是继续处理 backend 大基座热点、切回前端大页面热点，还是优先处理文档 / 配置 / 启动链治理

`ProcessFlowDesignServiceImpl`、`ExpensePaymentDomainSupport`、`ExpenseRelationWriteOffService`、`ExpenseSummaryAssembler`、`ExpenseVoucherGenerationServiceImpl` 和 `ArchiveAgentServiceImpl` 已完成收口，不再作为“下一优先级”保留在文档中。