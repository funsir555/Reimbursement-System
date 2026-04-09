import request, { buildQueryString } from './core'
import type { ExpenseActionUserOption, ExpenseApprovalActionPayload, ExpenseApprovalPendingItem, ExpenseDocumentDetail, ExpenseDocumentEditContext, ExpenseDocumentUpdatePayload, ExpenseTaskAddSignPayload, ExpenseTaskTransferPayload } from './shared'

export const expenseApprovalApi = {
  listPending: () => request<ExpenseApprovalPendingItem[]>('/auth/expense-approval/pending'),
  approve: (taskId: number, payload: ExpenseApprovalActionPayload = {}) =>
    request<ExpenseDocumentDetail>(`/auth/expense-approval/tasks/${taskId}/approve`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  reject: (taskId: number, payload: ExpenseApprovalActionPayload = {}) =>
    request<ExpenseDocumentDetail>(`/auth/expense-approval/tasks/${taskId}/reject`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  getModifyContext: (taskId: number) =>
    request<ExpenseDocumentEditContext>(`/auth/expense-approval/tasks/${taskId}/modify-context`),
  modify: (taskId: number, payload: ExpenseDocumentUpdatePayload) =>
    request<ExpenseDocumentDetail>(`/auth/expense-approval/tasks/${taskId}/modify`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  addSign: (taskId: number, payload: ExpenseTaskAddSignPayload) =>
    request<ExpenseDocumentDetail>(`/auth/expense-approval/tasks/${taskId}/add-sign`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  transfer: (taskId: number, payload: ExpenseTaskTransferPayload) =>
    request<ExpenseDocumentDetail>(`/auth/expense-approval/tasks/${taskId}/transfer`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  listActionUsers: (keyword?: string) =>
    request<ExpenseActionUserOption[]>(`/auth/expense-approval/action-users${buildQueryString({ keyword })}`)
}
