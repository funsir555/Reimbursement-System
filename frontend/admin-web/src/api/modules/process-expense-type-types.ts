// 这里定义 process-expense-type-types.ts 相关数据结构。
// 页面与 API 封装会依赖这些类型来约定字段。
// 如果改错，最容易影响列表、表单和接口联调。

import type { ProcessFormOption } from './process-template-types'

// 这是 ProcessExpenseTypeTreeNode 的数据结构。
export interface ProcessExpenseTypeTreeNode {
  id: number
  parentId?: number
  expenseCode: string
  expenseName: string
  status: number
  children: ProcessExpenseTypeTreeNode[]
}

export interface ProcessExpenseTypeConfigOption {
  value: string
  label: string
  description: string
}

export interface ProcessExpenseTypeDetail {
  id?: number
  parentId?: number
  expenseCode: string
  expenseName: string
  expenseDescription?: string
  codeLevel?: number
  codePrefix?: string
  scopeDeptIds: string[]
  scopeUserIds: string[]
  invoiceFreeMode: string
  taxDeductionMode: string
  taxSeparationMode: string
  status: number
}

export interface ProcessExpenseTypeMeta {
  invoiceFreeOptions: ProcessExpenseTypeConfigOption[]
  taxDeductionOptions: ProcessExpenseTypeConfigOption[]
  taxSeparationOptions: ProcessExpenseTypeConfigOption[]
  departmentOptions: ProcessFormOption[]
  userOptions: ProcessFormOption[]
}

export interface ProcessExpenseTypeSavePayload {
  expenseName: string
  expenseDescription?: string
  expenseCode: string
  scopeDeptIds: string[]
  scopeUserIds: string[]
  invoiceFreeMode: string
  taxDeductionMode: string
  taxSeparationMode: string
  status: number
}

export interface ProcessExpenseTypeStatusPayload {
  status: number
}
