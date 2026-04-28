import request, { buildQueryString } from './core'
import type {
  FinanceCloseLedgerCheckItem,
  FinanceCloseLedgerMeta,
  FinanceCloseLedgerReconcileResult,
  FinanceCloseLedgerRequest,
  FinanceCloseLedgerValidationResult
} from './close-ledger-types'

export type {
  FinanceCloseLedgerCheckItem,
  FinanceCloseLedgerMeta,
  FinanceCloseLedgerReconcileResult,
  FinanceCloseLedgerRequest,
  FinanceCloseLedgerValidationResult
} from './close-ledger-types'

export const closeLedgerApi = {
  getMeta: (params: { companyId?: string; iyear?: number; iperiod?: number } = {}) =>
    request<FinanceCloseLedgerMeta>(`/auth/finance/close-ledger/meta${buildQueryString(params)}`),
  reconcile: (payload: FinanceCloseLedgerRequest) =>
    request<FinanceCloseLedgerReconcileResult>('/auth/finance/close-ledger/reconcile', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  validate: (payload: FinanceCloseLedgerRequest) =>
    request<FinanceCloseLedgerValidationResult>('/auth/finance/close-ledger/validate', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  close: (payload: FinanceCloseLedgerRequest) =>
    request<FinanceCloseLedgerMeta>('/auth/finance/close-ledger/close', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
}
