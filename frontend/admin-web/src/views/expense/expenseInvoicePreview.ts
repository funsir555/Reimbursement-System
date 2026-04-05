import type { ExpenseAttachmentMeta, ProcessFormDesignBlock, ProcessFormDesignSchema } from '@/api'
import { getControlType } from '@/views/process/formDesignerHelper'
import { FIELD_INVOICE_ATTACHMENTS } from './expenseDetailRuntime'

export type ExpenseInvoiceTone = 'success' | 'warning' | 'danger' | 'info'
export type ExpenseInvoicePreviewKind = 'image' | 'pdf' | 'file'

export interface ExpenseInvoiceVerifyInfo {
  verifyStatus: string
  verifyTone: ExpenseInvoiceTone
  ocrStatus: string
  ocrTone: ExpenseInvoiceTone
  checkTime: string
  invoiceCode: string
  invoiceNumber: string
  invoiceType: string
  seller: string
  amount: number
  taxAmount: number
  issueDate: string
}

export interface ExpenseInvoicePreviewItem extends ExpenseInvoiceVerifyInfo {
  id: string
  fileName: string
  attachmentId?: string
  contentType?: string
  previewUrl?: string
  previewKind: ExpenseInvoicePreviewKind
  isImage: boolean
  isPdf: boolean
  isPreviewable: boolean
}

type BuildExpenseInvoicePreviewOptions = {
  schema?: ProcessFormDesignSchema | null
  formData?: Record<string, unknown> | null
  detailTitle?: string
  detailNo?: string
}

type ExpenseInvoiceVerifyInfoProfile = {
  verifyStatus: string
  verifyTone: ExpenseInvoiceTone
  ocrStatus: string
  ocrTone: ExpenseInvoiceTone
}

const INVOICE_TYPES = [
  '增值税电子普通发票',
  '增值税专用发票',
  '电子发票（铁路客票）',
  '数电发票（普通）'
] as const

const SELLERS = [
  '上海云旅服务有限公司',
  '杭州数智科技有限公司',
  '苏州商务会展有限公司',
  '深圳航信商务服务有限公司',
  '北京华誉企业管理有限公司'
] as const

const DEFAULT_VERIFY_PROFILE: ExpenseInvoiceVerifyInfoProfile = {
  verifyStatus: '已验真',
  verifyTone: 'success',
  ocrStatus: '已识别',
  ocrTone: 'success'
}

const VERIFY_PROFILES: ExpenseInvoiceVerifyInfoProfile[] = [
  DEFAULT_VERIFY_PROFILE,
  {
    verifyStatus: '验真中',
    verifyTone: 'warning',
    ocrStatus: '识别中',
    ocrTone: 'warning'
  },
  {
    verifyStatus: '待验真',
    verifyTone: 'info',
    ocrStatus: '待识别',
    ocrTone: 'info'
  },
  {
    verifyStatus: '验真异常',
    verifyTone: 'danger',
    ocrStatus: '需复核',
    ocrTone: 'warning'
  }
]

export function buildExpenseInvoicePreviewItems(
  options: BuildExpenseInvoicePreviewOptions
): ExpenseInvoicePreviewItem[] {
  const attachments = collectExpenseInvoiceAttachments(options.schema, options.formData)
  return attachments.map((attachment, index) => buildExpenseInvoicePreviewItem(attachment, index))
}

export function collectExpenseInvoiceAttachments(
  schema?: ProcessFormDesignSchema | null,
  formData?: Record<string, unknown> | null
): ExpenseAttachmentMeta[] {
  const primaryFiles = normalizeAttachments(formData?.[FIELD_INVOICE_ATTACHMENTS])
  if (primaryFiles.length > 0) {
    return primaryFiles
  }

  const blocks = Array.isArray(schema?.blocks) ? schema.blocks : []
  const fallbackFiles = blocks.flatMap((block) => collectBlockAttachments(block, formData))
  return uniqueAttachments(fallbackFiles)
}

export function collectExpenseInvoiceFileNames(
  schema?: ProcessFormDesignSchema | null,
  formData?: Record<string, unknown> | null
): string[] {
  return collectExpenseInvoiceAttachments(schema, formData).map((item) => item.fileName)
}

export function buildAuthorizedAttachmentPreviewUrl(previewUrl?: string) {
  const normalizedUrl = typeof previewUrl === 'string' ? previewUrl.trim() : ''
  if (!normalizedUrl) {
    return ''
  }

  const token = window.localStorage.getItem('token')
  if (!token) {
    return normalizedUrl
  }

  const separator = normalizedUrl.includes('?') ? '&' : '?'
  return `${normalizedUrl}${separator}token=${encodeURIComponent(token)}`
}

function collectBlockAttachments(
  block: ProcessFormDesignBlock,
  formData?: Record<string, unknown> | null
): ExpenseAttachmentMeta[] {
  const controlType = getControlType(block)
  if (!['ATTACHMENT', 'IMAGE'].includes(controlType)) {
    return []
  }
  return normalizeAttachments(formData?.[block.fieldKey])
}

