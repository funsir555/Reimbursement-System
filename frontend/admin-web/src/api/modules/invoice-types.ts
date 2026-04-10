import type { MoneyValue } from './core'

export interface InvoiceSummary {
  code: string
  number: string
  type: string
  seller: string
  amount: MoneyValue
  date: string
  status: string
  ocrStatus: string
}
