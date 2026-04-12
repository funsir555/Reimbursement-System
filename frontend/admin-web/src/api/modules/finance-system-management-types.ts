// 这里定义 finance-system-management-types.ts 相关数据结构。
// 页面与 API 封装会依赖这些类型来约定字段。
// 如果改错，最容易影响列表、表单和接口联调。

import type { FinanceCompanyOption } from './finance-context-types'

export interface FinanceAccountSetTemplateSummary {
  templateCode: string
  templateName: string
  accountingStandard?: string
  level1SubjectCount: number
  commonSubjectCount: number
}

export interface FinanceAccountSetReferenceOption {
  companyId: string
  companyName: string
  templateCode: string
  templateName: string
  enabledYearMonth?: string
  subjectCodeScheme?: string
  label: string
}

export interface FinanceAccountSetOption {
  value: string
  label: string
}

export interface FinanceAccountSetMeta {
  companyOptions: FinanceCompanyOption[]
  supervisorOptions: FinanceAccountSetOption[]
  templateOptions: FinanceAccountSetTemplateSummary[]
  referenceOptions: FinanceAccountSetReferenceOption[]
  defaultSubjectCodeScheme: string
}

// 这是 FinanceAccountSetSummary 的数据结构。
export interface FinanceAccountSetSummary {
  companyId: string
  companyCode?: string
  companyName: string
  status: string
  statusLabel: string
  enabledYearMonth?: string
  templateCode?: string
  templateName?: string
  supervisorUserId?: number
  supervisorName?: string
  createMode?: string
  referenceCompanyId?: string
  referenceCompanyName?: string
  subjectCodeScheme?: string
  subjectCount?: number
  lastTaskNo?: string
  lastTaskStatus?: string
  lastTaskProgress?: number
  lastTaskMessage?: string
  updatedAt?: string
}

export interface FinanceAccountSetCreatePayload {
  createMode: 'BLANK' | 'REFERENCE'
  referenceCompanyId?: string
  targetCompanyId: string
  enabledYearMonth: string
  templateCode?: string
  supervisorUserId: number
  subjectCodeScheme?: string
}

export interface FinanceAccountSetTaskStatus {
  taskNo: string
  companyId?: string
  taskType?: string
  status: string
  progress: number
  resultMessage?: string
  accountSetStatus?: string
  finished: boolean
  createdAt?: string
  updatedAt?: string
  finishedAt?: string
}
