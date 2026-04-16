// 这里集中封装 finance-bank.ts 相关接口。
// 上游通常是对应业务页面，下游对应后端同域接口。
// 如果改错，最容易影响页面的加载、保存或提交流程。

import request, { buildQueryString } from './core'
import type { FinanceBankBranchOption, FinanceBankBusinessScope, FinanceBankOption } from './finance-bank-types'

// 这一组方法供对应页面统一调用。
export const financeBankApi = {
  listBanks: (params?: { keyword?: string; businessScope?: FinanceBankBusinessScope }) =>
    request<FinanceBankOption[]>(`/auth/finance/banks${buildQueryString(params || {})}`),
  listBankProvinces: (params: { bankCode: string; businessScope?: FinanceBankBusinessScope }) =>
    request<string[]>(`/auth/finance/banks/provinces${buildQueryString(params)}`),
  listBankCities: (params: { bankCode: string; province: string; businessScope?: FinanceBankBusinessScope }) =>
    request<string[]>(`/auth/finance/banks/cities${buildQueryString(params)}`),
  listBankBranches: (params: {
    bankCode?: string
    province?: string
    city?: string
    keyword?: string
    businessScope?: FinanceBankBusinessScope
  }) =>
    request<FinanceBankBranchOption[]>(`/auth/finance/bank-branches${buildQueryString(params)}`),
  lookupBranchByCnaps: (cnapsCode: string) =>
    request<FinanceBankBranchOption | null>(`/auth/finance/bank-branches/lookup-by-cnaps${buildQueryString({ cnapsCode })}`)
}
