import request from './core'
import type {
  ConversationVO,
  MessageVO,
  ChatRequestDTO,
  ChatResponseVO,
  ChatMetaVO,
  ChatPage
} from './agent-chat-types'

/**
 * Agent 聊天 API
 * 所有接口路径使用 /auth/agent/chat/** 前缀
 * 避免使用 /agent/chat/** 导致网关路由失败
 */
export const agentChatApi = {
  availableEmployees: () => request<Array<{
    id: number; name: string; description?: string; binding_id: number; permission_code: string;
    agent_name: string; agent_version_id: number
  }>>('/auth/agent/chat/available-employees'),
  subscriptions: () => request<Array<{
    plan_id: number; employee_id: number; employee_name: string; agent_name: string;
    frequency: string; run_hour: number; run_minute: number; next_run_at?: string; subscribed: number
  }>>('/auth/agent/chat/subscriptions'),
  setSubscription: (planId: number, enabled: boolean) =>
    request<boolean>(`/auth/agent/chat/subscriptions/${planId}`, {
      method: 'PUT', body: JSON.stringify({ enabled })
    }),
  inbox: () => request<Array<{
    id: number; plan_id: number; employee_name: string; scheduled_at: string;
    status: string; result_text?: string; created_at: string
  }>>('/auth/agent/chat/inbox'),
  /**
   * 获取聊天元数据
   * GET /api/auth/agent/chat/meta
   */
  getMeta: () =>
    request<ChatMetaVO>('/auth/agent/chat/meta'),

  /**
   * 创建新会话
   * POST /api/auth/agent/chat/conversations
   */
  createConversation: () =>
    request<ConversationVO>('/auth/agent/chat/conversations', {
      method: 'POST'
    }),

  getConversations: (cursor?: string, excludePinned = false) => {
    const query = new URLSearchParams()
    if (cursor) query.set('cursor', cursor)
    if (excludePinned) query.set('excludePinned', 'true')
    return request<ChatPage<ConversationVO>>(`/auth/agent/chat/conversations${query.size ? `?${query}` : ''}`)
  },
  getPinnedConversations: () =>
    request<ConversationVO[]>('/auth/agent/chat/conversations/pinned'),
  renameConversation: (sessionId: string, title: string) =>
    request<boolean>(`/auth/agent/chat/conversations/${encodeURIComponent(sessionId)}/title`, {
      method: 'PATCH', body: JSON.stringify({ title })
    }),
  setConversationPinned: (sessionId: string, pinned: boolean) =>
    request<boolean>(`/auth/agent/chat/conversations/${encodeURIComponent(sessionId)}/pin`, {
      method: 'PUT', body: JSON.stringify({ pinned })
    }),
  deleteConversation: (sessionId: string) =>
    request<boolean>(`/auth/agent/chat/conversations/${encodeURIComponent(sessionId)}`, {
      method: 'DELETE'
    }),

  /**
   * 发送消息
   * POST /api/auth/agent/chat/conversations/{sessionId}/messages
   */
  sendMessage: (sessionId: string, payload: ChatRequestDTO) =>
    request<ChatResponseVO>(`/auth/agent/chat/conversations/${sessionId}/messages`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),

  /**
   * 获取会话历史消息
   * GET /api/auth/agent/chat/conversations/{sessionId}/messages
   */
  getMessages: (sessionId: string) =>
    request<MessageVO[]>(`/auth/agent/chat/conversations/${sessionId}/messages`),
  getMessagePage: (sessionId: string, cursor?: string) =>
    request<ChatPage<MessageVO>>(`/auth/agent/chat/conversations/${encodeURIComponent(sessionId)}/messages/page${cursor ? `?cursor=${encodeURIComponent(cursor)}` : ''}`),
  async streamMessage(sessionId: string, payload: ChatRequestDTO,
    onEvent: (event: 'session' | 'status' | 'delta' | 'final' | 'error' | 'done', data: string | ChatResponseVO) => void,
    signal?: AbortSignal): Promise<void> {
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || '/api'}/auth/agent/chat/conversations/${encodeURIComponent(sessionId)}/messages/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        Authorization: `Bearer ${localStorage.getItem('token') || ''}`
      },
      body: JSON.stringify(payload), signal
    })
    if (response.status === 401) throw new Error('登录已过期，请重新登录')
    if (!response.ok || !response.body) throw new Error('流式连接失败，请稍后重试')
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let completed = false
    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      buffer = buffer.replace(/\r\n/g, '\n')
      let boundary = buffer.indexOf('\n\n')
      while (boundary >= 0) {
        const frame = buffer.slice(0, boundary)
        buffer = buffer.slice(boundary + 2)
        const lines = frame.split('\n')
        const name = lines.find(line => line.startsWith('event:'))?.slice(6).trim()
        const data = lines.filter(line => line.startsWith('data:'))
          .map(line => line[5] === ' ' ? line.slice(6) : line.slice(5)).join('\n')
        if (name === 'error') throw new Error(data || '处理消息失败')
        if (name === 'final') {
          completed = true
          onEvent('final', JSON.parse(data) as ChatResponseVO)
        } else if (name === 'session' || name === 'status' || name === 'delta' || name === 'done') {
          onEvent(name, data)
        }
        boundary = buffer.indexOf('\n\n')
      }
    }
    if (!completed) throw new Error('连接中断，请重新打开会话查看结果')
  }
}
