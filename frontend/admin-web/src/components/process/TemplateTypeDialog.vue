<template>
  <el-dialog
    :model-value="modelValue"
    width="920px"
    top="9vh"
    destroy-on-close
    @close="$emit('update:modelValue', false)"
  >
    <template #header>
      <div>
        <h3 class="text-xl font-semibold text-slate-800">请选择单据模板类型</h3>
        <p class="text-sm text-slate-400 mt-1">
          先选择一个起始模板，再进入表单与流程的详细配置页面。
        </p>
      </div>
    </template>

    <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
      <button
        v-for="option in options"
        :key="option.code"
        type="button"
        class="rounded-3xl border border-slate-200 bg-slate-50 hover:bg-white hover:border-blue-300 hover:shadow-md transition-all p-6 text-left"
        @click="$emit('select', option.code)"
      >
        <div
          class="w-14 h-14 rounded-2xl flex items-center justify-center text-white shadow-sm"
          :style="{ background: accentMap[option.accent] || accentMap.blue }"
        >
          <el-icon :size="28">
            <component :is="iconMap[option.code as TemplateIconKey] || Document" />
          </el-icon>
        </div>

        <div class="mt-5">
          <div class="flex items-center justify-between gap-3">
            <h4 class="text-lg font-semibold text-slate-800">{{ option.name }}</h4>
            <span class="text-xs px-2 py-1 rounded-full bg-white text-slate-500 border border-slate-200">
              {{ option.subtitle }}
            </span>
          </div>
          <p class="text-sm text-slate-500 leading-6 mt-3">{{ option.description }}</p>
        </div>
      </button>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { CreditCard, Document, Files, Tickets } from '@element-plus/icons-vue'
import type { ProcessTemplateTypeOption } from '@/api'

defineProps<{
  modelValue: boolean
  options: ProcessTemplateTypeOption[]
}>()

defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'select', code: string): void
}>()

const iconMap = {
  report: Document,
  application: Files,
  loan: CreditCard,
  contract: Tickets
}

type TemplateIconKey = keyof typeof iconMap

const accentMap: Record<string, string> = {
  blue: 'linear-gradient(135deg, #2563eb 0%, #60a5fa 100%)',
  cyan: 'linear-gradient(135deg, #0891b2 0%, #67e8f9 100%)',
  orange: 'linear-gradient(135deg, #ea580c 0%, #fdba74 100%)',
  emerald: 'linear-gradient(135deg, #047857 0%, #34d399 100%)'
}
</script>
