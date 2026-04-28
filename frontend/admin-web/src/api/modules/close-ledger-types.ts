export interface FinanceCloseLedgerMeta {
  companyId: string
  companyName: string
  iyear: number
  iperiod: number
  iyperiod: number
  periodLabel: string
  status: string
  statusLabel: string
  closeNote?: string
  closedBy?: string
  closedAt?: string
  postStatus: string
  postStatusLabel: string
  unpostedVoucherCount: number
  reviewedVoucherCount: number
  errorVoucherCount: number
  postedVoucherCount: number
  fixedAssetClosed: boolean
  fixedAssetStatusLabel: string
}

export interface FinanceCloseLedgerCheckItem {
  code: string
  label: string
  passed: boolean
  message: string
}

export interface FinanceCloseLedgerReconcileResult {
  passed: boolean
  summaryMessage: string
  differenceSubjectCount: number
  differenceAssistCount: number
  missingAssistCount: number
  illegalAssistCount: number
  differenceSubjects: string[]
  differenceAssistKeys: string[]
  missingAssistSubjects: string[]
  illegalAssistMessages: string[]
}

export interface FinanceCloseLedgerValidationResult {
  passed: boolean
  generalPassed: boolean
  externalPassed: boolean
  alreadyClosed: boolean
  reconcilePassed: boolean
  postStatus: string
  postStatusLabel: string
  blockingReasons: string[]
  generalChecks: FinanceCloseLedgerCheckItem[]
  externalChecks: FinanceCloseLedgerCheckItem[]
}

export interface FinanceCloseLedgerRequest {
  companyId: string
  iyear: number
  iperiod: number
  closeNote?: string
}
