<template>
  <el-input
    :model-value="displayValue"
    :placeholder="placeholder"
    :disabled="disabled"
    :readonly="readonly"
    :clearable="clearable"
    @focus="handleFocus"
    @input="handleInput"
    @blur="handleBlur"
  />
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { normalizeMoneyValue, sanitizeMoneyDraftValue } from '@/utils/money'

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

const isFocused = ref(false)
const draftValue = ref('')

watch(
  () => props.modelValue,
  (nextValue) => {
    if (!isFocused.value) {
      draftValue.value = sanitizeMoneyDraftValue(nextValue, {
        allowNegative: props.allowNegative,
        fallback: ''
      })
    }
  },
  { immediate: true }
)

const displayValue = computed(() => (isFocused.value ? draftValue.value : props.modelValue || ''))

function sanitizeInput(value: string) {
  return sanitizeMoneyDraftValue(value, {
    allowNegative: props.allowNegative,
    fallback: ''
  })
}

function handleFocus() {
  isFocused.value = true
  draftValue.value = sanitizeInput(props.modelValue || '')
}

function handleInput(value: string) {
  const nextValue = sanitizeInput(value)
  draftValue.value = nextValue
  emit('update:modelValue', nextValue)
}

function handleBlur() {
  isFocused.value = false
  const nextValue = sanitizeInput(draftValue.value)
  draftValue.value = nextValue
  try {
    emit('update:modelValue', normalizeMoneyValue(nextValue, { allowNegative: props.allowNegative }))
  } catch {
    emit('update:modelValue', '')
  } finally {
    emit('blur')
  }
}
</script>
