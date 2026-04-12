// 这里定义 process-template-types.ts 相关数据结构。
// 页面与 API 封装会依赖这些类型来约定字段。
// 如果改错，最容易影响列表、表单和接口联调。

import type { MoneyValue } from './core'
import type { ProcessExpenseTypeTreeNode } from './process-expense-type-types'
import type { ProcessExpenseDetailDesignSummary } from './process-form-types'

export interface ProcessTemplateTypeOption {
  code: string
  name: string
  subtitle: string
  description: string
  accent: string
}

export interface ProcessFormOption {
  label: string
  value: string
}

export interface ProcessTemplateFormOptions {
  templateType: string
  templateTypeLabel: string
  categoryOptions: ProcessFormOption[]
  numberingRulePreview: string
  formDesignOptions: ProcessFormOption[]
  expenseDetailDesignOptions: ProcessExpenseDetailDesignSummary[]
  expenseDetailModeOptions: ProcessFormOption[]
  printModes: ProcessFormOption[]
  approvalFlows: ProcessFormOption[]
  paymentModes: ProcessFormOption[]
  allocationForms: ProcessFormOption[]
  expenseTypes: ProcessExpenseTypeTreeNode[]
  departmentOptions: ProcessFormOption[]
  aiAuditModes: ProcessFormOption[]
  tagOptions: ProcessFormOption[]
  installmentOptions: ProcessFormOption[]
}

export interface ProcessTemplateSavePayload {
  templateType: string
  templateName: string
  templateDescription: string
  category: string
  enabled: boolean
  formDesign: string
  expenseDetailDesign?: string
  expenseDetailModeDefault?: string
  printMode: string
  approvalFlow: string
  paymentMode: string
  allocationForm: string
  aiAuditMode: string
  scopeDeptIds: string[]
  scopeExpenseTypeCodes: string[]
  amountMin?: MoneyValue
  amountMax?: MoneyValue
  tagOption: string
  installmentOption: string
}

// 这是 ProcessTemplateDetail 的数据结构。
export interface ProcessTemplateDetail extends ProcessTemplateSavePayload {
  id: number
  templateCode: string
  templateTypeLabel: string
  expenseDetailType?: string
}

export interface ProcessTemplateSaveResult {
  id: number
  templateCode: string
  templateName: string
  status: string
}
