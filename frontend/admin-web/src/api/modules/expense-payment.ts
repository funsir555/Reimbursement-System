import request, { buildQueryString } from './core'
import type { ExpenseApprovalActionPayload } from './expense-approval-types'
import type { ExpenseBankLinkConfig, ExpenseBankLinkSavePayload, ExpenseBankLinkSummary, ExpensePaymentOrder } from './expense-payment-types'
import type { ExpenseDocumentDetail } from './expense-types'

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
  listBankLinks: () => request<ExpenseBankLinkSummary[]>('/auth/expense-payment/bank-links'),
  getBankLink: (companyBankAccountId: number) =>
    request<ExpenseBankLinkConfig>(`/auth/expense-payment/bank-links/${companyBankAccountId}`),
  updateBankLink: (companyBankAccountId: number, payload: ExpenseBankLinkSavePayload) =>
    request<ExpenseBankLinkConfig>(`/auth/expense-payment/bank-links/${companyBankAccountId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
}
