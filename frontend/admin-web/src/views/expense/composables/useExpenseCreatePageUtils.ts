import { normalizeMoneyValue } from '@/utils/money'

type TemplateCategoryGroup = {
  categoryCode: string
  categoryName: string
}

export function useExpenseCreatePageUtils(categoryGroups: TemplateCategoryGroup[]) {
  function detailTypeLabel(detailType?: string) {
    return detailType === 'ENTERPRISE_TRANSACTION' ? '企业往来' : '普通报销'
  }

  function enterpriseModeLabel(mode?: string) {
    if (mode === 'INVOICE_FULL_PAYMENT') {
      return '到票全部支付'
    }
    if (mode === 'PREPAY_UNBILLED') {
      return '预付未到票'
    }
    return ''
  }

  function cloneRecord(value: Record<string, unknown>) {
    return JSON.parse(JSON.stringify(value || {})) as Record<string, unknown>
  }

  function cloneValue<T>(value: T): T {
    return JSON.parse(JSON.stringify(value)) as T
  }

  function isKnownTemplateCategory(categoryCode?: string) {
    return categoryGroups.some((item) => item.categoryCode === categoryCode)
  }

  function isRecord(value: unknown): value is Record<string, unknown> {
    return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
  }

  function safeMoneyValue(value: unknown) {
    try {
      return normalizeMoneyValue(value as string | number | null | undefined, {
        allowNegative: true,
        fallback: '0.00'
      })
    } catch {
      return '0.00'
    }
  }

  function resolveErrorMessage(error: unknown, fallback: string) {
    return error instanceof Error && error.message ? error.message : fallback
  }

  return {
    detailTypeLabel,
    enterpriseModeLabel,
    cloneRecord,
    cloneValue,
    isKnownTemplateCategory,
    isRecord,
    safeMoneyValue,
    resolveErrorMessage
  }
}
