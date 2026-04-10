export interface FinanceCompanyOption {
  companyId: string
  companyCode: string
  companyName: string
  hasActiveAccountSet?: boolean
  label: string
  value: string
}

export interface FinanceContextMeta {
  companyOptions: FinanceCompanyOption[]
  currentUserCompanyId?: string
  defaultCompanyId?: string
}
