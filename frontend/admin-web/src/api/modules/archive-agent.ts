import request, { buildQueryString } from './core'
import type { ArchiveAgentDetail, ArchiveAgentMeta, ArchiveAgentRunDetail, ArchiveAgentRunRecord, ArchiveAgentSavePayload, ArchiveAgentSummary, ArchiveAgentTestRunPayload } from './shared'

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
