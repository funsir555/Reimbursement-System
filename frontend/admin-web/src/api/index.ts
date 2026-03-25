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
}

export interface LoginResponse {
  userId: number
  username: string
  name: string
  token: string
  expireIn: number
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
  numberingRules: ProcessFormOption[]
  printModes: ProcessFormOption[]
  approvalFlows: ProcessFormOption[]
  paymentModes: ProcessFormOption[]
  travelForms: ProcessFormOption[]
  allocationForms: ProcessFormOption[]
  expenseTypes: ProcessFormOption[]
  aiAuditModes: ProcessFormOption[]
  scopeOptions: ProcessFormOption[]
  tagOptions: ProcessFormOption[]
}

export interface ProcessTemplateSavePayload {
  templateType: string
  templateName: string
  templateDescription: string
  category: string
  numberingRule: string
  iconColor: string
  enabled: boolean
  printMode: string
  approvalFlow: string
  paymentMode: string
  splitPayment: boolean
  travelForm: string
  allocationForm: string
  expenseTypes: string[]
  aiAuditMode: string
  scopeOptions: string[]
  tagOptions: string[]
  relationRemark: string
  validationRemark: string
  installmentRemark: string
}

export interface ProcessTemplateSaveResult {
  id: number
  templateCode: string
  templateName: string
  status: string
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

  return result as ApiResponse<T>
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

export default request
