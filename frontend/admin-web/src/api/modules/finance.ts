// 这里集中封装 finance.ts 相关接口。
// 上游通常是对应业务页面，下游对应后端同域接口。
// 如果改错，最容易影响页面的加载、保存或提交流程。

import request, { buildQueryString, downloadBinaryFile } from './core'
import type { PageResult } from './core'
import type { FinanceVoucherDetail, FinanceVoucherMeta, FinanceVoucherQueryParams, FinanceVoucherSavePayload, FinanceVoucherSaveResult, FinanceVoucherSummary } from './finance-types'

// 这一组方法供对应页面统一调用。
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
