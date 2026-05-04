<template>
  <div class="space-y-5">
    <div class="flex flex-wrap items-start justify-between gap-3">
      <div>
        <p class="text-base font-semibold text-slate-800">{{ title }}</p>
        <p v-if="summary" class="mt-1 text-sm text-slate-500">{{ summary }}</p>
      </div>
      <el-button type="primary" plain @click="handlers.addGroup()">
        {{ addGroupText }}
      </el-button>
    </div>

    <div v-if="groups.length" class="space-y-4">
      <div
        v-for="group in groups"
        :key="group.groupNo"
        class="rounded-[24px] border border-slate-200 bg-slate-50 p-5 space-y-4"
      >
        <div class="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p class="text-sm font-semibold text-slate-800">{{ groupLabelPrefix }} {{ group.groupNo }}</p>
            <p class="mt-1 text-xs text-slate-400">{{ groupHint }}</p>
          </div>
          <div class="flex flex-wrap gap-2">
            <el-button plain @click="handlers.addCondition(group.groupNo)">{{ addConditionText }}</el-button>
            <el-button type="danger" text @click="handlers.removeGroup(group.groupNo)">{{ removeGroupText }}</el-button>
          </div>
        </div>

        <div v-if="group.conditions.length" class="space-y-3">
          <div
            v-for="(condition, conditionIndex) in group.conditions"
            :key="`${group.groupNo}-${conditionIndex}`"
            class="rounded-2xl border border-slate-200 bg-white p-4 space-y-4"
          >
            <div class="space-y-4">
              <div class="process-flow-condition-primary-grid grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1.2fr),minmax(0,0.9fr)]">
                <el-form-item label="条件字段" class="!mb-0">
                  <el-select
                    v-model="condition.fieldKey"
                    placeholder="请选择条件字段"
                    @change="handleFieldChange(condition)"
                  >
                    <el-option
                      v-for="field in fields"
                      :key="field.key"
                      :label="field.label"
                      :value="field.key"
                    />
                  </el-select>
                </el-form-item>

                <el-form-item label="比较方式" class="!mb-0">
                  <el-select
                    v-model="condition.operator"
                    placeholder="请选择比较方式"
                    @change="handleOperatorChange(condition)"
                  >
                    <el-option
                      v-for="item in operatorOptionsForField(condition.fieldKey)"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </div>

              <el-form-item
                label="比较值"
                class="process-flow-condition-value-row !mb-0"
                data-testid="process-flow-condition-value-row"
              >
                <template v-if="isBetweenOperator(condition.operator)">
                  <div class="grid grid-cols-2 gap-3">
                    <el-input-number
                      :model-value="betweenConditionValue(condition, 0)"
                      class="!w-full"
                      :controls="false"
                      placeholder="起始值"
                      @update:model-value="updateBetweenConditionValue(condition, 0, $event)"
                    />
                    <el-input-number
                      :model-value="betweenConditionValue(condition, 1)"
                      class="!w-full"
                      :controls="false"
                      placeholder="结束值"
                      @update:model-value="updateBetweenConditionValue(condition, 1, $event)"
                    />
                  </div>
                </template>

                <department-tree-select
                  v-else-if="isDepartmentCondition(condition) && isMultiOperator(condition.operator)"
                  v-model="condition.compareValue"
                  :options="conditionValueOptions(condition)"
                  multiple
                  :placeholder="multiValuePlaceholder(condition)"
                />

                <el-select
                  v-else-if="isMultiOperator(condition.operator)"
                  v-model="condition.compareValue"
                  multiple
                  filterable v-bind="globalFilterableSelectProps"
                  clearable
                  collapse-tags
                  collapse-tags-tooltip
                  :tag-tooltip="globalCollapseTagTooltipProps"
                  :allow-create="!usesOptionSelect(condition)"
                  default-first-option
                  :placeholder="multiValuePlaceholder(condition)"
                >
                  <el-option
                    v-for="item in conditionValueOptions(condition)"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>

                <department-tree-select
                  v-else-if="isDepartmentCondition(condition)"
                  v-model="condition.compareValue"
                  :options="conditionValueOptions(condition)"
                  :placeholder="singleValuePlaceholder(condition)"
                />

                <el-select
                  v-else-if="usesOptionSelect(condition)"
                  v-model="condition.compareValue"
                  filterable v-bind="globalFilterableSelectProps"
                  clearable
                  :placeholder="singleValuePlaceholder(condition)"
                >
                  <el-option
                    v-for="item in conditionValueOptions(condition)"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>

                <el-input-number
                  v-else-if="isNumberCondition(condition)"
                  v-model="condition.compareValue"
                  class="!w-full"
                  :controls="false"
                  placeholder="请输入数值"
                />

                <el-input
                  v-else
                  v-model="condition.compareValue"
                  :placeholder="singleValuePlaceholder(condition)"
                />
              </el-form-item>
            </div>

            <div class="flex justify-end">
              <el-button type="danger" text @click="handlers.removeCondition(group.groupNo, conditionIndex)">
                {{ removeConditionText }}
              </el-button>
            </div>
          </div>
        </div>

        <el-empty v-else :description="emptyConditionsText" :image-size="56" />
      </div>
    </div>

    <el-empty v-else :description="emptyGroupsText" :image-size="64" />
  </div>
</template>

