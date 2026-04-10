export interface FinanceBankOption {
  bankCode: string
  bankName: string
  value: string
  label: string
}

export interface FinanceBankBranchOption {
  id: number
  bankCode: string
  bankName: string
  province: string
  city: string
  branchCode: string
  branchName: string
  cnapsCode?: string
  value: string
  label: string
}
