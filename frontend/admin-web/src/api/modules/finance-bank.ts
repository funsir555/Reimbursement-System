// 这里集中封装 finance-bank.ts 相关接口。
// 上游通常是对应业务页面，下游对应后端同域接口。
// 如果改错，最容易影响页面的加载、保存或提交流程。

import request, { buildQueryString } from './core'
import type { FinanceBankBranchOption, FinanceBankOption } from './finance-bank-types'

// 这一组方法供对应页面统一调用。
export const financeBankApi = {
  listBanks: (keyword?: string) =>
    request<FinanceBankOption[]>(`/auth/finance/banks${buildQueryString({ keyword })}`),
  listBankBranches: (params: {
    bankCode?: string
    province?: string
    city?: string
    keyword?: string
  }) =>
    request<FinanceBankBranchOption[]>(`/auth/finance/bank-branches${buildQueryString(params)}`),
  lookupBranchByCnaps: (cnapsCode: string) =>
    request<FinanceBankBranchOption | null>(`/auth/finance/bank-branches/lookup-by-cnaps${buildQueryString({ cnapsCode })}`)
}
