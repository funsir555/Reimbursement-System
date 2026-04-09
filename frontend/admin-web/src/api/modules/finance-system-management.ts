import request from './core'
import type { FinanceAccountSetCreatePayload, FinanceAccountSetMeta, FinanceAccountSetSummary, FinanceAccountSetTaskStatus } from './shared'

export const financeSystemManagementApi = {
  getMeta: () => request<FinanceAccountSetMeta>('/auth/finance/system-management/meta'),
  listAccountSets: () => request<FinanceAccountSetSummary[]>('/auth/finance/system-management/account-sets'),
  createAccountSet: (payload: FinanceAccountSetCreatePayload) =>
    request<FinanceAccountSetTaskStatus>('/auth/finance/system-management/account-sets/create', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  getTaskStatus: (taskNo: string) =>
    request<FinanceAccountSetTaskStatus>(`/auth/finance/system-management/tasks/${encodeURIComponent(taskNo)}`)
}
