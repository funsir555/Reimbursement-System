// 这里集中封装 profile.ts 相关接口。
// 上游通常是对应业务页面，下游对应后端同域接口。
// 如果改错，最容易影响页面的加载、保存或提交流程。

import request from './core'
import type { ChangePasswordPayload, PersonalCenterData, UserBankAccountRecord, UserBankAccountSavePayload } from './profile-types'

// 这一组方法供对应页面统一调用。
export const profileApi = {
  getOverview: () => request<PersonalCenterData>('/auth/user-center/profile'),
  listBankAccounts: () => request<UserBankAccountRecord[]>('/auth/user-center/bank-accounts'),
  createBankAccount: (payload: UserBankAccountSavePayload) =>
    request<UserBankAccountRecord>('/auth/user-center/bank-accounts', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateBankAccount: (id: number, payload: UserBankAccountSavePayload) =>
    request<UserBankAccountRecord>(`/auth/user-center/bank-accounts/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  updateBankAccountStatus: (id: number, status: number) =>
    request<boolean>(`/auth/user-center/bank-accounts/${id}/status`, {
      method: 'POST',
      body: JSON.stringify({ status })
    }),
  setDefaultBankAccount: (id: number) =>
    request<boolean>(`/auth/user-center/bank-accounts/${id}/default`, {
      method: 'POST'
    }),
  changePassword: (payload: ChangePasswordPayload) =>
    request<boolean>('/auth/user-center/password', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
}
