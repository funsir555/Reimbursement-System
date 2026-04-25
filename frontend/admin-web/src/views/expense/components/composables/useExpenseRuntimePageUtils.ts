import { normalizeMoneyValue } from '@/utils/money'

export function useExpenseRuntimePageUtils() {
  function resolveErrorMessage(error: unknown, fallback: string) {
    return error instanceof Error && error.message ? error.message : fallback
  }

  function toOptionalMoney(value: unknown) {
    if (typeof value === 'number' && Number.isFinite(value)) {
      return normalizeMoneyValue(String(value))
    }

    if (typeof value === 'string' && value.trim()) {
      return normalizeMoneyValue(value)
    }

    return undefined
  }

  function toOptionalString(value: unknown) {
    if (typeof value === 'string') {
      return value
    }

    if (typeof value === 'number' && Number.isFinite(value)) {
      return String(value)
    }

    return undefined
  }

  return {
    resolveErrorMessage,
    toOptionalMoney,
    toOptionalString
  }
}
