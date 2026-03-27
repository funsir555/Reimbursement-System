const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

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

export interface LoginResponse {
  userId: number
  username: string
  name: string
  token: string
  expireIn: number
  roles: string[]
  permissionCodes: string[]
}

export interface DepartmentTreeNode {
  id: number
  companyId?: string
  deptCode: string
  leaderUserId?: number
  leaderName?: string
  deptName: string
  parentId?: number
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
  status?: number
}

export interface EmployeeQueryPayload {
  keyword?: string
  companyId?: string
  deptId?: number
  status?: number
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
  roleCode: string
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
  bankName?: string
  bankAccountName?: string
  bankAccountNo?: string
  status: number
  children: CompanyRecord[]
}

export interface CompanySavePayload {
  companyId?: string
  companyCode?: string
  companyName: string
  invoiceTitle?: string
  taxNo?: string
  bankName?: string
  bankAccountName?: string
  bankAccountNo?: string
  status?: number
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

export interface SystemSettingsBootstrapData {
  currentUser: UserProfile
  departments: DepartmentTreeNode[]
  employees: EmployeeRecord[]
  roles: RoleRecord[]
  permissions: PermissionTreeNode[]
  companies: CompanyRecord[]
  connectors: SyncConnectorConfig[]
  jobs: SyncJobRecord[]
}

export interface ExpenseSummary {
  no: string
  type: string
  reason: string
  amount: number
  date: string
  status: string
}

export interface ApprovalSummary {
  id: number
  title: string
  submitter: string
  time: string
  amount: number
  avatar: string
}

export interface InvoiceAlert {
  id: number
  title: string
  desc: string
  time: string
}

export interface InvoiceSummary {
  code: string
  number: string
  type: string
  seller: string
  amount: number
  date: string
  status: string
  ocrStatus: string
}

export interface DashboardData {
  user: UserProfile
  pendingApprovalCount: number
  pendingApprovalDelta: number
  monthlyExpenseAmount: number
  monthlyExpenseCount: number
  invoiceCount: number
  monthlyInvoiceCount: number
  budgetRemaining: number
  budgetUsageRate: number
  recentExpenses: ExpenseSummary[]
  pendingApprovals: ApprovalSummary[]
  invoiceAlerts: InvoiceAlert[]
}

export interface ProcessCenterNavItem {
  key: string
  label: string
  tip?: string
}

export interface ProcessCenterSummary {
  totalTemplates: number
  enabledTemplates: number
  draftTemplates: number
  aiAuditTemplates: number
}

export interface ProcessTemplateCard {
  id: number
  templateCode: string
  name: string
  templateType: string
  businessDomain: string
  description: string
  highlights: string[]
  flowName: string
  updatedAt: string
  owner: string
  color: string
}

export interface ProcessTemplateCategory {
  code: string
  name: string
  description: string
  templateCount: number
  templates: ProcessTemplateCard[]
}

export interface ProcessCenterOverview {
  navItems: ProcessCenterNavItem[]
  summary: ProcessCenterSummary
  categories: ProcessTemplateCategory[]
}

export interface ProcessTemplateTypeOption {
  code: string
  name: string
  subtitle: string
  description: string
  accent: string
}

export interface ProcessFormOption {
  label: string
  value: string
}

export interface ProcessTemplateFormOptions {
  templateType: string
  templateTypeLabel: string
  categoryOptions: ProcessFormOption[]
  numberingRulePreview: string
  printModes: ProcessFormOption[]
  approvalFlows: ProcessFormOption[]
  paymentModes: ProcessFormOption[]
  allocationForms: ProcessFormOption[]
  expenseTypes: ProcessExpenseTypeTreeNode[]
  aiAuditModes: ProcessFormOption[]
  scopeOptions: ProcessFormOption[]
  tagOptions: ProcessFormOption[]
  installmentOptions: ProcessFormOption[]
}

export interface ProcessTemplateSavePayload {
  templateType: string
  templateName: string
  templateDescription: string
  category: string
  iconColor: string
  enabled: boolean
  printMode: string
  approvalFlow: string
  paymentMode: string
  allocationForm: string
  expenseTypes: string[]
  aiAuditMode: string
  scopeOptions: string[]
  tagOption: string
  installmentOption: string
}

export interface ProcessTemplateSaveResult {
  id: number
  templateCode: string
  templateName: string
  status: string
}

export interface ProcessCustomArchiveRule {
  id?: number
  groupNo: number
  fieldKey: string
  operator: string
  compareValue: any
}

export interface ProcessCustomArchiveItem {
  id?: number
  itemCode?: string
  itemName: string
  priority?: number
  status?: number
  rules: ProcessCustomArchiveRule[]
}

export interface ProcessCustomArchiveSummary {
  id: number
  archiveCode: string
  archiveName: string
  archiveType: string
  archiveTypeLabel: string
  archiveDescription?: string
  status: number
  itemCount: number
  updatedAt: string
}

export interface ProcessCustomArchiveDetail {
  id?: number
  archiveCode?: string
  archiveName: string
  archiveType: string
  archiveTypeLabel?: string
  archiveDescription?: string
  status?: number
  items: ProcessCustomArchiveItem[]
}

export interface ProcessCustomArchiveSavePayload {
  archiveName: string
  archiveType: string
  archiveDescription?: string
  status?: number
  items: Array<{
    id?: number
    itemName: string
    priority?: number
    status?: number
    rules: ProcessCustomArchiveRule[]
  }>
}

export interface ProcessCustomArchiveOperator {
  key: string
  label: string
}

export interface ProcessCustomArchiveRuleField {
  key: string
  label: string
  valueType: string
  operatorKeys: string[]
}

export interface ProcessCustomArchiveMeta {
  archiveTypeOptions: ProcessFormOption[]
  operatorOptions: ProcessCustomArchiveOperator[]
  ruleFields: ProcessCustomArchiveRuleField[]
  departmentOptions: ProcessFormOption[]
  tagArchiveCode: string
  installmentArchiveCode: string
}

export interface ProcessCustomArchiveStatusPayload {
  status: number
}

export interface ProcessCustomArchiveResolvePayload {
  archiveCode: string
  context: Record<string, unknown>
}

export interface ProcessCustomArchiveResolveItem {
  itemCode: string
  itemName: string
  priority?: number
}

export interface ProcessCustomArchiveResolveResult {
  archiveCode: string
  archiveType: string
  items: ProcessCustomArchiveResolveItem[]
}

export interface ProcessExpenseTypeTreeNode {
  id: number
  parentId?: number
  expenseCode: string
  expenseName: string
  status: number
  children: ProcessExpenseTypeTreeNode[]
}

export interface ProcessExpenseTypeConfigOption {
  value: string
  label: string
  description: string
}

export interface ProcessExpenseTypeDetail {
  id?: number
  parentId?: number
  expenseCode: string
  expenseName: string
  expenseDescription?: string
  codeLevel?: number
  codePrefix?: string
  scopeDeptIds: string[]
  scopeUserIds: string[]
  invoiceFreeMode: string
  taxDeductionMode: string
  taxSeparationMode: string
  status: number
}

export interface ProcessExpenseTypeMeta {
  invoiceFreeOptions: ProcessExpenseTypeConfigOption[]
  taxDeductionOptions: ProcessExpenseTypeConfigOption[]
  taxSeparationOptions: ProcessExpenseTypeConfigOption[]
  departmentOptions: ProcessFormOption[]
  userOptions: ProcessFormOption[]
}

export interface ProcessExpenseTypeSavePayload {
  expenseName: string
  expenseDescription?: string
  expenseCode: string
  scopeDeptIds: string[]
  scopeUserIds: string[]
  invoiceFreeMode: string
  taxDeductionMode: string
  taxSeparationMode: string
  status: number
}

export interface ProcessExpenseTypeStatusPayload {
  status: number
}

export interface BankAccount {
  id: number
  bankName: string
  branchName?: string
  accountName: string
  accountNoMasked: string
  accountType: string
  defaultAccount: boolean
  status: string
}

export interface PersonalCenterData {
  user: UserProfile
  bankAccounts: BankAccount[]
}

export interface ChangePasswordPayload {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

export interface DownloadRecord {
  id: number
  fileName: string
  businessType: string
  status: string
  progress: number
  fileSize: string
  createdAt: string
  finishedAt?: string
}

export interface DownloadCenterData {
  inProgress: DownloadRecord[]
  history: DownloadRecord[]
}

export interface InvoiceTaskPayload {
  code: string
  number: string
}

export interface AsyncTaskSubmitResult {
  taskNo: string
  taskType: string
  businessType: string
  status: string
  message: string
  downloadRecordId?: number
}

export interface NotificationSummary {
  unreadCount: number
  latestTitle?: string
  latestContent?: string
  latestCreatedAt?: string
}

function clearLoginState() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
}

async function request<T>(url: string, options: RequestInit = {}): Promise<ApiResponse<T>> {
  const token = localStorage.getItem('token')
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>)
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(`${API_BASE_URL}${url}`, {
    ...options,
    headers
  })

