// 这里定义 expense-approval-types.ts 相关数据结构。
// 页面与 API 封装会依赖这些类型来约定字段。
// 如果改错，最容易影响列表、表单和接口联调。

import type { MoneyValue } from './core'

export interface ExpenseApprovalPendingItem {
  taskId: number
  documentCode: string
  documentTitle: string
  documentReason?: string
  templateName?: string
  templateType?: string
  templateTypeLabel?: string
  submitterName?: string
  submitterDeptName?: string
  amount: MoneyValue
  nodeKey: string
  nodeName: string
  status: string
  documentStatus?: string
  documentStatusLabel?: string
  submittedAt?: string
  paymentDate?: string
  paymentCompanyName?: string
  payeeName?: string
  counterpartyName?: string
  undertakeDepartmentNames?: string[]
  tagNames?: string[]
  taskCreatedAt?: string
}

export interface ExpenseApprovalActionPayload {
  comment?: string
}

export interface ExpenseTaskTransferPayload {
  targetUserId: number
  remark?: string
}

export interface ExpenseTaskAddSignPayload {
  targetUserId: number
  remark?: string
}

export interface ExpenseActionUserOption {
  userId: number
  name: string
  username?: string
  deptName?: string
  phone?: string
}
