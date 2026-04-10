import type { MoneyValue } from './core'
import type { FinanceVoucherDetail, FinanceVoucherOption } from './finance-types'

export interface VoucherGenerationMeta {
  companyOptions: FinanceVoucherOption[]
  templateOptions: FinanceVoucherOption[]
  expenseTypeOptions: FinanceVoucherOption[]
  accountOptions: FinanceVoucherOption[]
  voucherTypeOptions: FinanceVoucherOption[]
  pushStatusOptions: FinanceVoucherOption[]
  defaultCompanyId?: string
  latestBatchNo?: string
  pendingPushCount: number
  pushedVoucherCount: number
  pushFailureCount: number
  pendingPushAmount: MoneyValue
}

export interface VoucherTemplatePolicy {
  id: number
  companyId: string
  companyName?: string
  templateCode: string
  templateName?: string
  creditAccountCode: string
  creditAccountName?: string
  voucherType: string
  voucherTypeLabel?: string
  summaryRule?: string
  enabled: boolean
  updatedAt?: string
}

export interface VoucherTemplatePolicyPayload {
  companyId: string
  templateCode: string
  templateName?: string
  creditAccountCode: string
  creditAccountName?: string
  voucherType: string
  summaryRule?: string
  enabled?: number
}

export interface VoucherSubjectMapping {
  id: number
  companyId: string
  companyName?: string
  templateCode: string
  templateName?: string
  expenseTypeCode: string
  expenseTypeName?: string
  debitAccountCode: string
  debitAccountName?: string
  enabled: boolean
  updatedAt?: string
}

export interface VoucherSubjectMappingPayload {
  companyId: string
  templateCode: string
  templateName?: string
  expenseTypeCode: string
  expenseTypeName?: string
  debitAccountCode: string
  debitAccountName?: string
  enabled?: number
}

export interface VoucherPushDocument {
  companyId?: string
  companyName?: string
  documentCode: string
  templateCode?: string
  templateName?: string
  submitterUserId?: number
  submitterName?: string
  totalAmount: MoneyValue
  finishedAt?: string
  expenseSummary?: string
  canPush: boolean
  pushStatus: string
  pushStatusLabel?: string
  failureReason?: string
  voucherNo?: string
}

export interface VoucherPushBatchItem {
  documentCode: string
  companyId?: string
  templateCode?: string
  templateName?: string
  pushStatus: string
  voucherNo?: string
  errorMessage?: string
}

export interface VoucherPushBatchResult {
  latestBatchNo?: string
  successCount: number
  failureCount: number
  results: VoucherPushBatchItem[]
}

export interface VoucherGeneratedRecord {
  id: number
  companyId?: string
  companyName?: string
  batchNo?: string
  documentCode: string
  templateCode?: string
  templateName?: string
  submitterName?: string
  totalAmount: MoneyValue
  pushStatus: string
  pushStatusLabel?: string
  voucherNo?: string
  voucherType?: string
  voucherNumber?: number
  billDate?: string
  pushedAt?: string
  failureReason?: string
}

export interface VoucherEntrySnapshot {
  entryNo: number
  direction: string
  digest?: string
  accountCode: string
  accountName?: string
  expenseTypeCode?: string
  expenseTypeName?: string
  amount: MoneyValue
}

export interface VoucherGeneratedDetail {
  record: VoucherGeneratedRecord
  voucherDetail?: FinanceVoucherDetail | null
  entries: VoucherEntrySnapshot[]
}

function clearLoginState() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
}
