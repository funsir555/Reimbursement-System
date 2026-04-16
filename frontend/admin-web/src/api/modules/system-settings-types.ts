// 这里定义 system-settings-types.ts 相关数据结构。
// 页面与 API 封装会依赖这些类型来约定字段。
// 如果改错，最容易影响列表、表单和接口联调。

import type { UserProfile } from './auth-types'

export interface DepartmentTreeNode {
  id: number
  companyId?: string
  deptCode: string
  leaderUserId?: number
  leaderName?: string
  deptName: string
  parentId?: number
  statDepartmentBelong?: string
  statRegionBelong?: string
  statAreaBelong?: string
  syncSource: string
  syncManaged: boolean
  syncEnabled: boolean
  syncStatus?: string
  syncRemark?: string
  status: number
  sortOrder: number
  lastSyncAt?: string
  children: DepartmentTreeNode[]
}

export interface DepartmentSavePayload {
  companyId?: string
  deptCode?: string
  leaderUserId?: number
  deptName: string
  parentId?: number
  statDepartmentBelong?: string
  statRegionBelong?: string
  statAreaBelong?: string
  status?: number
  sortOrder?: number
  syncEnabled?: number
}

export interface EmployeeRecord {
  userId: number
  username: string
  name: string
  phone?: string
  email?: string
  companyId?: string
  companyName?: string
  deptId?: number
  deptName?: string
  position?: string
  laborRelationBelong?: string
  statDepartmentBelong?: string
  statRegionBelong?: string
  statAreaBelong?: string
  status: number
  sourceType: string
  syncManaged: boolean
  lastSyncAt?: string
  roleCodes: string[]
}

export interface EmployeeSavePayload {
  username: string
  name: string
  phone?: string
  email?: string
  companyId?: string
  deptId?: number
  position?: string
  laborRelationBelong?: string
  statDepartmentBelong?: string
  statRegionBelong?: string
  statAreaBelong?: string
  status?: number
}

export interface EmployeeEditorFormState extends EmployeeSavePayload {
  userId?: number
  roleIds: number[]
}

export interface EmployeeQueryPayload {
  keyword?: string
  companyId?: string
  deptId?: number
  status?: number
}

export interface FinanceEmployeeArchiveMeta {
  companies: CompanyRecord[]
  departments: DepartmentTreeNode[]
}

export interface RoleRecord {
  id: number
  roleCode: string
  roleName: string
  roleDescription?: string
  status: number
  permissionCodes: string[]
  userIds: number[]
  userNames: string[]
}

export interface RoleSavePayload {
  roleCode?: string
  roleName: string
  roleDescription?: string
  status?: number
}

export interface PermissionTreeNode {
  id: number
  permissionCode: string
  permissionName: string
  permissionType: string
  parentId?: number
  moduleCode?: string
  routePath?: string
  sortOrder?: number
  children: PermissionTreeNode[]
}

export interface CompanyRecord {
  companyId: string
  companyCode: string
  companyName: string
  invoiceTitle?: string
  taxNo?: string
  status: number
  children: CompanyRecord[]
}

export interface CompanyBankAccountRecord {
  id: number
  companyId: string
  companyName?: string
  bankName: string
  province?: string
  city?: string
  branchName?: string
  bankCode?: string
  branchCode?: string
  cnapsCode?: string
  accountName: string
  accountNo: string
  accountType?: string
  accountUsage?: string
  currencyCode?: string
  defaultAccount: number
  status: number
  remark?: string
  directConnectEnabled: number
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
  directConnectLastSyncAt?: string
  directConnectLastSyncStatus?: string
  directConnectLastErrorMsg?: string
  directConnectExtJson?: string
  createdAt?: string
  updatedAt?: string
}

export interface CompanySavePayload {
  companyId?: string
  companyCode?: string
  companyName: string
  invoiceTitle?: string
  taxNo?: string
  status?: number
}

export interface CompanyBankAccountSavePayload {
  companyId: string
  bankName: string
  province: string
  city: string
  branchName?: string
  bankCode?: string
  branchCode?: string
  cnapsCode?: string
  accountName: string
  accountNo: string
  accountType?: string
  accountUsage?: string
  currencyCode?: string
  defaultAccount?: number
  status?: number
  remark?: string
  directConnectEnabled?: number
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
  directConnectLastSyncAt?: string
  directConnectLastSyncStatus?: string
  directConnectLastErrorMsg?: string
  directConnectExtJson?: string
}

export interface SyncConnectorConfig {
  id: number
  platformCode: string
  platformName: string
  enabled: boolean
  autoSyncEnabled: boolean
  syncIntervalMinutes: number
  appKey?: string
  appSecret?: string
  appId?: string
  corpId?: string
  agentId?: string
  lastSyncAt?: string
  lastSyncStatus?: string
  lastSyncMessage?: string
}

export interface SyncConnectorSavePayload {
  platformCode: string
  enabled?: number
  autoSyncEnabled?: number
  syncIntervalMinutes?: number
  appKey?: string
  appSecret?: string
  appId?: string
  corpId?: string
  agentId?: string
}

export interface SyncJobRecord {
  id: number
  jobNo: string
  platformCode: string
  triggerType: string
  status: string
  successCount: number
  skippedCount: number
  failedCount: number
  deletedCount: number
  summary?: string
  startedAt?: string
  finishedAt?: string
}

// 这是 SystemSettingsBootstrapData 的数据结构。
export interface SystemSettingsBootstrapData {
  currentUser: UserProfile
  departments: DepartmentTreeNode[]
  employees: EmployeeRecord[]
  roles: RoleRecord[]
  permissions: PermissionTreeNode[]
  companies: CompanyRecord[]
  companyBankAccounts: CompanyBankAccountRecord[]
  connectors: SyncConnectorConfig[]
  jobs: SyncJobRecord[]
}
