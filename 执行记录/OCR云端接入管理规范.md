# OCR云端接入管理规范

## 当前基线说明

- 当前进度唯一基线：`C:\Users\funsir\Desktop\报销系统\执行记录\报销系统治理落地方案.md`
- 当前专题范围：`系统设置中心 > API接口 > OCR` 与 `发票附件上传后自动 OCR`
- 当前落地口径：首期仅阿里云真实接入；腾讯云、百度云仅保留厂商切换入口与待接入说明
- 当前文案约束：所有 OCR 相关中文 UI、注释、测试文案必须保持 UTF-8，不允许出现 `???`、`�`、mojibake

更新日期：2026-04-19

## 1. 文档定位

本文档是报销系统“OCR 云端接入”的正式管理规范与实施基线，不是原型草图、字段备忘录，也不是临时联调说明。
后续凡涉及 OCR 厂商接入、配置持久化、发票附件识别、错误收口、测试与验收，均必须先对照本规范执行。

本文档当前只规范以下两条链路：
- 系统设置中心 > API接口 > OCR
- 报销单创建/编辑/详情中的发票附件上传后自动识别

本文档当前不展开以下范围：
- 档案发票模块 `InvoiceListView` 的 mock 列表改造
- 发票验真真实接入
- 通用附件 OCR、合同附件 OCR、影像归档 OCR
- 异步 OCR 任务中心、OCR 结果独立业务表、跨模块共享识别结果

自本文档发布之日起，其作为 OCR 云端接入后续实施与修改的唯一专题基线。

## 2. 模块定义

OCR 云端接入是报销系统中的附件识别支撑能力，负责：
- 维护唯一生效的 OCR 厂商配置
- 对发票附件执行真实 OCR
- 将识别结果归一化为稳定快照
- 将快照回写到当前表单的 `invoiceAttachments`

OCR 云端接入当前不负责：
- 验真真伪、重复报销校验、税局查验
- 发票档案落库与跨单据复用
- 多页复杂 PDF 编排识别
- 非发票附件的通用文本抽取

## 3. 核心口径与边界

### 3.1 业务归属口径

OCR 只服务于“发票附件”链路，不得在首期实现中扩散到其它附件组件、档案模块或流程设计模块。
具体口径固定为：
- 只识别 `invoiceAttachments`
- 非发票附件上传成功后不触发 OCR
- 当前创建页、编辑页、详情页复用同一份附件 OCR 快照展示

### 3.2 上传与 OCR 的关系口径

上传成功不等于 OCR 成功。
必须满足以下规则：
- 附件上传成功后，前端立即自动触发 OCR
- OCR 成功时，将结构化快照写回当前附件项
- OCR 失败时，不回滚附件上传，不阻塞用户继续填写表单
- OCR 结果只反映当前附件识别状态，不作为整张单据提交的前置硬校验

### 3.3 厂商启用口径

运行时只允许一个启用中的 OCR 厂商。
具体规则固定为：
- 保存某个厂商并启用时，自动停用其它厂商
- 首期只允许阿里云执行真实保存和测试
- 腾讯云、百度云首期只允许 UI 切换，不允许真实保存、测试与调用

### 3.4 文件范围口径

首期识别范围固定为：
- 图片：`PNG / JPG / JPEG`
- PDF：仅单页 PDF 发票

首期明确不纳入完成口径：
- 多页 PDF
- 文本型 PDF 直提取
- 火车票、机票、出租车票等其它票据专用识别

### 3.5 文案与编码口径

所有 OCR 作用域内文案必须保持 UTF-8 原始中文。
出现以下任一情况，视为阻塞问题，不允许交付：
- `???`
- `�`
- 典型 mojibake，如 `閫`、`鎼`、`缁`

修复或新增中文文案时，必须优先从仓库 `HEAD` 或同域正常组件复制原文，不做二次转码。

## 4. 配置中心规范

### 4.1 页面入口

统一入口固定为：
- 前端：`系统设置中心 > API接口 > OCR`
- 后端接口域：`/auth/system-settings/ocr/providers/**`

“发票验真”“接口文档”仍保留为占位子项，不得与 OCR 配置页混用真实后端。

### 4.2 厂商配置字段

阿里云首期最小配置集固定为：
- `accessKeyId`
- `accessKeySecret`
- `endpoint`
- `connectTimeoutMs`
- `readTimeoutMs`
- `enabled`

其中：
- `accessKeySecret` 只允许写入时提交明文
- 读取时只能返回 `hasSecret` 与 `maskedSecret`
- 默认 `endpoint` 为 `ocr-api.cn-hangzhou.aliyuncs.com`

