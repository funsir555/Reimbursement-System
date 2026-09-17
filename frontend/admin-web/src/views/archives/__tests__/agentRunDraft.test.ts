import { beforeEach, describe, expect, it } from 'vitest'
import { readAgentRunDraft, removeAgentRunDraft, writeAgentRunDraft } from '../agentRunDraft'

describe('agentRunDraft', () => {
  const fallback = {
    triggerSource: 'workbench.manual',
    inputPayloadText: '{"query":"default"}'
  }

  beforeEach(() => {
    sessionStorage.clear()
  })

  it('restores an edited draft for the same user and agent', () => {
    writeAgentRunDraft(sessionStorage, 'user-1', 5, {
      triggerSource: 'workbench.manual',
      inputPayloadText: '{"query":"edited"}'
    })

    expect(readAgentRunDraft(sessionStorage, 'user-1', 5, fallback).inputPayloadText)
      .toBe('{"query":"edited"}')
  })

  it('isolates drafts by user and agent', () => {
    writeAgentRunDraft(sessionStorage, 'user-1', 5, {
      triggerSource: 'workbench.manual',
      inputPayloadText: '{"query":"edited"}'
    })

    expect(readAgentRunDraft(sessionStorage, 'user-2', 5, fallback)).toEqual(fallback)
    expect(readAgentRunDraft(sessionStorage, 'user-1', 6, fallback)).toEqual(fallback)
  })

  it('removes a draft when its agent is deleted', () => {
    writeAgentRunDraft(sessionStorage, 'user-1', 5, {
      triggerSource: 'workbench.manual',
      inputPayloadText: '{"query":"edited"}'
    })
    removeAgentRunDraft(sessionStorage, 'user-1', 5)

    expect(readAgentRunDraft(sessionStorage, 'user-1', 5, fallback)).toEqual(fallback)
  })
})
