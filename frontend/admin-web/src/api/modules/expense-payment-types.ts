import type { MoneyValue } from './core'

export interface ExpensePaymentOrder {
  taskId: number
  documentCode: string
  documentTitle: string
  templateName?: string
  templateType?: string
  templateTypeLabel?: string
  submitterName?: string
  submitterDeptName?: string
  currentNodeName?: string
  documentStatus?: string
  documentStatusLabel?: string
  amount: MoneyValue
  submittedAt?: string
  paymentDate?: string
  paymentCompanyName?: string
  paymentStatusCode?: string
  paymentStatusLabel?: string
  manualPaid?: boolean
  paidAt?: string
  receiptStatusLabel?: string
  receiptReceivedAt?: string
  bankFlowNo?: string
  companyBankAccountName?: string
  taskCreatedAt?: string
  allowRetry?: boolean
}

export interface ExpenseBankLinkSummary {
  companyBankAccountId: number
  companyId: string
  companyName?: string
  accountName: string
  accountNo: string
  bankName: string
  accountStatus?: number
  directConnectEnabled: boolean
  directConnectProvider?: string
  directConnectChannel?: string
  directConnectStatusLabel?: string
  lastDirectConnectStatus?: string
  lastReceiptStatus?: string
}

export interface ExpenseBankLinkConfig {
  companyBankAccountId: number
  companyId: string
  companyName?: string
  accountName: string
  accountNo: string
  bankName: string
  accountStatus?: number
  directConnectEnabled: boolean
  directConnectProvider?: string
  directConnectChannel?: string
  directConnectProtocol?: string
  directConnectCustomerNo?: string
  directConnectAppId?: string
  directConnectAccountAlias?: string
  directConnectAuthMode?: string
  directConnectApiBaseUrl?: string
  directConnectCertRef?: string
  directConnectSecretRef?: string
  directConnectSignType?: string
  directConnectEncryptType?: string
  operatorKey?: string
  callbackSecret?: string
  publicKeyRef?: string
  receiptQueryEnabled?: boolean
  lastDirectConnectStatus?: string
  lastDirectConnectError?: string
}

export interface ExpenseBankLinkSavePayload {
  enabled?: boolean
  directConnectProvider: string
  directConnectChannel: string
  directConnectProtocol?: string
  directConnectCustomerNo?: string
  directConnectAppId?: string
  directConnectAccountAlias?: string
  directConnectAuthMode?: string
  directConnectApiBaseUrl?: string
  directConnectCertRef?: string
  directConnectSecretRef?: string
  directConnectSignType?: string
  directConnectEncryptType?: string
  operatorKey?: string
  callbackSecret?: string
  publicKeyRef?: string
  receiptQueryEnabled?: boolean
}
