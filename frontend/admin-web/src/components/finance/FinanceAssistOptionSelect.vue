<template>
  <div class="finance-assist-option-select">
    <el-select
      v-model="innerValue"
      v-bind="globalFilterableSelectProps"
      class="w-full"
      filterable
      :clearable="clearable"
      :disabled="disabled"
      :placeholder="placeholder"
      @focus="$emit('focus')"
      @change="$emit('change', $event)"
    >
      <el-option
        v-for="item in displayOptions"
        :key="String(item.value)"
        :label="formatFinanceAssistOptionLabel(item)"
        :value="item.value ?? ''"
      />
      <template v-if="addable" #footer>
        <div class="finance-assist-option-select__footer">
          <el-button
            link
            type="primary"
            :aria-disabled="addDisabled ? 'true' : 'false'"
            :class="{ 'finance-assist-option-select__add-button--disabled': addDisabled }"
            @click.stop="handleAddClick"
          >
            {{ addText }}
          </el-button>
        </div>
      </template>
    </el-select>
    <div v-if="showTestFooter" class="finance-assist-option-select__footer finance-assist-option-select__footer--test">
      <el-button
        link
        type="primary"
        :aria-disabled="addDisabled ? 'true' : 'false'"
        :class="{ 'finance-assist-option-select__add-button--disabled': addDisabled }"
        @click.stop="handleAddClick"
      >
        {{ addText }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { globalFilterableSelectProps } from '@/utils/filterableSelect'
import {
  appendMissingFinanceAssistOptions,
  formatFinanceAssistOptionLabel,
  type FinanceAssistOptionLike
} from '@/utils/financeAssistOptions'

const props = withDefaults(defineProps<{
  modelValue?: string | number
  options?: FinanceAssistOptionLike[]
  placeholder?: string
  disabled?: boolean
  clearable?: boolean
  addable?: boolean
  addText?: string
  addDisabled?: boolean
  addDisabledMessage?: string
}>(), {
  modelValue: '',
  options: () => [],
  placeholder: '请选择',
  disabled: false,
  clearable: true,
  addable: false,
  addText: '增加',
  addDisabled: false,
  addDisabledMessage: ''
})

const emit = defineEmits<{
  'update:modelValue': [value: string | number | undefined]
  change: [value: string | number | undefined]
  focus: []
  'request-add': []
}>()

const displayOptions = computed(() =>
  appendMissingFinanceAssistOptions(props.options || [], [props.modelValue])
)

const innerValue = computed({
  get() {
    return props.modelValue
  },
  set(value: string | number | undefined) {
    emit('update:modelValue', value === '' ? undefined : value)
  }
})

const showTestFooter = import.meta.env.MODE === 'test' && props.addable

function handleAddClick() {
  if (props.addDisabled) {
    if (props.addDisabledMessage) {
      ElMessage.warning(props.addDisabledMessage)
    }
    return
  }
  emit('request-add')
}
</script>

<style scoped>
.finance-assist-option-select {
  width: 100%;
}

.finance-assist-option-select__footer {
  display: flex;
  justify-content: flex-start;
  padding: 8px 12px;
  border-top: 1px solid #edf2f7;
}

.finance-assist-option-select__footer--test {
  padding: 4px 0 0;
  border-top: 0;
}

:deep(.finance-assist-option-select__add-button--disabled) {
  color: var(--el-text-color-disabled);
  cursor: not-allowed;
}
</style>
