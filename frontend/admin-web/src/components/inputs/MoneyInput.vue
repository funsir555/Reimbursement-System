<template>
  <el-input
    :model-value="displayValue"
    :placeholder="placeholder"
    :disabled="disabled"
    :readonly="readonly"
    :clearable="clearable"
    @input="handleInput"
    @blur="handleBlur"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { normalizeMoneyValue } from '@/utils/money'

const props = withDefaults(defineProps<{
  modelValue?: string
  placeholder?: string
  disabled?: boolean
  readonly?: boolean
  clearable?: boolean
  allowNegative?: boolean
}>(), {
  modelValue: '',
  placeholder: '0.00',
  disabled: false,
  readonly: false,
  clearable: false,
  allowNegative: false
})

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
  (event: 'blur'): void
}>()

const displayValue = computed(() => props.modelValue || '')

function sanitizeInput(value: string) {
  const text = String(value || '').replace(/,/g, '').trim()
  if (!text) {
    return ''
  }
  const negativePrefix = props.allowNegative && text.startsWith('-') ? '-' : ''
  const pure = text.replace(/^-/, '').replace(/[^\d.]/g, '')
  const [wholePart = '', ...decimalParts] = pure.split('.')
  const decimalPart = decimalParts.join('').slice(0, 2)
  const normalizedWhole = wholePart.replace(/^0+(?=\d)/, '') || '0'
  return `${negativePrefix}${decimalPart ? `${normalizedWhole}.${decimalPart}` : normalizedWhole}`
}

function handleInput(value: string) {
  emit('update:modelValue', sanitizeInput(value))
}

function handleBlur() {
  try {
    emit('update:modelValue', normalizeMoneyValue(props.modelValue, { allowNegative: props.allowNegative }))
  } catch {
    emit('update:modelValue', '')
  } finally {
    emit('blur')
  }
}
</script>
