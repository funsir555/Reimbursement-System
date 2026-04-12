// 这里定义 invoice-types.ts 相关数据结构。
// 页面与 API 封装会依赖这些类型来约定字段。
// 如果改错，最容易影响列表、表单和接口联调。

import type { MoneyValue } from './core'

// 这是 InvoiceSummary 的数据结构。
export interface InvoiceSummary {
  code: string
  number: string
  type: string
  seller: string
  amount: MoneyValue
  date: string
  status: string
  ocrStatus: string
}
