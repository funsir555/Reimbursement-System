export type FinanceGeneralLedgerPeriodActionType = 'REOPEN' | 'UNPOST'

export interface FinanceGeneralLedgerPeriodStatusRow {
  iyear: number
  iperiod: number
  iyperiod: number
  periodLabel: string
  available: boolean
  voucherCount: number
  unpostedVoucherCount: number
  reviewedVoucherCount: number
  errorVoucherCount: number
  postedVoucherCount: number
  reviewStatus: string
  reviewStatusLabel: string
  postStatus: string
  postStatusLabel: string
  closeStatus: string
  closeStatusLabel: string
  periodTransferStatus: string
  periodTransferStatusLabel: string
  nextPeriodStatus: string
  nextPeriodStatusLabel: string
  allowedActions: FinanceGeneralLedgerPeriodActionType[]
  blockingReason?: string
}

export interface FinanceGeneralLedgerPeriodStatusOverview {
  companyId: string
  companyName: string
  iyear: number
  currentIyear: number
  currentIperiod: number
  currentPeriodLabel: string
  rows: FinanceGeneralLedgerPeriodStatusRow[]
}

export interface FinanceGeneralLedgerPeriodActionRequest {
  companyId: string
  iyear: number
  iperiod: number
  currentIyear: number
  currentIperiod: number
  password: string
}

export interface FinanceGeneralLedgerPeriodActionResult {
  actionType: FinanceGeneralLedgerPeriodActionType
  companyId: string
  iyear: number
  iperiod: number
  iyperiod: number
  periodLabel: string
  postStatus: string
  postStatusLabel: string
  closeStatus: string
  closeStatusLabel: string
}
