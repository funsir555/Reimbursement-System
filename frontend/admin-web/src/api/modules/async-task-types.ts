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
