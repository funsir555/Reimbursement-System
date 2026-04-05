import type { ProcessFlowDetail, ProcessFlowSavePayload } from '@/api'

type FlowPersistenceApi = {
  createFlow: (payload: ProcessFlowSavePayload) => Promise<{ data: ProcessFlowDetail }>
  updateFlow: (id: number, payload: ProcessFlowSavePayload) => Promise<{ data: ProcessFlowDetail }>
  publishFlow: (id: number) => Promise<{ data: ProcessFlowDetail }>
}

export async function persistFlowDraft(
  api: FlowPersistenceApi,
  flowId: number | undefined,
  payload: ProcessFlowSavePayload
): Promise<ProcessFlowDetail> {
  const response = flowId ? await api.updateFlow(flowId, payload) : await api.createFlow(payload)
  return response.data
}

export async function publishFlowAfterPersist(
  api: FlowPersistenceApi,
  flowId: number | undefined,
  payload: ProcessFlowSavePayload,
  onDraftPersisted?: (detail: ProcessFlowDetail) => Promise<void> | void
): Promise<{ draftDetail: ProcessFlowDetail; publishedDetail: ProcessFlowDetail }> {
  const draftDetail = await persistFlowDraft(api, flowId, payload)
  await onDraftPersisted?.(draftDetail)
  if (!draftDetail.id) {
    throw new Error('Flow id missing after save')
  }
  const publishedDetail = (await api.publishFlow(draftDetail.id)).data
  return {
    draftDetail,
    publishedDetail
  }
}
