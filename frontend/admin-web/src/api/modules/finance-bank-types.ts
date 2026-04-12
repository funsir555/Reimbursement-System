// 这里定义 finance-bank-types.ts 相关数据结构。
// 页面与 API 封装会依赖这些类型来约定字段。
// 如果改错，最容易影响列表、表单和接口联调。

// 这是 FinanceBankOption 的数据结构。
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
