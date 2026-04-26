# 2026-04-26 impl 目录后端 hotspot re-rank 基线

更新时间：2026-04-26

## 1. 本轮目标

- 只针对 `backend/auth-service/src/main/java/com/finex/auth/service/impl/` 做一次后端 hotspot 重排。
- 目标不是立刻启动大重构，而是形成下一阶段的正式治理基线：哪些类必须冻结新增逻辑、哪些类应进入下一批收口、哪些类暂时只做观察。
- 本文是《2026-04-24_项目长期架构治理工作流.md》在 `impl` 目录上的专项落地，不替代全项目热点清单。

## 2. 扫描范围与方法

### 2.1 扫描范围

- 扫描路径：`backend/auth-service/src/main/java/com/finex/auth/service/impl/`
- 覆盖内容：该目录下全部 `.java` 文件及其子目录
- 明确排除：`src/test`、`target`、`dist`、`node_modules`、运行时产物

### 2.2 评分维度

本轮 re-rank 不只看行数，综合采用以下维度判断：

1. 文件体量：行数、import 面、方法数是否显著超标
2. live truth 集中度：是否承载大量真实业务判定与状态迁移
3. 跨域耦合度：是否横跨多个子域、mapper、DTO/VO、外部协作点
4. 回归风险：改动后是否容易影响审批、支付、凭证、组织等主链路
5. 治理收益：是否适合继续沿用“thin facade / thin coordinator + owner 下沉”的治理模式

### 2.3 判断口径

- `P0`：已形成明显 mega support / mega base，必须继续治理，并冻结新增真相流入
- `P1`：已出现明显膨胀，建议作为后续主批次或紧跟批次治理
- `P2`：存在热点征兆，但短期先观察或按配套批次治理
- `Watchlist`：暂不立即开批，但要防止继续变胖

## 3. 当前事实基线

### 3.1 impl 目录总体规模

- 文件数：`170`
- 总行数：`42,901`
- `>=300` 行文件数：`33`
- `>=500` 行文件数：`18`
- `>=800` 行文件数：`8`
- `>=1000` 行文件数：`7`

结论：

- `impl` 当前已经不是“少数几个大文件”，而是进入“热点簇”阶段。
- 风险核心不只是大文件数量，而是多处 `Abstract*Support` 再次承担 live business truth，呈现二次屎山化回流迹象。

### 3.2 子域体量分布

| 子域 | 文件数 | 总行数 | >=500 行 | >=1000 行 | 判断 |
| --- | ---: | ---: | ---: | ---: | --- |
| `expense` | 51 | 19,705 | 8 | 2 | 当前第一风险域，体量与业务真相集中度都最高 |
| `(root)` | 39 | 7,595 | 4 | 0 | 以 facade / worker / coordinator 为主，需防止重新变胖 |
| `process` | 11 | 4,976 | 2 | 1 | 跨模板、流程、档案、类型的耦合面偏宽 |
| `settings` | 7 | 2,919 | 1 | 1 | 工程与组织真相混合，适合继续收口 |
| `financearchive` | 16 | 2,791 | 1 | 0 | 有热点，但风险低于主线域 |
| `fixedasset` | 7 | 2,623 | 1 | 1 | 已形成完整子系统级 support 膨胀 |
| `voucher` | 5 | 2,550 | 1 | 1 | 财务主规则集中，回归风险高 |
| `expensevoucher` | 5 | 1,704 | 1 | 1 | 偏桥接生成链，属后续梯队 |

## 4. Top hotspot 清单

按当前代码状态统计，`impl` 目录行数 Top 20 如下：

