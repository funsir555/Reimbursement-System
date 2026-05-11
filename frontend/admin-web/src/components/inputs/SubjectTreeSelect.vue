<template>
  <select
    v-if="useTestSelectFallback"
    v-bind="attrs"
    class="w-full"
    :disabled="disabled"
    :value="nativeSelectValue"
    @focus="emit('focus')"
    @change="handleNativeChange"
  >
    <option v-if="clearable" value="">{{ placeholder }}</option>
    <option
      v-for="item in flatOptions"
      :key="item.value"
      :value="item.value"
      :disabled="item.disabled"
    >
      {{ item.label }}
    </option>
  </select>
  <el-tree-select
    v-else
    v-model="innerValue"
    v-bind="treeSelectBindings"
    class="w-full"
    :data="displayTreeOptions"
    node-key="value"
    check-strictly
    filterable
    :clearable="clearable"
    :disabled="disabled"
    :placeholder="placeholder"
    :props="{ label: 'label', children: 'children', value: 'value', disabled: 'disabled' }"
    :filter-node-method="filterSubjectTreeNode"
    @focus="emit('focus')"
    @change="emit('change', $event)"
    @visible-change="emit('visible-change', $event)"
  />
</template>

<script setup lang="ts">
import { computed, useAttrs } from 'vue'
import type { FinanceVoucherOption } from '@/api'
import { globalFilterableSelectProps } from '@/utils/filterableSelect'
import { appendMissingFinanceAssistOptions } from '@/utils/financeAssistOptions'
import { buildSubjectTreeOptions, filterSubjectTreeNode, flattenSubjectTreeOptions } from '@/utils/subjectTree'

defineOptions({
  inheritAttrs: false
})

const props = withDefaults(defineProps<{
  modelValue?: string
  options?: FinanceVoucherOption[]
  disabled?: boolean
  placeholder?: string
  clearable?: boolean
  forceTreeSelect?: boolean
}>(), {
  modelValue: '',
  options: () => [],
  disabled: false,
  placeholder: '请选择科目',
  clearable: true,
  forceTreeSelect: false
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  focus: []
  change: [value: string]
  'visible-change': [visible: boolean]
}>()

const attrs = useAttrs()

const treeSelectBindings = computed(() => ({
  ...globalFilterableSelectProps,
  ...attrs
}))

const selectedValue = computed(() => normalizeValue(props.modelValue))

const baseTreeOptions = computed(() => buildSubjectTreeOptions(props.options || []))

const displayOptions = computed(() => appendMissingFinanceAssistOptions(props.options || [], [selectedValue.value]))

const displayTreeOptions = computed(() => {
  if (displayOptions.value.length === (props.options || []).length) {
    return baseTreeOptions.value
  }
  return buildSubjectTreeOptions(displayOptions.value)
})

const flatOptions = computed(() => flattenSubjectTreeOptions(displayTreeOptions.value))

const useTestSelectFallback = import.meta.env.MODE === 'test' && !props.forceTreeSelect

const nativeSelectValue = computed(() => selectedValue.value || '')

const innerValue = computed({
  get() {
    return selectedValue.value || undefined
  },
  set(value: string | undefined) {
    emit('update:modelValue', normalizeValue(value))
  }
})

function handleNativeChange(event: Event) {
  const target = event.target
  if (!(target instanceof HTMLSelectElement)) {
    return
  }
  const value = normalizeValue(target.value)
  emit('update:modelValue', value)
  emit('change', value)
}

function normalizeValue(value?: string | null) {
  return String(value || '').trim()
}
</script>
