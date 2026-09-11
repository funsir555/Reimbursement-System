import type { FinanceVoucherOption } from './finance-types'

export type FinancePeriodTransferRuleType = 'PROFIT' | 'CUSTOM' | 'MANUFACTURE'
export type FinancePeriodTransferGranularity = 'SUBJECT' | 'ASSIST'
export type FinancePeriodTransferAllocationMode = 'FULL' | 'MANUAL_RATIO' | 'PROJECT'
export type FinancePeriodTransferMetricType = 'ENDING_BALANCE' | 'CURRENT_DEBIT' | 'CURRENT_CREDIT' | 'CURRENT_NET'

export interface FinancePeriodTransferMeta {
  companyId: string
  companyName: string
  iyear: number
  iperiod: number
  iyperiod: number
  periodLabel: string
  defaultVoucherType: string
  defaultVoucherTypeLabel: string
  accountOptions: FinanceVoucherOption[]
  projectClassOptions: FinanceVoucherOption[]
  projectOptions: FinanceVoucherOption[]
  metricOptions: FinanceVoucherOption[]
  transferGranularityOptions: FinanceVoucherOption[]
  allocationModeOptions: FinanceVoucherOption[]
  ruleTypeOptions: FinanceVoucherOption[]
}

export interface FinancePeriodTransferRuleLine {
  id?: number
  lineNo?: number
  sourceSubjectCode?: string
  sourceSubjectName?: string
  targetSubjectCode?: string
  targetSubjectName?: string
  metricType?: FinancePeriodTransferMetricType
  ratio?: number
  allocationProjectClass?: string
  allocationProjectId?: string
  allocationProjectName?: string
  remark?: string
}

export interface FinancePeriodTransferRule {
  id?: number
  companyId: string
  ruleType: FinancePeriodTransferRuleType
  ruleTypeLabel?: string
  ruleName: string
  enabled: boolean
  voucherType?: string
  includeUnposted: boolean
  transferGranularity?: FinancePeriodTransferGranularity
  subjectLevel?: number
  sourceSubjectCode?: string
  sourceSubjectName?: string
  targetSubjectCode?: string
  targetSubjectName?: string
  allocationMode?: FinancePeriodTransferAllocationMode
  regenerateStrategy?: string
  createdAt?: string
  updatedAt?: string
  lines: FinancePeriodTransferRuleLine[]
}

export interface FinancePeriodTransferPreviewRequest {
  companyId: string
  iyear: number
  iperiod: number
  ruleId: number
}

export interface FinancePeriodTransferGenerateRequest {
  companyId: string
  iyear: number
  iperiod: number
  runId: number
  previewToken: string
}

export interface FinancePeriodTransferPreviewDetail {
  detailId?: number
  lineNo: number
  sourceSubjectCode?: string
  sourceSubjectName?: string
  sourceAssistLabel?: string
  targetSubjectCode?: string
  targetSubjectName?: string
  metricType?: string
  metricTypeLabel?: string
  direction?: string
  directionLabel?: string
  amount: number
  skipReason?: string
  sourceTraceKey?: string
}

export interface FinancePeriodTransferPreviewResult {
  runId: number
  ruleId: number
  ruleType: FinancePeriodTransferRuleType
  ruleTypeLabel?: string
  ruleName: string
  previewToken: string
  voucherType: string
  generatedEntryCount: number
  skippedEntryCount: number
  totalAmount: number
  message?: string
  details: FinancePeriodTransferPreviewDetail[]
}

export interface FinancePeriodTransferGenerateResult {
  runId: number
  voucherNo?: string
  displayVoucherNo?: string
  generatedEntryCount: number
  skippedEntryCount: number
  totalAmount: number
  message?: string
}

export interface FinancePeriodTransferRun {
  id: number
  ruleId: number
  ruleType: FinancePeriodTransferRuleType
  ruleTypeLabel?: string
  ruleName: string
  iyear: number
  iperiod: number
  iyperiod: number
  status: string
  statusLabel?: string
  previewToken?: string
  includeUnposted: boolean
  voucherType?: string
  voucherNo?: string
  generatedEntryCount: number
  skippedEntryCount: number
  totalAmount: number
  blockedMessage?: string
  previewedBy?: string
  previewedAt?: string
  generatedBy?: string
  generatedAt?: string
}

export interface FinancePeriodTransferRunDetail {
  run: FinancePeriodTransferRun
  details: FinancePeriodTransferPreviewDetail[]
}
