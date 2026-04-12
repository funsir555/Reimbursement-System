// 这里集中封装 expense-approval.ts 相关接口。
// 上游通常是对应业务页面，下游对应后端同域接口。
// 如果改错，最容易影响页面的加载、保存或提交流程。

import request, { buildQueryString } from './core'
import type { ExpenseActionUserOption, ExpenseApprovalActionPayload, ExpenseApprovalPendingItem, ExpenseTaskAddSignPayload, ExpenseTaskTransferPayload } from './expense-approval-types'
import type { ExpenseDocumentEditContext, ExpenseDocumentUpdatePayload } from './expense-create-types'
import type { ExpenseDocumentDetail } from './expense-types'

// 这一组方法供对应页面统一调用。
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
