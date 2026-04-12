// 这里集中封装 finance-archive.ts 相关接口。
// 上游通常是对应业务页面，下游对应后端同域接口。
// 如果改错，最容易影响页面的加载、保存或提交流程。

import request, { buildQueryString } from './core'
import type { FinanceAccountSubjectDetail, FinanceAccountSubjectMeta, FinanceAccountSubjectSavePayload, FinanceAccountSubjectSummary, FinanceCustomerDetail, FinanceCustomerSavePayload, FinanceCustomerSummary, FinanceProjectArchiveMeta, FinanceProjectClassSavePayload, FinanceProjectClassSummary, FinanceProjectDetail, FinanceProjectSavePayload, FinanceProjectSummary, FinanceVendorDetail, FinanceVendorSavePayload, FinanceVendorSummary } from './finance-archive-types'
import type { EmployeeQueryPayload, EmployeeRecord, FinanceEmployeeArchiveMeta } from './system-settings-types'

// 这一组方法供对应页面统一调用。
export const financeArchiveApi = {
  getAccountSubjectMeta: () =>
    request<FinanceAccountSubjectMeta>('/auth/finance/archives/account-subjects/meta'),
  // 处理 listAccountSubjects 请求。
  listAccountSubjects: (params: {
    companyId: string
    keyword?: string
    subjectCategory?: string
    status?: number
    bclose?: number
  }) =>
    request<FinanceAccountSubjectSummary[]>(`/auth/finance/archives/account-subjects${buildQueryString(params)}`),
  getAccountSubjectDetail: (companyId: string, subjectCode: string) =>
    request<FinanceAccountSubjectDetail>(
      `/auth/finance/archives/account-subjects/${encodeURIComponent(subjectCode)}${buildQueryString({ companyId })}`
    ),
  createAccountSubject: (companyId: string, payload: FinanceAccountSubjectSavePayload) =>
    request<FinanceAccountSubjectDetail>(`/auth/finance/archives/account-subjects${buildQueryString({ companyId })}`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateAccountSubject: (companyId: string, subjectCode: string, payload: FinanceAccountSubjectSavePayload) =>
    request<FinanceAccountSubjectDetail>(
      `/auth/finance/archives/account-subjects/${encodeURIComponent(subjectCode)}${buildQueryString({ companyId })}`,
      {
        method: 'PUT',
        body: JSON.stringify(payload)
      }
    ),
  updateAccountSubjectStatus: (companyId: string, subjectCode: string, status: number) =>
    request<boolean>(
      `/auth/finance/archives/account-subjects/${encodeURIComponent(subjectCode)}/status${buildQueryString({ companyId })}`,
      {
        method: 'POST',
        body: JSON.stringify({ status })
      }
    ),
  updateAccountSubjectClose: (companyId: string, subjectCode: string, bclose: number) =>
    request<boolean>(
      `/auth/finance/archives/account-subjects/${encodeURIComponent(subjectCode)}/close${buildQueryString({ companyId })}`,
      {
        method: 'POST',
        body: JSON.stringify({ bclose })
      }
    ),
  getProjectArchiveMeta: (companyId: string) =>
    request<FinanceProjectArchiveMeta>(`/auth/finance/archives/projects/meta${buildQueryString({ companyId })}`),
  listProjectClasses: (params: { companyId: string; keyword?: string; status?: number }) =>
    request<FinanceProjectClassSummary[]>(`/auth/finance/archives/projects/classes${buildQueryString(params)}`),
  createProjectClass: (companyId: string, payload: FinanceProjectClassSavePayload) =>
    request<FinanceProjectClassSummary>(`/auth/finance/archives/projects/classes${buildQueryString({ companyId })}`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateProjectClass: (companyId: string, projectClassCode: string, payload: FinanceProjectClassSavePayload) =>
    request<FinanceProjectClassSummary>(
      `/auth/finance/archives/projects/classes/${encodeURIComponent(projectClassCode)}${buildQueryString({ companyId })}`,
      {
        method: 'PUT',
        body: JSON.stringify(payload)
      }
    ),
  updateProjectClassStatus: (companyId: string, projectClassCode: string, status: number) =>
    request<boolean>(
      `/auth/finance/archives/projects/classes/${encodeURIComponent(projectClassCode)}/status${buildQueryString({ companyId })}`,
      {
        method: 'POST',
        body: JSON.stringify({ status })
      }
    ),
  // 处理 listProjects 请求。
  listProjects: (params: {
    companyId: string
    keyword?: string
    projectClassCode?: string
    status?: number
    bclose?: number
  }) => request<FinanceProjectSummary[]>(`/auth/finance/archives/projects${buildQueryString(params)}`),
  getProjectDetail: (companyId: string, projectCode: string) =>
    request<FinanceProjectDetail>(
      `/auth/finance/archives/projects/${encodeURIComponent(projectCode)}${buildQueryString({ companyId })}`
    ),
  createProject: (companyId: string, payload: FinanceProjectSavePayload) =>
    request<FinanceProjectDetail>(`/auth/finance/archives/projects${buildQueryString({ companyId })}`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateProject: (companyId: string, projectCode: string, payload: FinanceProjectSavePayload) =>
    request<FinanceProjectDetail>(
      `/auth/finance/archives/projects/${encodeURIComponent(projectCode)}${buildQueryString({ companyId })}`,
      {
        method: 'PUT',
        body: JSON.stringify(payload)
      }
    ),
  updateProjectStatus: (companyId: string, projectCode: string, status: number) =>
    request<boolean>(
      `/auth/finance/archives/projects/${encodeURIComponent(projectCode)}/status${buildQueryString({ companyId })}`,
      {
        method: 'POST',
        body: JSON.stringify({ status })
      }
    ),
  updateProjectClose: (companyId: string, projectCode: string, bclose: number) =>
    request<boolean>(
      `/auth/finance/archives/projects/${encodeURIComponent(projectCode)}/close${buildQueryString({ companyId })}`,
      {
        method: 'POST',
        body: JSON.stringify({ bclose })
      }
    ),
  listCustomers: (params: { companyId: string; keyword?: string; includeDisabled?: boolean }) =>
    request<FinanceCustomerSummary[]>(`/auth/finance/archives/customers${buildQueryString(params)}`),
  getCustomerDetail: (companyId: string, customerCode: string) =>
    request<FinanceCustomerDetail>(`/auth/finance/archives/customers/${encodeURIComponent(customerCode)}${buildQueryString({ companyId })}`),
  createCustomer: (companyId: string, payload: FinanceCustomerSavePayload) =>
    request<FinanceCustomerDetail>(`/auth/finance/archives/customers${buildQueryString({ companyId })}`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateCustomer: (companyId: string, customerCode: string, payload: FinanceCustomerSavePayload) =>
    request<FinanceCustomerDetail>(`/auth/finance/archives/customers/${encodeURIComponent(customerCode)}${buildQueryString({ companyId })}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  disableCustomer: (companyId: string, customerCode: string) =>
    request<boolean>(`/auth/finance/archives/customers/${encodeURIComponent(customerCode)}${buildQueryString({ companyId })}`, {
      method: 'DELETE'
    }),
  listSuppliers: (params: { companyId: string; keyword?: string; includeDisabled?: boolean }) =>
    request<FinanceVendorSummary[]>(`/auth/finance/archives/suppliers${buildQueryString(params)}`),
  getSupplierDetail: (companyId: string, vendorCode: string) =>
    request<FinanceVendorDetail>(`/auth/finance/archives/suppliers/${encodeURIComponent(vendorCode)}${buildQueryString({ companyId })}`),
  createSupplier: (companyId: string, payload: FinanceVendorSavePayload) =>
    request<FinanceVendorDetail>(`/auth/finance/archives/suppliers${buildQueryString({ companyId })}`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateSupplier: (companyId: string, vendorCode: string, payload: FinanceVendorSavePayload) =>
    request<FinanceVendorDetail>(`/auth/finance/archives/suppliers/${encodeURIComponent(vendorCode)}${buildQueryString({ companyId })}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  disableSupplier: (companyId: string, vendorCode: string) =>
    request<boolean>(`/auth/finance/archives/suppliers/${encodeURIComponent(vendorCode)}${buildQueryString({ companyId })}`, {
      method: 'DELETE'
    }),
  getEmployeeMeta: () =>
    request<FinanceEmployeeArchiveMeta>('/auth/finance/archives/employees/meta'),
  queryEmployees: (payload: EmployeeQueryPayload = {}) =>
    request<EmployeeRecord[]>('/auth/finance/archives/employees/query', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
}
