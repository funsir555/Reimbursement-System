import request, { buildQueryString } from './core'
import type {
  OpeningAssistBalanceLine,
  OpeningBalanceAssistSavePayload,
  OpeningBalanceCarryForwardPreviewResult,
  OpeningBalanceCommitPayload,
  OpeningBalanceMeta,
  OpeningBalanceReconcileResult,
  OpeningBalanceRow,
  OpeningBalanceSavePayload,
  OpeningBalanceTaskRequest,
  OpeningBalanceTaskSubmitResult,
  OpeningBalanceTrialResult
} from './opening-balance-types'

export type {
  OpeningAssistBalanceLine,
  OpeningBalanceAssistDraftLine,
  OpeningBalanceAssistSavePayload,
  OpeningBalanceCarryForwardPreviewResult,
  OpeningBalanceCommitPayload,
  OpeningBalanceMeta,
  OpeningBalanceReconcileResult,
  OpeningBalanceRow,
  OpeningBalanceSavePayload,
  OpeningBalanceTaskRequest,
  OpeningBalanceTaskSubmitResult,
  OpeningBalanceTrialResult
} from './opening-balance-types'

export const openingBalanceApi = {
  getMeta: (params: { companyId?: string; iyear?: number; iperiod?: number } = {}) =>
    request<OpeningBalanceMeta>(`/auth/finance/opening-balance/meta${buildQueryString(params)}`),
  listRows: (params: { companyId: string; iyear: number; iperiod: number }) =>
    request<OpeningBalanceRow[]>(`/auth/finance/opening-balance/rows${buildQueryString(params)}`),
  saveRows: (payload: OpeningBalanceSavePayload) =>
    request<OpeningBalanceRow[]>('/auth/finance/opening-balance/rows', {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  getAssistBalances: (subjectCode: string, params: { companyId: string; iyear: number; iperiod: number }) =>
    request<OpeningAssistBalanceLine[]>(`/auth/finance/opening-balance/${encodeURIComponent(subjectCode)}/assist-balances${buildQueryString(params)}`),
  saveAssistBalances: (subjectCode: string, payload: OpeningBalanceAssistSavePayload) =>
    request<OpeningAssistBalanceLine[]>(`/auth/finance/opening-balance/${encodeURIComponent(subjectCode)}/assist-balances`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  commit: (payload: OpeningBalanceCommitPayload) =>
    request<OpeningBalanceRow[]>('/auth/finance/opening-balance/commit', {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  openBook: (payload: OpeningBalanceTaskRequest) =>
    request<OpeningBalanceTaskSubmitResult>('/auth/finance/opening-balance/open-book', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  carryForward: (payload: OpeningBalanceTaskRequest) =>
    request<OpeningBalanceTaskSubmitResult>('/auth/finance/opening-balance/carry-forward', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  carryForwardPreview: (payload: OpeningBalanceTaskRequest) =>
    request<OpeningBalanceCarryForwardPreviewResult>('/auth/finance/opening-balance/carry-forward-preview', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  trialBalance: (payload: OpeningBalanceTaskRequest) =>
    request<OpeningBalanceTrialResult>('/auth/finance/opening-balance/trial-balance', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  reconcile: (payload: OpeningBalanceTaskRequest) =>
    request<OpeningBalanceReconcileResult>('/auth/finance/opening-balance/reconcile', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
}
