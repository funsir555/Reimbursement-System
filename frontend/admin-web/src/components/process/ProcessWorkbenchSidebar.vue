<template>
  <aside class="w-60 bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden shrink-0">
    <div class="px-5 py-4 border-b border-slate-100 bg-slate-50/80">
      <p class="text-sm font-semibold text-slate-800">流程配置中心</p>
      <p class="text-xs text-slate-400 mt-1">统一维护角色、单据、表单与审核规则</p>
    </div>

    <div class="py-3">
      <button
        v-for="item in items"
        :key="item.key"
        type="button"
        class="w-full px-5 py-3 text-left transition-colors flex items-start justify-between gap-3 process-side-item"
        :class="{ 'process-side-item-active': item.key === activeKey }"
        @click="$emit('select', item.key)"
      >
        <span>
          <span class="block text-sm font-medium">{{ item.label }}</span>
          <span v-if="item.tip" class="block text-xs text-slate-400 mt-1 leading-5">{{ item.tip }}</span>
        </span>
        <span
          class="mt-1 w-2 h-2 rounded-full flex-shrink-0"
          :class="item.key === activeKey ? 'bg-blue-500' : 'bg-slate-200'"
        />
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import type { ProcessCenterNavItem } from '@/api'

defineProps<{
  items: ProcessCenterNavItem[]
  activeKey: string
}>()

defineEmits<{
  (e: 'select', key: string): void
}>()
</script>

<style scoped>
.process-side-item {
  border-left: 3px solid transparent;
}

.process-side-item:hover {
  background: #f8fbff;
}

.process-side-item-active {
  background: #eff6ff;
  border-left-color: #2563eb;
  color: #2563eb;
}
</style>
