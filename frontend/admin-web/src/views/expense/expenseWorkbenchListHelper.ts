import type { ExpenseApprovalPendingItem, ExpensePaymentOrder, ExpenseSummary } from '@/api'

export type ExpenseWorkbenchRow = ExpenseSummary | ExpenseApprovalPendingItem | ExpensePaymentOrder

export type ExpenseWorkbenchColumnKey =
  | 'documentCode'
  | 'documentTitle'
  | 'documentReason'
  | 'templateName'
  | 'templateTypeLabel'
  | 'submitterName'
  | 'submitterDeptName'
  | 'currentNodeName'
  | 'documentStatusLabel'
  | 'amount'
  | 'submittedAt'
  | 'paymentDate'
  | 'paymentCompanyName'
  | 'payeeName'
  | 'counterpartyName'
  | 'undertakeDepartmentNames'
  | 'tagNames'
  | 'taskCreatedAt'

export interface ExpenseWorkbenchColumnDefinition {
  key: ExpenseWorkbenchColumnKey
  label: string
  width?: number
  minWidth?: number
}

export interface ExpenseWorkbenchFilters {
  documentCode: string
  submitterName: string
  templateName: string
  documentStatusLabel: string
  submittedDateRange: string[]
  paymentDateRange: string[]
  paymentCompanyName: string
  payeeName: string
  counterpartyName: string
  submitterDeptName: string
  undertakeDepartmentName: string
  tagName: string
}

export type ExpenseWorkbenchPageKey = 'list' | 'approval' | 'documents' | 'paymentOrders'

export type ExpenseWorkbenchColumnWidthMap = Partial<Record<ExpenseWorkbenchColumnKey, number>>

export const EXPENSE_WORKBENCH_COLUMNS: ExpenseWorkbenchColumnDefinition[] = [
  { key: 'documentCode', label: '单据编号', width: 180 },
  { key: 'documentTitle', label: '单据标题', minWidth: 200 },
  { key: 'documentReason', label: '事由', minWidth: 180 },
  { key: 'templateName', label: '模板名称', minWidth: 160 },
  { key: 'templateTypeLabel', label: '模板类型', width: 120 },
  { key: 'submitterName', label: '提单人', width: 120 },
  { key: 'submitterDeptName', label: '提单人部门', minWidth: 160 },
  { key: 'currentNodeName', label: '当前节点', minWidth: 150 },
  { key: 'documentStatusLabel', label: '状态', width: 110 },
  { key: 'amount', label: '金额', width: 140 },
  { key: 'submittedAt', label: '提交日期', width: 168 },
  { key: 'paymentDate', label: '支付日期', width: 130 },
  { key: 'paymentCompanyName', label: '付款公司', minWidth: 180 },
  { key: 'payeeName', label: '收款人', minWidth: 140 },
  { key: 'counterpartyName', label: '往来单位', minWidth: 180 },
  { key: 'undertakeDepartmentNames', label: '承担部门', minWidth: 180 },
  { key: 'tagNames', label: '标签', minWidth: 160 },
  { key: 'taskCreatedAt', label: '待办到达时间', width: 168 }
]

export const EXPENSE_WORKBENCH_STATUS_OPTIONS = ['草稿', '审批中', '已通过', '已驳回', '流程异常', '待支付', '支付中', '支付完成', '支付异常']

export const EXPENSE_WORKBENCH_DEFAULT_COLUMNS: Record<ExpenseWorkbenchPageKey, ExpenseWorkbenchColumnKey[]> = {
  list: ['documentCode', 'documentTitle', 'templateName', 'documentStatusLabel', 'currentNodeName', 'submittedAt', 'amount'],
  approval: ['documentCode', 'documentTitle', 'submitterName', 'templateName', 'currentNodeName', 'documentStatusLabel', 'submittedAt', 'amount'],
  documents: ['documentCode', 'documentTitle', 'submitterName', 'templateName', 'documentStatusLabel', 'currentNodeName', 'submittedAt', 'amount'],
  paymentOrders: ['documentCode', 'documentTitle', 'submitterName', 'templateName', 'documentStatusLabel', 'currentNodeName', 'taskCreatedAt', 'amount']
}

export const EXPENSE_WORKBENCH_STORAGE_KEYS: Record<ExpenseWorkbenchPageKey, string> = {
  list: 'expense:list:visible-columns',
  approval: 'expense:approval:visible-columns',
  documents: 'expense:documents:visible-columns',
  paymentOrders: 'expense:payment:orders:visible-columns'
}

export const EXPENSE_WORKBENCH_COLUMN_ORDER_STORAGE_KEYS: Record<ExpenseWorkbenchPageKey, string> = {
  list: 'expense:list:column-order',
  approval: 'expense:approval:column-order',
  documents: 'expense:documents:column-order',
  paymentOrders: 'expense:payment:orders:column-order'
}

