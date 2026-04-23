import type {
  ExpenseDetailInstance,
  ProcessFormDesignBlock,
  ProcessFormDesignSchema
} from '@/api'
import { centsToMoney, normalizeMoneyValue, toMoneyCents, type MoneyInputValue } from '@/utils/money'
import {
  BUSINESS_SCENARIO_MODE_FULL,
  BUSINESS_SCENARIO_MODE_PREPAY,
  getControlType,
  isBusinessScenarioBlock,
  normalizeBusinessScenarioEnabledModes
} from '@/views/process/formDesignerHelper'

export const DETAIL_TYPE_NORMAL = 'NORMAL_REIMBURSEMENT'
export const DETAIL_TYPE_ENTERPRISE = 'ENTERPRISE_TRANSACTION'

export const MODE_PREPAY_UNBILLED = BUSINESS_SCENARIO_MODE_PREPAY
export const MODE_INVOICE_FULL_PAYMENT = BUSINESS_SCENARIO_MODE_FULL

export const FIELD_EXPENSE_TYPE_CODE = 'expenseTypeCode'
export const FIELD_BUSINESS_SCENARIO = 'businessScenario'
export const FIELD_DETAIL_AMOUNT = 'amount'
export const FIELD_INVOICE_AMOUNT = 'invoiceAmount'
export const FIELD_ACTUAL_PAYMENT_AMOUNT = 'actualPaymentAmount'
export const FIELD_INVOICE_ATTACHMENTS = 'invoiceAttachments'
export const FIELD_PENDING_WRITE_OFF_AMOUNT = 'pendingWriteOffAmount'

export function buildExpenseDetailFormData(
  schema: ProcessFormDesignSchema | null | undefined,
  detailType?: string,
  currentFormData?: Record<string, unknown> | null,
  defaultBusinessScenario?: string
) {
  const next = buildSchemaDefaultValues(schema)
  const current = cloneRecord(currentFormData)

  Object.entries(current).forEach(([key, value]) => {
    next[key] = cloneValue(value)
  })

  const resolvedScenario = resolveBusinessScenario(next, detailType, defaultBusinessScenario, schema)
  next[FIELD_BUSINESS_SCENARIO] = resolvedScenario || ''
  delete next[FIELD_PENDING_WRITE_OFF_AMOUNT]

  return next
}

export function enrichExpenseDetailInstance(
  detail: ExpenseDetailInstance,
  defaultBusinessScenario?: string,
  schema?: ProcessFormDesignSchema | null
): ExpenseDetailInstance {
  const nextFormData = cloneRecord(detail.formData)
  const detailType = String(detail.detailType || '')
  const businessSceneMode = resolveBusinessScenario(nextFormData, detailType, defaultBusinessScenario, schema)
  const enterpriseMode = detailType === DETAIL_TYPE_ENTERPRISE ? businessSceneMode : ''

  nextFormData[FIELD_BUSINESS_SCENARIO] = businessSceneMode || ''
  delete nextFormData[FIELD_PENDING_WRITE_OFF_AMOUNT]

  return {
    ...detail,
    enterpriseMode,
    expenseTypeCode: trimToUndefined(nextFormData[FIELD_EXPENSE_TYPE_CODE]),
    businessSceneMode,
    formData: nextFormData
  }
}

export function ensureExpenseDetailFormDefaults(
  formData: Record<string, unknown>,
  schema: ProcessFormDesignSchema | null | undefined,
  detailType?: string,
  defaultBusinessScenario?: string
) {
  const defaults = buildExpenseDetailFormData(schema, detailType, formData, defaultBusinessScenario)
  let changed = false

  Object.entries(defaults).forEach(([key, value]) => {
    const currentValue = formData[key]
    if (key === FIELD_BUSINESS_SCENARIO) {
      if (currentValue !== value) {
        formData[key] = cloneValue(value)
        changed = true
      }
      return
    }
    if (!(key in formData) || formData[key] === undefined || formData[key] === null) {
      formData[key] = cloneValue(value)
      changed = true
    }
  })

  if (FIELD_PENDING_WRITE_OFF_AMOUNT in formData) {
    delete formData[FIELD_PENDING_WRITE_OFF_AMOUNT]
    changed = true
  }

  return changed
}

export function isExpenseDetailBlockVisible(
  block: ProcessFormDesignBlock,
  formData: Record<string, unknown>,
  detailType?: string,
  defaultBusinessScenario?: string,
  schema?: ProcessFormDesignSchema | null
) {
  const visibleSceneModes = Array.isArray(block.props.visibleSceneModes)
    ? block.props.visibleSceneModes.map((item) => String(item))
    : []

  if (visibleSceneModes.length === 0) {
    return true
  }

  const businessScenario = resolveBusinessScenario(formData, detailType, defaultBusinessScenario, schema)
  return businessScenario ? visibleSceneModes.includes(businessScenario) : false
}

