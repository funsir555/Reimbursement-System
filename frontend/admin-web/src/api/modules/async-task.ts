// 这里集中封装 async-task.ts 相关接口。
// 上游通常是对应业务页面，下游对应后端同域接口。
// 如果改错，最容易影响页面的加载、保存或提交流程。

import request from './core'
import type { AsyncTaskSubmitResult, ExpenseExportPayload, InvoiceTaskPayload } from './async-task-types'

// 这一组方法供对应页面统一调用。
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
