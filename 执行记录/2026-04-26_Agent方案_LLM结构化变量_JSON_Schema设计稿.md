# Agent 方案配套文档一：LLM 输出结构化变量 JSON Schema 设计稿

## 1. 文档目标

本文档用于约束云端大模型在 Agent 执行链路中的输出格式。

目标不是让大模型直接输出 SQL、Python 或文件路径，而是让大模型只输出“结构化任务变量”，由后端 Runtime 和 Tool Executor 继续处理。

核心原则：

- LLM 只负责意图理解和参数生成
- Runtime 负责 Schema 校验、权限校验、默认值补齐
- Tool 负责真实执行
- 不允许 LLM 直接输出任意 SQL 或任意 Python

---

## 2. 适用场景

本 Schema 主要覆盖以下几类任务：

- 导出类任务
  - 例如：导出报销单、导出发票明细、导出档案数据
- 统计类任务
  - 例如：统计近 30 天报销总额、按状态分布统计、按月份趋势统计
- 文件产出类任务
  - 例如：生成 Excel、生成 CSV、压缩包加密
- 组合型任务
  - 例如：先查数，再透视，再导出，再压缩加密

---

## 3. 总体结构

LLM 输出统一采用单一 JSON 对象，顶层结构如下：

```json
{
  "schemaVersion": "1.0",
  "taskType": "EXPORT_EXPENSE_REPORT",
  "subject": "expense_documents",
  "intent": "export_report",
  "authScope": "USER_ORG_ALLOWED_SCOPE",
  "filters": {},
  "groupBy": [],
  "columns": [],
  "metrics": [],
  "postProcess": {},
  "output": {},
  "toolPlan": [],
  "explanation": ""
}
```

字段职责：

- `schemaVersion`
  - 当前协议版本
- `taskType`
  - 任务类型编码，供 Runtime 路由
- `subject`
  - 数据主题
- `intent`
  - 本次操作意图，例如统计、导出、汇总、明细
- `authScope`
  - LLM 理解到的访问范围，最终仍以服务端权限为准
- `filters`
  - 筛选条件
- `groupBy`
  - 分组维度
- `columns`
  - 明细导出列
- `metrics`
  - 聚合指标
- `postProcess`
  - 排序、透视、重命名、sheet 组织等后处理要求
- `output`
  - 输出文件需求
- `toolPlan`
  - 期望工具链，仅作建议，最终由 Runtime 决定
- `explanation`
  - 给日志和调试看的简短解释

---

## 4. 顶层 JSON Schema

