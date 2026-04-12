// 这里定义 async-task-types.ts 相关数据结构。
// 页面与 API 封装会依赖这些类型来约定字段。
// 如果改错，最容易影响列表、表单和接口联调。

export interface InvoiceTaskPayload {
  code: string
  number: string
}

export interface AsyncTaskSubmitResult {
  taskNo: string
  taskType: string
  businessType: string
  status: string
  message: string
  downloadRecordId?: number
}

export type ExpenseExportScene = 'MY_EXPENSES' | 'PENDING_APPROVAL' | 'DOCUMENT_QUERY' | 'OUTSTANDING'

export interface ExpenseExportPayload {
  scene: ExpenseExportScene
  documentCodes?: string[]
  taskIds?: number[]
  kind?: 'LOAN' | 'PREPAY_REPORT'
}
