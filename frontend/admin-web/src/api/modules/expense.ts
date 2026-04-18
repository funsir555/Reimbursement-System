import request, { buildQueryString, requestBinary, type BinaryFileResponse } from './core'
import type {
  ExpenseDocumentEditContext,
  ExpenseDocumentSubmitResult,
  ExpenseDocumentUpdatePayload
} from './expense-create-types'
import type {
  ExpenseDetailInstanceDetail,
  ExpenseDocumentCommentPayload,
  ExpenseDocumentDetail,
  ExpenseDocumentNavigation,
  ExpenseDocumentPickerResult,
  ExpenseDocumentReminderPayload,
  ExpenseSummary
} from './expense-types'

export type ExpensePrintOrientation = 'PORTRAIT' | 'LANDSCAPE'

function buildBatchPrintFallbackFileName(documentCodes: string[]) {
  if (documentCodes.length === 1) {
    return `expense-document-${documentCodes[0] || 'unknown'}.pdf`
  }
  return `expense-documents-batch-${documentCodes.length}.pdf`
}

export const expenseApi = {
  list: () => request<ExpenseSummary[]>('/auth/expenses'),
  queryDocuments: () => request<ExpenseSummary[]>('/auth/expenses/query-documents'),
  getDetail: (documentCode: string) =>
    request<ExpenseDocumentDetail>(`/auth/expenses/${encodeURIComponent(documentCode)}`, {
      timeoutMs: 10000,
      timeoutMessage: '加载单据详情超时，请稍后重试'
    }),
  getExpenseDetail: (documentCode: string, detailNo: string) =>
    request<ExpenseDetailInstanceDetail>(
      `/auth/expenses/${encodeURIComponent(documentCode)}/details/${encodeURIComponent(detailNo)}`,
      {
        timeoutMs: 10000,
        timeoutMessage: '加载费用明细超时，请稍后重试'
      }
    ),
  getPrintPdf: (documentCode: string, orientation: ExpensePrintOrientation = 'PORTRAIT') =>
    requestBinary(
      `/auth/expenses/${encodeURIComponent(documentCode)}/print-pdf${buildQueryString({ orientation })}`,
      {
        timeoutMs: 30000,
        timeoutMessage: '生成打印 PDF 超时，请稍后重试',
        fallbackFileName: `expense-document-${documentCode || 'unknown'}.pdf`
      }
    ),
  getBatchPrintPdf: (documentCodes: string[], orientation: ExpensePrintOrientation = 'PORTRAIT') =>
    requestBinary(
      `/auth/expenses/print-pdf/batch${buildQueryString({
        documentCodes: documentCodes.join(','),
        orientation
      })}`,
      {
        timeoutMs: 60000,
        timeoutMessage: '生成批量打印 PDF 超时，请稍后重试',
        fallbackFileName: buildBatchPrintFallbackFileName(documentCodes)
      }
    ),
  getDocumentPicker: (params: {
    relationType: 'RELATED' | 'WRITEOFF'
    templateTypes?: string[]
    keyword?: string
    page?: number
    pageSize?: number
    excludeDocumentCode?: string
  }) => {
    const search = new URLSearchParams()
    search.append('relationType', params.relationType)
    params.templateTypes?.forEach((item) => {
      if (item) {
        search.append('templateTypes', item)
      }
    })
    if (params.keyword) {
      search.append('keyword', params.keyword)
    }
    if (params.page) {
      search.append('page', String(params.page))
    }
    if (params.pageSize) {
      search.append('pageSize', String(params.pageSize))
    }
    if (params.excludeDocumentCode) {
      search.append('excludeDocumentCode', params.excludeDocumentCode)
    }
    return request<ExpenseDocumentPickerResult>(`/auth/expenses/document-picker?${search.toString()}`)
  },
  recall: (documentCode: string) =>
    request<ExpenseDocumentDetail>(`/auth/expenses/${encodeURIComponent(documentCode)}/recall`, {
      method: 'POST'
    }),
  comment: (documentCode: string, payload: ExpenseDocumentCommentPayload = {}) =>
    request<ExpenseDocumentDetail>(`/auth/expenses/${encodeURIComponent(documentCode)}/comments`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  remind: (documentCode: string, payload: ExpenseDocumentReminderPayload = {}) =>
    request<ExpenseDocumentDetail>(`/auth/expenses/${encodeURIComponent(documentCode)}/reminders`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  getNavigation: (documentCode: string) =>
    request<ExpenseDocumentNavigation>(`/auth/expenses/${encodeURIComponent(documentCode)}/navigation`, {
      timeoutMs: 5000,
      timeoutMessage: '加载单据导航超时，请稍后重试'
    }),
  getEditContext: (documentCode: string) =>
    request<ExpenseDocumentEditContext>(`/auth/expenses/${encodeURIComponent(documentCode)}/edit-context`),
  resubmit: (documentCode: string, payload: ExpenseDocumentUpdatePayload) =>
    request<ExpenseDocumentSubmitResult>(`/auth/expenses/${encodeURIComponent(documentCode)}/resubmit`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
}

export type ExpensePrintPdfResponse = BinaryFileResponse
