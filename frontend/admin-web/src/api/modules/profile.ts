import request from './core'
import type { ChangePasswordPayload, PersonalCenterData, UserBankAccountRecord, UserBankAccountSavePayload } from './shared'

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
