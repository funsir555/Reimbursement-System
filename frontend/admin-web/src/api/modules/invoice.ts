import request from './core'
import type { InvoiceSummary } from './invoice-types'

export const invoiceApi = {
  list: () => request<InvoiceSummary[]>('/auth/invoices')
}
