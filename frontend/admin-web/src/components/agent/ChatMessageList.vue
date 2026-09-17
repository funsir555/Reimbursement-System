<template>
  <div class="message-list">
    <div
      v-for="msg in messages"
      :key="getMessageKey(msg)"
      class="message-item"
      :class="`message-${msg.role}`"
    >
      <!-- 头像 -->
      <div class="message-avatar">
        <el-avatar v-if="msg.role === 'user'" :size="36" :icon="User" />
        <el-avatar v-else :size="36" class="bg-blue-500">
          <pixel-duck-bot-icon class="h-5 w-5" />
        </el-avatar>
      </div>

      <!-- 消息内容 -->
      <div class="message-content">
        <div
          class="message-text"
          :class="{
            'message-pending': msg.status === 'pending',
            'message-failed': msg.status === 'failed'
          }"
        >
          {{ msg.content }}

          <!-- 失败提示 -->
          <div v-if="msg.status === 'failed'" class="message-error">
            <el-icon><Warning /></el-icon>
            <span>{{ msg.errorMessage || '发送失败' }}</span>
            <el-button
              link
              type="primary"
              size="small"
              @click="$emit('retry', msg.clientMessageId!)"
            >
              重试
            </el-button>
          </div>
        </div>

        <div class="message-time">
          {{ formatTime(msg.createdAt) }}
        </div>
      </div>
    </div>

    <!-- 加载中状态 -->
    <div v-if="loading" class="message-item message-assistant">
      <div class="message-avatar">
        <el-avatar :size="36" class="bg-blue-500">
          <pixel-duck-bot-icon class="h-5 w-5" />
        </el-avatar>
      </div>
      <div class="message-content">
        <div class="message-text">
          <el-skeleton :rows="2" animated />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { User, Warning } from '@element-plus/icons-vue'
import PixelDuckBotIcon from '@/components/icons/PixelDuckBotIcon.vue'
import type { MessageVO } from '@/api/modules/agent-chat-types'

defineProps<{
  messages: MessageVO[]
  loading?: boolean
  getMessageKey: (msg: MessageVO) => string
}>()

defineEmits<{
  retry: [messageId: string]
}>()

function formatTime(timestamp?: Date | string) {
  if (!timestamp) return ''
  const date = typeof timestamp === 'string' ? new Date(timestamp) : timestamp
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.message-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.message-item {
  display: flex;
  gap: 12px;
  max-width: 75%;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-assistant {
  align-self: flex-start;
}

.message-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.message-text {
  padding: 14px 18px;
  border-radius: 16px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  transition: all 0.2s;
}

.message-user .message-text {
  background: var(--chat-user-bubble);
  color: white;
  border-bottom-right-radius: 4px;
}

.message-assistant .message-text {
  background: var(--chat-assistant-bubble);
  color: var(--ink);
  border: 1px solid var(--chat-border);
  border-bottom-left-radius: 4px;
  box-shadow: 0 2px 8px rgba(39, 48, 71, 0.04);
}

.message-pending {
  opacity: 0.6;
}

.message-failed {
  border-color: #f56c6c !important;
  background: #fef0f0 !important;
}

.message-error {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #f56c6c;
  font-size: 12px;
  color: #f56c6c;
}

.message-time {
  font-size: 12px;
  color: rgba(39, 48, 71, 0.5);
  padding: 0 4px;
}

.message-user .message-time {
  text-align: right;
}
</style>
