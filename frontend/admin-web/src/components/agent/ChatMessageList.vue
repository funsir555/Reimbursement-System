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

        <div v-if="isActiveClarification(msg) && msg.options?.length" class="message-options">
          <el-button
            v-for="option in msg.options"
            :key="option.id"
            size="small"
            plain
            @click="$emit('choose', option.id, option.label)"
          >{{ option.label }}</el-button>
        </div>

        <div v-if="isActiveClarification(msg) && msg.clarifications?.length" class="clarification-groups">
          <div v-for="group in msg.clarifications" :key="group.key" class="clarification-group">
            <div class="clarification-question">{{ group.question }}</div>
            <el-select v-if="group.options.length" v-model="answersFor(msg)[group.key]"
              :placeholder="'请选择' + group.question" class="clarification-select">
              <el-option v-for="option in group.options" :key="option.id"
                :value="option.id" :label="option.label" />
            </el-select>
            <el-input v-else v-model="answersFor(msg)[group.key]" maxlength="200"
              :placeholder="'请输入' + group.question" class="clarification-select" />
          </div>
          <el-button type="primary" size="small"
            :disabled="!msg.clarifications.some(group => answersFor(msg)[group.key]?.trim())"
            @click="submitClarification(msg)">确认选择</el-button>
        </div>

        <div v-if="msg.sources?.length" class="message-sources">
          <span v-for="source in msg.sources" :key="source.workflowBindingId">
            工作流版本 #{{ source.agentVersionId }}
          </span>
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
import { reactive } from 'vue'
import { User, Warning } from '@element-plus/icons-vue'
import PixelDuckBotIcon from '@/components/icons/PixelDuckBotIcon.vue'
import type { MessageVO } from '@/api/modules/agent-chat-types'

const props = defineProps<{
  messages: MessageVO[]
  loading?: boolean
  getMessageKey: (msg: MessageVO) => string
}>()

const emit = defineEmits<{
  retry: [messageId: string]
  choose: [optionId: string, label: string]
  clarify: [answers: Record<string, string>, labels: string[]]
}>()

const selectedAnswers = reactive<Record<string, Record<string, string>>>({})

function isActiveClarification(message: MessageVO): boolean {
  if (props.loading || message.kind !== 'CLARIFY') return false
  const latest = [...props.messages].reverse().find(item => item.role === 'assistant')
  return latest?.kind === 'CLARIFY' && props.getMessageKey(latest) === props.getMessageKey(message)
}

function answersFor(message: MessageVO): Record<string, string> {
  const key = props.getMessageKey(message)
  return selectedAnswers[key] ?? (selectedAnswers[key] = {})
}

function submitClarification(message: MessageVO) {
  const groups = message.clarifications || []
  const answers = answersFor(message)
  if (!groups.length || !groups.some(group => answers[group.key]?.trim())) return
  const selected = Object.fromEntries(Object.entries(answers).filter(([, value]) => value?.trim()))
  emit('clarify', selected, groups.filter(group => selected[group.key]).map(group => {
    const value = selected[group.key]
    const label = group.options.find(option => option.id === value)?.label || value
    return `${group.question}：${label}`
  }))
}

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

.message-options { display: flex; flex-wrap: wrap; gap: 8px; }
.clarification-groups { display: flex; flex-direction: column; gap: 10px; padding: 12px; border: 1px solid var(--chat-border); border-radius: 12px; background: white; }
.clarification-group { display: flex; flex-direction: column; gap: 4px; }
.clarification-question { font-size: 13px; color: var(--ink); }
.clarification-select { width: min(320px, 100%); }
.message-sources { display: flex; flex-wrap: wrap; gap: 8px; font-size: 12px; color: rgba(39, 48, 71, 0.65); }

.message-user .message-time {
  text-align: right;
}
</style>
