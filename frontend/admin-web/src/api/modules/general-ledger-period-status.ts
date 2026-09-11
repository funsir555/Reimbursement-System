import request, { buildQueryString } from './core'
import type {
  FinanceGeneralLedgerPeriodActionRequest,
  FinanceGeneralLedgerPeriodActionResult,
  FinanceGeneralLedgerPeriodStatusOverview
} from './general-ledger-period-status-types'

export type {
  FinanceGeneralLedgerPeriodActionRequest,
  FinanceGeneralLedgerPeriodActionResult,
  FinanceGeneralLedgerPeriodActionType,
  FinanceGeneralLedgerPeriodStatusOverview,
  FinanceGeneralLedgerPeriodStatusRow
} from './general-ledger-period-status-types'

export const generalLedgerPeriodStatusApi = {
  getOverview: (params: { companyId?: string; iyear?: number; currentIyear: number; currentIperiod: number }) =>
    request<FinanceGeneralLedgerPeriodStatusOverview>(
      `/auth/finance/general-ledger/period-status/overview${buildQueryString(params)}`
    ),
  reopen: (payload: FinanceGeneralLedgerPeriodActionRequest) =>
    request<FinanceGeneralLedgerPeriodActionResult>('/auth/finance/general-ledger/period-status/reopen', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  unpost: (payload: FinanceGeneralLedgerPeriodActionRequest) =>
    request<FinanceGeneralLedgerPeriodActionResult>('/auth/finance/general-ledger/period-status/unpost', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
}
