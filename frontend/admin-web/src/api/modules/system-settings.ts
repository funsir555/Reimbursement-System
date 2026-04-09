import request from './core'
import type { CompanyBankAccountRecord, CompanyBankAccountSavePayload, CompanyRecord, CompanySavePayload, DepartmentSavePayload, DepartmentTreeNode, EmployeeQueryPayload, EmployeeRecord, EmployeeSavePayload, PermissionTreeNode, RoleRecord, RoleSavePayload, SyncConnectorConfig, SyncConnectorSavePayload, SyncJobRecord, SystemSettingsBootstrapData } from './shared'

export const systemSettingsApi = {
  getBootstrap: () => request<SystemSettingsBootstrapData>('/auth/system-settings/bootstrap'),
  listDepartments: () => request<DepartmentTreeNode[]>('/auth/system-settings/departments'),
  createDepartment: (payload: DepartmentSavePayload) =>
    request<DepartmentTreeNode>('/auth/system-settings/departments', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateDepartment: (id: number, payload: DepartmentSavePayload) =>
    request<DepartmentTreeNode>(`/auth/system-settings/departments/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  deleteDepartment: (id: number) =>
    request<boolean>(`/auth/system-settings/departments/${id}`, {
      method: 'DELETE'
    }),
  queryEmployees: (payload: EmployeeQueryPayload = {}) =>
    request<EmployeeRecord[]>('/auth/system-settings/employees/query', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  createEmployee: (payload: EmployeeSavePayload) =>
    request<EmployeeRecord>('/auth/system-settings/employees', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateEmployee: (id: number, payload: EmployeeSavePayload) =>
    request<EmployeeRecord>(`/auth/system-settings/employees/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  deleteEmployee: (id: number) =>
    request<boolean>(`/auth/system-settings/employees/${id}`, {
      method: 'DELETE'
    }),
  listRoles: () => request<RoleRecord[]>('/auth/system-settings/roles'),
  createRole: (payload: RoleSavePayload) =>
    request<RoleRecord>('/auth/system-settings/roles', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateRole: (id: number, payload: RoleSavePayload) =>
    request<RoleRecord>(`/auth/system-settings/roles/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  deleteRole: (id: number) =>
    request<boolean>(`/auth/system-settings/roles/${id}`, {
      method: 'DELETE'
    }),
  assignRolePermissions: (id: number, permissionCodes: string[]) =>
    request<boolean>(`/auth/system-settings/roles/${id}/permissions`, {
      method: 'POST',
      body: JSON.stringify({ permissionCodes })
    }),
  assignUserRoles: (id: number, roleIds: number[]) =>
    request<boolean>(`/auth/system-settings/users/${id}/roles`, {
      method: 'POST',
      body: JSON.stringify({ roleIds })
    }),
  getPermissionTree: () => request<PermissionTreeNode[]>('/auth/system-settings/permissions/tree'),
  listCompanies: () => request<CompanyRecord[]>('/auth/system-settings/companies'),
  createCompany: (payload: CompanySavePayload) =>
    request<CompanyRecord>('/auth/system-settings/companies', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateCompany: (companyId: string, payload: CompanySavePayload) =>
    request<CompanyRecord>(`/auth/system-settings/companies/${companyId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  deleteCompany: (companyId: string) =>
    request<boolean>(`/auth/system-settings/companies/${companyId}`, {
      method: 'DELETE'
    }),
  listCompanyBankAccounts: () => request<CompanyBankAccountRecord[]>('/auth/system-settings/company-bank-accounts'),
  createCompanyBankAccount: (payload: CompanyBankAccountSavePayload) =>
    request<CompanyBankAccountRecord>('/auth/system-settings/company-bank-accounts', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateCompanyBankAccount: (id: number, payload: CompanyBankAccountSavePayload) =>
    request<CompanyBankAccountRecord>(`/auth/system-settings/company-bank-accounts/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  deleteCompanyBankAccount: (id: number) =>
    request<boolean>(`/auth/system-settings/company-bank-accounts/${id}`, {
      method: 'DELETE'
    }),
  listSyncConnectors: () => request<SyncConnectorConfig[]>('/auth/system-settings/sync/connectors'),
  updateSyncConnector: (payload: SyncConnectorSavePayload) =>
    request<SyncConnectorConfig>('/auth/system-settings/sync/connectors', {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  runSync: (platformCodes: string[], triggerType = 'MANUAL') =>
    request<SyncJobRecord>('/auth/system-settings/sync/run', {
      method: 'POST',
      body: JSON.stringify({ platformCodes, triggerType })
    }),
  listSyncJobs: () => request<SyncJobRecord[]>('/auth/system-settings/sync/jobs')
}
