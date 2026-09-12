import type { MoneyValue, PageResult } from './core'
import type { FinanceCompanyOption } from './finance-context-types'
import type { FinanceVoucherOption } from './finance-types'

export type LedgerReportKind =
  | 'BALANCE_SHEET'
  | 'GENERAL_LEDGER'
  | 'DETAIL_LEDGER'
  | 'PROJECT_DETAIL_LEDGER'
  | 'SUPPLIER_DETAIL_LEDGER'
  | 'CUSTOMER_DETAIL_LEDGER'
  | 'PERSONAL_DETAIL_LEDGER'
  | 'QUANTITY_AMOUNT_DETAIL_LEDGER'
  | 'SEQUENCE_LEDGER'

export type DetailLedgerKind =
  | 'DETAIL'
  | 'PROJECT'
  | 'SUPPLIER'
  | 'CUSTOMER'
  | 'PERSONAL'
  | 'QUANTITY_AMOUNT'

export interface FinanceLedgerReportMeta {
  companyOptions: FinanceCompanyOption[]
  departmentOptions: FinanceVoucherOption[]
  employeeOptions: FinanceVoucherOption[]
  makerOptions: FinanceVoucherOption[]
  voucherTypeOptions: FinanceVoucherOption[]
  accountOptions: FinanceVoucherOption[]
  customerOptions: FinanceVoucherOption[]
  supplierOptions: FinanceVoucherOption[]
  projectClassOptions: FinanceVoucherOption[]
  projectOptions: FinanceVoucherOption[]
  defaultCompanyId?: string
  defaultYear?: number
  defaultPeriod?: number
  defaultYearPeriod?: number
  periodStartYear?: number
  periodStartMonth?: number
  periodEndYear?: number
  periodEndMonth?: number
}

export interface FinanceLedgerReportQueryParams {
  companyId: string
  iyear?: number
  iperiod?: number
  iyearFrom?: number
  iperiodFrom?: number
  iyearTo?: number
  iperiodTo?: number
  ledgerKind?: DetailLedgerKind
  accountCodeFrom?: string
  accountCodeTo?: string
  cdeptId?: string
  cpersonId?: string
  ccusId?: string
  csupId?: string
  citemClass?: string
  citemId?: string
  balanceAssistDisplay?: 'NAME' | 'CODE' | 'CODE_NAME'
  subjectLevelRange?: string
  includeUnposted?: boolean
  voucherNo?: string
  csign?: string
  summary?: string
  cbill?: string
  page?: number
  pageSize?: number
}

export interface FinanceBalanceSheetRow {
  subjectCode: string
  subjectName: string
  subjectLevel?: number
  rowType?: 'SUBJECT' | 'ASSIST' | 'CATEGORY_TOTAL'
  subjectCategory?: string
  beginDebit: MoneyValue
  beginCredit: MoneyValue
  periodDebit: MoneyValue
  periodCredit: MoneyValue
  endDebit: MoneyValue
  endCredit: MoneyValue
}

export interface FinanceDetailLedgerRow {
  ledgerKind: string
  groupKey: string
  rowType: 'OPENING' | 'ENTRY' | 'TOTAL' | 'ENDING'
  subjectCode: string
  subjectName: string
  assistLabel?: string
  dbillDate?: string
  voucherNo?: string
  displayVoucherNo?: string
  summary?: string
  voucherTypeLabel?: string
  makerName?: string
  debit: MoneyValue
  credit: MoneyValue
  balance: MoneyValue
  balanceDirection?: string
  quantityDebit?: MoneyValue
  quantityCredit?: MoneyValue
  quantityBalance?: MoneyValue
  measureUnit?: string
}

export interface FinanceGeneralLedgerSection {
  subjectCode: string
  subjectName: string
  beginDebit: MoneyValue
  beginCredit: MoneyValue
  totalDebit: MoneyValue
  totalCredit: MoneyValue
  endDebit: MoneyValue
  endCredit: MoneyValue
  rowCount: number
  rows: FinanceDetailLedgerRow[]
}

export interface FinanceSequenceLedgerRow {
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
  summary: string
  cbill: string
  idoc: number
  status: string
  statusLabel: string
  totalDebit: MoneyValue
  totalCredit: MoneyValue
}

export type FinanceLedgerBalancePage = PageResult<FinanceBalanceSheetRow>
export type FinanceLedgerGeneralPage = PageResult<FinanceGeneralLedgerSection>
export type FinanceLedgerDetailPage = PageResult<FinanceDetailLedgerRow>
export type FinanceLedgerSequencePage = PageResult<FinanceSequenceLedgerRow>