### 4.3 测试配置规则

系统设置页必须支持“测试配置”。
测试规则固定为：
- 只对阿里云发起真实测试
- 使用系统内置发票样张，不使用业务附件
- 回写最近测试时间、最近测试状态、最近测试消息
- 错误 AK/SK、未配置、超时、厂商异常都必须返回明确提示

## 5. 运行时接口与数据规范

### 5.1 后端公开接口

首期 OCR 相关接口固定为：
- `GET /auth/system-settings/bootstrap` 扩展 `ocrProviders`
- `PUT /auth/system-settings/ocr/providers/{providerCode}`
- `POST /auth/system-settings/ocr/providers/{providerCode}/test`
- `POST /auth/expenses/attachments/{attachmentId}/ocr`

### 5.2 附件 OCR 返回口径

附件级 OCR 归一化结果固定输出：
- `status`
- `providerCode`
- `providerName`
- `requestId`
- `recognizedAt`
- `invoiceCode`
- `invoiceNumber`
- `invoiceDate`
- `invoiceType`
- `sellerName`
- `totalAmount`
- `taxAmount`
- `message`

不允许把完整云厂商原始 payload 直接塞入表单 JSON。

### 5.3 状态口径

首期状态固定为：
- `SUCCESS`
- `UNCONFIGURED`
- `UNSUPPORTED_FILE`
- `TIMEOUT`
- `PROVIDER_ERROR`
- `PARSE_FAILED`
- `FAILED`

前后端必须使用同一套状态码和明确中文 message，不允许页面自行拼接含糊状态。

## 6. 前端展示规范

发票工作台与发票预览必须改为真实 OCR 读取逻辑。
固定规则如下：
- 优先读取 `attachment.ocr`
- 没有 OCR 快照时显示“待识别”
- 未启用厂商时显示“未配置 OCR”
- 识别失败时显示稳定状态与提示
- 不再基于文件名 hash 伪造发票代码、号码、金额、销方、验真状态

创建页、编辑页、详情页必须复用同一套附件 OCR 快照，不得出现同一附件在不同页面显示不同识别结果。

## 7. 数据库与 SQL 规范

### 7.1 配置表

OCR 厂商配置使用独立表：
- `sys_ocr_provider_config`

最小字段固定包括：
- `provider_code`
- `provider_name`
- `enabled`
- `config_json`
- `last_test_at`
- `last_test_status`
- `last_test_message`
- `created_at`
- `updated_at`

### 7.2 SQL 同步要求

每次修改 OCR 配置中心能力时，必须同步检查并补齐：
- `backend/sql/init.sql`
- `backend/sql/migrate_system_settings.sql`
- `backend/sql/refresh_system_settings_permissions.sql`
- `backend/sql/grant_super_admin_all_permissions.sql`

不得只改 Java/Vue，不补权限与初始化脚本。

## 8. 权限规范

OCR 配置中心权限固定为：
- `settings:api_interfaces:view`
- `settings:api_interfaces:ocr_edit`
- `settings:api_interfaces:ocr_test`

前端入口、保存按钮、测试按钮都必须受权限控制，不允许只在前端隐藏而后端不校验。

## 9. 测试规范

首期至少覆盖以下验证：

### 9.1 后端

- `SystemSettingsControllerTest`
- `ExpenseAttachmentControllerTest`
- 阿里云响应归一化测试
- `mvn -q -pl auth-service -DskipTests compile`

### 9.2 前端

- `SystemSettingsView` OCR 配置页测试
- `ExpenseRuntimeFormEditor` 附件上传后自动 OCR 测试
- `ExpenseCreateView` 发票工作台挂载测试
- `expenseInvoicePreview` 真实 OCR 快照展示测试
- `npm run build-only`

### 9.3 文案回归

OCR 相关源码与测试必须做定向乱码扫描，确保不再出现：
- `???`
- `�`
- `閫`
- `鎼`
- `缁`

## 10. 验收口径

首期验收以以下结果为准：
- 系统设置中心可查看、保存、测试阿里云 OCR 配置
- 右上角可切换腾讯云、百度云，并明确显示待接入
- 上传发票附件后自动触发 OCR
- OCR 成功时，发票列表展示真实代码、号码、日期、销方、金额、税额
- OCR 失败时，不回滚上传，页面状态明确
- 现有档案发票 mock 模块和验真 mock 流程不被误改

如后续要扩展到多页 PDF、更多厂商、验真联动或独立 OCR 结果表，必须先更新本规范，再实施代码。