function normalizeAttachments(value: unknown): ExpenseAttachmentMeta[] {
  if (Array.isArray(value)) {
    return uniqueAttachments(value.flatMap((item) => normalizeAttachments(item)))
  }

  if (typeof value === 'string') {
    const trimmed = value.trim()
    return trimmed
      ? [{
          fileName: trimmed
        }]
      : []
  }

  if (isRecord(value)) {
    const fileName = firstNonBlank(value.fileName, value.name, value.label, value.value, value.url)
    if (!fileName) {
      return []
    }
    return [{
      attachmentId: firstNonBlank(value.attachmentId, value.id),
      fileName,
      contentType: firstNonBlank(value.contentType, value.mimeType, value.type),
      fileSize: toOptionalNumber(value.fileSize, value.size),
      previewUrl: firstNonBlank(value.previewUrl, value.fileUrl, value.url)
    }]
  }

  return []
}

function buildExpenseInvoicePreviewItem(attachment: ExpenseAttachmentMeta, index: number): ExpenseInvoicePreviewItem {
  const fileName = attachment.fileName
  const seed = stableHash(fileName)
  const typeIndex = seed % INVOICE_TYPES.length
  const sellerIndex = (seed >> 3) % SELLERS.length
  const verifyProfile = VERIFY_PROFILES[(seed >> 5) % VERIFY_PROFILES.length] ?? DEFAULT_VERIFY_PROFILE
  const issueDate = buildStableDate(seed, 0)
  const checkTime = buildStableDateTime(seed, 4)
  const amount = normalizeAmount(260 + (seed % 7800) + ((seed >> 7) % 100) / 100)
  const taxRate = [0.03, 0.06, 0.09, 0.13][(seed >> 9) % 4] || 0.06
  const taxAmount = normalizeAmount(amount * taxRate)
  const previewKind = resolvePreviewKind(attachment)

  return {
    id: attachment.attachmentId || `${seed}-${index}`,
    attachmentId: attachment.attachmentId,
    fileName,
    contentType: attachment.contentType,
    previewUrl: attachment.previewUrl,
    previewKind,
    isImage: previewKind === 'image',
    isPdf: previewKind === 'pdf',
    isPreviewable: Boolean(attachment.previewUrl) && previewKind !== 'file',
    invoiceCode: padNumber(String(seed % 10 ** 10), 10),
    invoiceNumber: padNumber(String((seed * 97) % 10 ** 8), 8),
    invoiceType: INVOICE_TYPES[typeIndex] || INVOICE_TYPES[0],
    seller: SELLERS[sellerIndex] || SELLERS[0],
    amount,
    taxAmount,
    issueDate,
    checkTime,
    verifyStatus: verifyProfile.verifyStatus,
    verifyTone: verifyProfile.verifyTone,
    ocrStatus: verifyProfile.ocrStatus,
    ocrTone: verifyProfile.ocrTone
  }
}

function resolvePreviewKind(attachment: ExpenseAttachmentMeta): ExpenseInvoicePreviewKind {
  const contentType = String(attachment.contentType || '').toLowerCase()
  const fileName = String(attachment.fileName || '').toLowerCase()
  if (contentType.startsWith('image/') || /\.(png|jpe?g|gif|webp|bmp|svg)$/i.test(fileName)) {
    return 'image'
  }
  if (contentType === 'application/pdf' || fileName.endsWith('.pdf')) {
    return 'pdf'
  }
  return 'file'
}

function uniqueAttachments(attachments: ExpenseAttachmentMeta[]): ExpenseAttachmentMeta[] {
  const seen = new Set<string>()
  const result: ExpenseAttachmentMeta[] = []

  attachments.forEach((item, index) => {
    const fileName = String(item.fileName || '').trim()
    if (!fileName) {
      return
    }

    const key = item.attachmentId || `${fileName}|${item.previewUrl || ''}|${index}`
    if (seen.has(key)) {
      return
    }
    seen.add(key)
    result.push({
      attachmentId: item.attachmentId,
      fileName,
      contentType: item.contentType,
      fileSize: item.fileSize,
      previewUrl: item.previewUrl
    })
  })

  return result
}

function buildStableDate(seed: number, dayOffset: number) {
  const year = 2024 + (seed % 3)
  const month = (seed % 12) + 1
  const day = ((seed >> 4) % 26) + 1 + dayOffset
  return `${year}-${padNumber(String(month), 2)}-${padNumber(String(Math.min(day, 28)), 2)}`
}

function buildStableDateTime(seed: number, dayOffset: number) {
  const date = buildStableDate(seed, dayOffset)
  const hour = padNumber(String((seed >> 6) % 24), 2)
  const minute = padNumber(String((seed >> 10) % 60), 2)
  const second = padNumber(String((seed >> 12) % 60), 2)
  return `${date} ${hour}:${minute}:${second}`
}

function normalizeAmount(value: number) {
  return Number(value.toFixed(2))
}

function padNumber(value: string, length: number) {
  return value.padStart(length, '0').slice(-length)
}

function stableHash(input: string) {
  let hash = 0
  for (const char of input) {
    hash = (hash * 131 + char.charCodeAt(0)) % 2147483647
  }
  return Math.abs(hash)
}

function firstNonBlank(...values: unknown[]) {
  for (const value of values) {
    if (typeof value === 'string' && value.trim()) {
      return value.trim()
    }
  }
  return undefined
}

function toOptionalNumber(...values: unknown[]) {
  for (const value of values) {
    if (typeof value === 'number' && Number.isFinite(value)) {
      return value
    }
    if (typeof value === 'string' && value.trim()) {
      const parsed = Number(value)
      if (Number.isFinite(parsed)) {
        return parsed
      }
    }
  }
  return undefined
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}
