<template>
  <div class="space-y-6">
    <div class="rounded-[24px] border border-slate-200 bg-slate-50 px-5 py-4">
      <div class="grid grid-cols-1 gap-4 text-sm text-slate-600 xl:grid-cols-3">
        <div>表单组件数：{{ blocks.length }}</div>
        <div>布局模式：{{ schema?.layoutMode || 'TWO_COLUMN' }}</div>
        <div>当前视图：只读详情</div>
      </div>
    </div>

    <div class="grid grid-cols-1 gap-5 md:grid-cols-2">
      <div
        v-for="block in blocks"
        :key="block.blockId"
        class="rounded-[24px] border border-slate-200 bg-white p-5 shadow-sm"
        :class="block.span === 2 ? 'md:col-span-2' : ''"
      >
        <template v-if="controlType(block) === 'SECTION'">
          <p class="text-lg font-semibold text-slate-800">{{ block.label }}</p>
          <p class="mt-2 whitespace-pre-wrap text-sm leading-7 text-slate-500">
            {{ String(block.props.content || block.helpText || '') || '暂无内容' }}
          </p>
        </template>

        <template v-else>
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <p class="text-sm font-semibold text-slate-800">{{ block.label }}</p>
              <p v-if="block.helpText" class="mt-1 text-xs leading-6 text-slate-400">{{ block.helpText }}</p>
            </div>
            <el-tag v-if="block.required" size="small" type="danger" effect="plain">必填</el-tag>
          </div>

          <div class="mt-4 rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-3">
            <template v-if="displayLines(block).length">
              <p
                v-for="(line, index) in displayLines(block)"
                :key="`${block.blockId}-${index}`"
                class="break-words text-sm leading-7 text-slate-700"
              >
                {{ line }}
              </p>
            </template>
            <p v-else class="text-sm leading-7 text-slate-400">未填写</p>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ProcessFormDesignBlock, ProcessFormDesignSchema, ProcessFormOption } from '@/api'
import {
  getBusinessComponentDefinition,
  getControlType,
  getOptionItems
} from '@/views/process/formDesignerHelper'

const props = withDefaults(defineProps<{
  schema: ProcessFormDesignSchema
  formData: Record<string, unknown>
  departmentOptions?: ProcessFormOption[]
}>(), {
  departmentOptions: () => []
})

const blocks = computed(() => props.schema?.blocks || [])
const departmentMap = computed(() => new Map((props.departmentOptions || []).map((item) => [item.value, item.label])))

function controlType(block: ProcessFormDesignBlock) {
  return getControlType(block)
}

function businessCode(block: ProcessFormDesignBlock) {
  return getBusinessComponentDefinition(String(block.props.componentCode || ''))?.code || String(block.props.componentCode || '')
}

function displayLines(block: ProcessFormDesignBlock) {
  const rawValue = props.formData?.[block.fieldKey]

  if (block.kind === 'CONTROL') {
    return controlDisplayLines(block, rawValue)
  }

  if (block.kind === 'BUSINESS_COMPONENT') {
    if (businessCode(block) === 'undertake-department') {
      return normalizeLines(rawValue).map((item) => departmentMap.value.get(item) || item)
    }
    return normalizeLines(rawValue)
  }

  return normalizeLines(rawValue)
}

function controlDisplayLines(block: ProcessFormDesignBlock, rawValue: unknown) {
  const type = controlType(block)
  if (['SELECT', 'MULTI_SELECT', 'RADIO', 'CHECKBOX'].includes(type)) {
    const optionMap = new Map(getOptionItems(block).map((item) => [String(item.value), item.label]))
    return normalizeLines(rawValue).map((item) => optionMap.get(item) || item)
  }
  if (type === 'SWITCH') {
    return [rawValue ? '已开启' : '已关闭']
  }
  if (type === 'DATE_RANGE' && Array.isArray(rawValue)) {
    return rawValue.map((item) => String(item))
  }
  return normalizeLines(rawValue)
}

function normalizeLines(value: unknown): string[] {
  if (Array.isArray(value)) {
    return value
      .map((item) => formatValue(item))
      .filter((item) => item.trim().length > 0)
  }
  const text = formatValue(value)
  return text ? text.split('\n').filter((item) => item.trim().length > 0) : []
}

function formatValue(value: unknown): string {
  if (value === null || value === undefined) {
    return ''
  }
  if (typeof value === 'object') {
    if ('label' in (value as Record<string, unknown>) && typeof (value as Record<string, unknown>).label === 'string') {
      return String((value as Record<string, unknown>).label)
    }
    if ('value' in (value as Record<string, unknown>) && typeof (value as Record<string, unknown>).value === 'string') {
      return String((value as Record<string, unknown>).value)
    }
    return JSON.stringify(value)
  }
  return String(value)
}
</script>
