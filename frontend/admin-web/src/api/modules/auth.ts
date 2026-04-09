import request from './core'
import type { LoginResponse, UserProfile } from './shared'

export const authApi = {
  loginByPassword: (username: string, password: string) =>
    request<LoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    }),
  getCurrentUser: () => request<UserProfile>('/auth/me')
}