<script setup lang="ts">
import type { ProcessFlowCondition, ProcessFlowConditionField, ProcessFlowConditionGroup, ProcessFormOption } from '@/api'
import DepartmentTreeSelect from '@/components/inputs/DepartmentTreeSelect.vue'
import { globalCollapseTagTooltipProps } from '@/utils/collapseTagTooltip'
import { globalFilterableSelectProps } from '@/utils/filterableSelect'


type OptionSourceMap = Partial<Record<string, ProcessFormOption[]>>

const props = withDefaults(defineProps<{
  groups: ProcessFlowConditionGroup[]
  fields: ProcessFlowConditionField[]
  operatorOptions: ProcessFormOption[]
  optionSources?: OptionSourceMap
  title?: string
  summary?: string
  addGroupText?: string
  addConditionText?: string
  removeGroupText?: string
  removeConditionText?: string
  emptyGroupsText?: string
  emptyConditionsText?: string
  groupLabelPrefix?: string
  groupHint?: string
  handlers: {
    addGroup: () => void
    removeGroup: (groupNo: number) => void
    addCondition: (groupNo: number) => void
    removeCondition: (groupNo: number, index: number) => void
  }
}>(), {
  optionSources: () => ({}),
  title: '条件设置',
  summary: '请配置条件组与条件项。',
  addGroupText: '新增条件组',
  addConditionText: '新增条件',
  removeGroupText: '删除条件组',
  removeConditionText: '删除条件',
  emptyGroupsText: '当前还没有条件组',
  emptyConditionsText: '当前条件组还没有条件项',
  groupLabelPrefix: '条件组',
  groupHint: '组内条件按“且”计算，组间按“或”计算。'
})

function getField(fieldKey?: string) {
  return props.fields.find((item) => item.key === fieldKey) || props.fields[0]
}

function operatorOptionsForField(fieldKey?: string) {
  const field = getField(fieldKey)
  if (!field) {
    return props.operatorOptions
  }
  return props.operatorOptions.filter((item) => field.operatorKeys.includes(item.value))
}

function isMultiOperator(operator?: string) {
  return operator === 'IN' || operator === 'NOT_IN'
}

function isBetweenOperator(operator?: string) {
  return operator === 'BETWEEN'
}

function betweenConditionValue(condition: ProcessFlowCondition, index: number) {
  return Array.isArray(condition.compareValue) ? condition.compareValue[index] as number | null : null
}

function updateBetweenConditionValue(condition: ProcessFlowCondition, index: number, value?: number) {
  const nextValue = Array.isArray(condition.compareValue) ? [...condition.compareValue] : [null, null]
  nextValue[index] = value ?? null
  condition.compareValue = nextValue
}

function normalizeScalarValue(value: unknown, valueType?: string) {
  if (valueType === 'number') {
    if (value === undefined || value === null || value === '') {
      return null
    }
    const numeric = Number(value)
    return Number.isFinite(numeric) ? numeric : null
  }
  if (value === undefined || value === null) {
    return ''
  }
  return String(value)
}

function normalizeCompareValue(value: unknown, valueType?: string, operator?: string) {
  if (isBetweenOperator(operator)) {
    const source = Array.isArray(value) ? value : []
    return [
      normalizeScalarValue(source[0], valueType),
      normalizeScalarValue(source[1], valueType)
    ]
  }
  if (isMultiOperator(operator)) {
    const source = Array.isArray(value)
      ? value
      : value === undefined || value === null || value === ''
        ? []
        : [value]
    return source.map((item) => normalizeScalarValue(item, valueType))
  }
  return normalizeScalarValue(value, valueType)
}

function conditionValueOptions(condition: Pick<ProcessFlowCondition, 'fieldKey'>) {
  const field = getField(condition.fieldKey)
  if (!field) {
    return []
  }
  return props.optionSources[field.valueType] || []
}

function usesOptionSelect(condition: Pick<ProcessFlowCondition, 'fieldKey'>) {
  return conditionValueOptions(condition).length > 0
}

function isNumberCondition(condition: Pick<ProcessFlowCondition, 'fieldKey'>) {
  return getField(condition.fieldKey)?.valueType === 'number'
}

function isDepartmentCondition(condition: Pick<ProcessFlowCondition, 'fieldKey'>) {
  return getField(condition.fieldKey)?.valueType === 'department'
}

function singleValuePlaceholder(condition: Pick<ProcessFlowCondition, 'fieldKey'>) {
  const field = getField(condition.fieldKey)
  if (!field) {
    return '请选择或输入比较值'
  }
  if (usesOptionSelect(condition)) {
    return `请选择${field.label}`
  }
  if (field.valueType === 'number') {
    return '请输入数值'
  }
  return `请输入${field.label}`
}

function multiValuePlaceholder(condition: Pick<ProcessFlowCondition, 'fieldKey'>) {
  const field = getField(condition.fieldKey)
  if (!field) {
    return '请选择多个比较值'
  }
  return usesOptionSelect(condition) ? `请选择多个${field.label}` : `请输入多个${field.label}`
}

function handleFieldChange(condition: ProcessFlowCondition) {
  const field = getField(condition.fieldKey)
  const nextOperator = operatorOptionsForField(field?.key)[0]?.value || 'EQ'
  condition.fieldKey = field?.key || ''
  condition.operator = nextOperator
  condition.compareValue = normalizeCompareValue(undefined, field?.valueType, nextOperator)
}

function handleOperatorChange(condition: ProcessFlowCondition) {
  const field = getField(condition.fieldKey)
  condition.compareValue = normalizeCompareValue(condition.compareValue, field?.valueType, condition.operator)
}
</script>
