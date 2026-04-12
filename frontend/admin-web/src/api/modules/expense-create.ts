// 这里集中封装 expense-create.ts 相关接口。
// 上游通常是对应业务页面，下游对应后端同域接口。
// 如果改错，最容易影响页面的加载、保存或提交流程。

import request, { buildQueryString } from './core'
import type { ExpenseAttachmentMeta, ExpenseCreatePayeeAccountOption, ExpenseCreatePayeeAccountOptionsParams, ExpenseCreatePayeeOption, ExpenseCreatePayeeOptionsParams, ExpenseCreateTemplateDetail, ExpenseCreateTemplateSummary, ExpenseCreateVendorOption, ExpenseDocumentSubmitPayload, ExpenseDocumentSubmitResult } from './expense-create-types'
import type { FinanceVendorDetail, FinanceVendorSavePayload } from './finance-archive-types'

// 这一组方法供对应页面统一调用。
export const expenseCreateApi = {
  listTemplates: () =>
    request<ExpenseCreateTemplateSummary[]>('/auth/expenses/create/templates', {
      timeoutMs: 10000,
      timeoutMessage: '加载模板列表超时，请稍后重试'
    }),
  getTemplateDetail: (templateCode: string) =>
    request<ExpenseCreateTemplateDetail>(`/auth/expenses/create/templates/${encodeURIComponent(templateCode)}`, {
      timeoutMs: 10000,
      timeoutMessage: '加载模板详情超时，请稍后重试'
    }),
  listVendorOptions: (keyword?: string, includeDisabled?: boolean) =>
    request<ExpenseCreateVendorOption[]>(`/auth/expenses/create/vendors/options${buildQueryString({ keyword, includeDisabled })}`),
  createVendor: (payload: FinanceVendorSavePayload) =>
    request<FinanceVendorDetail>('/auth/expenses/create/vendors', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  listPayeeOptions: (params: ExpenseCreatePayeeOptionsParams = {}) =>
    request<ExpenseCreatePayeeOption[]>(`/auth/expenses/create/payees/options${buildQueryString(params)}`),
  listPayeeAccountOptions: (params: ExpenseCreatePayeeAccountOptionsParams = {}) =>
    request<ExpenseCreatePayeeAccountOption[]>(`/auth/expenses/create/payee-accounts/options${buildQueryString(params)}`),
  uploadAttachment: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request<ExpenseAttachmentMeta>('/auth/expenses/attachments', {
      method: 'POST',
      body: formData
    })
  },
  submit: (payload: ExpenseDocumentSubmitPayload) =>
    request<ExpenseDocumentSubmitResult>('/auth/expenses/create/documents', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
}
