// 这里集中封装 expense-payment.ts 相关接口。
// 上游通常是对应业务页面，下游对应后端同域接口。
// 如果改错，最容易影响页面的加载、保存或提交流程。

import request, { buildQueryString } from './core'
import type { ExpenseApprovalActionPayload } from './expense-approval-types'
import type { AsyncTaskSubmitResult } from './async-task-types'
import type { ExpenseBankLinkConfig, ExpenseBankLinkSavePayload, ExpenseBankLinkSummary, ExpensePaymentOrder } from './expense-payment-types'
import type { ExpenseDocumentDetail } from './expense-types'

// 这一组方法供对应页面统一调用。
export const expensePaymentApi = {
  listOrders: (status?: string) =>
    request<ExpensePaymentOrder[]>(`/auth/expense-payment/orders${buildQueryString({ status })}`),
  startTask: (taskId: number) =>
    request<ExpenseDocumentDetail>(`/auth/expense-payment/tasks/${taskId}/start`, {
      method: 'POST'
    }),
  completeTask: (taskId: number, payload: ExpenseApprovalActionPayload = {}) =>
    request<ExpenseDocumentDetail>(`/auth/expense-payment/tasks/${taskId}/complete`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  markException: (taskId: number, payload: ExpenseApprovalActionPayload = {}) =>
    request<ExpenseDocumentDetail>(`/auth/expense-payment/tasks/${taskId}/exception`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  submitOrderExport: (taskIds: number[]) =>
    request<AsyncTaskSubmitResult>('/auth/expense-payment/orders/export', {
      method: 'POST',
      body: JSON.stringify({ taskIds })
    }),
  rejectTasks: (taskIds: number[], payload: ExpenseApprovalActionPayload = {}) =>
    request<boolean>('/auth/expense-payment/tasks/reject', {
      method: 'POST',
      body: JSON.stringify({
        taskIds,
        comment: payload.comment || ''
      })
    }),
  listBankLinks: () => request<ExpenseBankLinkSummary[]>('/auth/expense-payment/bank-links'),
  getBankLink: (companyBankAccountId: number) =>
    request<ExpenseBankLinkConfig>(`/auth/expense-payment/bank-links/${companyBankAccountId}`),
  updateBankLink: (companyBankAccountId: number, payload: ExpenseBankLinkSavePayload) =>
    request<ExpenseBankLinkConfig>(`/auth/expense-payment/bank-links/${companyBankAccountId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
}
