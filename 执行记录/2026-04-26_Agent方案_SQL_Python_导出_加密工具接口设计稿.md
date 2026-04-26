# Agent 方案配套文档二：SQL / Python / 导出 / 加密 四类工具接口设计稿

## 1. 文档目标

本文档定义 Agent 体系下四类核心工具的接口设计：

- SQL 工具
- Python 工具
- 导出工具
- 加密工具

设计目标：

- 统一工具调用协议
- 明确每类工具的输入输出
- 明确安全边界
- 让 Runtime 可以标准化编排和审计

---

## 2. 工具设计总原则

所有工具必须满足：

- 工具由后端注册，不由 LLM 自定义
- 工具只接受结构化参数，不接受任意脚本
- 工具返回统一结构，便于 Runtime 记录步骤日志
- 工具必须可审计、可超时、可限流、可失败回传

统一边界：

- SQL 工具：不接受原始 SQL 文本
- Python 工具：不接受任意 Python 代码
- 导出工具：不接受文件系统任意路径
- 加密工具：不接受用户自定义弱加密参数

---

## 3. 统一工具调用协议

建议所有工具都走统一 envelope。

### 3.1 ToolExecutionRequest

```json
{
  "requestId": "toolreq_20260426_0001",
  "runId": 1001,
  "stepId": 2001,
  "toolCode": "sql.report.expense_detail",
  "toolType": "SQL",
  "operator": {
    "userId": 1,
    "username": "admin"
  },
  "context": {
    "agentId": 10,
    "agentName": "报销导出助手",
    "subject": "expense_documents",
    "taskType": "EXPORT_EXPENSE_REPORT"
  },
  "payload": {}
}
```

字段说明：

- `requestId`
  - 本次工具调用唯一号
- `runId`
  - 所属 AgentRun
- `stepId`
  - 所属 AgentRunStep
- `toolCode`
  - 工具编码
- `toolType`
  - 工具类型：`SQL / PYTHON / EXPORT / ENCRYPT`
- `operator`
  - 当前操作者信息
- `context`
  - Agent 运行上下文
- `payload`
  - 工具入参主体

### 3.2 ToolExecutionResponse

```json
{
  "requestId": "toolreq_20260426_0001",
  "toolCode": "sql.report.expense_detail",
  "success": true,
  "summary": "成功查询 382 条报销记录",
  "outputType": "DATASET",
  "output": {},
  "metrics": {
    "durationMs": 842,
    "rowCount": 382,
    "fileCount": 0
  },
  "warnings": [],
  "error": null
}
```

字段说明：

- `success`
  - 是否成功
- `summary`
  - 给步骤日志展示的摘要
- `outputType`
  - 输出类型：`DATASET / FILE / ARCHIVE / MESSAGE / NONE`
- `output`
  - 真正输出结果
- `metrics`
  - 耗时、行数、文件数量等运行指标
- `warnings`
  - 非阻断告警
- `error`
  - 失败信息

### 3.3 ToolExecutionError

```json
{
  "code": "SQL_TEMPLATE_NOT_FOUND",
  "message": "未找到对应的 SQL 报表模板",
  "retryable": false,
  "details": {
    "toolCode": "sql.report.expense_detail"
  }
}
```

---

## 4. SQL 工具接口设计

## 4.1 目标定位

SQL 工具只负责受控取数，不开放自由 SQL。

适用场景：

- 查询明细数据
- 汇总统计
- 趋势统计
- 固定报表取数

## 4.2 推荐工具编码

- `sql.report.expense_detail`
- `sql.report.expense_summary`
- `sql.report.invoice_detail`
- `sql.report.invoice_summary`
- `sql.report.agent_run_summary`

## 4.3 SQL 工具请求结构