  const result = await response.json().catch(() => ({ message: '请求失败' }))

  if (response.status === 401) {
    clearLoginState()
    window.location.href = '/login'
    throw new Error(result.message || '登录已过期，请重新登录')
  }

  if (!response.ok) {
    throw new Error(result.message || `HTTP ${response.status}`)
  }

  const payload = result as ApiResponse<T>

  if (payload.code === 401) {
    clearLoginState()
    window.location.href = '/login'
    throw new Error(payload.message || '登录已过期，请重新登录')
  }

  if (payload.code !== 200) {
    throw new Error(payload.message || '请求失败')
  }

  return payload
}

export const authApi = {
  loginByPassword: (username: string, password: string) =>
    request<LoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    }),
  getCurrentUser: () => request<UserProfile>('/auth/me')
}

export const dashboardApi = {
  getOverview: () => request<DashboardData>('/auth/dashboard')
}

export const expenseApi = {
  list: () => request<ExpenseSummary[]>('/auth/expenses')
}

export const invoiceApi = {
  list: () => request<InvoiceSummary[]>('/auth/invoices')
}

export const processApi = {
  getOverview: () => request<ProcessCenterOverview>('/auth/process-management/overview'),
  getTemplateTypes: () => request<ProcessTemplateTypeOption[]>('/auth/process-management/template-types'),
  getFormOptions: (templateType: string) =>
    request<ProcessTemplateFormOptions>(`/auth/process-management/form-options?templateType=${templateType}`),
  createTemplate: (payload: ProcessTemplateSavePayload) =>
    request<ProcessTemplateSaveResult>('/auth/process-management/templates', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  listCustomArchives: () =>
    request<ProcessCustomArchiveSummary[]>('/auth/process-management/custom-archives'),
  getCustomArchiveMeta: () =>
    request<ProcessCustomArchiveMeta>('/auth/process-management/custom-archives/meta'),
  getCustomArchiveDetail: (id: number) =>
    request<ProcessCustomArchiveDetail>(`/auth/process-management/custom-archives/${id}`),
  createCustomArchive: (payload: ProcessCustomArchiveSavePayload) =>
    request<ProcessCustomArchiveDetail>('/auth/process-management/custom-archives', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateCustomArchive: (id: number, payload: ProcessCustomArchiveSavePayload) =>
    request<ProcessCustomArchiveDetail>(`/auth/process-management/custom-archives/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  updateCustomArchiveStatus: (id: number, payload: ProcessCustomArchiveStatusPayload) =>
    request<boolean>(`/auth/process-management/custom-archives/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify(payload)
    }),
  deleteCustomArchive: (id: number) =>
    request<boolean>(`/auth/process-management/custom-archives/${id}`, {
      method: 'DELETE'
    }),
  resolveCustomArchive: (payload: ProcessCustomArchiveResolvePayload) =>
    request<ProcessCustomArchiveResolveResult>('/auth/process-management/custom-archives/resolve', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  listExpenseTypesTree: () =>
    request<ProcessExpenseTypeTreeNode[]>('/auth/process-management/expense-types/tree'),
  getExpenseTypeMeta: () =>
    request<ProcessExpenseTypeMeta>('/auth/process-management/expense-types/meta'),
  getExpenseTypeDetail: (id: number) =>
    request<ProcessExpenseTypeDetail>(`/auth/process-management/expense-types/${id}`),
  createExpenseType: (payload: ProcessExpenseTypeSavePayload) =>
    request<ProcessExpenseTypeDetail>('/auth/process-management/expense-types', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateExpenseType: (id: number, payload: ProcessExpenseTypeSavePayload) =>
    request<ProcessExpenseTypeDetail>(`/auth/process-management/expense-types/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  updateExpenseTypeStatus: (id: number, payload: ProcessExpenseTypeStatusPayload) =>
    request<boolean>(`/auth/process-management/expense-types/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify(payload)
    }),
  deleteExpenseType: (id: number) =>
    request<boolean>(`/auth/process-management/expense-types/${id}`, {
      method: 'DELETE'
    })
}

export const profileApi = {
  getOverview: () => request<PersonalCenterData>('/auth/user-center/profile'),
  changePassword: (payload: ChangePasswordPayload) =>
    request<boolean>('/auth/user-center/password', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
}

export const downloadApi = {
  getCenter: () => request<DownloadCenterData>('/auth/user-center/downloads')
}

export const asyncTaskApi = {
  exportInvoices: () =>
    request<AsyncTaskSubmitResult>('/auth/async-tasks/exports/invoices', {
      method: 'POST'
    }),
  verifyInvoice: (payload: InvoiceTaskPayload) =>
    request<AsyncTaskSubmitResult>('/auth/async-tasks/invoices/verify', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  ocrInvoice: (payload: InvoiceTaskPayload) =>
    request<AsyncTaskSubmitResult>('/auth/async-tasks/invoices/ocr', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
}

export const notificationApi = {
  getSummary: () => request<NotificationSummary>('/auth/async-tasks/notifications/summary')
}

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

export default request
