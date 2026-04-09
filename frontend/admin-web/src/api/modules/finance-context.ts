import request from './core'
import type { FinanceContextMeta } from './shared'

export const financeContextApi = {
  getMeta: () => request<FinanceContextMeta>('/auth/finance/context/meta')
}
