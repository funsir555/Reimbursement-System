// 这里定义 dashboard-types.ts 相关数据结构。
// 页面与 API 封装会依赖这些类型来约定字段。
// 如果改错，最容易影响列表、表单和接口联调。

import type { MoneyValue } from './core'
import type { UserProfile } from './auth-types'
import type { ExpenseSummary } from './expense-types'

export interface ApprovalSummary {
  id: number
  title: string
  submitter: string
  time: string
  amount: MoneyValue
  avatar: string
}

export interface InvoiceAlert {
  id: number
  title: string
  desc: string
  time: string
}

export interface DashboardData {
  user: UserProfile
  pendingApprovalCount: number
  pendingApprovalDelta: number
  pendingRepaymentCount: number
  pendingPrepayWriteOffCount: number
  unusedApplicationCount: number
  unpaidContractCount: number
  monthlyExpenseAmount: MoneyValue
  monthlyExpenseCount: number
  invoiceCount: number
  monthlyInvoiceCount: number
  budgetRemaining: MoneyValue
  budgetUsageRate: number
  recentExpenses: ExpenseSummary[]
  pendingApprovals: ApprovalSummary[]
  invoiceAlerts: InvoiceAlert[]
}

export type DashboardOutstandingKind = 'LOAN' | 'PREPAY_REPORT'

export interface DashboardWriteoffBindingPayload {
  targetDocumentCode: string
  sourceReportDocumentCode: string
}
