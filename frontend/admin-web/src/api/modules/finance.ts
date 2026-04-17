import request, { buildQueryString, downloadBinaryFile } from './core'
import type { PageResult } from './core'
import type {
  FinanceVoucherActionResult,
  FinanceVoucherBatchActionPayload,
  FinanceVoucherBatchActionResult,
  FinanceVoucherDetail,
  FinanceVoucherMeta,
  FinanceVoucherQueryParams,
  FinanceVoucherSavePayload,
  FinanceVoucherSaveResult,
  FinanceVoucherSummary
} from './finance-types'

export type {
  FinanceVoucherActionResult,
  FinanceVoucherBatchActionPayload,
  FinanceVoucherBatchActionResult,
  FinanceVoucherDetail,
  FinanceVoucherMeta,
  FinanceVoucherQueryParams,
  FinanceVoucherSavePayload,
  FinanceVoucherSaveResult,
  FinanceVoucherSummary
} from './finance-types'

export const financeApi = {
  getVoucherMeta: (params: { companyId?: string; billDate?: string; csign?: string } = {}) =>
    request<FinanceVoucherMeta>(`/auth/finance/vouchers/meta${buildQueryString(params)}`),
  listVouchers: (params: FinanceVoucherQueryParams) =>
    request<PageResult<FinanceVoucherSummary>>(`/auth/finance/vouchers${buildQueryString(params)}`),
  getVoucherDetail: (companyId: string, voucherNo: string) =>
    request<FinanceVoucherDetail>(`/auth/finance/vouchers/${encodeURIComponent(voucherNo)}${buildQueryString({ companyId })}`),
  createVoucher: (payload: FinanceVoucherSavePayload) =>
    request<FinanceVoucherSaveResult>('/auth/finance/vouchers', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateVoucher: (companyId: string, voucherNo: string, payload: FinanceVoucherSavePayload) =>
    request<FinanceVoucherSaveResult>(`/auth/finance/vouchers/${encodeURIComponent(voucherNo)}${buildQueryString({ companyId })}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  reviewVoucher: (companyId: string, voucherNo: string) =>
    request<FinanceVoucherActionResult>(`/auth/finance/vouchers/${encodeURIComponent(voucherNo)}/review${buildQueryString({ companyId })}`, {
      method: 'POST'
    }),
  unreviewVoucher: (companyId: string, voucherNo: string) =>
    request<FinanceVoucherActionResult>(`/auth/finance/vouchers/${encodeURIComponent(voucherNo)}/unreview${buildQueryString({ companyId })}`, {
      method: 'POST'
    }),
  markVoucherError: (companyId: string, voucherNo: string) =>
    request<FinanceVoucherActionResult>(`/auth/finance/vouchers/${encodeURIComponent(voucherNo)}/mark-error${buildQueryString({ companyId })}`, {
      method: 'POST'
    }),
  clearVoucherError: (companyId: string, voucherNo: string) =>
    request<FinanceVoucherActionResult>(`/auth/finance/vouchers/${encodeURIComponent(voucherNo)}/clear-error${buildQueryString({ companyId })}`, {
      method: 'POST'
    }),
  batchUpdateVoucherState: (payload: FinanceVoucherBatchActionPayload) =>
    request<FinanceVoucherBatchActionResult>('/auth/finance/vouchers/actions', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  exportVouchers: (params: FinanceVoucherQueryParams) =>
    downloadBinaryFile(`/auth/finance/vouchers/export${buildQueryString(params)}`, '\u51ed\u8bc1\u67e5\u8be2.csv')
}
