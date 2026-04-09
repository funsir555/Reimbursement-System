import request, { buildQueryString } from './core'
import type { ProcessCenterOverview, ProcessCustomArchiveDetail, ProcessCustomArchiveMeta, ProcessCustomArchiveResolvePayload, ProcessCustomArchiveResolveResult, ProcessCustomArchiveSavePayload, ProcessCustomArchiveStatusPayload, ProcessCustomArchiveSummary, ProcessExpenseDetailDesignDetail, ProcessExpenseDetailDesignSavePayload, ProcessExpenseDetailDesignSummary, ProcessExpenseTypeDetail, ProcessExpenseTypeMeta, ProcessExpenseTypeSavePayload, ProcessExpenseTypeStatusPayload, ProcessExpenseTypeTreeNode, ProcessFlowDetail, ProcessFlowMeta, ProcessFlowResolveApproversPayload, ProcessFlowResolveApproversResult, ProcessFlowSavePayload, ProcessFlowScene, ProcessFlowSceneSavePayload, ProcessFlowStatusPayload, ProcessFlowSummary, ProcessFormDesignDetail, ProcessFormDesignSavePayload, ProcessFormDesignSummary, ProcessTemplateDetail, ProcessTemplateFormOptions, ProcessTemplateSavePayload, ProcessTemplateSaveResult, ProcessTemplateTypeOption } from './shared'

export const processApi = {
  getOverview: () => request<ProcessCenterOverview>('/auth/process-management/overview'),
  getTemplateTypes: () => request<ProcessTemplateTypeOption[]>('/auth/process-management/template-types'),
  getFormOptions: (templateType: string) =>
    request<ProcessTemplateFormOptions>(`/auth/process-management/form-options?templateType=${templateType}`),
  getTemplateDetail: (id: number) =>
    request<ProcessTemplateDetail>(`/auth/process-management/templates/${id}`),
  createTemplate: (payload: ProcessTemplateSavePayload) =>
    request<ProcessTemplateSaveResult>('/auth/process-management/templates', {
      method: 'POST',
      body: JSON.stringify(payload),
      timeoutMs: 15000,
      timeoutMessage: '保存模板超时，请检查后端服务或稍后重试'
    }),
  updateTemplate: (id: number, payload: ProcessTemplateSavePayload) =>
    request<ProcessTemplateSaveResult>(`/auth/process-management/templates/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
      timeoutMs: 15000,
      timeoutMessage: '保存模板超时，请检查后端服务或稍后重试'
    }),
  deleteTemplate: (id: number) =>
    request<boolean>(`/auth/process-management/templates/${id}`, {
      method: 'DELETE'
    }),
  listExpenseDetailDesigns: () =>
    request<ProcessExpenseDetailDesignSummary[]>('/auth/process-management/expense-detail-designs'),
  getExpenseDetailDesignDetail: (id: number) =>
    request<ProcessExpenseDetailDesignDetail>(`/auth/process-management/expense-detail-designs/${id}`),
  createExpenseDetailDesign: (payload: ProcessExpenseDetailDesignSavePayload) =>
    request<ProcessExpenseDetailDesignDetail>('/auth/process-management/expense-detail-designs', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateExpenseDetailDesign: (id: number, payload: ProcessExpenseDetailDesignSavePayload) =>
    request<ProcessExpenseDetailDesignDetail>(`/auth/process-management/expense-detail-designs/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  deleteExpenseDetailDesign: (id: number) =>
    request<boolean>(`/auth/process-management/expense-detail-designs/${id}`, {
      method: 'DELETE'
    }),
  listFormDesigns: (templateType?: string) =>
    request<ProcessFormDesignSummary[]>(`/auth/process-management/form-designs${buildQueryString({ templateType })}`),
  getFormDesignDetail: (id: number) =>
    request<ProcessFormDesignDetail>(`/auth/process-management/form-designs/${id}`),
  createFormDesign: (payload: ProcessFormDesignSavePayload) =>
    request<ProcessFormDesignDetail>('/auth/process-management/form-designs', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateFormDesign: (id: number, payload: ProcessFormDesignSavePayload) =>
    request<ProcessFormDesignDetail>(`/auth/process-management/form-designs/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  deleteFormDesign: (id: number) =>
    request<boolean>(`/auth/process-management/form-designs/${id}`, {
      method: 'DELETE'
    }),
  listFlows: () =>
    request<ProcessFlowSummary[]>('/auth/process-management/flows'),
  getFlowMeta: () =>
    request<ProcessFlowMeta>('/auth/process-management/flows/meta'),
  getFlowDetail: (id: number) =>
    request<ProcessFlowDetail>(`/auth/process-management/flows/${id}`),
  createFlow: (payload: ProcessFlowSavePayload) =>
    request<ProcessFlowDetail>('/auth/process-management/flows', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateFlow: (id: number, payload: ProcessFlowSavePayload) =>
    request<ProcessFlowDetail>(`/auth/process-management/flows/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  publishFlow: (id: number) =>
    request<ProcessFlowDetail>(`/auth/process-management/flows/${id}/publish`, {
      method: 'POST'
    }),
  updateFlowStatus: (id: number, payload: ProcessFlowStatusPayload) =>
    request<boolean>(`/auth/process-management/flows/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify(payload)
    }),
  resolveFlowApprovers: (payload: ProcessFlowResolveApproversPayload) =>
    request<ProcessFlowResolveApproversResult>('/auth/process-management/flows/resolve-approvers', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  createFlowScene: (payload: ProcessFlowSceneSavePayload) =>
    request<ProcessFlowScene>('/auth/process-management/flow-scenes', {
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
