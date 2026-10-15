import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { agentChatApi } from '@/api'
import type { ChatMetaVO, ConversationVO, MessageVO, ChatResponseVO } from '@/api/modules/agent-chat-types'

const MODEL_SELECTION_STORAGE_KEY = 'finwork-chat-model-selection'

/**
 * Agent 聊天会话管理
 * 负责会话生命周期、消息管理、发送逻辑、错误处理
 */
export function useAgentChatSession() {
  const sessionId = ref<string>()
  const messages = ref<MessageVO[]>([])
  const conversations = ref<ConversationVO[]>([])
  const pinnedConversations = ref<ConversationVO[]>([])
  const conversationCursor = ref<string>()
  const messageCursor = ref<string>()
  const loadingConversations = ref(false)
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
  const selectedEmployeeId = ref<number>()
  const selectedWorkflowBindingId = ref<number>()
  const availableEmployees = ref<Awaited<ReturnType<typeof agentChatApi.availableEmployees>>['data']>([])
  let conversationLoadVersion = 0

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
    await Promise.all([loadMeta(), loadAvailableEmployees(), refreshConversationLists()])
    const initial = urlSessionId || pinnedConversations.value[0]?.sessionId || conversations.value[0]?.sessionId
    if (initial) await openSession(initial)
  }

  async function loadConversations(reset = false) {
    if (!reset && (loadingConversations.value || !conversationCursor.value)) return
    const version = ++conversationLoadVersion
    loadingConversations.value = true
    try {
      const { data } = await agentChatApi.getConversations(reset ? undefined : conversationCursor.value, true)
      if (version !== conversationLoadVersion) return
      conversations.value = reset ? (data.items || [])
        : [...conversations.value, ...(data.items || []).filter(item =>
          !conversations.value.some(current => current.sessionId === item.sessionId))]
      conversationCursor.value = data.nextCursor || undefined
    } catch (error: any) {
      if (version === conversationLoadVersion) ElMessage.error(error?.message || '加载历史会话失败')
    } finally {
      if (version === conversationLoadVersion) loadingConversations.value = false
    }
  }

  async function loadPinnedConversations() {
    try {
      pinnedConversations.value = (await agentChatApi.getPinnedConversations()).data || []
    } catch (error: any) {
      ElMessage.error(error?.message || '加载置顶会话失败')
    }
  }

  async function refreshConversationLists() {
    await Promise.all([loadPinnedConversations(), loadConversations(true)])
  }

  async function renameConversation(target: string, title: string): Promise<boolean> {
    try {
      await agentChatApi.renameConversation(target, title)
      await refreshConversationLists()
      return true
    } catch (error: any) {
      ElMessage.error(error?.message || '重命名失败')
      return false
    }
  }

  async function setConversationPinned(target: string, pinned: boolean): Promise<boolean> {
    try {
      await agentChatApi.setConversationPinned(target, pinned)
      await refreshConversationLists()
      return true
    } catch (error: any) {
      ElMessage.error(error?.message || '置顶操作失败')
      return false
    }
  }

  async function deleteConversation(target: string): Promise<boolean> {
    try {
      await agentChatApi.deleteConversation(target)
      await refreshConversationLists()
      if (sessionId.value === target) {
        const next = pinnedConversations.value[0]?.sessionId || conversations.value[0]?.sessionId
        if (next) await openSession(next)
        else await createNewSession()
      }
      return true
    } catch (error: any) {
      ElMessage.error(error?.message || '删除会话失败')
      return false
    }
  }

  async function openSession(nextSessionId: string) {
    if (sending.value) return
    sessionId.value = nextSessionId
    messages.value = []
    messageCursor.value = undefined
    await loadMessages()
  }

  /**
   * 加载当前会话的消息历史
   */
  async function loadMessages() {
    if (!sessionId.value) return

    loading.value = true
    try {
      const current = sessionId.value
      const { data } = await agentChatApi.getMessagePage(current)
      if (sessionId.value !== current) return
      messages.value = (data.items || []).map(ensureMessageKey)
      messageCursor.value = data.nextCursor || undefined
    } catch (error: any) {
      if (error?.message === '会话不存在') {
        const fallback = [...pinnedConversations.value, ...conversations.value]
          .find(item => item.sessionId !== sessionId.value)?.sessionId
        sessionId.value = fallback
        messages.value = []
        messageCursor.value = undefined
        if (fallback) await loadMessages()
      } else {
        ElMessage.error(error?.message || '加载消息失败')
      }
    } finally {
      loading.value = false
    }
  }

  async function loadOlderMessages() {
    if (!sessionId.value || !messageCursor.value || loading.value) return
    loading.value = true
    try {
      const current = sessionId.value
      const { data } = await agentChatApi.getMessagePage(current, messageCursor.value)
      if (sessionId.value !== current) return
      messages.value = [...(data.items || []).map(ensureMessageKey), ...messages.value]
      messageCursor.value = data.nextCursor || undefined
    } catch (error: any) {
      ElMessage.error(error?.message || '加载更早消息失败')
    } finally {
      loading.value = false
    }
  }

  /**
   * 发送消息（使用 clientMessageId 追踪）
   */
  async function sendMessage(content: string, selectedOptions?: string[],
    selectedAnswers?: Record<string, string>): Promise<boolean> {
    if (!content.trim() || sending.value) {
      return false
    }
    sending.value = true
    let confirmed = Boolean(sessionId.value)
    let attemptedSession: string | undefined
    try {
      const currentSession = sessionId.value
        || (await agentChatApi.createConversation()).data.sessionId
      attemptedSession = currentSession
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
      const assistantMessage: MessageVO = {
        clientMessageId: generateClientMessageId(), role: 'assistant', content: '',
        status: 'pending', createdAt: new Date()
      }
      messages.value.push(assistantMessage)

      // 2. 调用后端 API
      let finalResponse: ChatResponseVO | undefined
      await agentChatApi.streamMessage(currentSession, {
        message: messageText,
        providerCode: selectedProviderCode.value,
        model: selectedModel.value,
        digitalEmployeeId: selectedEmployeeId.value,
        workflowBindingId: selectedWorkflowBindingId.value,
        selectedOptions,
        selectedAnswers
      }, (event, data) => {
        if (event === 'session' && data === currentSession) {
          confirmed = true
          sessionId.value = currentSession
        }
        if (event === 'status') assistantMessage.progress = String(data)
        if (event === 'delta') assistantMessage.content += String(data)
        if (event === 'final') finalResponse = data as ChatResponseVO
      })
      if (!finalResponse) throw new Error('未收到完整回答')
      const data: ChatResponseVO = finalResponse
      if (!confirmed && data.sessionId === currentSession) {
        confirmed = true
        sessionId.value = currentSession
      }

      // 3. 标记用户消息为成功
      updateMessageStatus(clientMessageId, 'success')

      assistantMessage.content = data.content
      assistantMessage.kind = data.kind === 'CHAT' ? undefined : data.kind
      assistantMessage.options = data.options || []
      assistantMessage.clarifications = data.clarifications || []
      assistantMessage.sources = data.sources || []
      assistantMessage.status = 'success'
      assistantMessage.progress = undefined
      assistantMessage.createdAt = data.timestamp || new Date()
      await refreshConversationLists()

      return true
    } catch (error: any) {
      ElMessage.error(error?.message || '发送失败')
      let recovered = false
      if (!confirmed && attemptedSession) {
        try {
          const { data } = await agentChatApi.getMessagePage(attemptedSession)
          confirmed = true
          sessionId.value = attemptedSession
          messages.value = (data.items || []).map(ensureMessageKey)
          messageCursor.value = data.nextCursor || undefined
          recovered = true
        } catch { /* 首条消息尚未落库，保持空白输入页 */ }
      }
      if (!confirmed) {
        messages.value = []
        messageCursor.value = undefined
      } else if (sessionId.value && !recovered) await loadMessages()
      return false
    } finally {
      sending.value = false
    }
  }

  /**
   * 创建新会话
   */
  async function createNewSession() {
    if (sending.value) return
    sessionId.value = undefined
    messages.value = []
    messageCursor.value = undefined
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

  async function loadAvailableEmployees() {
    try { availableEmployees.value = (await agentChatApi.availableEmployees()).data || [] }
    catch { availableEmployees.value = [] }
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
    conversations,
    pinnedConversations,
    conversationCursor,
    messageCursor,
    loadingConversations,
    loading,
    sending,
    metaLoading,
    modelProviders,
    currentModelOptions,
    selectedProviderCode,
    selectedModel,
    selectedEmployeeId,
    selectedWorkflowBindingId,
    availableEmployees,
    loadAvailableEmployees,

    // 方法
    initSession,
    loadMeta,
    loadMessages,
    loadConversations,
    refreshConversationLists,
    renameConversation,
    setConversationPinned,
    deleteConversation,
    loadOlderMessages,
    openSession,
    sendMessage,
    selectProvider,
    selectModel,
    createNewSession,
    retryMessage,
    getMessageKey
  }
}
