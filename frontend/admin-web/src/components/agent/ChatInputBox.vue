<template>
  <div class="chat-input-container">
    <el-input
      :model-value="modelValue"
      @update:model-value="$emit('update:modelValue', $event)"
      type="textarea"
      :rows="3"
      :maxlength="1000"
      show-word-limit
      placeholder="输入消息，按 Ctrl+Enter 发送..."
      :disabled="disabled || sending"
      @keydown.ctrl.enter="handleSend"
      class="chat-textarea"
    />
    <div class="input-actions">
      <div class="input-hint">
        <el-icon><InfoFilled /></el-icon>
        <span>Ctrl + Enter 快速发送</span>
      </div>
      <el-button
        type="primary"
        :icon="Promotion"
        :loading="sending"
        :disabled="!modelValue.trim() || disabled"
        @click="handleSend"
      >
        {{ sending ? '发送中...' : '发送' }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Promotion, InfoFilled } from '@element-plus/icons-vue'

defineProps<{
  modelValue: string
  sending?: boolean
  disabled?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'send': []
}>()

function handleSend() {
  emit('send')
}
</script>

<style scoped>
.chat-input-container {
  padding: 16px 24px 20px;
  background: white;
  border-top: 1px solid var(--chat-border);
  box-shadow: 0 -2px 8px rgba(39, 48, 71, 0.04);
}

.chat-textarea {
  margin-bottom: 12px;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.input-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: rgba(39, 48, 71, 0.5);
}
</style>
