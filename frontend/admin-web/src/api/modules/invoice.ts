import request from './core'
import type { InvoiceSummary } from './shared'

export const invoiceApi = {
  list: () => request<InvoiceSummary[]>('/auth/invoices')
}
