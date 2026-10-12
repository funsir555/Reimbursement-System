import request from './core'

export interface DigitalEmployee {
  id: number
  name: string
  company_id: string
  responsible_user_id: number
  description?: string
  avatar_code?: string
  lifecycle_status: 'DRAFT' | 'ACTIVE' | 'SUSPENDED' | 'ARCHIVED'
  daily_run_limit?: number
  concurrency_limit?: number
  version_no: number
  assigned_count?: number
  workflow_count?: number
  subscriber_count?: number
  exception_count?: number
  bindings?: WorkflowBinding[]
  userIds?: number[]
  companyLimits?: Array<{ permission_code: string; company_id: string }>
}

export interface WorkflowBinding {
  id: number
  share_id: number
  permission_code: string
  agent_id: number
  agent_version_id: number
  agent_name: string
  share_status: string
  agent_status: string
  published: number
  status: number
}

export interface WorkflowShare {
  id: number
  agent_id: number
  agent_version_id: number
  agent_name: string
  permission_code: string
  permission_codes?: string[]
  tool_nodes?: Array<{ nodeKey: string; toolCode: string;
    fields: Array<{ key: string; label: string; type: string; required: boolean }> }>
  version_no: number
  version_label?: string
  author_user_id: number
  reviewer_user_id?: number
  status: string
}

export interface EmployeePlan {
  id: number
  employee_id: number
  workflow_binding_id: number
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY'
  run_hour: number
  run_minute: number
  week_day?: number
  month_day?: number
  parameter_json: string
  status: string
  next_run_at?: string
  version_no: number
  subscriber_count?: number
}

export interface PlanPayload {
  workflowBindingId: number
  frequency: EmployeePlan['frequency']
  runHour: number
  runMinute: number
  weekDay?: number
  monthDay?: number
  parameters: Record<string, unknown>
  versionNo?: number
  enabled: boolean
}

export interface EmployeeMetrics {
  days: number
  assignedUsers: number
  subscribers: number
  successfulRuns: number
  zeroResults: number
  permissionDenials: number
  executionFailures: number
  onTimeDeliveries: number
  dailyRunLimit?: number
  concurrencyLimit?: number
  runsToday: number
  pausedPlans: number
}

export interface EmployeeDirectory {
  users: Array<{ id: number; name: string; username: string; company_id: string }>
  companies: Array<{ company_id: string; company_name: string }>
}

const base = '/auth/agent/digital-employees'
export const digitalEmployeeApi = {
  list: () => request<DigitalEmployee[]>(base),
  directory: () => request<EmployeeDirectory>(`${base}/directory`),
  preview: (id: number, userId: number, permissionCode: string) =>
    request<{ authorized: boolean; companyIds?: string[]; companyCount?: number; reason?: string }>(
      `${base}/${id}/preview?userId=${userId}&permissionCode=${encodeURIComponent(permissionCode)}`),
  detail: (id: number) => request<DigitalEmployee>(`${base}/${id}`),
  create: (payload: { name: string; description?: string; avatarCode?: string; responsibleUserId: number }) =>
    request<DigitalEmployee>(base, { method: 'POST', body: JSON.stringify(payload) }),
  update: (id: number, payload: { name: string; description?: string; avatarCode?: string; responsibleUserId: number; versionNo: number }) =>
    request<DigitalEmployee>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  status: (id: number, status: string, versionNo: number) =>
    request<DigitalEmployee>(`${base}/${id}/status`, { method: 'PUT', body: JSON.stringify({ status, versionNo }) }),
  assign: (id: number, userIds: number[], versionNo: number) =>
    request<DigitalEmployee>(`${base}/${id}/users`, { method: 'PUT', body: JSON.stringify({ userIds, versionNo }) }),
  bind: (id: number, shareId: number, permissionCode: string, companyLimit: string[], versionNo: number,
         companyLimits?: Record<string, string[]>) =>
    request<DigitalEmployee>(`${base}/${id}/workflows`, { method: 'POST', body: JSON.stringify({ shareId, permissionCode, companyLimit, versionNo, companyLimits }) }),
  unbind: (id: number, bindingId: number, versionNo: number) =>
    request<boolean>(`${base}/${id}/workflows/${bindingId}?versionNo=${versionNo}`, { method: 'DELETE' }),
  budget: (id: number, dailyRunLimit: number, concurrencyLimit: number, versionNo: number) =>
    request<DigitalEmployee>(`${base}/${id}/budget`, { method: 'PUT', body: JSON.stringify({ dailyRunLimit, concurrencyLimit, versionNo }) }),
  plans: (id: number) => request<EmployeePlan[]>(`${base}/${id}/plans`),
  savePlan: (id: number, payload: PlanPayload, planId?: number) =>
    request<EmployeePlan>(`${base}/${id}/plans${planId ? `/${planId}` : ''}`, {
      method: planId ? 'PUT' : 'POST', body: JSON.stringify(payload)
    }),
  metrics: (id: number, days: 7 | 30) => request<EmployeeMetrics>(`${base}/${id}/metrics?days=${days}`),
  audit: (id: number) => request<Array<Record<string, string | number>>>(`${base}/${id}/audit`),
  shares: () => request<WorkflowShare[]>(`${base}/workflow-shares`),
  submitShare: (agentId: number, versionId: number) =>
    request<WorkflowShare>(`${base}/workflow-shares`, { method: 'POST', body: JSON.stringify({ agentId, versionId }) }),
  reviewShare: (shareId: number, approve: boolean) =>
    request<WorkflowShare>(`${base}/workflow-shares/${shareId}/review`, { method: 'PUT', body: JSON.stringify({ approve }) }),
  revokeShare: (shareId: number) =>
    request<WorkflowShare>(`${base}/workflow-shares/${shareId}/revoke`, { method: 'PUT' })
}
