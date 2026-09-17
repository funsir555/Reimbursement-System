import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { agentChatApi } from '@/api'
import type { ChatMetaVO, MessageVO } from '@/api/modules/agent-chat-types'

const MODEL_SELECTION_STORAGE_KEY = 'finwork-chat-model-selection'

/**
 * Agent 聊天会话管理
 * 负责会话生命周期、消息管理、发送逻辑、错误处理
 */
export function useAgentChatSession() {
  const sessionId = ref<string>()
  const messages = ref<MessageVO[]>([])
  const loading = ref(false)
  const sending = ref(false)
  const metaLoading = ref(false)
  const chatMeta = ref<ChatMetaVO>({
    modelProviders: [],
    modelOptions: [],
    defaultProviderCode: '',
    defaultModel: ''
  })
  const selectedProviderCode = ref('')
  const selectedModel = ref('')

  const modelProviders = computed(() => chatMeta.value.modelProviders || [])
  const currentModelOptions = computed(() => {
    return (chatMeta.value.modelOptions || []).filter(option => {
      return option.providerCode === selectedProviderCode.value
    })
  })

  /**
   * 初始化会话
   * @param urlSessionId 从 URL 参数传入的 sessionId
   */
  async function initSession(urlSessionId?: string) {
    await loadMeta()

    if (urlSessionId) {
      // URL 指定了会话 ID，加载该会话
      sessionId.value = urlSessionId
      await loadMessages()
    } else {
      // 创建新会话
      await createNewSession()
    }
  }

  /**
   * 加载当前会话的消息历史
   */
  async function loadMessages() {
    if (!sessionId.value) return

    loading.value = true
    try {
      const { data } = await agentChatApi.getMessages(sessionId.value)
      messages.value = (data || []).map(ensureMessageKey)
    } catch (error: any) {
      ElMessage.error(error?.message || '加载消息失败')
    } finally {
      loading.value = false
    }
  }

  /**
   * 发送消息（使用 clientMessageId 追踪）
   */
  async function sendMessage(content: string): Promise<boolean> {
    if (!content.trim() || !sessionId.value || sending.value) {
      return false
    }

    const messageText = content.trim()
    const clientMessageId = generateClientMessageId()

    // 1. 乐观更新：添加用户消息
    const userMessage: MessageVO = {
      clientMessageId,
      role: 'user',
      content: messageText,
      status: 'pending',
      createdAt: new Date()
    }
    messages.value.push(userMessage)

    sending.value = true
    try {
      // 2. 调用后端 API
      const { data } = await agentChatApi.sendMessage(sessionId.value, {
        message: messageText,
        providerCode: selectedProviderCode.value,
        model: selectedModel.value
      })

      // 3. 标记用户消息为成功
      updateMessageStatus(clientMessageId, 'success')

      // 4. 添加 AI 回复
      messages.value.push({
        clientMessageId: generateClientMessageId(),
        role: 'assistant',
        content: data.content,
        status: 'success',
        createdAt: data.timestamp || new Date()
      })

      return true
    } catch (error: any) {
      // 5. 标记用户消息为失败（不删除，允许重试）
      updateMessageStatus(clientMessageId, 'failed', error?.message || '发送失败')

      // 业务错误处理
      if (error.message?.includes('会话不存在')) {
        ElMessage.error('会话已失效，正在创建新会话...')
        await createNewSession()
      } else {
        ElMessage.error(error?.message || '发送消息失败')
      }

      return false
    } finally {
      sending.value = false
    }
  }

  /**
   * 创建新会话
   */
  async function createNewSession() {
    try {
      const { data } = await agentChatApi.createConversation()
      sessionId.value = data.sessionId
      messages.value = []
      ElMessage.success('新会话已创建')
    } catch (error: any) {
      ElMessage.error(error?.message || '创建会话失败')
    }
  }

  /**
   * 重试失败的消息
   */
  async function retryMessage(messageId: string) {
    const message = messages.value.find(m => m.clientMessageId === messageId)
    if (!message || message.role !== 'user') return

    // 重置状态为 pending
    updateMessageStatus(messageId, 'pending')

    // 重新发送
    await sendMessage(message.content)
  }

  /**
   * 加载模型元数据。
   */
  async function loadMeta() {
    metaLoading.value = true
    try {
      const { data } = await agentChatApi.getMeta()
      chatMeta.value = normalizeMeta(data)
      restoreModelSelection()
    } catch (error: any) {
      chatMeta.value = getFallbackMeta()
      restoreModelSelection()
      ElMessage.warning(error?.message || '模型配置加载失败，已使用本地默认模型')
    } finally {
      metaLoading.value = false
    }
  }

  /**
   * 切换 Provider，并自动选择该 Provider 的首个可用模型。
   */
  function selectProvider(providerCode: string) {
    selectedProviderCode.value = providerCode
    const options = (chatMeta.value.modelOptions || []).filter(option => option.providerCode === providerCode)
    const nextModel = options.find(option => option.available)?.model || options[0]?.model || ''
    selectedModel.value = nextModel
    persistModelSelection()
  }

  function selectModel(model: string) {
    selectedModel.value = model
    persistModelSelection()
  }

  // ========== 辅助函数 ==========

  /**
   * 生成客户端消息唯一 ID
   */
  function generateClientMessageId(): string {
    return `client_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`
  }

  /**
   * 确保消息有稳定的 key（用于 Vue 列表渲染）
   */
  function ensureMessageKey(message: MessageVO): MessageVO {
    if (!message.clientMessageId && !message.id) {
      message.clientMessageId = generateClientMessageId()
    }
    return message
  }

  /**
   * 更新消息状态（按 clientMessageId 精确定位）
   */
  function updateMessageStatus(
    clientMessageId: string,
    status: MessageVO['status'],
    errorMessage?: string
  ) {
    const index = messages.value.findIndex(m => m.clientMessageId === clientMessageId)
    if (index !== -1) {
      const message = messages.value[index]
      if (message) {
        message.status = status
        if (errorMessage) {
          message.errorMessage = errorMessage
        }
      }
    }
  }

  /**
   * 获取消息的稳定 key（优先级：服务端 id > clientMessageId）
   */
  function getMessageKey(message: MessageVO): string {
    return message.id?.toString() || message.clientMessageId || generateClientMessageId()
  }

  function normalizeMeta(meta?: ChatMetaVO): ChatMetaVO {
    if (!meta || !Array.isArray(meta.modelProviders) || meta.modelProviders.length === 0) {
      return getFallbackMeta()
    }
    return {
      modelProviders: meta.modelProviders,
      modelOptions: Array.isArray(meta.modelOptions) ? meta.modelOptions : [],
      defaultProviderCode: meta.defaultProviderCode || meta.modelProviders.find(provider => provider.available)?.code || meta.modelProviders[0]?.code || '',
      defaultModel: meta.defaultModel || ''
    }
  }

  function restoreModelSelection() {
    const saved = readSavedModelSelection()
    const providerCode = isProviderSelectable(saved.providerCode)
      ? saved.providerCode
      : chatMeta.value.defaultProviderCode

    selectedProviderCode.value = providerCode
    const options = (chatMeta.value.modelOptions || []).filter(option => option.providerCode === providerCode)
    const savedModel = options.find(option => option.model === saved.model && option.available !== false)?.model
    selectedModel.value = savedModel || chatMeta.value.defaultModel || options.find(option => option.available !== false)?.model || options[0]?.model || ''
    persistModelSelection()
  }

  function readSavedModelSelection() {
    try {
      const raw = localStorage.getItem(MODEL_SELECTION_STORAGE_KEY)
      if (!raw) {
        return { providerCode: '', model: '' }
      }
      const parsed = JSON.parse(raw) as { providerCode?: string; model?: string }
      return {
        providerCode: parsed.providerCode || '',
        model: parsed.model || ''
      }
    } catch {
      return { providerCode: '', model: '' }
    }
  }

  function persistModelSelection() {
    localStorage.setItem(MODEL_SELECTION_STORAGE_KEY, JSON.stringify({
      providerCode: selectedProviderCode.value,
      model: selectedModel.value
    }))
  }

  function isProviderSelectable(providerCode: string) {
    return (chatMeta.value.modelProviders || []).some(provider => {
      return provider.code === providerCode && provider.available !== false
    })
  }

  function getFallbackMeta(): ChatMetaVO {
    return {
      modelProviders: [
        {
          code: 'LOCAL_7B',
          label: '本地模型',
          available: true,
          deployment: 'ON_PREMISE',
          dataResidency: 'ON_PREMISE',
          maxContextTokens: 8192,
          defaultModel: 'deepseek-r1:7b'
        }
      ],
      modelOptions: [
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
  }

  return {
    // 状态
    sessionId,
    messages,
    loading,
    sending,
    metaLoading,
    modelProviders,
    currentModelOptions,
    selectedProviderCode,
    selectedModel,

    // 方法
    initSession,
    loadMeta,
    loadMessages,
    sendMessage,
    selectProvider,
    selectModel,
    createNewSession,
    retryMessage,
    getMessageKey
  }
}
