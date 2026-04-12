// 这里定义 process-archive-types.ts 相关数据结构。
// 页面与 API 封装会依赖这些类型来约定字段。
// 如果改错，最容易影响列表、表单和接口联调。

import type { ProcessFormOption } from './process-template-types'

export interface ProcessCustomArchiveRule {
  id?: number
  groupNo: number
  fieldKey: string
  operator: string
  compareValue: unknown
}

export interface ProcessCustomArchiveItem {
  id?: number
  itemCode?: string
  itemName: string
  priority?: number
  status?: number
  rules: ProcessCustomArchiveRule[]
}

// 这是 ProcessCustomArchiveSummary 的数据结构。
export interface ProcessCustomArchiveSummary {
  id: number
  archiveCode: string
  archiveName: string
  archiveType: string
  archiveTypeLabel: string
  archiveDescription?: string
  status: number
  itemCount: number
  updatedAt: string
}

export interface ProcessCustomArchiveDetail {
  id?: number
  archiveCode?: string
  archiveName: string
  archiveType: string
  archiveTypeLabel?: string
  archiveDescription?: string
  status?: number
  items: ProcessCustomArchiveItem[]
}

export interface ProcessCustomArchiveSavePayload {
  archiveName: string
  archiveType: string
  archiveDescription?: string
  status?: number
  items: Array<{
    id?: number
    itemName: string
    priority?: number
    status?: number
    rules: ProcessCustomArchiveRule[]
  }>
}

export interface ProcessCustomArchiveOperator {
  key: string
  label: string
}

export interface ProcessCustomArchiveRuleField {
  key: string
  label: string
  valueType: string
  operatorKeys: string[]
}

export interface ProcessCustomArchiveMeta {
  archiveTypeOptions: ProcessFormOption[]
  operatorOptions: ProcessCustomArchiveOperator[]
  ruleFields: ProcessCustomArchiveRuleField[]
  departmentOptions: ProcessFormOption[]
  tagArchiveCode: string
  installmentArchiveCode: string
}

export interface ProcessCustomArchiveStatusPayload {
  status: number
}

export interface ProcessCustomArchiveResolvePayload {
  archiveCode: string
  context: Record<string, unknown>
}

export interface ProcessCustomArchiveResolveItem {
  itemCode: string
  itemName: string
  priority?: number
}

export interface ProcessCustomArchiveResolveResult {
  archiveCode: string
  archiveType: string
  items: ProcessCustomArchiveResolveItem[]
}
