import { describe, expect, it, vi } from 'vitest'
import type { ProcessFlowDetail, ProcessFlowSavePayload } from '@/api'
import { persistFlowDraft, publishFlowAfterPersist } from '@/views/process/flowDesignerPersistence'

function buildPayload(): ProcessFlowSavePayload {
  return {
    flowName: 'Flow Alpha',
    flowDescription: 'desc',
    nodes: [
      {
        nodeKey: 'approval-1',
        nodeType: 'APPROVAL',
        nodeName: 'Approval Node 1',
        displayOrder: 1,
        config: {
          approverType: 'DESIGNATED_MEMBER',
          managerConfig: {},
          designatedMemberConfig: {
            userIds: [101, 202]
          },
          manualSelectConfig: {
            candidateScope: 'ALL_ACTIVE_USERS'
          }
        }
      }
    ],
    routes: []
  }
}

function buildDetail(id: number): ProcessFlowDetail {
  return {
    id,
    flowCode: `FLOW-${id}`,
    flowName: 'Flow Alpha',
    flowDescription: 'desc',
    status: 'DRAFT',
    statusLabel: 'Draft',
    nodes: [],
    routes: []
  }
}

describe('flowDesignerPersistence', () => {
  it('persists designated member payload when updating an existing flow', async () => {
    const payload = buildPayload()
    const api = {
      createFlow: vi.fn(),
      updateFlow: vi.fn().mockResolvedValue({ data: buildDetail(9) }),
      publishFlow: vi.fn()
    }

    await persistFlowDraft(api, 9, payload)

    expect(api.updateFlow).toHaveBeenCalledWith(9, payload)
    expect(api.updateFlow.mock.calls[0][1].nodes[0].config.designatedMemberConfig.userIds).toEqual([101, 202])
    expect(api.createFlow).not.toHaveBeenCalled()
  })

  it('updates first and then publishes for an existing flow', async () => {
    const payload = buildPayload()
    const callOrder: string[] = []
    const draftDetail = buildDetail(9)
    const publishedDetail = {
      ...buildDetail(9),
      status: 'ENABLED',
      statusLabel: 'Enabled'
    }
    const api = {
      createFlow: vi.fn(),
      updateFlow: vi.fn().mockImplementation(async () => {
        callOrder.push('update')
        return { data: draftDetail }
      }),
      publishFlow: vi.fn().mockImplementation(async () => {
        callOrder.push('publish')
        return { data: publishedDetail }
      })
    }
    const onDraftPersisted = vi.fn().mockResolvedValue(undefined)

    const result = await publishFlowAfterPersist(api, 9, payload, onDraftPersisted)

    expect(callOrder).toEqual(['update', 'publish'])
    expect(onDraftPersisted).toHaveBeenCalledWith(draftDetail)
    expect(result.draftDetail).toEqual(draftDetail)
    expect(result.publishedDetail).toEqual(publishedDetail)
  })

  it('creates first and then publishes for a new flow', async () => {
    const payload = buildPayload()
    const callOrder: string[] = []
    const api = {
      createFlow: vi.fn().mockImplementation(async () => {
        callOrder.push('create')
        return { data: buildDetail(12) }
      }),
      updateFlow: vi.fn(),
      publishFlow: vi.fn().mockImplementation(async () => {
        callOrder.push('publish')
        return { data: { ...buildDetail(12), status: 'ENABLED', statusLabel: 'Enabled' } }
      })
    }

    await publishFlowAfterPersist(api, undefined, payload)

    expect(callOrder).toEqual(['create', 'publish'])
    expect(api.updateFlow).not.toHaveBeenCalled()
    expect(api.publishFlow).toHaveBeenCalledWith(12)
  })

  it('does not publish when persisting the draft fails', async () => {
    const payload = buildPayload()
    const api = {
      createFlow: vi.fn(),
      updateFlow: vi.fn().mockRejectedValue(new Error('save failed')),
      publishFlow: vi.fn()
    }

    await expect(publishFlowAfterPersist(api, 9, payload)).rejects.toThrow('save failed')
    expect(api.publishFlow).not.toHaveBeenCalled()
  })
})
