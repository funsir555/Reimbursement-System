import request from './core'
import type {
  ConversationVO,
  MessageVO,
  ChatRequestDTO,
  ChatResponseVO,
  ChatMetaVO
} from './agent-chat-types'

/**
 * Agent 聊天 API
 * 所有接口路径使用 /auth/agent/chat/** 前缀
 * 避免使用 /agent/chat/** 导致网关路由失败
 */
export const agentChatApi = {
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
    request<MessageVO[]>(`/auth/agent/chat/conversations/${sessionId}/messages`)
}
