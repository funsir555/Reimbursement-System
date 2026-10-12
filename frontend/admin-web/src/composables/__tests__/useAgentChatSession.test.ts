import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAgentChatSession } from '@/composables/useAgentChatSession'

const mocks = vi.hoisted(() => ({
  agentChatApi: {
    getMeta: vi.fn(),
    createConversation: vi.fn(),
    getMessages: vi.fn(),
    sendMessage: vi.fn()
  },
  elMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  }
}))

vi.mock('@/api', () => ({
  agentChatApi: mocks.agentChatApi
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage
}))

describe('useAgentChatSession', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    mocks.agentChatApi.getMeta.mockResolvedValue({
      data: {
        modelProviders: [
          {
            code: 'DEEPSEEK',
            label: 'DeepSeek',
            available: true,
            defaultModel: 'deepseek-chat'
          },
          {
            code: 'LOCAL_7B',
            label: '本地模型',
            available: true,
            defaultModel: 'deepseek-r1:7b'
          }
        ],
        modelOptions: [
          {
            providerCode: 'DEEPSEEK',
            model: 'deepseek-chat',
            label: 'DeepSeek Chat',
            available: true
          },
          {
            providerCode: 'LOCAL_7B',
            model: 'deepseek-r1:7b',
            label: 'Ollama 本地模型（deepseek-r1:7b）',
            available: true
          }
        ],
        defaultProviderCode: 'LOCAL_7B',
        defaultModel: 'deepseek-r1:7b'
      }
    })
    mocks.agentChatApi.createConversation.mockResolvedValue({
      data: {
        sessionId: 'session-1',
        messageCount: 0
      }
    })
    mocks.agentChatApi.sendMessage.mockResolvedValue({
      data: {
        sessionId: 'session-1',
        content: '好的',
        timestamp: new Date().toISOString()
      }
    })
  })

  it('loads model metadata and selects the default local model', async () => {
    const session = useAgentChatSession()

    await session.initSession()

    expect(session.selectedProviderCode.value).toBe('LOCAL_7B')
    expect(session.selectedModel.value).toBe('deepseek-r1:7b')
    expect(session.currentModelOptions.value).toHaveLength(1)
  })

  it('sends selected provider and model with chat messages', async () => {
    const session = useAgentChatSession()

    await session.initSession()
    session.selectProvider('DEEPSEEK')
    await session.sendMessage('你好')

    expect(mocks.agentChatApi.sendMessage).toHaveBeenCalledWith('session-1', {
      message: '你好',
      providerCode: 'DEEPSEEK',
      model: 'deepseek-chat'
    })
  })

  it('keeps grouped clarification choices in the conversation', async () => {
    mocks.agentChatApi.sendMessage.mockResolvedValueOnce({ data: {
      sessionId: 'session-1', kind: 'CLARIFY', content: '请补充查询条件',
      clarifications: [{ key: 'voucherStatus', question: '请选择凭证范围', options: [
        { id: 'POSTED_ONLY', label: '仅已记账凭证' }
      ] }]
    } })
    const session = useAgentChatSession()
    await session.initSession()
    await session.sendMessage('查询供应商往来明细')
    expect(session.messages.value.at(-1)?.clarifications?.[0]?.key).toBe('voucherStatus')

    await session.sendMessage('仅已记账凭证', undefined, { voucherStatus: 'POSTED_ONLY' })
    expect(mocks.agentChatApi.sendMessage).toHaveBeenLastCalledWith('session-1',
      expect.objectContaining({ selectedAnswers: { voucherStatus: 'POSTED_ONLY' } }))
  })
})