export const EXPENSE_WORKBENCH_COLUMN_WIDTHS_STORAGE_KEY = 'expense:workbench:column-widths'

export const EXPENSE_WORKBENCH_DEFAULT_COLUMN_ORDER: ExpenseWorkbenchColumnKey[] = EXPENSE_WORKBENCH_COLUMNS.map((column) => column.key)

export function createExpenseWorkbenchFilters(): ExpenseWorkbenchFilters {
  return {
    documentCode: '',
    submitterName: '',
    templateName: '',
    documentStatusLabel: '',
    submittedDateRange: [],
    paymentDateRange: [],
    paymentCompanyName: '',
    payeeName: '',
    counterpartyName: '',
    submitterDeptName: '',
    undertakeDepartmentName: '',
    tagName: ''
  }
}

export function loadVisibleColumns(
  storageKey: string,
  defaults: ExpenseWorkbenchColumnKey[],
  allowedColumns: ExpenseWorkbenchColumnKey[]
) {
  if (typeof window === 'undefined') {
    return [...defaults]
  }
  try {
    const raw = window.localStorage.getItem(storageKey)
    if (!raw) {
      return [...defaults]
    }
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      return [...defaults]
    }
    const allowed = new Set(allowedColumns)
    const filtered = parsed.filter(
      (item): item is ExpenseWorkbenchColumnKey => typeof item === 'string' && allowed.has(item as ExpenseWorkbenchColumnKey)
    )
    return filtered.length ? filtered : [...defaults]
  } catch {
    return [...defaults]
  }
}

export function saveVisibleColumns(storageKey: string, columns: ExpenseWorkbenchColumnKey[]) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(storageKey, JSON.stringify(columns))
}

export function loadColumnOrder(
  storageKey: string,
  defaults: ExpenseWorkbenchColumnKey[],
  allowedColumns: ExpenseWorkbenchColumnKey[]
) {
  const fallback = defaults.filter((key) => allowedColumns.includes(key))
  if (typeof window === 'undefined') {
    return [...fallback]
  }
  try {
    const raw = window.localStorage.getItem(storageKey)
    if (!raw) {
      return [...fallback]
    }
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      return [...fallback]
    }
    const allowed = new Set(allowedColumns)
    const deduped: ExpenseWorkbenchColumnKey[] = []
    parsed.forEach((item) => {
      if (typeof item !== 'string') {
        return
      }
      const key = item as ExpenseWorkbenchColumnKey
      if (allowed.has(key) && !deduped.includes(key)) {
        deduped.push(key)
      }
    })
    fallback.forEach((key) => {
      if (!deduped.includes(key)) {
        deduped.push(key)
      }
    })
    return deduped.length ? deduped : [...fallback]
  } catch {
    return [...fallback]
  }
}

export function saveColumnOrder(storageKey: string, columnOrder: ExpenseWorkbenchColumnKey[]) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(storageKey, JSON.stringify(columnOrder))
}

export function loadColumnWidths(allowedColumns: ExpenseWorkbenchColumnKey[]) {
  if (typeof window === 'undefined') {
    return {} as ExpenseWorkbenchColumnWidthMap
  }
  try {
    const raw = window.localStorage.getItem(EXPENSE_WORKBENCH_COLUMN_WIDTHS_STORAGE_KEY)
    if (!raw) {
      return {} as ExpenseWorkbenchColumnWidthMap
    }
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return {} as ExpenseWorkbenchColumnWidthMap
    }
    const allowed = new Set(allowedColumns)
    return Object.entries(parsed).reduce<ExpenseWorkbenchColumnWidthMap>((result, [key, value]) => {
      if (!allowed.has(key as ExpenseWorkbenchColumnKey)) {
        return result
      }
      const width = Number(value)
      if (Number.isFinite(width) && width > 0) {
        result[key as ExpenseWorkbenchColumnKey] = width
      }
      return result
    }, {})
  } catch {
    return {} as ExpenseWorkbenchColumnWidthMap
  }
}

export function saveColumnWidth(
  columnKey: ExpenseWorkbenchColumnKey,
  width: number,
  currentWidths: ExpenseWorkbenchColumnWidthMap
) {
  const normalizedWidth = Math.round(width)
  if (!Number.isFinite(normalizedWidth) || normalizedWidth <= 0) {
    return currentWidths
  }
  const nextWidths = {
    ...currentWidths,
    [columnKey]: normalizedWidth
  }
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(EXPENSE_WORKBENCH_COLUMN_WIDTHS_STORAGE_KEY, JSON.stringify(nextWidths))
  }
  return nextWidths
}

