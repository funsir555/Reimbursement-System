import type { MoneyValue } from './core'

export interface FinanceVoucherOption {
  value: string
  code?: string
  name?: string
  label: string
  parentValue?: string
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
  cexchName?: string
  nfrat?: number
  md?: MoneyValue
  mc?: MoneyValue
  ndS?: number
  ncS?: number
}

export interface FinanceVoucherForm {
  companyId: string
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
  voucherTypeOptions: FinanceVoucherOption[]
  currencyOptions: FinanceVoucherOption[]
  accountOptions: FinanceVoucherOption[]
  customerOptions: FinanceVoucherOption[]
  supplierOptions: FinanceVoucherOption[]
  projectClassOptions: FinanceVoucherOption[]
  projectOptions: FinanceVoucherOption[]
  defaultCompanyId?: string
  defaultBillDate: string
  defaultPeriod: number
  defaultVoucherType: string
  suggestedVoucherNo: number
  defaultMaker: string
  defaultAttachedDocCount: number
  defaultCurrency: string
}

export interface FinanceVoucherQueryParams {
  companyId: string
  voucherNo?: string
  csign?: string
  billMonth?: string
  billMonthFrom?: string
  billMonthTo?: string
  summary?: string
  page?: number
  pageSize?: number
}

export interface FinanceVoucherSummary {
  voucherNo: string
  displayVoucherNo: string
  companyId: string
  iperiod: number
  csign: string
  voucherTypeLabel: string
  dbillDate: string
  summary: string
  cbill: string
  idoc: number
  status: string
  statusLabel: string
  editable: boolean
  entryCount: number
  totalDebit: MoneyValue
  totalCredit: MoneyValue
}

export interface FinanceVoucherDetail {
  voucherNo: string
  displayVoucherNo: string
  companyId: string
  iperiod: number
  csign: string
  voucherTypeLabel: string
  inoId: number
  dbillDate: string
  idoc: number
  cbill: string
  ctext1?: string
  ctext2?: string
  status: string
  statusLabel: string
  editable: boolean
  totalDebit: MoneyValue
  totalCredit: MoneyValue
  entries: FinanceVoucherEntry[]
}

export interface FinanceVoucherSaveResult {
  voucherNo: string
  companyId: string
  iperiod: number
  csign: string
  inoId: number
  entryCount: number
  totalDebit: MoneyValue
  totalCredit: MoneyValue
  status: string
}
