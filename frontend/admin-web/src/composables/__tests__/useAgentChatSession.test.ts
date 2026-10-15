import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAgentChatSession } from '@/composables/useAgentChatSession'

const mocks = vi.hoisted(() => ({
  agentChatApi: {
    getMeta: vi.fn(), availableEmployees: vi.fn(), createConversation: vi.fn(),
    getConversations: vi.fn(), getPinnedConversations: vi.fn(),
    renameConversation: vi.fn(), setConversationPinned: vi.fn(), deleteConversation: vi.fn(),
    getMessagePage: vi.fn(), streamMessage: vi.fn()
  },
  elMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() }
}))

vi.mock('@/api', () => ({ agentChatApi: mocks.agentChatApi }))
vi.mock('element-plus', () => ({ ElMessage: mocks.elMessage }))

describe('useAgentChatSession', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    mocks.agentChatApi.getMeta.mockResolvedValue({ data: {
      modelProviders: [{ code: 'LOCAL_7B', label: '本地模型', available: true }],
      modelOptions: [{ providerCode: 'LOCAL_7B', model: 'local-model', label: '本地模型', available: true }],
      defaultProviderCode: 'LOCAL_7B', defaultModel: 'local-model'
    } })
    mocks.agentChatApi.availableEmployees.mockResolvedValue({ data: [] })
    mocks.agentChatApi.getConversations.mockResolvedValue({ data: { items: [], nextCursor: null } })
    mocks.agentChatApi.getPinnedConversations.mockResolvedValue({ data: [] })
    mocks.agentChatApi.renameConversation.mockResolvedValue({ data: true })
    mocks.agentChatApi.setConversationPinned.mockResolvedValue({ data: true })
    mocks.agentChatApi.deleteConversation.mockResolvedValue({ data: true })
    mocks.agentChatApi.getMessagePage.mockResolvedValue({ data: { items: [], nextCursor: null } })
    mocks.agentChatApi.createConversation.mockResolvedValue({ data: { sessionId: 'session-1' } })
    mocks.agentChatApi.streamMessage.mockImplementation(async (_id, _payload, onEvent) => {
      onEvent('session', 'session-1')
      onEvent('status', '正在生成回答')
      onEvent('delta', '好')
      onEvent('delta', '的')
      onEvent('final', { sessionId: 'session-1', kind: 'CHAT', content: '好的' })
    })
  })

  it('loads history without creating an empty conversation on page entry', async () => {
    mocks.agentChatApi.getConversations.mockResolvedValueOnce({ data: {
      items: [{ sessionId: 'old-1', title: '报销单', companyId: 'COMPANY_A' }], nextCursor: 'next'
    } })
    const session = useAgentChatSession()
    await session.initSession()
    expect(session.sessionId.value).toBe('old-1')
    expect(session.conversations.value[0]?.title).toBe('报销单')
    expect(session.conversationCursor.value).toBe('next')
    expect(mocks.agentChatApi.createConversation).not.toHaveBeenCalled()
    expect(mocks.agentChatApi.getConversations).toHaveBeenCalledWith(undefined, true)
  })

  it('shows pinned conversations independently and opens the newest pinned one', async () => {
    mocks.agentChatApi.getPinnedConversations.mockResolvedValueOnce({ data: [
      { sessionId: 'pinned-1', title: '置顶会话', pinned: true }
    ] })
    mocks.agentChatApi.getConversations.mockResolvedValueOnce({ data: {
      items: [{ sessionId: 'recent-1', title: '普通会话', pinned: false }], nextCursor: null
    } })
    const session = useAgentChatSession()
    await session.initSession()
    expect(session.sessionId.value).toBe('pinned-1')
    expect(session.pinnedConversations.value).toHaveLength(1)
    expect(session.conversations.value).toHaveLength(1)
  })

  it('renames and pins a conversation, then refreshes both groups', async () => {
    const session = useAgentChatSession()
    await session.initSession()
    mocks.agentChatApi.getConversations.mockResolvedValueOnce({ data: {
      items: [{ sessionId: 'one', title: '新名称', pinned: false }], nextCursor: null
    } })
    expect(await session.renameConversation('one', '新名称')).toBe(true)
    expect(mocks.agentChatApi.renameConversation).toHaveBeenCalledWith('one', '新名称')
    expect(session.conversations.value[0]?.title).toBe('新名称')
    mocks.agentChatApi.getPinnedConversations.mockResolvedValueOnce({ data: [
      { sessionId: 'one', title: '新名称', pinned: true }
    ] })
    mocks.agentChatApi.getConversations.mockResolvedValueOnce({ data: { items: [], nextCursor: null } })
    expect(await session.setConversationPinned('one', true)).toBe(true)
    expect(session.pinnedConversations.value[0]?.sessionId).toBe('one')
    expect(session.conversations.value).toEqual([])
    mocks.agentChatApi.getPinnedConversations.mockResolvedValueOnce({ data: [] })
    mocks.agentChatApi.getConversations.mockResolvedValueOnce({ data: {
      items: [{ sessionId: 'one', title: '新名称', pinned: false }], nextCursor: null
    } })
    expect(await session.setConversationPinned('one', false)).toBe(true)
    expect(session.pinnedConversations.value).toEqual([])
    expect(session.conversations.value[0]?.sessionId).toBe('one')
  })

  it('deleting the current conversation opens the next available one', async () => {
    mocks.agentChatApi.getPinnedConversations.mockResolvedValueOnce({ data: [
      { sessionId: 'current', pinned: true }
    ] })
    mocks.agentChatApi.getConversations.mockResolvedValueOnce({ data: {
      items: [{ sessionId: 'next', pinned: false }], nextCursor: null
    } })
    const session = useAgentChatSession()
    await session.initSession()
    mocks.agentChatApi.getPinnedConversations.mockResolvedValueOnce({ data: [] })
    mocks.agentChatApi.getConversations.mockResolvedValueOnce({ data: {
      items: [{ sessionId: 'next', pinned: false }], nextCursor: null
    } })
    expect(await session.deleteConversation('current')).toBe(true)
    expect(mocks.agentChatApi.deleteConversation).toHaveBeenCalledWith('current')
    expect(session.sessionId.value).toBe('next')
  })

  it('keeps the current session when deletion fails', async () => {
    mocks.agentChatApi.getConversations.mockResolvedValueOnce({ data: {
      items: [{ sessionId: 'current' }], nextCursor: null
    } })
    const session = useAgentChatSession()
    await session.initSession()
    mocks.agentChatApi.deleteConversation.mockRejectedValueOnce(new Error('无权访问'))
    expect(await session.deleteConversation('current')).toBe(false)
    expect(session.sessionId.value).toBe('current')
    expect(mocks.agentChatApi.getConversations).toHaveBeenCalledTimes(1)
  })

  it('deleting another conversation keeps the current one open', async () => {
    mocks.agentChatApi.getConversations.mockResolvedValueOnce({ data: {
      items: [{ sessionId: 'current' }, { sessionId: 'other' }], nextCursor: null
    } })
    const session = useAgentChatSession()
    await session.initSession()
    mocks.agentChatApi.getConversations.mockResolvedValueOnce({ data: {
      items: [{ sessionId: 'current' }], nextCursor: null
    } })
    expect(await session.deleteConversation('other')).toBe(true)
    expect(session.sessionId.value).toBe('current')
    expect(mocks.agentChatApi.getMessagePage).toHaveBeenCalledTimes(1)
  })

  it('does not let an older page response overwrite a rename refresh', async () => {
    mocks.agentChatApi.getConversations.mockResolvedValueOnce({ data: {
      items: [{ sessionId: 'current', title: '旧名称' }], nextCursor: 'older'
    } })
    const session = useAgentChatSession()
    await session.initSession()
    let finishOlder!: (value: unknown) => void
    mocks.agentChatApi.getConversations.mockReturnValueOnce(new Promise(resolve => { finishOlder = resolve }))
    const loadingOlder = session.loadConversations()
    mocks.agentChatApi.getConversations.mockResolvedValueOnce({ data: {
      items: [{ sessionId: 'current', title: '新名称' }], nextCursor: null
    } })
    expect(await session.renameConversation('current', '新名称')).toBe(true)
    finishOlder({ data: { items: [{ sessionId: 'stale' }], nextCursor: null } })
    await loadingOlder
    expect(session.conversations.value.map(item => item.title)).toEqual(['新名称'])
  })

  it('creates on first send and updates one assistant message from streamed events', async () => {
    const session = useAgentChatSession()
    await session.initSession()
    await session.sendMessage('你好')
    expect(mocks.agentChatApi.createConversation).toHaveBeenCalledTimes(1)
    expect(mocks.agentChatApi.streamMessage).toHaveBeenCalledWith('session-1',
      expect.objectContaining({ message: '你好', providerCode: 'LOCAL_7B', model: 'local-model' }),
      expect.any(Function))
    expect(session.messages.value).toHaveLength(2)
    expect(session.messages.value[1]?.content).toBe('好的')
    expect(session.messages.value[1]?.status).toBe('success')
  })

  it('keeps the new session out of the URL until the first message is confirmed', async () => {
    let finish!: () => void
    mocks.agentChatApi.streamMessage.mockImplementationOnce(async (_id, _payload, onEvent) => {
      await new Promise<void>(resolve => { finish = resolve })
      onEvent('session', 'session-1')
      onEvent('final', { sessionId: 'session-1', kind: 'CHAT', content: '好的' })
    })
    const session = useAgentChatSession()
    const pending = session.sendMessage('你好')
    await vi.waitFor(() => expect(mocks.agentChatApi.streamMessage).toHaveBeenCalled())
    expect(session.sessionId.value).toBeUndefined()
    finish()
    expect(await pending).toBe(true)
    expect(session.sessionId.value).toBe('session-1')
  })

  it('clears optimistic messages when the stream fails before persistence', async () => {
    mocks.agentChatApi.streamMessage.mockRejectedValueOnce(new Error('连接失败'))
    mocks.agentChatApi.getMessagePage.mockRejectedValueOnce(new Error('会话不存在'))
    const session = useAgentChatSession()
    expect(await session.sendMessage('你好')).toBe(false)
    expect(session.sessionId.value).toBeUndefined()
    expect(session.messages.value).toEqual([])
    expect(mocks.agentChatApi.getMessagePage).toHaveBeenCalledWith('session-1')
  })

  it('recovers the saved first message when the session event is lost', async () => {
    mocks.agentChatApi.streamMessage.mockRejectedValueOnce(new Error('连接中断'))
    mocks.agentChatApi.getMessagePage.mockResolvedValueOnce({ data: {
      items: [{ id: 1, role: 'user', content: '你好' }], nextCursor: null
    } })
    const session = useAgentChatSession()
    expect(await session.sendMessage('你好')).toBe(false)
    expect(session.sessionId.value).toBe('session-1')
    expect(session.messages.value.map(item => item.content)).toEqual(['你好'])
  })

  it('reloads saved user message after a post-confirmation disconnect', async () => {
    mocks.agentChatApi.streamMessage.mockImplementationOnce(async (_id, _payload, onEvent) => {
      onEvent('session', 'session-1')
      throw new Error('连接中断')
    })
    mocks.agentChatApi.getMessagePage.mockResolvedValueOnce({ data: {
      items: [{ id: 1, role: 'user', content: '你好' }], nextCursor: null
    } })
    const session = useAgentChatSession()
    expect(await session.sendMessage('你好')).toBe(false)
    expect(session.sessionId.value).toBe('session-1')
    expect(session.messages.value.map(item => item.content)).toEqual(['你好'])
  })

  it('falls back when an old link points to a cleaned empty session', async () => {
    mocks.agentChatApi.getConversations.mockResolvedValueOnce({ data: {
      items: [{ sessionId: 'valid-1' }], nextCursor: null
    } })
    mocks.agentChatApi.getMessagePage.mockRejectedValueOnce(new Error('会话不存在'))
    const session = useAgentChatSession()
    await session.initSession('cleaned-1')
    expect(session.sessionId.value).toBe('valid-1')
  })

  it('does not create a second conversation while the first send is creating one', async () => {
    let completeCreation!: (value: { data: { sessionId: string } }) => void
    mocks.agentChatApi.createConversation.mockReturnValueOnce(new Promise(resolve => {
      completeCreation = resolve
    }))
    const session = useAgentChatSession()
    const first = session.sendMessage('第一条消息')
    const second = await session.sendMessage('重复提交')
    expect(second).toBe(false)
    expect(mocks.agentChatApi.createConversation).toHaveBeenCalledTimes(1)
    completeCreation({ data: { sessionId: 'session-1' } })
    expect(await first).toBe(true)
  })

  it('loads more sessions and older messages with separate cursors', async () => {
    mocks.agentChatApi.getConversations
      .mockResolvedValueOnce({ data: { items: [{ sessionId: 'a' }], nextCursor: 'page-2' } })
      .mockResolvedValueOnce({ data: { items: [{ sessionId: 'b' }], nextCursor: null } })
    mocks.agentChatApi.getMessagePage
      .mockResolvedValueOnce({ data: { items: [{ id: 2, role: 'user', content: 'new' }], nextCursor: 'older' } })
      .mockResolvedValueOnce({ data: { items: [{ id: 1, role: 'user', content: 'old' }], nextCursor: null } })
    const session = useAgentChatSession()
    await session.initSession()
    await session.loadConversations()
    await session.loadOlderMessages()
    expect(session.conversations.value.map(item => item.sessionId)).toEqual(['a', 'b'])
    expect(session.messages.value.map(item => item.content)).toEqual(['old', 'new'])
  })

  it('keeps grouped clarification choices on the final streamed response', async () => {
    mocks.agentChatApi.streamMessage.mockImplementationOnce(async (_id, _payload, onEvent) => {
      onEvent('final', { sessionId: 'session-1', kind: 'CLARIFY', content: '请选择',
        clarifications: [{ key: 'voucherStatus', question: '凭证范围', options: [
          { id: 'POSTED_ONLY', label: '仅已记账' }
        ] }] })
    })
    const session = useAgentChatSession()
    await session.initSession()
    await session.sendMessage('查询往来明细')
    expect(session.messages.value.at(-1)?.clarifications?.[0]?.key).toBe('voucherStatus')
  })
})
