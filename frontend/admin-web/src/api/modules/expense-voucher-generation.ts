// 这里集中封装 expense-voucher-generation.ts 相关接口。
// 上游通常是对应业务页面，下游对应后端同域接口。
// 如果改错，最容易影响页面的加载、保存或提交流程。

import request, { buildQueryString } from './core'
import type { PageResult } from './core'
import type { VoucherGeneratedDetail, VoucherGeneratedRecord, VoucherGenerationMeta, VoucherPushBatchResult, VoucherPushDocument, VoucherSubjectMapping, VoucherSubjectMappingPayload, VoucherTemplatePolicy, VoucherTemplatePolicyPayload } from './expense-voucher-generation-types'

// 这一组方法供对应页面统一调用。
export const expenseVoucherGenerationApi = {
  getMeta: () => request<VoucherGenerationMeta>('/auth/expenses/voucher-generation/meta'),
  getTemplatePolicies: (params: { companyId?: string; templateCode?: string; enabled?: number; page?: number; pageSize?: number } = {}) =>
    request<PageResult<VoucherTemplatePolicy>>(`/auth/expenses/voucher-generation/mappings${buildQueryString({ type: 'template', ...params })}`),
  createTemplatePolicy: (payload: VoucherTemplatePolicyPayload) =>
    request<VoucherTemplatePolicy>('/auth/expenses/voucher-generation/mappings/template-policy', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateTemplatePolicy: (id: number, payload: VoucherTemplatePolicyPayload) =>
    request<VoucherTemplatePolicy>(`/auth/expenses/voucher-generation/mappings/template-policy/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  getSubjectMappings: (params: { companyId?: string; templateCode?: string; expenseTypeCode?: string; enabled?: number; page?: number; pageSize?: number } = {}) =>
    request<PageResult<VoucherSubjectMapping>>(`/auth/expenses/voucher-generation/mappings${buildQueryString({ type: 'subject', ...params })}`),
  createSubjectMapping: (payload: VoucherSubjectMappingPayload) =>
    request<VoucherSubjectMapping>('/auth/expenses/voucher-generation/mappings/subject-lines', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateSubjectMapping: (id: number, payload: VoucherSubjectMappingPayload) =>
    request<VoucherSubjectMapping>(`/auth/expenses/voucher-generation/mappings/subject-lines/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  getPushDocuments: (params: { companyId?: string; templateCode?: string; keyword?: string; pushStatus?: string; dateFrom?: string; dateTo?: string; page?: number; pageSize?: number } = {}) =>
    request<PageResult<VoucherPushDocument>>(`/auth/expenses/voucher-generation/push-documents${buildQueryString(params)}`),
  pushDocuments: (documentCodes: string[]) =>
    request<VoucherPushBatchResult>('/auth/expenses/voucher-generation/push', {
      method: 'POST',
      body: JSON.stringify({ documentCodes })
    }),
  getGeneratedVouchers: (params: { companyId?: string; templateCode?: string; documentCode?: string; voucherNo?: string; pushStatus?: string; dateFrom?: string; dateTo?: string; page?: number; pageSize?: number } = {}) =>
    request<PageResult<VoucherGeneratedRecord>>(`/auth/expenses/voucher-generation/vouchers${buildQueryString(params)}`),
  getGeneratedVoucherDetail: (id: number) =>
    request<VoucherGeneratedDetail>(`/auth/expenses/voucher-generation/vouchers/${id}`)
}
