import { isZeroMoney, normalizeMoneyValue } from '@/utils/money'

export function useFinanceNewVoucherPageUtils() {
  function resolveErrorMessage(error: unknown, fallback: string) {
    return error instanceof Error && error.message ? error.message : fallback
  }

  function toOptionalString(value?: string | null) {
    const normalized = String(value ?? '').trim()
    return normalized || undefined
  }

  function toOptionalMoney(value?: string | null) {
    const normalized = normalizeMoneyValue(value || '', { allowNegative: true, fallback: '' })
    if (!normalized || isZeroMoney(normalized)) {
      return undefined
    }
    return normalized
  }

  function toOptionalDecimal(value?: number | null, precision = 2) {
    if (value === undefined || value === null) {
      return undefined
    }
    const numericValue = Number(value)
    if (!Number.isFinite(numericValue) || numericValue === 0) {
      return undefined
    }
    return Number(numericValue.toFixed(precision))
  }

  return {
    resolveErrorMessage,
    toOptionalString,
    toOptionalMoney,
    toOptionalDecimal
  }
}
