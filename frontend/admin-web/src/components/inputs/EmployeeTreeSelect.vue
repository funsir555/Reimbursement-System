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
  <el-select
    v-else-if="remote"
    v-model="innerValue"
    v-bind="globalFilterableSelectProps"
    class="w-full"
    remote
    filterable
    :multiple="multiple"
    :clearable="clearable"
    :collapse-tags="collapseTags"
    :collapse-tags-tooltip="collapseTagsTooltip"
    :tag-tooltip="tagTooltip"
    :disabled="disabled"
    :placeholder="placeholder"
    :loading="loading"
    :remote-method="handleRemoteSearch"
  >
    <template
      v-for="group in remoteGroups"
      :key="group.label"
    >
      <el-option-group :label="group.label">
        <el-option
          v-for="item in group.options"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-option-group>
    </template>
  </el-select>
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
    :props="{ label: 'label', children: 'children', value: 'value', disabled: 'disabled' }"
    :filter-node-method="filterEmployeeTreeNode"
  />
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { globalCollapseTagTooltipProps, type GlobalCollapseTagTooltipProps } from '@/utils/collapseTagTooltip'
import { globalFilterableSelectProps } from '@/utils/filterableSelect'
import {
  appendMissingEmployeeTreeOptions,
  buildEmployeeTreeOptions,
  filterEmployeeTreeNode,
  flattenSelectableEmployeeTreeNodes,
  normalizeEmployeeTreeValue,
  type EmployeeTreeLabelMode,
  type EmployeeTreeDepartmentLike,
  type EmployeeTreeEmployeeLike,
  type EmployeeTreeExtraOptionLike,
  type EmployeeTreeNode,
  type EmployeeTreeValue,
  type EmployeeTreeValueType
} from '@/utils/employeeTree'

const props = withDefaults(defineProps<{
  modelValue?: unknown
  departments?: EmployeeTreeDepartmentLike[]
  employees?: EmployeeTreeEmployeeLike[]
  multiple?: boolean
  disabled?: boolean
  placeholder?: string
  clearable?: boolean
  collapseTags?: boolean
  collapseTagsTooltip?: boolean
  valueType?: EmployeeTreeValueType
  forceTreeSelect?: boolean
  tagTooltip?: GlobalCollapseTagTooltipProps
  extraOptions?: EmployeeTreeExtraOptionLike[]
  remote?: boolean
  remoteMethod?: (keyword: string) => Promise<EmployeeTreeEmployeeLike[] | void> | EmployeeTreeEmployeeLike[] | void
  labelMode?: EmployeeTreeLabelMode
}>(), {
  departments: () => [],
  employees: () => [],
  multiple: false,
  disabled: false,
  placeholder: '请选择人员',
  clearable: true,
  collapseTags: true,
  collapseTagsTooltip: true,
  valueType: 'string',
  forceTreeSelect: false,
  tagTooltip: () => globalCollapseTagTooltipProps,
  extraOptions: () => [],
  remote: false,
  remoteMethod: undefined,
  labelMode: 'default'
})

const emit = defineEmits<{
  'update:modelValue': [value: EmployeeTreeValue | EmployeeTreeValue[] | undefined]
}>()

const remoteEmployees = ref<EmployeeTreeEmployeeLike[]>([])
const loading = ref(false)

const selectedValues = computed(() => {
  const value = props.modelValue
  const source = Array.isArray(value)
    ? value
    : value === undefined || value === null || value === ''
      ? []
      : [value]
  return source
    .map((item) => normalizeEmployeeTreeValue(item, props.valueType))
    .filter((item): item is EmployeeTreeValue => item !== undefined)
})

const effectiveEmployees = computed(() => (
  props.remote ? remoteEmployees.value : (props.employees || [])
))

const baseTreeOptions = computed(() => buildEmployeeTreeOptions(
  props.departments || [],
  effectiveEmployees.value,
  {
    valueType: props.valueType,
    extraOptions: props.extraOptions || [],
    labelMode: props.labelMode
  }
))

const displayTreeOptions = computed(() => {
  const missingTree = appendMissingEmployeeTreeOptions(baseTreeOptions.value, selectedValues.value, props.valueType)
  return missingTree.length === baseTreeOptions.value.length ? baseTreeOptions.value : missingTree
})

const flatOptions = computed(() => flattenSelectableEmployeeTreeNodes(displayTreeOptions.value))

const remoteGroups = computed(() => groupRemoteOptions(displayTreeOptions.value))

const useTestSelectFallback = computed(() => import.meta.env.MODE === 'test' && !props.forceTreeSelect)

const nativeSelectValue = computed(() => props.multiple ? selectedValues.value.map(String) : String(selectedValues.value[0] ?? ''))

const innerValue = computed({
  get() {
    return props.multiple ? selectedValues.value : selectedValues.value[0]
  },
  set(value: EmployeeTreeValue | EmployeeTreeValue[] | undefined) {
    if (props.multiple) {
      const source = Array.isArray(value) ? value : value === undefined || value === null ? [] : [value]
      emit('update:modelValue', source
        .map((item) => normalizeEmployeeTreeValue(item, props.valueType))
        .filter((item): item is EmployeeTreeValue => item !== undefined))
      return
    }
    emit('update:modelValue', normalizeEmployeeTreeValue(value, props.valueType))
  }
})

watch(
  () => [props.remote, props.employees] as const,
  ([remote, employees]) => {
    if (!remote) {
      remoteEmployees.value = employees || []
    }
  },
  { immediate: true }
)

async function handleRemoteSearch(keyword: string) {
  if (!props.remoteMethod) {
    remoteEmployees.value = props.employees || []
    return
  }
  loading.value = true
  try {
    const result = await props.remoteMethod(keyword)
    remoteEmployees.value = Array.isArray(result) ? result : props.employees || []
  } finally {
    loading.value = false
  }
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

function groupRemoteOptions(nodes: EmployeeTreeNode[]) {
  return nodes
    .filter((node) => node.isDepartment)
    .map((node) => ({
      label: node.label,
      options: flattenSelectableEmployeeTreeNodes(node.children)
    }))
    .filter((group) => group.options.length)
}
</script>
