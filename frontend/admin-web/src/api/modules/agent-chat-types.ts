/**
 * Agent 聊天 API 类型定义
 * Phase 1: 基础聊天 + 模型选择
 */

/**
 * 会话信息
 */
export interface ConversationVO {
  sessionId: string
  messageCount: number
  lastMessageAt?: string | Date
  createdAt?: string | Date
}

/**
 * 消息对象
 * 预留 id、clientMessageId 和状态字段用于消息追踪和失败重试
 */
export interface MessageVO {
  id?: number                           // 服务端消息 ID
  clientMessageId?: string              // 客户端生成的唯一 ID
  role: 'user' | 'assistant' | 'system'
  content: string
  status?: 'pending' | 'success' | 'failed'  // 消息状态
  errorMessage?: string                 // 失败原因
  createdAt?: string | Date
}

/**
 * 发送消息请求
 */
export interface ChatRequestDTO {
  message: string
  providerCode?: string
  model?: string
}

/**
 * 聊天响应
 */
export interface ChatResponseVO {
  sessionId: string
  content: string
  intent?: string
  timestamp?: string | Date
}

/**
 * 模型 Provider
 */
export interface ChatModelProviderVO {
  code: string
  label: string
  available: boolean
  deployment?: string
  dataResidency?: string
  maxContextTokens?: number
  defaultModel?: string
}

/**
 * 模型选项
 */
export interface ChatModelOptionVO {
  providerCode: string
  model: string
  label: string
  available: boolean
}

/**
 * 聊天元数据
 */
export interface ChatMetaVO {
  modelProviders: ChatModelProviderVO[]
  modelOptions: ChatModelOptionVO[]
  defaultProviderCode: string
  defaultModel: string
}
