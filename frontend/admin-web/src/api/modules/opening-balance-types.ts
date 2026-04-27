import type { AsyncTaskSubmitResult } from './async-task-types'
import type { MoneyValue } from './core'

export interface OpeningBalanceOption {
  value: string
  code?: string
  name?: string
  label: string
  parentValue?: string
}

export interface OpeningBalanceMeta {
  companyOptions: OpeningBalanceOption[]
  departmentOptions: OpeningBalanceOption[]
  employeeOptions: OpeningBalanceOption[]
  customerOptions: OpeningBalanceOption[]
  supplierOptions: OpeningBalanceOption[]
  projectClassOptions: OpeningBalanceOption[]
  projectOptions: OpeningBalanceOption[]
  defaultCompanyId?: string
  defaultYear: number
  defaultPeriod: number
  defaultYearPeriod: number
  status: string
  statusLabel: string
  opened: boolean
}

export interface OpeningBalanceRow {
  subjectCode: string
  subjectName: string
  subjectLevel?: number
  leafFlag?: number
  editable: boolean
  assistRequired: boolean
  balanceDirection?: string
  balanceDirectionLabel?: string
  cexchName?: string
  currencyCode?: string
  bperson?: number
  bcus?: number
  bsup?: number
  bdept?: number
  bitem?: number
  cassItem?: string
  mb: MoneyValue
}

export interface OpeningBalanceRowSavePayload {
  subjectCode: string
  mb?: MoneyValue
  mbF?: MoneyValue
  nbS?: number
}

export interface OpeningBalanceSavePayload {
  companyId: string
  iyear: number
  iperiod: number
  rows: OpeningBalanceRowSavePayload[]
}

export interface OpeningAssistBalanceLine {
  cdeptId?: string
  cpersonId?: string
  ccusId?: string
  csupId?: string
  citemClass?: string
  citemId?: string
  mb?: MoneyValue
  mbF?: MoneyValue
  nbS?: number
}

export interface OpeningBalanceAssistSavePayload {
  companyId: string
  iyear: number
  iperiod: number
  lines: OpeningAssistBalanceLine[]
}

export interface OpeningBalanceTaskRequest {
  companyId: string
  iyear: number
  iperiod: number
}

export interface OpeningBalanceTrialResult {
  balanced: boolean
  totalDebit: MoneyValue
  totalCredit: MoneyValue
  difference: MoneyValue
  abnormalSubjects: OpeningBalanceRow[]
}

export interface OpeningBalanceReconcileResult {
  matched: boolean
  differenceSubjects: OpeningBalanceRow[]
  missingAssistSubjects: OpeningBalanceRow[]
  illegalAssistMessages: string[]
}

export type OpeningBalanceTaskSubmitResult = AsyncTaskSubmitResult