| 排名 | 文件 | 行数 | import | 方法数 | 初步判断 |
| --- | --- | ---: | ---: | ---: | --- |
| 1 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/expense/AbstractExpenseDocumentSupport.java` | 5422 | 126 | 206 | `P0` |
| 2 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/expense/AbstractExpenseWorkflowSupport.java` | 4248 | 45 | 89 | `P0` |
| 3 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/process/AbstractProcessManagementSupport.java` | 2811 | 91 | 162 | `P0` |
| 4 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/fixedasset/AbstractFixedAssetSupport.java` | 2235 | 79 | 122 | `P1` |
| 5 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/voucher/AbstractFinanceVoucherSupport.java` | 2141 | 55 | 111 | `P1` |
| 6 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/settings/AbstractSystemSettingsDomainSupport.java` | 2093 | 73 | 111 | `P1` |
| 7 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/expensevoucher/AbstractExpenseVoucherGenerationSupport.java` | 1085 | 55 | 65 | `P2` |
| 8 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/financearchive/AbstractFinanceAccountSubjectArchiveSupport.java` | 956 | 29 | 54 | `P2` |
| 9 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/expense/ExpenseApprovalProjectionSupport.java` | 810 | 26 | 32 | `P2` |
| 10 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/expense/ExpenseDocumentPdfRenderer.java` | 807 | 27 | 65 | `P2` |
| 11 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/ExpenseDetailSystemFieldSupport.java` | 627 | 18 | 38 | `Watchlist` |
| 12 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/expense/ExpenseApprovalDomainSupport.java` | 625 | 33 | 33 | `Watchlist` |
| 13 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/expense/AbstractExpensePaymentSupport.java` | 603 | 32 | 35 | `Watchlist` |
| 14 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/process/AbstractProcessFlowDesignSupport.java` | 588 | 43 | 46 | `Watchlist` |
| 15 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/FinanceAccountSetTaskWorker.java` | 578 | 34 | 24 | `Watchlist` |
| 16 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/expense/ExpenseQueryDomainSupport.java` | 548 | 34 | 26 | `Watchlist` |
| 17 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/AsyncTaskWorker.java` | 545 | 34 | 26 | `Watchlist` |
| 18 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/ProcessManagementServiceImpl.java` | 542 | 52 | 45 | `Watchlist` |
| 19 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/expense/AbstractExpenseRelationWriteOffSupport.java` | 542 | 25 | 25 | `Watchlist` |
| 20 | `backend/auth-service/src/main/java/com/finex/auth/service/impl/SystemSettingsServiceImpl.java` | 498 | 49 | 36 | `Watchlist` |

## 5. 正式 re-rank 结果

### 5.1 `P0`：必须继续治理的主热点

#### 1. `backend/auth-service/src/main/java/com/finex/auth/service/impl/expense/AbstractExpenseDocumentSupport.java`

- 当前全仓最典型的 live-truth mega support。
- import 面极宽，横跨报销单据、审批、支付、模板、流程、供应商、部门、银行记录等多条主链。
- 体量、方法数、耦合面都已明显超出“共享 support”合理范围。
- 下一阶段建议：按 `document-query / document-mutation / submit / edit-context / payee-vendor / attachment / summary-read` 等 owner 再次下沉，`Abstract*Support` 只保留底层 helper。

#### 2. `backend/auth-service/src/main/java/com/finex/auth/service/impl/process/AbstractProcessManagementSupport.java`

- 当前 process 域最明显的二次 mega base。
- 同时牵涉流程中心、模板、费用类型、自定义档案、表单/流程设计等多块真相。
- 风险在于跨域边界太宽，容易把已拆开的 process owner 再次吸回统一大基座。
- 下一阶段建议：按 `template / expense-type / custom-archive / process-center / publish-state / option-loader` 再拆 owner，并冻结新逻辑继续流入该类。

#### 3. `backend/auth-service/src/main/java/com/finex/auth/service/impl/expense/AbstractExpenseWorkflowSupport.java`

- 审批流转、节点推进、任务状态、权限判断等运行态规则高度集中。
- 文件体量极大，属于改一处容易牵动审批主链的高风险热点。
- 下一阶段建议：继续按 `workflow-query / action-execution / node-state / permission-resolve / repair-support` 拆分，并将该类降为共享底座。

### 5.2 `P1`：后续紧跟批次热点

#### 4. `backend/auth-service/src/main/java/com/finex/auth/service/impl/fixedasset/AbstractFixedAssetSupport.java`

- 已经接近“固定资产子系统全部堆进一个 support”。
- 卡片、变更、处置、折旧、期末、凭证联查等职责混住。
- 建议在 `expense / process` 主线后作为优先批次推进。

#### 5. `backend/auth-service/src/main/java/com/finex/auth/service/impl/voucher/AbstractFinanceVoucherSupport.java`

- 凭证录入、分录、辅助核算、现金流、保存校验等财务主规则集中。
- 财务域对兼容性和数据正确性要求高，越晚治理，回归成本越高。
- 建议采用 `meta / row-mutation / assist-cashflow / validation-save / query-view` owner 模式逐步拆开。

#### 6. `backend/auth-service/src/main/java/com/finex/auth/service/impl/settings/AbstractSystemSettingsDomainSupport.java`

- 组织、角色、公司、员工、银行账户、同步连接器等真相混在一起。
- 虽然用户侧运行链爆炸半径低于 expense，但工程耦合度很高。
- 建议结合已完成的前端 `SystemSettingsView.vue` 收口成果，后端继续按 `organization / role / company / company-account / connector-sync / bootstrap` 归位 owner。

### 5.3 `P2`：观察后按配套批次治理

1. `backend/auth-service/src/main/java/com/finex/auth/service/impl/expensevoucher/AbstractExpenseVoucherGenerationSupport.java`
2. `backend/auth-service/src/main/java/com/finex/auth/service/impl/financearchive/AbstractFinanceAccountSubjectArchiveSupport.java`
3. `backend/auth-service/src/main/java/com/finex/auth/service/impl/expense/ExpenseApprovalProjectionSupport.java`
4. `backend/auth-service/src/main/java/com/finex/auth/service/impl/expense/ExpenseDocumentPdfRenderer.java`

判断口径：

- 已出现明显体量热点，但要么偏桥接生成链，要么偏 projection / renderer / read-model，优先级低于真正的 live truth mega base。
- 不建议现在并发开太多批次；更适合作为主域治理时的陪跑项或下一轮候选。

### 5.4 `Watchlist`：冻结新增流入，暂不开主批

1. `backend/auth-service/src/main/java/com/finex/auth/service/impl/ExpenseDetailSystemFieldSupport.java`
2. `backend/auth-service/src/main/java/com/finex/auth/service/impl/expense/ExpenseApprovalDomainSupport.java`
3. `backend/auth-service/src/main/java/com/finex/auth/service/impl/expense/AbstractExpensePaymentSupport.java`
4. `backend/auth-service/src/main/java/com/finex/auth/service/impl/process/AbstractProcessFlowDesignSupport.java`
5. `backend/auth-service/src/main/java/com/finex/auth/service/impl/FinanceAccountSetTaskWorker.java`
6. `backend/auth-service/src/main/java/com/finex/auth/service/impl/AsyncTaskWorker.java`
7. `backend/auth-service/src/main/java/com/finex/auth/service/impl/ProcessManagementServiceImpl.java`
8. `backend/auth-service/src/main/java/com/finex/auth/service/impl/expense/AbstractExpenseRelationWriteOffSupport.java`
9. `backend/auth-service/src/main/java/com/finex/auth/service/impl/SystemSettingsServiceImpl.java`

原则：

- 这些类不一定需要立刻再开一批，但必须防止重新长成新一轮 mega facade / mega support。
- 之后若行数、import 面、方法数继续明显上涨，应自动提升优先级。

## 6. 正式治理顺位建议

当前建议的后端 `impl` 主批次顺位如下：

1. `AbstractExpenseDocumentSupport`
2. `AbstractProcessManagementSupport`
3. `AbstractExpenseWorkflowSupport`
4. `AbstractFixedAssetSupport`
5. `AbstractFinanceVoucherSupport`
6. `AbstractSystemSettingsDomainSupport`

解释：

- `expense` 是当前最重风险源：体量最大、主链最密、运行态真相最集中。
- `process` 的核心风险是跨域耦合和边界回流，不尽快冻结会反向污染已完成的 process owner 收口成果。
- `fixedasset / voucher / settings` 都已具备完整子系统 support 膨胀特征，但优先级略低于 expense/process 主线。

## 7. 实施纪律

### 7.1 从本轮开始的硬约束

- 新业务规则禁止继续写入上述 `P0 / P1` 热点类。
- 若必须兼容旧入口，只允许保留 thin facade / thin coordinator / thin base，不允许继续叠加 live truth。
- 每开一批，都必须先补 focused tests，再迁移真相，再冻结旧入口。

### 7.2 推荐治理模式

继续沿用已验证有效的模式：

1. 保留现有 public facade / controller / service 契约不变
2. 将 live truth 下沉到更清晰的 domain owner / support owner
3. `Abstract*Support` 只保留跨 owner 共用的底层 helper
4. 用 focused tests + full test 守住收口验收

### 7.3 当前不建议做的事

- 不建议一次性全量重写 `impl`
- 不建议为了“看起来更先进”而直接切微服务
- 不建议在同一批里并发处理多个 `P0` 热点，避免回归面失控

## 8. 本轮结论

- `backend/auth-service/src/main/java/com/finex/auth/service/impl/` 当前仍然存在明确的屎山化征兆，且属于需要持续治理的状态。
- 当前最关键的问题不是“文件大”，而是 `Abstract*Support` 再次汇聚业务真相，形成第二轮 mega support 回流。
- 正式基线已确定：下一阶段后端 `impl` 治理优先顺位固定为
  1. `AbstractExpenseDocumentSupport`
  2. `AbstractProcessManagementSupport`
  3. `AbstractExpenseWorkflowSupport`
  4. `AbstractFixedAssetSupport`
  5. `AbstractFinanceVoucherSupport`
  6. `AbstractSystemSettingsDomainSupport`
- 如果业务开发要继续并行推进，可以继续开发，但必须执行“冻结新增真相流入热点类”的纪律；否则后续回归成本会继续上升。