export function isExpenseDetailBlockReadOnly(block: ProcessFormDesignBlock) {
  return Boolean(block.props.readOnly)
}

export function resolveBusinessScenario(
  formData: Record<string, unknown> | null | undefined,
  detailType?: string,
  defaultBusinessScenario?: string,
  schema?: ProcessFormDesignSchema | null
) {
  const normalizedDetailType = detailType === DETAIL_TYPE_ENTERPRISE ? DETAIL_TYPE_ENTERPRISE : DETAIL_TYPE_NORMAL
  if (normalizedDetailType === DETAIL_TYPE_NORMAL) {
    return MODE_INVOICE_FULL_PAYMENT
  }

  const enabledModes = resolveEnabledBusinessScenarioModes(schema, normalizedDetailType)
  const rawValue = trimToUndefined(formData?.[FIELD_BUSINESS_SCENARIO])
  if (rawValue && enabledModes.includes(rawValue)) {
    return rawValue
  }

  const schemaDefaultValue = resolveSchemaDefaultBusinessScenario(schema, enabledModes)
  if (schemaDefaultValue) {
    return schemaDefaultValue
  }

  const fallbackValue = trimToUndefined(defaultBusinessScenario)
  if (fallbackValue && enabledModes.includes(fallbackValue)) {
    return fallbackValue
  }

  return ''
}

export function resolveExpenseDetailAmount(
  formData: Record<string, unknown> | null | undefined,
  detailType?: string,
  defaultBusinessScenario?: string,
  schema?: ProcessFormDesignSchema | null
) {
  const detail = formData || {}
  const businessScenario = resolveBusinessScenario(detail, detailType, defaultBusinessScenario, schema)
  if (detailType === DETAIL_TYPE_ENTERPRISE && !businessScenario) {
    return ''
  }
  const actualPaymentAmount = safeMoneyValue(detail[FIELD_ACTUAL_PAYMENT_AMOUNT])
  if (actualPaymentAmount) {
    return actualPaymentAmount
  }
  const detailAmount = safeMoneyValue(detail[FIELD_DETAIL_AMOUNT])
  const invoiceAmount = safeMoneyValue(detail[FIELD_INVOICE_AMOUNT])
  if (businessScenario === MODE_PREPAY_UNBILLED) {
    return detailAmount || invoiceAmount
  }
  if (detailType === DETAIL_TYPE_ENTERPRISE) {
    return invoiceAmount || detailAmount
  }
  return detailAmount || invoiceAmount
}

export function sumExpenseDetailAmounts(
  details: Array<Pick<ExpenseDetailInstance, 'detailType' | 'businessSceneMode' | 'enterpriseMode' | 'formData'>> | null | undefined,
  defaultBusinessScenario?: string,
  schema?: ProcessFormDesignSchema | null
) {
  if (!Array.isArray(details) || details.length === 0) {
    return ''
  }
  let total = 0n
  let resolved = false
  details.forEach((detail) => {
    const amount = resolveExpenseDetailAmount(
      isRecord(detail.formData) ? detail.formData : {},
      String(detail.detailType || ''),
      trimToUndefined(detail.businessSceneMode) || trimToUndefined(detail.enterpriseMode) || defaultBusinessScenario,
      schema
    )
    if (!amount) {
      return
    }
    total += toMoneyCents(amount)
    resolved = true
  })
  return resolved ? centsToMoney(total) : ''
}

export function sumMainFormAmounts(
  blocks: ProcessFormDesignBlock[] | null | undefined,
  formData: Record<string, unknown> | null | undefined
) {
  if (!Array.isArray(blocks) || blocks.length === 0) {
    return ''
  }
  let total = 0n
  let resolved = false
  blocks
    .filter((block) => block.kind === 'CONTROL' && getControlType(block) === 'AMOUNT')
    .forEach((block) => {
      const amount = safeMoneyValue(formData?.[block.fieldKey])
      if (!amount) {
        return
      }
      total += toMoneyCents(amount)
      resolved = true
    })
  return resolved ? centsToMoney(total) : ''
}

