<template>
  <div class="agent-chat-container">
    <!-- 会话头部 -->
    <header class="chat-header">
      <div class="chat-header-left">
        <pixel-duck-bot-icon class="h-8 w-8" />
        <div>
          <h2>AI 助手</h2>
          <p class="chat-subtitle">报销与费用管理智能对话</p>
        </div>
      </div>
      <div class="chat-header-actions">
        <el-select
          v-model="selectedProviderCode"
          class="provider-select"
          :loading="metaLoading"
          placeholder="Provider"
          @change="handleProviderChange"
        >
          <el-option
            v-for="provider in modelProviders"
            :key="provider.code"
            :label="provider.label"
            :value="provider.code"
            :disabled="provider.available === false"
          >
            <span>{{ provider.label }}</span>
            <span class="option-meta">{{ provider.deployment || 'MODEL' }}</span>
          </el-option>
        </el-select>
        <el-select
          v-model="selectedModel"
          class="model-select"
          filterable
          allow-create
          default-first-option
          :disabled="currentModelOptions.length === 0"
          placeholder="模型"
          @change="handleModelChange"
        >
          <el-option
            v-for="model in currentModelOptions"
            :key="model.model"
            :label="model.label"
            :value="model.model"
            :disabled="model.available === false"
          />
        </el-select>
        <el-button :icon="Plus" @click="handleCreateNewConversation">
          新建会话
        </el-button>
      </div>
    </header>

    <!-- 消息列表 -->
    <div class="message-list-wrapper" ref="messageListRef">
      <chat-message-list
        v-if="messages.length > 0"
        :messages="messages"
        :loading="loading"
        :get-message-key="getMessageKey"
        @retry="handleRetry"
      />

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <pixel-duck-bot-icon class="h-16 w-16 opacity-30" />
        <h3>开始与 AI 助手对话</h3>
        <p>我可以帮您查询报销单、解答费用相关问题</p>
        <div class="example-queries">
          <el-tag @click="handleExampleClick('我的报销单有哪些？')">
            我的报销单有哪些？
          </el-tag>
          <el-tag @click="handleExampleClick('本月的报销总额是多少？')">
            本月的报销总额是多少？
          </el-tag>
          <el-tag @click="handleExampleClick('待审批的报销单有几个？')">
            待审批的报销单有几个？
          </el-tag>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <chat-input-box
      v-model="userInput"
      :sending="sending"
      :disabled="!sessionId"
      @send="handleSendMessage"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import PixelDuckBotIcon from '@/components/icons/PixelDuckBotIcon.vue'
import ChatMessageList from '@/components/agent/ChatMessageList.vue'
import ChatInputBox from '@/components/agent/ChatInputBox.vue'
import { useAgentChatSession } from '@/composables/useAgentChatSession'

const route = useRoute()
const messageListRef = ref<HTMLElement>()
const userInput = ref('')

// 使用 Composable 管理会话状态
const {
  sessionId,
  messages,
  loading,
  sending,
  metaLoading,
  modelProviders,
  currentModelOptions,
  selectedProviderCode,
  selectedModel,
  initSession,
  sendMessage,
  selectProvider,
  selectModel,
  createNewSession,
  retryMessage,
  getMessageKey
} = useAgentChatSession()

onMounted(async () => {
  const urlSessionId = route.query.sessionId as string
  await initSession(urlSessionId)
})

// 监听消息变化，自动滚动到底部
watch(() => messages.value.length, () => {
  scrollToBottom()
})

async function handleSendMessage() {
  if (!userInput.value.trim()) return

  const success = await sendMessage(userInput.value)
  if (success) {
    userInput.value = ''
  }
}

async function handleCreateNewConversation() {
  await createNewSession()
  userInput.value = ''
}

function handleProviderChange(value: string) {
  selectProvider(value)
}

function handleModelChange(value: string) {
  selectModel(value)
}

function handleExampleClick(query: string) {
  userInput.value = query
  handleSendMessage()
}

function handleRetry(messageId: string) {
  retryMessage(messageId)
}

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}
</script>

<style scoped>
/* 复用 Agent 工作台的 CSS 变量 */
.agent-chat-container {
  --ink: #273047;
  --accent: #d97706;
  --mint: #1f9f9f;
  --chat-user-bubble: #3b82f6;
  --chat-assistant-bubble: #ffffff;
  --chat-border: rgba(39, 48, 71, 0.08);

  display: flex;
  flex-direction: column;
  height: 100%;  /* 适应 MainLayout 提供的容器高度 */
  min-height: 0;
  background: #f8fafc;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: white;
  border-bottom: 1px solid var(--chat-border);
  box-shadow: 0 2px 8px rgba(39, 48, 71, 0.04);
}

.chat-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--ink);
}

.chat-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: rgba(39, 48, 71, 0.6);
}

.chat-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.provider-select {
  width: 150px;
}

.model-select {
  width: 220px;
}

.option-meta {
  float: right;
  margin-left: 16px;
  color: rgba(39, 48, 71, 0.45);
  font-size: 12px;
}

.message-list-wrapper {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 24px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  color: rgba(39, 48, 71, 0.6);
}

.empty-state h3 {
  margin: 16px 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--ink);
}

.empty-state p {
  margin: 0 0 24px;
  font-size: 14px;
}

.example-queries {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
  max-width: 600px;
}

.example-queries .el-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.example-queries .el-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
}

@media (max-width: 900px) {
  .chat-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .chat-header-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .provider-select,
  .model-select {
    flex: 1 1 180px;
    width: auto;
  }
}
</style>