export function resolveOrderedColumnDefinitions(keys: ExpenseWorkbenchColumnKey[]) {
  return keys
    .map((key) => EXPENSE_WORKBENCH_COLUMNS.find((column) => column.key === key))
    .filter((column): column is ExpenseWorkbenchColumnDefinition => Boolean(column))
}

export function resolveVisibleColumnDefinitions(
  visibleKeys: ExpenseWorkbenchColumnKey[],
  columnOrder: ExpenseWorkbenchColumnKey[] = EXPENSE_WORKBENCH_DEFAULT_COLUMN_ORDER,
  columnWidths: ExpenseWorkbenchColumnWidthMap = {}
) {
  const visibleSet = new Set(visibleKeys)
  return resolveOrderedColumnDefinitions(columnOrder)
    .filter((column) => visibleSet.has(column.key))
    .map((column) => ({
      ...column,
      width: resolveColumnWidth(column, columnWidths)
    }))
}

export function sortVisibleColumnsByOrder(
  visibleKeys: ExpenseWorkbenchColumnKey[],
  columnOrder: ExpenseWorkbenchColumnKey[]
) {
  const visibleSet = new Set(visibleKeys)
  return columnOrder.filter((key) => visibleSet.has(key))
}

export function moveColumnOrder(
  columnOrder: ExpenseWorkbenchColumnKey[],
  draggedKey: ExpenseWorkbenchColumnKey,
  targetKey: ExpenseWorkbenchColumnKey
) {
  if (draggedKey === targetKey) {
    return [...columnOrder]
  }
  const sourceIndex = columnOrder.indexOf(draggedKey)
  const targetIndex = columnOrder.indexOf(targetKey)
  if (sourceIndex < 0 || targetIndex < 0) {
    return [...columnOrder]
  }
  const nextOrder = columnOrder.filter((key) => key !== draggedKey)
  const nextTargetIndex = nextOrder.indexOf(targetKey)
  nextOrder.splice(nextTargetIndex, 0, draggedKey)
  return nextOrder
}

export function buildColumnGridDisplayOrder<T>(items: T[], columnCount = 2) {
  if (columnCount <= 1 || items.length <= 1) {
    return [...items]
  }
  const rows = Math.ceil(items.length / columnCount)
  const arranged: T[] = []
  for (let rowIndex = 0; rowIndex < rows; rowIndex += 1) {
    for (let columnIndex = 0; columnIndex < columnCount; columnIndex += 1) {
      const itemIndex = columnIndex * rows + rowIndex
      if (itemIndex < items.length) {
        arranged.push(items[itemIndex] as T)
      }
    }
  }
  return arranged
}

export function filterExpenseWorkbenchRows<T extends ExpenseWorkbenchRow>(
  rows: T[],
  filters: ExpenseWorkbenchFilters
) {
  return rows.filter((row) => {
    if (!matchesText(resolveDocumentCode(row), filters.documentCode)) {
      return false
    }
    if (!matchesText(row.submitterName, filters.submitterName)) {
      return false
    }
    if (!matchesText(row.templateName, filters.templateName)) {
      return false
    }
    if (filters.documentStatusLabel && resolveDocumentStatusLabel(row) !== filters.documentStatusLabel) {
      return false
    }
    if (!matchesDateRange(resolveSubmittedDate(row), filters.submittedDateRange)) {
      return false
    }
    if (!matchesDateRange(row.paymentDate, filters.paymentDateRange)) {
      return false
    }
    if (!matchesText(row.paymentCompanyName, filters.paymentCompanyName)) {
      return false
    }
    if (!matchesText(getOptionalStringField(row, 'payeeName'), filters.payeeName)) {
      return false
    }
    if (!matchesText(getOptionalStringField(row, 'counterpartyName'), filters.counterpartyName)) {
      return false
    }
    if (!matchesText(row.submitterDeptName, filters.submitterDeptName)) {
      return false
    }
    if (!matchesArrayText(getOptionalStringArrayField(row, 'undertakeDepartmentNames'), filters.undertakeDepartmentName)) {
      return false
    }
    if (!matchesArrayText(getOptionalStringArrayField(row, 'tagNames'), filters.tagName)) {
      return false
    }
    return true
  })
}

export function formatMultiValue(values?: string[]) {
  return Array.isArray(values) && values.length ? values.join('、') : '-'
}

export function resolveDocumentCode(row: ExpenseWorkbenchRow) {
  return row.documentCode || ('no' in row ? row.no : '') || ''
}

export function resolveDocumentReason(row: ExpenseWorkbenchRow) {
  return getOptionalStringField(row, 'documentReason') || ('reason' in row ? row.reason : '') || ''
}

export function resolveCurrentNodeName(row: ExpenseWorkbenchRow) {
  return ('currentNodeName' in row ? row.currentNodeName : undefined) || ('nodeName' in row ? row.nodeName : undefined) || ''
}

