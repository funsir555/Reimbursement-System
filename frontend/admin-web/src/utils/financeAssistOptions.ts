export interface FinanceAssistOptionLike {
  value?: string | number | null
  code?: string | number | null
  name?: string | null
  label?: string | null
  parentValue?: string | number | null
}

export function normalizeFinanceAssistText(value?: unknown) {
  const text = String(value ?? '').trim()
  return text || undefined
}

export function formatFinanceAssistOptionLabel(option?: FinanceAssistOptionLike | null) {
  if (!option) {
    return ''
  }
  const code = normalizeFinanceAssistText(option.code)
  const name = normalizeFinanceAssistText(option.name)
  const label = normalizeFinanceAssistText(option.label)
  const value = normalizeFinanceAssistText(option.value)
  if (code && name) {
    return `${code}  ${name}`
  }
  return name || code || label || value || ''
}

export function appendMissingFinanceAssistOptions<T extends FinanceAssistOptionLike>(
  optionList: T[] = [],
  selectedValues: Array<string | number | null | undefined> = []
) {
  const result = [...optionList]
  const existingValues = new Set(result.map((item) => normalizeFinanceAssistText(item.value)))

  selectedValues.forEach((item) => {
    const normalizedValue = normalizeFinanceAssistText(item)
    if (!normalizedValue || existingValues.has(normalizedValue)) {
      return
    }
    existingValues.add(normalizedValue)
    result.push({
      value: normalizedValue,
      code: normalizedValue,
      label: normalizedValue
    } as T)
  })

  return result
}