```json
{
  "templateCode": "expense_detail_default",
  "subject": "expense_documents",
  "filters": {
    "dateFrom": "2026-04-01",
    "dateTo": "2026-04-30",
    "companyId": "C001",
    "statusList": ["APPROVED"]
  },
  "columns": [
    "documentCode",
    "applicantName",
    "departmentName",
    "totalAmount",
    "approvedAt"
  ],
  "groupBy": [],
  "metrics": [],
  "sort": {
    "field": "approvedAt",
    "order": "desc"
  },
  "limit": 50000
}
```

## 4.4 SQL 工具响应结构

```json
{
  "datasetId": "dataset_20260426_0001",
  "columns": [
    { "field": "documentCode", "label": "单据编号", "type": "string" },
    { "field": "applicantName", "label": "申请人", "type": "string" },
    { "field": "totalAmount", "label": "金额", "type": "number" },
    { "field": "approvedAt", "label": "审批时间", "type": "datetime" }
  ],
  "rows": [
    {
      "documentCode": "EXP20260401001",
      "applicantName": "张三",
      "totalAmount": 1280.50,
      "approvedAt": "2026-04-02 10:22:33"
    }
  ],
  "summary": {
    "rowCount": 382,
    "truncated": false
  }
}
```

## 4.5 SQL 工具服务端规则

- 必须使用只读数据源
- 必须通过模板映射到预定义 SQL
- 所有变量只能走参数绑定
- `limit` 必须有上限
- 禁止多语句
- 禁止系统表
- 结果集列必须来自白名单

## 4.6 审计字段建议

- `templateCode`
- `subject`
- `filters`
- `columns`
- `limit`
- `generatedSqlDigest`
- `rowCount`
- `durationMs`

---

## 5. Python 工具接口设计

## 5.1 目标定位

Python 工具不是“任意脚本执行器”，而是“预置数据处理器”。

适用场景：

- 透视
- 汇总后再整理
- 多表合并
- Sheet 结构组装
- 文件格式预处理

## 5.2 推荐工具编码

- `python.dataset.pivot`
- `python.dataset.normalize`
- `python.dataset.merge_sheets`
- `python.dataset.enrich_labels`

## 5.3 Python 工具请求结构

```json
{
  "jobCode": "pivot_expense_by_department",
  "inputDataset": {
    "datasetId": "dataset_20260426_0001",
    "columns": [
      "departmentName",
      "status",
      "totalAmount"
    ],
    "rows": []
  },
  "options": {
    "pivot": {
      "index": ["departmentName"],
      "columns": ["status"],
      "values": ["totalAmount"],
      "aggregation": "sum"
    },
    "fillNullWith": 0
  }
}
```

## 5.4 Python 工具响应结构

```json
{
  "datasetId": "dataset_20260426_0002",
  "sheets": [
    {
      "sheetName": "部门状态透视",
      "columns": [
        { "field": "departmentName", "label": "部门", "type": "string" },
        { "field": "APPROVED", "label": "已审批", "type": "number" },
        { "field": "PAID", "label": "已付款", "type": "number" }
      ],
      "rows": []
    }
  ],
  "summary": {
    "sheetCount": 1,
    "rowCount": 20
  }
}
```

## 5.5 Python 工具服务端规则

- 只允许调用注册过的 `jobCode`
- 不接收 Python 代码文本
- 不允许访问任意文件路径
- 不允许联网
- 必须设置超时
- 处理结果必须是结构化数据，不直接落磁盘

## 5.6 推荐运行方式

首版推荐两种方式二选一：

- Java 内嵌轻量处理逻辑，工具名叫 Python，但先不真正起 Python 解释器
- 独立 Python worker，通过固定协议通信

如果上真实 Python runtime，建议隔离：

- 独立进程
- 临时工作目录
- 资源限制
- 白名单 job 注册

---

## 6. 导出工具接口设计

## 6.1 目标定位

导出工具负责把结构化数据集变成最终交付文件。

适用场景：

- Excel 导出
- CSV 导出
- 多 Sheet 导出
- 模板化导出

## 6.2 推荐工具编码

