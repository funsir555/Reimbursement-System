// 这里定义 finance-types.ts 相关数据结构。
// 页面与 API 封装会依赖这些类型来约定字段。
// 如果改错，最容易影响列表、表单和接口联调。

import type { MoneyValue } from './core'
import type { EmployeeDirectoryEntry } from './system-settings-types'

export interface FinanceVoucherOption {
  value: string
  code?: string
  name?: string
  label: string
  parentValue?: string
  subjectCategory?: string
  subjectCategoryLabel?: string
  subjectLevel?: number
  bperson?: number
  bcus?: number
  bsup?: number
  bdept?: number
  bitem?: number
  cassItem?: string
  leafFlag?: number
  bcash?: number
}

export interface FinanceVoucherEntry {
  inid?: number
  cdigest: string
  ccode: string
  ccodeName?: string
  cdeptId?: string
  cpersonId?: string
  ccusId?: string
  csupId?: string
  citemClass?: string
  citemId?: string
  cashFlowItemId?: number
  cashFlowItemName?: string
  cashFlowSubjectCode?: string
  cashFlowSubjectName?: string
  cashFlowAmount?: MoneyValue
  cexchName?: string
  currencyCode?: string
  nfrat?: number
  md?: MoneyValue
  mc?: MoneyValue
  ndS?: number
  ncS?: number
}

export interface FinanceVoucherForm {
  companyId: string
  iyear?: number
  iyperiod?: number
  iperiod: number
  csign: string
  inoId?: number
  dbillDate: string
  idoc: number
  cbill: string
  ctext1?: string
  ctext2?: string
  entries: FinanceVoucherEntry[]
}

export type FinanceVoucherSavePayload = FinanceVoucherForm

export interface FinanceVoucherMeta {
  companyOptions: FinanceVoucherOption[]
  departmentOptions: FinanceVoucherOption[]
  employeeOptions: FinanceVoucherOption[]
  makerOptions: FinanceVoucherOption[]
  employeeDirectory: EmployeeDirectoryEntry[]
  voucherTypeOptions: FinanceVoucherOption[]
  currencyOptions: FinanceVoucherOption[]
  accountOptions: FinanceVoucherOption[]
  customerOptions: FinanceVoucherOption[]
  supplierOptions: FinanceVoucherOption[]
  projectClassOptions: FinanceVoucherOption[]
  projectOptions: FinanceVoucherOption[]
  cashFlowOptions: FinanceVoucherOption[]
  defaultCompanyId?: string
  defaultYear?: number
  defaultYearPeriod?: number
  defaultBillDate: string
  defaultPeriod: number
  defaultVoucherType: string
  suggestedVoucherNo: number
  periodStatus?: 'OPEN' | 'CLOSED'
  periodStatusLabel?: string
  defaultMaker: string
  defaultAttachedDocCount: number
  defaultCurrency: string
  defaultCurrencyCode?: string
  defaultCurrencyName?: string
}

export interface FinanceVoucherQueryParams {
  companyId: string
  voucherNo?: string
  status?: string
  csign?: string
  billMonth?: string
  billMonthFrom?: string
  billMonthTo?: string
  cbill?: string
  summary?: string
  page?: number
  pageSize?: number
}

// 这是 FinanceVoucherSummary 的数据结构。
export interface FinanceVoucherSummary {
  voucherNo: string
  displayVoucherNo: string
  companyId: string
  iyear: number
  iyperiod: number
  iperiod: number
  csign: string
  voucherTypeLabel: string
  dbillDate: string
  summary: string
  cbill: string
  checkerName?: string
  checkedAt?: string
  postedAt?: string
  idoc: number
  status: string
  statusLabel: string
  editable: boolean
  periodStatus?: 'OPEN' | 'CLOSED'
  periodStatusLabel?: string
  voidedAt?: string
  voidedByName?: string
  reversedFromVoucherNo?: string
  reversedByVoucherNo?: string
  entryCount: number
  totalDebit: MoneyValue
  totalCredit: MoneyValue
}

export interface FinanceVoucherDetail {
  voucherNo: string
  displayVoucherNo: string
  companyId: string
  iyear: number
  iyperiod: number
  iperiod: number
  csign: string
  voucherTypeLabel: string
  inoId: number
  dbillDate: string
  idoc: number
  cbill: string
  checkerName?: string
  checkedAt?: string
  postedAt?: string
  ctext1?: string
  ctext2?: string
  status: string
  statusLabel: string
  editable: boolean
  periodStatus?: 'OPEN' | 'CLOSED'
  periodStatusLabel?: string
  voidedAt?: string
  voidedByName?: string
  reversedFromVoucherNo?: string
  reversedByVoucherNo?: string
  totalDebit: MoneyValue
  totalCredit: MoneyValue
  entries: FinanceVoucherEntry[]
}

export interface FinanceVoucherSaveResult {
  voucherNo: string
  companyId: string
  iyear: number
  iyperiod: number
  iperiod: number
  csign: string
  inoId: number
  entryCount: number
  totalDebit: MoneyValue
  totalCredit: MoneyValue
  status: string
  checkedAt?: string | null
  postedAt?: string | null
  voucherNoAutoForwarded?: boolean
  requestedInoId?: number
  requestedDisplayVoucherNo?: string
  occupiedByUserName?: string
}

export interface FinanceVoucherActionResult {
  action: string
  voucherNo: string
  iyear?: number
  iyperiod?: number
  status: string
  statusLabel: string
  periodStatus?: 'OPEN' | 'CLOSED'
  periodStatusLabel?: string
  checkerName?: string
  checkedAt?: string | null
  postedAt?: string | null
  voidedAt?: string | null
  voidedByName?: string
  reversedFromVoucherNo?: string
  reversedByVoucherNo?: string
  nextVoucherNo?: string
  lastVoucherOfMonth?: boolean
}

export interface FinanceVoucherBatchActionPayload {
  companyId: string
  action: 'REVIEW' | 'UNREVIEW' | 'MARK_ERROR' | 'CLEAR_ERROR'
  voucherNos: string[]
}

export interface FinanceVoucherBatchActionResult {
  action: string
  successCount: number
  voucherNos: string[]
}
