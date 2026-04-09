import request, { buildQueryString, downloadBinaryFile } from './core'
import type { FinanceVoucherDetail, FinanceVoucherMeta, FinanceVoucherQueryParams, FinanceVoucherSavePayload, FinanceVoucherSaveResult, FinanceVoucherSummary, PageResult } from './shared'

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
  exportVouchers: (params: FinanceVoucherQueryParams) =>
    downloadBinaryFile(`/auth/finance/vouchers/export${buildQueryString(params)}`, '凭证查询.csv')
}
