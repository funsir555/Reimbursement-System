// 这里集中封装 archive-agent.ts 相关接口。
// 上游通常是对应业务页面，下游对应后端同域接口。
// 如果改错，最容易影响页面的加载、保存或提交流程。

import request, { buildQueryString } from './core'
import type { ArchiveAgentDetail, ArchiveAgentMeta, ArchiveAgentRunDetail, ArchiveAgentRunPreflight, ArchiveAgentRunRecord, ArchiveAgentSavePayload, ArchiveAgentSummary, ArchiveAgentTestRunPayload, ArchiveAgentToolTestResult } from './archive-agent-types'

const AGENT_WORKBENCH_API_PREFIX = '/auth/agent/workbench'

// 这一组方法供对应页面统一调用。
export const archiveAgentApi = {
  list: (params: { keyword?: string; status?: string } = {}) =>
    request<ArchiveAgentSummary[]>(`${AGENT_WORKBENCH_API_PREFIX}${buildQueryString(params)}`),
  getMeta: () =>
    request<ArchiveAgentMeta>(`${AGENT_WORKBENCH_API_PREFIX}/meta`),
  getDetail: (id: number) =>
    request<ArchiveAgentDetail>(`${AGENT_WORKBENCH_API_PREFIX}/${id}`),
  create: (payload: ArchiveAgentSavePayload) =>
    request<ArchiveAgentDetail>(AGENT_WORKBENCH_API_PREFIX, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  update: (id: number, payload: ArchiveAgentSavePayload) =>
    request<ArchiveAgentDetail>(`${AGENT_WORKBENCH_API_PREFIX}/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  updateTools: (id: number, payload: { expectedVersionNo: number; toolCodes: string[] }) =>
    request<ArchiveAgentDetail>(`${AGENT_WORKBENCH_API_PREFIX}/${id}/tools`, {
      method: 'PUT', body: JSON.stringify(payload)
    }),
  testTool: (id: number, toolCode: string, payload: { queryTemplateCode?: string; parameters: Record<string, unknown> }) =>
    request<ArchiveAgentToolTestResult>(`${AGENT_WORKBENCH_API_PREFIX}/${id}/tools/${encodeURIComponent(toolCode)}/test`, {
      method: 'POST', body: JSON.stringify(payload)
    }),
  searchToolSuppliers: (id: number, companyId: string, keyword: string) =>
    request<Array<{ code: string; name: string }>>(`${AGENT_WORKBENCH_API_PREFIX}/${id}/tools/finance.query_supplier_detail_ledger/suppliers${buildQueryString({ companyId, keyword })}`),
  publish: (id: number) =>
    request<ArchiveAgentDetail>(`${AGENT_WORKBENCH_API_PREFIX}/${id}/publish`, {
      method: 'POST'
    }),
  remove: (id: number) =>
    request<null>(`${AGENT_WORKBENCH_API_PREFIX}/${id}`, {
      method: 'DELETE'
    }),
  run: (id: number, payload: ArchiveAgentTestRunPayload) =>
    request<ArchiveAgentRunRecord>(`${AGENT_WORKBENCH_API_PREFIX}/${id}/run`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  prepareRun: (id: number, payload: ArchiveAgentTestRunPayload) =>
    request<ArchiveAgentRunPreflight>(`${AGENT_WORKBENCH_API_PREFIX}/${id}/run/prepare`, {
      method: 'POST', body: JSON.stringify(payload)
    }),
  listRuns: (id: number) =>
    request<ArchiveAgentRunRecord[]>(`${AGENT_WORKBENCH_API_PREFIX}/${id}/runs`),
  getRunDetail: (runId: number) =>
    request<ArchiveAgentRunDetail>(`${AGENT_WORKBENCH_API_PREFIX}/runs/${runId}`),
  toggleStatus: (id: number, status: string) =>
    request<ArchiveAgentDetail>(`${AGENT_WORKBENCH_API_PREFIX}/${id}/toggle-status`, {
      method: 'POST',
      body: JSON.stringify({ status })
    })
}
