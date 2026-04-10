import request from './core'
import type { AsyncTaskSubmitResult, ExpenseExportPayload, InvoiceTaskPayload } from './async-task-types'

export const asyncTaskApi = {
  exportInvoices: () =>
    request<AsyncTaskSubmitResult>('/auth/async-tasks/exports/invoices', {
      method: 'POST'
    }),
  exportExpenseScene: (payload: ExpenseExportPayload) =>
    request<AsyncTaskSubmitResult>('/auth/async-tasks/exports/expenses', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  verifyInvoice: (payload: InvoiceTaskPayload) =>
    request<AsyncTaskSubmitResult>('/auth/async-tasks/invoices/verify', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  ocrInvoice: (payload: InvoiceTaskPayload) =>
    request<AsyncTaskSubmitResult>('/auth/async-tasks/invoices/ocr', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
}