export function resolveDocumentStatusLabel(row: ExpenseWorkbenchRow) {
  return row.documentStatusLabel || getOptionalStringField(row, 'status') || ''
}

export function getExpenseWorkbenchStatusType(status: string) {
  const map: Record<string, string> = {
    审批中: 'warning',
    待支付: 'warning',
    支付中: 'warning',
    已通过: 'success',
    支付完成: 'success',
    已驳回: 'danger',
    支付异常: 'danger',
    流程异常: 'info',
    草稿: 'info'
  }
  return map[status] || 'info'
}

export function isExpenseWorkbenchPendingLikeStatus(status: string) {
  return ['审批中', '待支付', '支付中'].includes(status)
}

export function isExpenseWorkbenchCompletedLikeStatus(status: string) {
  return ['已通过', '支付完成'].includes(status)
}

export function isExpenseWorkbenchExceptionLikeStatus(status: string) {
  return ['流程异常', '支付异常'].includes(status)
}

export function resolveSubmittedDate(row: ExpenseWorkbenchRow) {
  return (row.submittedAt || ('date' in row ? row.date : '') || '').slice(0, 10)
}

export function resolveColumnText(row: ExpenseWorkbenchRow, key: ExpenseWorkbenchColumnKey) {
  switch (key) {
    case 'documentCode':
      return resolveDocumentCode(row)
    case 'documentTitle':
      return row.documentTitle || '-'
    case 'documentReason':
      return resolveDocumentReason(row) || '-'
    case 'templateName':
      return row.templateName || '-'
    case 'templateTypeLabel':
      return row.templateTypeLabel || '-'
    case 'submitterName':
      return row.submitterName || '-'
    case 'submitterDeptName':
      return row.submitterDeptName || '-'
    case 'currentNodeName':
      return resolveCurrentNodeName(row) || '-'
    case 'documentStatusLabel':
      return resolveDocumentStatusLabel(row) || '-'
    case 'amount':
      return Number(row.amount || 0)
    case 'submittedAt':
      return row.submittedAt || ('date' in row ? row.date : '') || '-'
    case 'paymentDate':
      return row.paymentDate || '-'
    case 'paymentCompanyName':
      return row.paymentCompanyName || '-'
    case 'payeeName':
      return getOptionalStringField(row, 'payeeName') || '-'
    case 'counterpartyName':
      return getOptionalStringField(row, 'counterpartyName') || '-'
    case 'undertakeDepartmentNames':
      return formatMultiValue(getOptionalStringArrayField(row, 'undertakeDepartmentNames'))
    case 'tagNames':
      return formatMultiValue(getOptionalStringArrayField(row, 'tagNames'))
    case 'taskCreatedAt':
      return 'taskCreatedAt' in row ? row.taskCreatedAt || '-' : '-'
    default:
      return '-'
  }
}

function getOptionalStringField(row: ExpenseWorkbenchRow, key: string) {
  const value = (row as unknown as Record<string, unknown>)[key]
  return typeof value === 'string' ? value : undefined
}

function getOptionalStringArrayField(row: ExpenseWorkbenchRow, key: string) {
  const value = (row as unknown as Record<string, unknown>)[key]
  return Array.isArray(value) && value.every((item) => typeof item === 'string') ? value : undefined
}

function resolveColumnWidth(
  column: ExpenseWorkbenchColumnDefinition,
  columnWidths: ExpenseWorkbenchColumnWidthMap
) {
  const savedWidth = columnWidths[column.key]
  if (typeof savedWidth !== 'number' || !Number.isFinite(savedWidth) || savedWidth <= 0) {
    return column.width
  }
  const minWidth = column.minWidth || 0
  return Math.max(savedWidth, minWidth)
}

function matchesText(source: string | undefined, keyword: string) {
  const normalizedKeyword = keyword.trim().toLowerCase()
  if (!normalizedKeyword) {
    return true
  }
  return (source || '').toLowerCase().includes(normalizedKeyword)
}

function matchesArrayText(source: string[] | undefined, keyword: string) {
  const normalizedKeyword = keyword.trim().toLowerCase()
  if (!normalizedKeyword) {
    return true
  }
  return (source || []).some((item) => item.toLowerCase().includes(normalizedKeyword))
}

function matchesDateRange(source: string | undefined, dateRange: string[]) {
  if (!Array.isArray(dateRange) || dateRange.length < 2 || !dateRange[0] || !dateRange[1]) {
    return true
  }
  const normalizedSource = (source || '').slice(0, 10)
  if (!normalizedSource) {
    return false
  }
  return normalizedSource >= dateRange[0] && normalizedSource <= dateRange[1]
}
