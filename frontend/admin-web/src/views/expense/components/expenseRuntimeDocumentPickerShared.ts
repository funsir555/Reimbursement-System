import type {
  ExpenseDocumentPickerGroup,
  ExpenseDocumentPickerItem,
  ExpenseRelatedDocumentValue,
  ExpenseWriteOffDocumentValue
} from '@/api'
import { compareMoney, formatMoney, normalizeMoneyValue, subtractMoney } from '@/utils/money'

export type DocumentRelationType = 'RELATED' | 'WRITEOFF'

export type RuntimeDocumentRecord = ExpenseRelatedDocumentValue &
  Partial<ExpenseWriteOffDocumentValue>

export function createDocumentPickerDialogState() {
  return {
    visible: false,
    fieldKey: '',
    relationType: 'RELATED' as DocumentRelationType,
    keyword: '',
    loading: false,
    groups: [] as ExpenseDocumentPickerGroup[],
    activeTemplateType: '',
    selectedCodes: [] as string[],
    itemsByCode: {} as Record<string, RuntimeDocumentRecord>
  }
}

export function isRelatedDocumentBusinessCode(code: string) {
  return code === 'related-document'
}

export function isWriteOffDocumentBusinessCode(code: string) {
  return code === 'writeoff-document'
}

export function isDocumentBusinessCode(code: string) {
  return isRelatedDocumentBusinessCode(code) || isWriteOffDocumentBusinessCode(code)
}

export function documentRelationTypeFromBusinessCode(code: string): DocumentRelationType {
  return isWriteOffDocumentBusinessCode(code) ? 'WRITEOFF' : 'RELATED'
}

export function documentBlockHintFromBusinessCode(code: string) {
  return isWriteOffDocumentBusinessCode(code)
    ? '支持点击页签切换报销单与借款单，选中后逐条填写本次核销金额。'
    : '支持点击页签切换报销单、申请单、合同单与借款单，并同时关联多张已审批通过的单据。'
}

export function resolveDocumentPickerActiveTemplateType(
  groups: ExpenseDocumentPickerGroup[],
  activeTemplateType: string,
  selectedCodes: string[]
) {
  if (!groups.length) {
    return ''
  }
  if (groups.some((group) => group.templateType === activeTemplateType)) {
    return activeTemplateType
  }
  const selectedGroup = groups.find((group) =>
    group.items.some((item) => selectedCodes.includes(item.documentCode))
  )
  return selectedGroup?.templateType || groups[0]?.templateType || ''
}

export function normalizeRelatedDocumentValues(value: unknown): RuntimeDocumentRecord[] {
  const records = normalizeDocumentValueList(value)
  return records
    .map((item) => ({
      documentCode: firstNonBlank(item.documentCode, item.value) || '',
      documentTitle: firstNonBlank(item.documentTitle, item.label),
      templateType: firstNonBlank(item.templateType),
      templateTypeLabel: firstNonBlank(item.templateTypeLabel),
      templateName: firstNonBlank(item.templateName),
      status: firstNonBlank(item.status),
      statusLabel: firstNonBlank(item.statusLabel)
    }))
    .filter((item) => Boolean(item.documentCode))
}

export function normalizeWriteOffDocumentValues(value: unknown): RuntimeDocumentRecord[] {
  const records = normalizeDocumentValueList(value)
  return records
    .map((item) => {
      const availableWriteOffAmount = toOptionalMoney(item.availableWriteOffAmount)
      const writeOffAmount = toOptionalMoney(item.writeOffAmount)
      return {
        documentCode: firstNonBlank(item.documentCode, item.value) || '',
        documentTitle: firstNonBlank(item.documentTitle, item.label),
        templateType: firstNonBlank(item.templateType),
        templateTypeLabel: firstNonBlank(item.templateTypeLabel),
        templateName: firstNonBlank(item.templateName),
        status: firstNonBlank(item.status),
        statusLabel: firstNonBlank(item.statusLabel),
        writeOffSourceKind: firstNonBlank(item.writeOffSourceKind),
        availableWriteOffAmount,
        writeOffAmount,
        remainingAmount:
          toOptionalMoney(item.remainingAmount) ??
          (!availableWriteOffAmount || !writeOffAmount
            ? undefined
            : compareMoney(availableWriteOffAmount, writeOffAmount) >= 0
              ? subtractMoney(availableWriteOffAmount, writeOffAmount)
              : '0.00')
      }
    })
    .filter((item) => Boolean(item.documentCode))
}

export function toDocumentRecord(item: ExpenseDocumentPickerItem): RuntimeDocumentRecord {
  return {
    documentCode: item.documentCode,
    documentTitle: item.documentTitle,
    templateType: item.templateType,
    templateTypeLabel: item.templateTypeLabel,
    templateName: item.templateName,
    status: item.status,
    statusLabel: item.statusLabel,
    writeOffSourceKind: item.writeOffSourceKind,
    availableWriteOffAmount: item.availableWriteOffAmount
  }
}

export function mergeDocumentRecord(
  current: RuntimeDocumentRecord | undefined,
  next: RuntimeDocumentRecord
) {
  if (!current) {
    return cloneDocumentRecord(next)
  }
  return {
    ...current,
    ...next,
    writeOffAmount: current.writeOffAmount ?? next.writeOffAmount,
    remainingAmount: current.remainingAmount ?? next.remainingAmount
  }
}

export function cloneDocumentRecord(item: RuntimeDocumentRecord): RuntimeDocumentRecord {
  return { ...item }
}

export function toRelatedDocumentValue(item: RuntimeDocumentRecord): ExpenseRelatedDocumentValue {
  return {
    documentCode: item.documentCode,
    documentTitle: item.documentTitle,
    templateType: item.templateType,
    templateTypeLabel: item.templateTypeLabel,
    templateName: item.templateName,
    status: item.status,
    statusLabel: item.statusLabel
  }
}

export function toWriteOffDocumentValue(item: RuntimeDocumentRecord): ExpenseWriteOffDocumentValue {
  return {
    ...toRelatedDocumentValue(item),
    writeOffSourceKind: item.writeOffSourceKind,
    availableWriteOffAmount: item.availableWriteOffAmount,
    writeOffAmount: item.writeOffAmount,
    remainingAmount: item.remainingAmount
  }
}

export function resolveTemplateTypeLabel(templateType?: string) {
  if (templateType === 'application') return '申请单'
  if (templateType === 'contract') return '合同单'
  if (templateType === 'loan') return '借款单'
  return '报销单'
}

export function writeOffSourceKindLabel(value?: string) {
  if (value === 'LOAN') return '借款单'
  if (value === 'PREPAY_REPORT') return '预付报销单'
  return '待识别'
}

export function formatDocumentAmount(value: unknown) {
  const amount = toOptionalMoney(value)
  if (amount === undefined) {
    return '--'
  }
  return formatMoney(amount)
}

function normalizeDocumentValueList(value: unknown): Record<string, unknown>[] {
  if (Array.isArray(value)) {
    return value.flatMap((item) => normalizeDocumentValueList(item))
  }
  if (isRecord(value)) {
    return [value]
  }
  return []
}

function firstNonBlank(...values: unknown[]) {
  for (const value of values) {
    if (value === null || value === undefined) {
      continue
    }
    const text = String(value).trim()
    if (text) {
      return text
    }
  }
  return ''
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return !!value && typeof value === 'object'
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
