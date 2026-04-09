import request, { buildQueryString } from './core'
import type { FinanceBankBranchOption, FinanceBankOption } from './shared'

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
