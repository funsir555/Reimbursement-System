<template>
  <select
    v-if="useTestSelectFallback"
    class="w-full"
    :disabled="disabled"
    :multiple="multiple"
    :value="nativeSelectValue"
    @change="handleNativeChange"
  >
    <option v-if="!multiple && clearable" value="">{{ placeholder }}</option>
    <option
      v-for="item in flatOptions"
      :key="item.value"
      :value="item.value"
    >
      {{ item.label }}
    </option>
  </select>
  <el-tree-select
    v-else
    v-model="innerValue"
    v-bind="globalFilterableSelectProps"
    class="w-full"
    :data="displayTreeOptions"
    node-key="value"
    check-strictly
    filterable
    :clearable="clearable"
    :disabled="disabled"
    :multiple="multiple"
    :show-checkbox="multiple"
    :collapse-tags="collapseTags"
    :collapse-tags-tooltip="collapseTagsTooltip"
    :tag-tooltip="tagTooltip"
    :placeholder="placeholder"
    :props="{ label: 'label', children: 'children', value: 'value' }"
    :filter-node-method="filterDepartmentTreeNode"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { globalCollapseTagTooltipProps, type GlobalCollapseTagTooltipProps } from '@/utils/collapseTagTooltip'
import { globalFilterableSelectProps } from '@/utils/filterableSelect'
import {
  appendMissingDepartmentTreeOptions,
  buildDepartmentTreeOptions,
  filterDepartmentTreeNode,
  normalizeDepartmentValue,
  type DepartmentOptionLike,
  type DepartmentTreeOption,
  type DepartmentTreeValue,
  type DepartmentTreeValueType
} from '@/utils/departmentTree'

const props = withDefaults(defineProps<{
  modelValue?: unknown
  options?: DepartmentOptionLike[]
  multiple?: boolean
  disabled?: boolean
  placeholder?: string
  clearable?: boolean
  collapseTags?: boolean
  collapseTagsTooltip?: boolean
  valueType?: DepartmentTreeValueType
  forceTreeSelect?: boolean
  tagTooltip?: GlobalCollapseTagTooltipProps
}>(), {
  options: () => [],
  multiple: false,
  disabled: false,
  placeholder: '请选择部门',
  clearable: true,
  collapseTags: true,
  collapseTagsTooltip: true,
  valueType: 'string',
  forceTreeSelect: false,
  tagTooltip: () => globalCollapseTagTooltipProps
})

const emit = defineEmits<{
  'update:modelValue': [value: DepartmentTreeValue | DepartmentTreeValue[] | undefined]
}>()

const selectedValues = computed(() => {
  const value = props.modelValue
  const source = Array.isArray(value)
    ? value
    : value === undefined || value === null || value === ''
      ? []
      : [value]
  return source
    .map((item) => normalizeDepartmentValue(item, props.valueType))
    .filter((item): item is DepartmentTreeValue => item !== undefined)
})

const baseTreeOptions = computed(() => buildDepartmentTreeOptions(props.options || [], { valueType: props.valueType }))

const displayTreeOptions = computed(() => {
  const missingTree = appendMissingDepartmentTreeOptions(baseTreeOptions.value, selectedValues.value, props.valueType)
  return missingTree.length === baseTreeOptions.value.length ? baseTreeOptions.value : missingTree
})

const flatOptions = computed(() => flattenTreeOptions(displayTreeOptions.value))

const useTestSelectFallback = import.meta.env.MODE === 'test' && !props.forceTreeSelect

const nativeSelectValue = computed(() => props.multiple ? selectedValues.value.map(String) : String(selectedValues.value[0] ?? ''))

const innerValue = computed({
  get() {
    return props.multiple ? selectedValues.value : selectedValues.value[0]
  },
  set(value: DepartmentTreeValue | DepartmentTreeValue[] | undefined) {
    if (props.multiple) {
      const source = Array.isArray(value) ? value : value === undefined || value === null ? [] : [value]
      emit('update:modelValue', source
        .map((item) => normalizeDepartmentValue(item, props.valueType))
        .filter((item): item is DepartmentTreeValue => item !== undefined))
      return
    }
    emit('update:modelValue', normalizeDepartmentValue(value, props.valueType))
  }
})

function flattenTreeOptions(nodes: DepartmentTreeOption[]) {
  const result: Array<{ value: DepartmentTreeValue; label: string }> = []
  const walk = (items: DepartmentTreeOption[]) => {
    items.forEach((item) => {
      result.push({ value: item.value, label: item.label })
      if (item.children.length) {
        walk(item.children)
      }
    })
  }
  walk(nodes)
  return result
}

function handleNativeChange(event: Event) {
  const target = event.target
  if (!(target instanceof HTMLSelectElement)) {
    return
  }
  if (props.multiple) {
    innerValue.value = Array.from(target.selectedOptions).map((item) => item.value)
    return
  }
  innerValue.value = target.value
}
</script>
