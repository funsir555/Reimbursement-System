// 这里集中封装 archive-agent.ts 相关接口。
// 上游通常是对应业务页面，下游对应后端同域接口。
// 如果改错，最容易影响页面的加载、保存或提交流程。

import request, { buildQueryString } from './core'
import type { ArchiveAgentDetail, ArchiveAgentMeta, ArchiveAgentRunDetail, ArchiveAgentRunRecord, ArchiveAgentSavePayload, ArchiveAgentSummary, ArchiveAgentTestRunPayload } from './archive-agent-types'

// 这一组方法供对应页面统一调用。
export const archiveAgentApi = {
  list: (params: { keyword?: string; status?: string } = {}) =>
    request<ArchiveAgentSummary[]>(`/auth/archives/agents${buildQueryString(params)}`),
  getMeta: () =>
    request<ArchiveAgentMeta>('/auth/archives/agents/meta'),
  getDetail: (id: number) =>
    request<ArchiveAgentDetail>(`/auth/archives/agents/${id}`),
  create: (payload: ArchiveAgentSavePayload) =>
    request<ArchiveAgentDetail>('/auth/archives/agents', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  update: (id: number, payload: ArchiveAgentSavePayload) =>
    request<ArchiveAgentDetail>(`/auth/archives/agents/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
  publish: (id: number) =>
    request<ArchiveAgentDetail>(`/auth/archives/agents/${id}/publish`, {
      method: 'POST'
    }),
  run: (id: number, payload: ArchiveAgentTestRunPayload) =>
    request<ArchiveAgentRunRecord>(`/auth/archives/agents/${id}/run`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  listRuns: (id: number) =>
    request<ArchiveAgentRunRecord[]>(`/auth/archives/agents/${id}/runs`),
  getRunDetail: (runId: number) =>
    request<ArchiveAgentRunDetail>(`/auth/archives/agents/runs/${runId}`),
  toggleStatus: (id: number, status: string) =>
    request<ArchiveAgentDetail>(`/auth/archives/agents/${id}/toggle-status`, {
      method: 'POST',
      body: JSON.stringify({ status })
    })
}
