// 这里定义 auth-types.ts 相关数据结构。
// 页面与 API 封装会依赖这些类型来约定字段。
// 如果改错，最容易影响列表、表单和接口联调。

// 这是 UserProfile 的数据结构。
export interface UserProfile {
  userId: number
  username: string
  name: string
  phone?: string
  email?: string
  position?: string
  laborRelationBelong?: string
  companyId?: string
  roles: string[]
  permissionCodes: string[]
}

// 这是 LoginResponse 的数据结构。
export interface LoginResponse {
  userId: number
  username: string
  name: string
  token: string
  expireIn: number
  roles: string[]
  permissionCodes: string[]
}
