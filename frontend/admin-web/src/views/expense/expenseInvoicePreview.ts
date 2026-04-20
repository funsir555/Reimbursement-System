import type { ExpenseAttachmentMeta, ProcessFormDesignBlock, ProcessFormDesignSchema } from '@/api'
import { getControlType } from '@/views/process/formDesignerHelper'
import { FIELD_INVOICE_ATTACHMENTS } from './expenseDetailRuntime'

export type ExpenseInvoiceTone = 'success' | 'warning' | 'danger' | 'info'
export type ExpenseInvoicePreviewKind = 'image' | 'pdf' | 'file'

export interface ExpenseInvoicePreviewItem {
  id: string
  fileName: string
  attachmentId?: string
  contentType?: string
  previewUrl?: string
  previewKind: ExpenseInvoicePreviewKind
  isImage: boolean
  isPdf: boolean
  isPreviewable: boolean
  statusLabel: string
  statusTone: ExpenseInvoiceTone
  statusMessage: string
  providerName: string
  recognizedAt: string
  invoiceCode: string
  invoiceNumber: string
  invoiceType: string
  sellerName: string
  invoiceDate: string
  totalAmount: number | null
  taxAmount: number | null
}

type BuildExpenseInvoicePreviewOptions = {
  schema?: ProcessFormDesignSchema | null
  formData?: Record<string, unknown> | null
}

type OcrStatusView = {
  label: string
  tone: ExpenseInvoiceTone
  fallbackMessage: string
}

const OCR_STATUS_VIEW_MAP: Record<string, OcrStatusView> = {
  SUCCESS: { label: '已识别', tone: 'success', fallbackMessage: '发票信息识别成功' },
  UNCONFIGURED: { label: '未配置 OCR', tone: 'info', fallbackMessage: '请先在系统设置中心配置并启用 OCR 厂商' },
  UNSUPPORTED_FILE: { label: '文件不支持', tone: 'warning', fallbackMessage: '当前仅支持 PDF、PNG、JPG、JPEG 发票附件' },
  TIMEOUT: { label: '识别超时', tone: 'warning', fallbackMessage: 'OCR 请求超时，请稍后重试' },
  PROVIDER_ERROR: { label: '厂商异常', tone: 'danger', fallbackMessage: '云端 OCR 服务调用失败，请稍后重试' },
  PARSE_FAILED: { label: '解析失败', tone: 'warning', fallbackMessage: 'OCR 返回结果无法解析，请人工复核' },
  FAILED: { label: '识别失败', tone: 'danger', fallbackMessage: 'OCR 识别失败，请人工复核' }
}

const PENDING_STATUS_VIEW: OcrStatusView = {
  label: '待识别',
  tone: 'info',
  fallbackMessage: '上传成功后会自动触发 OCR 识别'
}

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
    return trimmed ? [{ fileName: trimmed }] : []
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
      previewUrl: firstNonBlank(value.previewUrl, value.fileUrl, value.url),
      ocr: normalizeOcrSnapshot(value.ocr)
    }]
  }

  return []
}

function buildExpenseInvoicePreviewItem(attachment: ExpenseAttachmentMeta, index: number): ExpenseInvoicePreviewItem {
  const previewKind = resolvePreviewKind(attachment)
  const snapshot = normalizeOcrSnapshot(attachment.ocr)
  const statusView = resolveStatusView(snapshot?.status)
  const totalAmount = toOptionalNumber(snapshot?.totalAmount)
  const taxAmount = toOptionalNumber(snapshot?.taxAmount)

  return {
    id: attachment.attachmentId || `${attachment.fileName}-${index}`,
    attachmentId: attachment.attachmentId,
    fileName: attachment.fileName,
    contentType: attachment.contentType,
    previewUrl: attachment.previewUrl,
    previewKind,
    isImage: previewKind === 'image',
    isPdf: previewKind === 'pdf',
    isPreviewable: Boolean(attachment.previewUrl) && previewKind !== 'file',
    statusLabel: statusView.label,
    statusTone: statusView.tone,
    statusMessage: firstNonBlank(snapshot?.message) || statusView.fallbackMessage,
    providerName: firstNonBlank(snapshot?.providerName) || '未配置',
    recognizedAt: firstNonBlank(snapshot?.recognizedAt) || '--',
    invoiceCode: firstNonBlank(snapshot?.invoiceCode) || '--',
    invoiceNumber: firstNonBlank(snapshot?.invoiceNumber) || '--',
    invoiceType: firstNonBlank(snapshot?.invoiceType) || '--',
    sellerName: firstNonBlank(snapshot?.sellerName) || '--',
    invoiceDate: firstNonBlank(snapshot?.invoiceDate) || '--',
    totalAmount,
    taxAmount
  }
}

function resolveStatusView(status?: string) {
  if (!status) {
    return PENDING_STATUS_VIEW
  }
  return OCR_STATUS_VIEW_MAP[String(status).trim().toUpperCase()] || OCR_STATUS_VIEW_MAP.FAILED
}

function normalizeOcrSnapshot(value: unknown): ExpenseAttachmentMeta['ocr'] {
  if (!isRecord(value)) {
    return undefined
  }
  return {
    status: firstNonBlank(value.status) || '',
    providerCode: firstNonBlank(value.providerCode),
    providerName: firstNonBlank(value.providerName),
    requestId: firstNonBlank(value.requestId),
    recognizedAt: firstNonBlank(value.recognizedAt),
    invoiceCode: firstNonBlank(value.invoiceCode),
    invoiceNumber: firstNonBlank(value.invoiceNumber),
    invoiceDate: firstNonBlank(value.invoiceDate),
    invoiceType: firstNonBlank(value.invoiceType),
    sellerName: firstNonBlank(value.sellerName),
    totalAmount: toOptionalNumber(value.totalAmount),
    taxAmount: toOptionalNumber(value.taxAmount),
    message: firstNonBlank(value.message)
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
      previewUrl: item.previewUrl,
      ocr: normalizeOcrSnapshot(item.ocr)
    })
  })

  return result
}

function firstNonBlank(...values: unknown[]) {
  for (const value of values) {
    if (typeof value === 'string') {
      const trimmed = value.trim()
      if (trimmed) {
        return trimmed
      }
      continue
    }
    if (typeof value === 'number') {
      return String(value)
    }
  }
  return ''
}

function toOptionalNumber(...values: unknown[]) {
  for (const value of values) {
    if (typeof value === 'number' && Number.isFinite(value)) {
      return value
    }
    if (typeof value === 'string') {
      const trimmed = value.trim()
      if (!trimmed) {
        continue
      }
      const parsed = Number(trimmed)
      if (Number.isFinite(parsed)) {
        return parsed
      }
    }
  }
  return null
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}
