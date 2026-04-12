// 这里定义 profile-types.ts 相关数据结构。
// 页面与 API 封装会依赖这些类型来约定字段。
// 如果改错，最容易影响列表、表单和接口联调。

import type { UserProfile } from './auth-types'

export interface UserBankAccountRecord {
  id: number
  bankCode?: string
  bankName: string
  province?: string
  city?: string
  branchCode?: string
  branchName?: string
  cnapsCode?: string
  accountName: string
  accountNo: string
  accountNoMasked: string
  accountType: string
  defaultAccount: boolean
  status: number
  statusLabel: string
  createdAt?: string
  updatedAt?: string
}

export interface UserBankAccountSavePayload {
  accountName: string
  accountNo: string
  accountType?: string
  bankName: string
  bankCode?: string
  province: string
  city: string
  branchName: string
  branchCode?: string
  cnapsCode?: string
  defaultAccount?: number
  status?: number
}

export interface PersonalCenterData {
  user: UserProfile
  bankAccounts: UserBankAccountRecord[]
}

export interface ChangePasswordPayload {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}
