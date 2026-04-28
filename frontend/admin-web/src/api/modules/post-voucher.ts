import request, { buildQueryString } from './core'
import type {
  FinancePostVoucherMeta,
  FinancePostVoucherTaskRequest,
  FinancePostVoucherTaskStatus,
  FinancePostVoucherTaskSubmitResult
} from './post-voucher-types'

export type {
  FinancePostVoucherMeta,
  FinancePostVoucherTaskRequest,
  FinancePostVoucherTaskStatus,
  FinancePostVoucherTaskSubmitResult
} from './post-voucher-types'

export const postVoucherApi = {
  getMeta: (params: { companyId?: string; iyear?: number; iperiod?: number } = {}) =>
    request<FinancePostVoucherMeta>(`/auth/finance/post-voucher/meta${buildQueryString(params)}`),
  runPosting: (payload: FinancePostVoucherTaskRequest) =>
    request<FinancePostVoucherTaskSubmitResult>('/auth/finance/post-voucher/run', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  getTaskStatus: (taskNo: string) =>
    request<FinancePostVoucherTaskStatus>(`/auth/finance/post-voucher/tasks/${encodeURIComponent(taskNo)}`)
}
