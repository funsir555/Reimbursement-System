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

    <div class="finance-tabs-tools">
      <label v-if="(companyOptions || []).length" class="finance-company-switcher">
        <span>当前公司</span>
        <el-select
          :model-value="currentCompanyId"
          class="finance-company-select"
          filterable
          :loading="companyLoading || companySwitching"
          :disabled="companyLoading || companySwitching"
          placeholder="请选择公司"
          @update:model-value="handleCompanyChange"
        >
          <el-option
            v-for="item in companyOptions"
            :key="item.companyId"
            :label="item.label"
            :value="item.companyId"
          />
        </el-select>
      </label>

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
  </div>
</template>

<script setup lang="ts">
import { Close, Operation } from '@element-plus/icons-vue'
import type { FinanceWorkspaceTab } from '@/stores/financeWorkspace'
import type { FinanceCompanyOption } from '@/api'

const props = defineProps<{
  tabs: FinanceWorkspaceTab[]
  activePath: string
  companyOptions?: FinanceCompanyOption[]
  currentCompanyId?: string
  companyLoading?: boolean
  companySwitching?: boolean
}>()

const emit = defineEmits<{
  select: [path: string]
  close: [path: string]
  closeOthers: [path: string]
  closeRight: [path: string]
  changeCompany: [companyId: string]
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

function handleCompanyChange(companyId: string) {
  emit('changeCompany', companyId)
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

.finance-tabs-tools {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
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

.finance-company-switcher {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid #d5dde9;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.92);
  padding: 6px 10px;
}

.finance-company-switcher span {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 600;
  color: #4d6179;
}

.finance-company-select {
  width: 260px;
}

:deep(.finance-company-select .el-select__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #d8e2f0 inset;
}

@media (max-width: 1024px) {
  .finance-tabs-wrap {
    flex-direction: column;
    align-items: stretch;
  }

  .finance-tabs-tools {
    justify-content: space-between;
  }
}

@media (max-width: 768px) {
  .finance-tabs-tools {
    flex-direction: column;
    align-items: stretch;
  }

  .finance-company-switcher {
    width: 100%;
    justify-content: space-between;
  }

  .finance-company-select {
    width: 100%;
  }
}
</style>
