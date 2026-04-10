import request from './core'
import type { FinanceContextMeta } from './finance-context-types'

export const financeContextApi = {
  getMeta: () => request<FinanceContextMeta>('/auth/finance/context/meta')
}
