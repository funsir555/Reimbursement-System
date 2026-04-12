// 这里集中封装 finance-system-management.ts 相关接口。
// 上游通常是对应业务页面，下游对应后端同域接口。
// 如果改错，最容易影响页面的加载、保存或提交流程。

import request from './core'
import type { FinanceAccountSetCreatePayload, FinanceAccountSetMeta, FinanceAccountSetSummary, FinanceAccountSetTaskStatus } from './finance-system-management-types'

// 这一组方法供对应页面统一调用。
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