export function resolveDocumentTotalAmount(
  blocks: ProcessFormDesignBlock[] | null | undefined,
  formData: Record<string, unknown> | null | undefined,
  details: Array<Pick<ExpenseDetailInstance, 'detailType' | 'businessSceneMode' | 'enterpriseMode' | 'formData'>> | null | undefined,
  defaultBusinessScenario?: string,
  expenseDetailSchema?: ProcessFormDesignSchema | null
) {
  const detailAmount = sumExpenseDetailAmounts(details, defaultBusinessScenario, expenseDetailSchema)
  if (detailAmount) {
    return detailAmount
  }
  return sumMainFormAmounts(blocks, formData) || safeMoneyValue(formData?.__totalAmount) || '0.00'
}

export function resolveInvoiceOcrTotal(attachments: unknown) {
  const normalizedAttachments = Array.isArray(attachments) ? attachments : []
  let total = 0n
  let resolved = false
  normalizedAttachments.forEach((item) => {
    if (!item || typeof item !== 'object' || Array.isArray(item)) {
      return
    }
    const ocr = (item as { ocr?: { totalAmount?: unknown } }).ocr
    const amount = safeMoneyValue(ocr?.totalAmount)
    if (!amount) {
      return
    }
    total += toMoneyCents(amount)
    resolved = true
  })
  return resolved ? centsToMoney(total) : ''
}

export function syncInvoiceAmountWithOcr(
  formData: Record<string, unknown>,
  previousOcrTotal: MoneyInputValue,
  nextOcrTotal: MoneyInputValue
) {
  const previous = safeMoneyValue(previousOcrTotal)
  const next = safeMoneyValue(nextOcrTotal)
  const current = safeMoneyValue(formData[FIELD_INVOICE_AMOUNT])
  if (!next) {
    if (!current || current === previous) {
      formData[FIELD_INVOICE_AMOUNT] = ''
      return true
    }
    return false
  }
  if (!current || current === previous) {
    formData[FIELD_INVOICE_AMOUNT] = next
    return true
  }
  return false
}

function resolveEnabledBusinessScenarioModes(
  schema: ProcessFormDesignSchema | null | undefined,
  detailType?: string
) {
  if (detailType !== DETAIL_TYPE_ENTERPRISE) {
    return [MODE_INVOICE_FULL_PAYMENT]
  }
  const scenarioBlock = Array.isArray(schema?.blocks)
    ? schema?.blocks.find((block) => isBusinessScenarioBlock(block))
    : undefined
  return normalizeBusinessScenarioEnabledModes(
    scenarioBlock?.props?.enabledSceneModes,
    detailType,
    scenarioBlock?.props?.options
  )
}

function resolveSchemaDefaultBusinessScenario(
  schema: ProcessFormDesignSchema | null | undefined,
  enabledModes: string[]
) {
  const scenarioBlock = Array.isArray(schema?.blocks)
    ? schema?.blocks.find((block) => isBusinessScenarioBlock(block))
    : undefined
  const defaultValue = trimToUndefined(scenarioBlock?.defaultValue)
  return defaultValue && enabledModes.includes(defaultValue) ? defaultValue : ''
}

function buildSchemaDefaultValues(schema: ProcessFormDesignSchema | null | undefined) {
  const result: Record<string, unknown> = {}
  const blocks = Array.isArray(schema?.blocks) ? schema.blocks : []

  blocks.forEach((block) => {
    if (!block?.fieldKey) {
      return
    }

    if (block.defaultValue !== undefined) {
      result[block.fieldKey] = cloneValue(block.defaultValue)
      return
    }

    const controlType = getControlType(block)
    if (['MULTI_SELECT', 'CHECKBOX', 'DATE_RANGE', 'ATTACHMENT', 'IMAGE'].includes(controlType)) {
      result[block.fieldKey] = []
      return
    }

    if (controlType === 'SWITCH') {
      result[block.fieldKey] = false
      return
    }

    result[block.fieldKey] = ''
  })

  return result
}

function cloneRecord(value: Record<string, unknown> | null | undefined) {
  const next: Record<string, unknown> = {}
  if (!value) {
    return next
  }
  Object.entries(value).forEach(([key, item]) => {
    next[key] = cloneValue(item)
  })
  return next
}

function cloneValue<T>(value: T): T {
  if (Array.isArray(value)) {
    return value.map((item) => cloneValue(item)) as T
  }
  if (value && typeof value === 'object') {
    return Object.fromEntries(
      Object.entries(value as Record<string, unknown>).map(([key, item]) => [key, cloneValue(item)])
    ) as T
  }
  return value
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}

function safeMoneyValue(value: unknown) {
  try {
    return normalizeMoneyValue(value as MoneyInputValue, { fallback: '' })
  } catch {
    return ''
  }
}

function trimToUndefined(value: unknown) {
  if (typeof value !== 'string') {
    return value === null || value === undefined ? undefined : String(value)
  }
  const trimmed = value.trim()
  return trimmed ? trimmed : undefined
}
