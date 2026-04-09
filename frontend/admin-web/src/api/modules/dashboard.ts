import request, { buildQueryString } from './core'
import type { DashboardData, DashboardOutstandingKind, DashboardWriteoffBindingPayload, ExpenseDocumentPickerResult, ExpenseSummary } from './shared'

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