- `export.excel`
- `export.csv`

## 6.3 导出工具请求结构

```json
{
  "format": "xlsx",
  "fileName": "2026年4月已审批报销单.xlsx",
  "templateCode": "expense_detail_default",
  "sheets": [
    {
      "sheetName": "报销明细",
      "columns": [
        { "field": "documentCode", "label": "单据编号", "type": "string" },
        { "field": "applicantName", "label": "申请人", "type": "string" },
        { "field": "totalAmount", "label": "金额", "type": "number" },
        { "field": "approvedAt", "label": "审批时间", "type": "datetime" }
      ],
      "rows": []
    }
  ],
  "options": {
    "autoColumnWidth": true,
    "freezeHeader": true,
    "maxRowsPerSheet": 100000
  }
}
```

## 6.4 导出工具响应结构

```json
{
  "artifactId": "artifact_20260426_0001",
  "artifactType": "FILE",
  "fileName": "2026年4月已审批报销单.xlsx",
  "mediaType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  "storageKey": "agent-artifacts/2026/04/26/run-1001/expense-report.xlsx",
  "fileSize": 245871,
  "checksum": "sha256:xxxxxxxx",
  "sheetCount": 1
}
```

## 6.5 导出工具服务端规则

- 文件名由系统清洗，禁止路径穿越
- 所有导出文件统一进对象存储或受控目录
- 输出必须带 `artifactId` 和 `storageKey`
- 支持模板编号，但不允许用户上传任意模板直接执行

---

## 7. 加密工具接口设计

## 7.1 目标定位

加密工具负责对导出文件进行压缩、密码保护和产物封装。

适用场景：

- 单文件加密压缩
- 多文件打包
- 下载前统一封装

## 7.2 推荐工具编码

- `package.zip_encrypt`

## 7.3 加密工具请求结构

```json
{
  "archiveName": "2026年4月已审批报销单.zip",
  "inputArtifacts": [
    {
      "artifactId": "artifact_20260426_0001",
      "fileName": "2026年4月已审批报销单.xlsx",
      "storageKey": "agent-artifacts/2026/04/26/run-1001/expense-report.xlsx"
    }
  ],
  "encryption": {
    "enabled": true,
    "algorithm": "ZIP_AES_256",
    "passwordMode": "SYSTEM_GENERATED",
    "passwordDelivery": "inbox"
  },
  "options": {
    "deleteSourceAfterPackage": false
  }
}
```

## 7.4 加密工具响应结构

```json
{
  "artifactId": "artifact_20260426_0002",
  "artifactType": "ARCHIVE",
  "fileName": "2026年4月已审批报销单.zip",
  "storageKey": "agent-artifacts/2026/04/26/run-1001/expense-report.zip",
  "fileSize": 128732,
  "encrypted": true,
  "algorithm": "ZIP_AES_256",
  "passwordTicketId": "pwd_20260426_0001",
  "downloadUrl": "/auth/files/agent-artifacts/1001",
  "expiresAt": "2026-04-27 13:00:00"
}
```

## 7.5 加密工具服务端规则

- 密码由系统生成，不接受 LLM 指定密码
- 密码不与下载链接同通道返回
- 加密方式固定白名单，例如 `ZIP_AES_256`
- 下载对象必须可过期、可撤销
- 建议支持单独密码派发记录

---

## 8. 工具注册元数据设计

建议所有工具在系统里有统一元数据。

```json
{
  "toolCode": "sql.report.expense_detail",
  "toolType": "SQL",
  "label": "报销明细查询",
  "description": "按条件查询报销单明细",
  "available": true,
  "requiresCredential": false,
  "timeoutSeconds": 60,
  "idempotent": true,
  "outputType": "DATASET"
}
```

推荐字段：

- `toolCode`
- `toolType`
- `label`
- `description`
- `available`
- `requiresCredential`
- `timeoutSeconds`
- `idempotent`
- `outputType`

---

## 9. Runtime 编排建议