建议采用 JSON Schema Draft 2020-12。

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://finex.local/schemas/agent-intent.schema.json",
  "title": "AgentIntentPayload",
  "type": "object",
  "additionalProperties": false,
  "required": [
    "schemaVersion",
    "taskType",
    "subject",
    "intent",
    "filters",
    "groupBy",
    "columns",
    "metrics",
    "postProcess",
    "output",
    "toolPlan",
    "explanation"
  ],
  "properties": {
    "schemaVersion": {
      "type": "string",
      "const": "1.0"
    },
    "taskType": {
      "type": "string",
      "enum": [
        "EXPORT_EXPENSE_REPORT",
        "EXPORT_INVOICE_DETAIL",
        "EXPORT_ARCHIVE_DATA",
        "QUERY_EXPENSE_SUMMARY",
        "QUERY_INVOICE_SUMMARY",
        "QUERY_AGENT_RUN_SUMMARY"
      ]
    },
    "subject": {
      "type": "string",
      "enum": [
        "expense_documents",
        "invoices",
        "archives",
        "agent_runs"
      ]
    },
    "intent": {
      "type": "string",
      "enum": [
        "export_report",
        "summary_stats",
        "detail_query",
        "trend_analysis",
        "distribution_analysis"
      ]
    },
    "authScope": {
      "type": "string",
      "enum": [
        "CURRENT_USER_ONLY",
        "CURRENT_COMPANY_ONLY",
        "USER_ORG_ALLOWED_SCOPE",
        "SYSTEM_ENFORCED"
      ],
      "default": "SYSTEM_ENFORCED"
    },
    "filters": {
      "$ref": "#/$defs/filters"
    },
    "groupBy": {
      "type": "array",
      "items": {
        "type": "string"
      },
      "maxItems": 5
    },
    "columns": {
      "type": "array",
      "items": {
        "type": "string"
      },
      "maxItems": 50
    },
    "metrics": {
      "type": "array",
      "items": {
        "$ref": "#/$defs/metric"
      },
      "maxItems": 10
    },
    "postProcess": {
      "$ref": "#/$defs/postProcess"
    },
    "output": {
      "$ref": "#/$defs/output"
    },
    "toolPlan": {
      "type": "array",
      "items": {
        "$ref": "#/$defs/toolPlanItem"
      },
      "maxItems": 10
    },
    "explanation": {
      "type": "string",
      "maxLength": 500
    }
  },
  "$defs": {
    "filters": {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "dateFrom": {
          "type": "string",
          "format": "date"
        },
        "dateTo": {
          "type": "string",
          "format": "date"
        },
        "companyId": {
          "type": "string",
          "maxLength": 64
        },
        "departmentId": {
          "type": "string",
          "maxLength": 64
        },
        "applicantId": {
          "type": "string",
          "maxLength": 64
        },
        "statusList": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "maxItems": 20
        },
        "keyword": {
          "type": "string",
          "maxLength": 100
        },
        "documentCodes": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "maxItems": 100
        },
        "invoiceTypes": {
          "type": "array",
          "items": {
            "type": "string"
          },
          "maxItems": 20
        },
        "custom": {
          "type": "object",
          "additionalProperties": {
            "type": ["string", "number", "boolean", "array", "object", "null"]
          }
        }
      }
    },
    "metric": {
      "type": "object",
      "additionalProperties": false,
      "required": ["name", "aggregation"],
      "properties": {
        "name": {
          "type": "string",
          "maxLength": 64
        },
        "aggregation": {
          "type": "string",
          "enum": ["count", "sum", "avg", "min", "max", "distinct_count"]
        },
        "field": {
          "type": "string",
          "maxLength": 64
        },
        "alias": {
          "type": "string",
          "maxLength": 64
        }
      }
    },
    "postProcess": {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "sortBy": {
          "type": "string",
          "maxLength": 64
        },
        "sortOrder": {
          "type": "string",
          "enum": ["asc", "desc"]
        },
        "limit": {
          "type": "integer",
          "minimum": 1,
          "maximum": 100000
        },
        "needPivot": {
          "type": "boolean"
        },
        "pivot": {
          "type": "object",
          "additionalProperties": false,
          "properties": {
            "index": {
              "type": "array",
              "items": { "type": "string" },
              "maxItems": 10
            },
            "columns": {
              "type": "array",
              "items": { "type": "string" },
              "maxItems": 10
            },
            "values": {
              "type": "array",
              "items": { "type": "string" },
              "maxItems": 10
            }
          }
        },
        "sheetName": {
          "type": "string",
          "maxLength": 31
        }
      }
    },
    "output": {
      "type": "object",
      "additionalProperties": false,
      "required": ["format", "encrypt", "zip"],
      "properties": {
        "format": {
          "type": "string",
          "enum": ["xlsx", "csv", "zip"]
        },
        "fileName": {
          "type": "string",
          "maxLength": 128
        },
        "encrypt": {
          "type": "boolean"
        },
        "zip": {
          "type": "boolean"
        },
        "passwordDelivery": {
          "type": "string",
          "enum": ["none", "inbox", "sms", "email", "manual"]
        },
        "templateCode": {
          "type": "string",
          "maxLength": 64
        }
      }
    },
    "toolPlanItem": {
      "type": "object",
      "additionalProperties": false,
      "required": ["toolCode", "purpose"],
      "properties": {
        "toolCode": {
          "type": "string",
          "maxLength": 64
        },
        "purpose": {
          "type": "string",
          "maxLength": 120
        },
        "required": {
          "type": "boolean",
          "default": true
        }
      }
    }
  }
}
```

---

## 5. 运行时强校验规则

JSON Schema 只做第一层校验，Runtime 还需要做第二层业务校验。

### 5.1 顶层规则

- `taskType` 必须和 `subject` 匹配
- `intent` 必须落在允许集合里
- `output.format` 不能和 `zip/encrypt` 组合冲突
- `columns`、`groupBy`、`metrics` 不能全部为空
- `filters.dateFrom <= filters.dateTo`

### 5.2 主题白名单规则

按 `subject` 绑定允许字段：

- `expense_documents`
  
  - 可用列：`documentCode`, `applicantName`, `departmentName`, `totalAmount`, `status`, `approvedAt`, `submitAt`
  - 可用分组：`status`, `departmentName`, `month`
  - 可用指标：`count`, `sum(totalAmount)`, `avg(totalAmount)`

- `invoices`
  
  - 可用列：`invoiceCode`, `invoiceType`, `taxAmount`, `amount`, `verifyStatus`, `invoiceDate`
  - 可用分组：`invoiceType`, `verifyStatus`, `month`
  - 可用指标：`count`, `sum(amount)`, `sum(taxAmount)`

- `agent_runs`
  
  - 可用列：`runNo`, `status`, `triggerType`, `startedAt`, `durationMs`
  - 可用分组：`status`, `triggerType`, `day`
  - 可用指标：`count`, `avg(durationMs)`

### 5.3 禁止项

以下内容禁止由 LLM 输出并进入执行层：

- 原始 SQL 字段
- Python 代码字段
- shell 命令字段
- 文件系统绝对路径
- 数据库连接信息
- 任意 HTTP URL 作为执行目标

如果 LLM 输出这些内容，Runtime 直接拒绝执行。

---

## 6. 推荐任务类型枚举

建议首版固定为以下枚举：

```json
[
  "EXPORT_EXPENSE_REPORT",
  "EXPORT_INVOICE_DETAIL",
  "EXPORT_ARCHIVE_DATA",
  "QUERY_EXPENSE_SUMMARY",
  "QUERY_INVOICE_SUMMARY",
  "QUERY_AGENT_RUN_SUMMARY"
]
```

说明：

- `EXPORT_*`
  - 最终目标是文件产出
- `QUERY_*`
  - 最终目标是结构化结果或页面展示，不一定导出文件

---

## 7. 推荐数据主题枚举

```json
[
  "expense_documents",
  "invoices",
  "archives",
  "agent_runs"
]
```

后续新增主题时，必须同步补：

- 字段白名单
- 指标白名单
- 分组白名单
- SQL 模板映射
- 前端 metadata

---

## 8. 典型样例一：导出已审批报销单

```json
{
  "schemaVersion": "1.0",
  "taskType": "EXPORT_EXPENSE_REPORT",
  "subject": "expense_documents",
  "intent": "export_report",
  "authScope": "USER_ORG_ALLOWED_SCOPE",
  "filters": {
    "dateFrom": "2026-04-01",
    "dateTo": "2026-04-30",
    "statusList": ["APPROVED"],
    "companyId": "C001"
  },
  "groupBy": [],
  "columns": [
    "documentCode",
    "applicantName",
    "departmentName",
    "totalAmount",
    "status",
    "approvedAt"
  ],
  "metrics": [],
  "postProcess": {
    "sortBy": "approvedAt",
    "sortOrder": "desc",
    "limit": 50000,
    "sheetName": "已审批报销单"
  },
  "output": {
    "format": "xlsx",
    "fileName": "2026年4月已审批报销单.xlsx",
    "encrypt": true,
    "zip": true,
    "passwordDelivery": "inbox",
    "templateCode": "expense_detail_default"
  },
  "toolPlan": [
    {
      "toolCode": "sql.report.expense_detail",
      "purpose": "查询报销单明细"
    },
    {
      "toolCode": "export.excel",
      "purpose": "生成 Excel 文件"
    },
    {
      "toolCode": "package.zip_encrypt",
      "purpose": "压缩并加密导出文件"
    }
  ],
  "explanation": "用户要求导出 2026 年 4 月已审批报销单，并需要加密压缩后交付。"
}
```

---

## 9. 典型样例二：统计近 30 天报销金额趋势

```json
{
  "schemaVersion": "1.0",
  "taskType": "QUERY_EXPENSE_SUMMARY",
  "subject": "expense_documents",
  "intent": "trend_analysis",
  "authScope": "USER_ORG_ALLOWED_SCOPE",
  "filters": {
    "dateFrom": "2026-03-27",
    "dateTo": "2026-04-26",
    "companyId": "C001"
  },
  "groupBy": ["month"],
  "columns": [],
  "metrics": [
    {
      "name": "expense_count",
      "aggregation": "count",
      "alias": "单据数"
    },
    {
      "name": "expense_total_amount",
      "aggregation": "sum",
      "field": "totalAmount",
      "alias": "总金额"
    }
  ],
  "postProcess": {
    "sortBy": "month",
    "sortOrder": "asc",
    "limit": 1000
  },
  "output": {
    "format": "xlsx",
    "fileName": "近30天报销趋势.xlsx",
    "encrypt": false,
    "zip": false,
    "passwordDelivery": "none",
    "templateCode": "expense_trend_default"
  },
  "toolPlan": [
    {
      "toolCode": "sql.report.expense_summary",
      "purpose": "查询报销趋势统计"
    }
  ],
  "explanation": "用户希望查看近 30 天报销单据数量和总金额趋势。"
}
```

---

## 10. 推荐 Prompt 输出约束

为了让模型稳定输出该 Schema，建议系统 Prompt 明确约束：

```text
你是一个企业数据导出 Agent 的意图解析器。
你的唯一任务是把用户自然语言请求转换成严格合法的 JSON。
禁止输出 SQL、禁止输出 Python、禁止输出解释性散文。
只能输出一个 JSON 对象，字段必须符合既定 Schema。
如果用户意图不明确，优先补默认值；若仍无法确定，输出最保守的结构化结果，并在 explanation 中说明不确定点。
```

再补一个格式约束：

```text
输出必须满足：
- 只能输出 JSON
- 不要使用 Markdown 代码块
- 不要输出额外说明
- 所有字段名严格使用英文小驼峰或固定枚举
```

---

## 11. 运行时落地建议

建议 Runtime 对 LLM 输出按以下顺序处理：

1. JSON 解析
2. JSON Schema 校验
3. 业务枚举校验
4. 字段白名单校验
5. 权限范围重写
6. 默认值补齐
7. 生成 ToolRequest
8. 写入 AgentRunStep 调试日志

也就是说：

- LLM 输出只是草案
- Runtime 才是最终参数装配者

---

## 12. 版本演进建议

### V1

- 只支持固定主题
- 只支持固定任务类型
- 只支持固定输出格式
- 不支持嵌套复杂条件

### V1.1

- 支持 `and/or` 条件组合
- 支持多 Sheet 输出
- 支持导出模板编号

### V2

- 支持主题注册中心
- 支持更细的权限语义
- 支持复杂后处理 DSL，但仍不开放任意代码执行

---

## 13. 最终结论

这份 JSON Schema 的定位不是“表达所有可能需求”，而是“把 LLM 变成可控的意图生成器”。

一句话概括：

> LLM 输出任务变量，Runtime 把变量变成安全的工具调用。