// 这里定义 finance-context-types.ts 相关数据结构。
// 页面与 API 封装会依赖这些类型来约定字段。
// 如果改错，最容易影响列表、表单和接口联调。

export interface FinanceCompanyOption {
  companyId: string
  companyCode: string
  companyName: string
  hasActiveAccountSet?: boolean
  label: string
  value: string
}

// 这是 FinanceContextMeta 的数据结构。
export interface FinanceContextMeta {
  companyOptions: FinanceCompanyOption[]
  currentUserCompanyId?: string
  defaultCompanyId?: string
}