建议 Runtime 按以下规则编排四类工具：

### 9.1 标准链路

```text
SQL -> Python(可选) -> Export -> Encrypt -> Notify
```

### 9.2 输出对接规则

- SQL 工具输出 `DATASET`
- Python 工具输入 `DATASET`，输出 `DATASET`
- 导出工具输入 `DATASET`，输出 `FILE`
- 加密工具输入 `FILE`，输出 `ARCHIVE`

### 9.3 类型兼容矩阵

| 上一步输出 | 下一步允许工具 |
|---|---|
| DATASET | PYTHON / EXPORT |
| FILE | ENCRYPT |
| ARCHIVE | NOTIFY / FILE_CENTER |
| NONE | 无 |

---

## 10. 失败码建议

建议统一错误码前缀：

### SQL
- `SQL_TEMPLATE_NOT_FOUND`
- `SQL_FILTER_INVALID`
- `SQL_COLUMN_NOT_ALLOWED`
- `SQL_LIMIT_EXCEEDED`
- `SQL_READONLY_REQUIRED`

### Python
- `PY_JOB_NOT_FOUND`
- `PY_PAYLOAD_INVALID`
- `PY_RUNTIME_TIMEOUT`
- `PY_RUNTIME_FAILED`

### Export
- `EXPORT_FORMAT_NOT_SUPPORTED`
- `EXPORT_TEMPLATE_NOT_FOUND`
- `EXPORT_DATASET_EMPTY`
- `EXPORT_FILE_WRITE_FAILED`

### Encrypt
- `ENCRYPT_ALGORITHM_NOT_SUPPORTED`
- `ENCRYPT_SOURCE_NOT_FOUND`
- `ENCRYPT_PASSWORD_GENERATE_FAILED`
- `ENCRYPT_PACKAGE_FAILED`

---

## 11. 审计日志设计建议

每次工具调用建议至少记录：

```json
{
  "runId": 1001,
  "stepId": 2001,
  "toolCode": "sql.report.expense_detail",
  "toolType": "SQL",
  "operatorUserId": 1,
  "requestDigest": "sha256:xxxx",
  "responseSummary": "成功查询 382 条记录",
  "durationMs": 842,
  "success": true,
  "createdAt": "2026-04-26 13:00:00"
}
```

如果是导出和加密工具，还应记录：

- `artifactId`
- `storageKey`
- `fileName`
- `fileSize`
- `expiresAt`
- `encrypted`

---

## 12. 首版落地建议

如果你要先做最小闭环，建议首版只实现以下工具：

### SQL
- `sql.report.expense_detail`
- `sql.report.expense_summary`

### Python
- `python.dataset.pivot`

### Export
- `export.excel`

### Encrypt
- `package.zip_encrypt`

这 4 类加起来就足够支撑：

- 用户自然语言提需求
- LLM 输出结构化变量
- SQL 取数
- Python 简单加工
- 导出 Excel
- ZIP 加密
- 发给用户下载

---

## 13. 和当前 Agent 架构的对应关系

你现在已有的思路是：

- `ModelProviderAdapter`
- `ToolExecutor`
- `AgentRun`
- `AgentRunStep`
- `AgentRunArtifact`

所以这份接口稿可以直接映射成：

- SQL / Python / Export / Encrypt 都实现为 `ToolExecutor`
- 每次工具调用都写 `AgentRunStep`
- 文件结果落 `AgentRunArtifact`
- LLM 负责产出 JSON，Runtime 负责把 JSON 转成工具请求

也就是说，这不是另起炉灶，而是在你现在框架上补全“真实执行能力”。

---

## 14. 最终结论

这四类工具的核心设计思想是：

- 对 LLM 开放“能力描述”
- 对执行层开放“结构化参数”
- 对系统内部保留“安全实现细节”

一句话概括：

> 工具不是让模型自由执行，而是让模型在受控能力边界内驱动系统完成任务。