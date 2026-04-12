// 这里集中封装 dashboard.ts 相关接口。
// 上游通常是对应业务页面，下游对应后端同域接口。
// 如果改错，最容易影响页面的加载、保存或提交流程。

import request, { buildQueryString } from './core'
import type { DashboardData, DashboardOutstandingKind, DashboardWriteoffBindingPayload } from './dashboard-types'
import type { ExpenseDocumentPickerResult, ExpenseSummary } from './expense-types'

// 这一组方法供对应页面统一调用。
export const dashboardApi = {
  getOverview: () => request<DashboardData>('/auth/dashboard'),
  listOutstandingDocuments: (kind: DashboardOutstandingKind) =>
    request<ExpenseSummary[]>(`/auth/dashboard/outstanding-documents${buildQueryString({ kind })}`),
  getWriteoffReportPicker: (targetDocumentCode: string, keyword?: string, page?: number, pageSize?: number) =>
    request<ExpenseDocumentPickerResult>(
      `/auth/dashboard/writeoff-report-picker${buildQueryString({ targetDocumentCode, keyword, page, pageSize })}`
    ),
  bindWriteoff: (payload: DashboardWriteoffBindingPayload) =>
    request<boolean>('/auth/dashboard/writeoff-bindings', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
}
