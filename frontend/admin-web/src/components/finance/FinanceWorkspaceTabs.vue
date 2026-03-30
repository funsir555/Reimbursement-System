<template>
  <div class="finance-tabs-wrap">
    <div class="finance-tabs-list">
      <button
        v-for="tab in tabs"
        :key="tab.path"
        type="button"
        class="finance-tab"
        :class="tab.path === activePath ? 'finance-tab-active' : ''"
        @click="$emit('select', tab.path)"
      >
        <span class="finance-tab-title">{{ tab.title }}</span>
        <el-button
          v-if="tabs.length > 1"
          circle
          text
          class="finance-tab-close"
          @click.stop="$emit('close', tab.path)"
        >
          <el-icon><Close /></el-icon>
        </el-button>
      </button>
    </div>

    <el-dropdown trigger="click" @command="handleCommand">
      <button type="button" class="finance-tabs-actions">
        <el-icon><Operation /></el-icon>
        <span>页签</span>
      </button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="close-current">关闭当前</el-dropdown-item>
          <el-dropdown-item command="close-others">关闭其他</el-dropdown-item>
          <el-dropdown-item command="close-right">关闭右侧</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup lang="ts">
import { Close, Operation } from '@element-plus/icons-vue'
import type { FinanceWorkspaceTab } from '@/stores/financeWorkspace'

const props = defineProps<{
  tabs: FinanceWorkspaceTab[]
  activePath: string
}>()

const emit = defineEmits<{
  select: [path: string]
  close: [path: string]
  closeOthers: [path: string]
  closeRight: [path: string]
}>()

function handleCommand(command: string) {
  if (!props.activePath) {
    return
  }

  if (command === 'close-current') {
    emit('close', props.activePath)
    return
  }
  if (command === 'close-others') {
    emit('closeOthers', props.activePath)
    return
  }
  if (command === 'close-right') {
    emit('closeRight', props.activePath)
  }
}
</script>

<style scoped>
.finance-tabs-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #dbe4f0;
  background: linear-gradient(180deg, #f9fbff 0%, #f3f7fd 100%);
  padding: 10px 14px 8px;
}

.finance-tabs-list {
  display: flex;
  min-width: 0;
  flex: 1;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.finance-tab {
  display: inline-flex;
  min-width: 168px;
  max-width: 240px;
  align-items: center;
  gap: 8px;
  border: 1px solid #d5dde9;
  border-radius: 12px 12px 0 0;
  background: #edf2f9;
  padding: 8px 10px 7px 12px;
  color: #516174;
  transition: all 0.18s ease;
}

.finance-tab:hover {
  border-color: #bfd1ea;
  background: #f6f9fe;
  color: #28415f;
}

.finance-tab-active {
  border-color: #b8cce6;
  border-bottom-color: #f8fbff;
  background: #f8fbff;
  color: #1f3c63;
  box-shadow: 0 -1px 0 #e7effa inset;
}

.finance-tab-title {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 600;
}

.finance-tab-close {
  height: 20px;
  width: 20px;
  color: inherit;
}

.finance-tabs-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #d5dde9;
  border-radius: 10px;
  background: #fff;
  padding: 7px 10px;
  font-size: 12px;
  color: #4d6179;
}
</style>
