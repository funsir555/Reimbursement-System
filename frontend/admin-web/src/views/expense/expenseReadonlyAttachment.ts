import type { ExpenseAttachmentMeta, ProcessFormDesignBlock } from '@/api'
import { getControlType } from '@/views/process/formDesignerHelper'
import { FIELD_INVOICE_ATTACHMENTS } from './expenseDetailRuntime'

export type ExpenseReadonlyAttachmentActionKind = 'preview' | 'download' | 'unavailable'

export type ExpenseReadonlyAttachmentItem = {
  id: string
  fileName: string
  attachmentId?: string
  contentType?: string
  fileSize?: number
  previewUrl?: string
  actionUrl?: string
  previewable: boolean
  actionKind: ExpenseReadonlyAttachmentActionKind
}

const PREVIEWABLE_IMAGE_EXTENSIONS = /\.(png|jpe?g|gif|webp|bmp|svg)$/i
const PREVIEWABLE_IMAGE_TYPES = new Set([
  'image/png',
  'image/jpeg',
  'image/gif',
  'image/webp',
  'image/bmp',
  'image/svg+xml'
])

export function buildExpenseReadonlyAttachmentItems(
  block: ProcessFormDesignBlock,
  value: unknown,
  documentCode?: string
): ExpenseReadonlyAttachmentItem[] {
  return normalizeExpenseReadonlyAttachments(value).map((attachment, index) =>
    buildExpenseReadonlyAttachmentItem(block, attachment, index, documentCode)
  )
}

export function normalizeExpenseReadonlyAttachments(value: unknown): ExpenseAttachmentMeta[] {
  if (Array.isArray(value)) {
    return uniqueExpenseReadonlyAttachments(value.flatMap((item) => normalizeExpenseReadonlyAttachments(item)))
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
      previewUrl: firstNonBlank(value.previewUrl, value.fileUrl, value.url)
    }]
  }

  return []
}

export function buildAuthorizedExpenseReadonlyAttachmentUrl(url?: string) {
  const normalizedUrl = typeof url === 'string' ? url.trim() : ''
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

export function openExpenseReadonlyAttachmentPreview(url?: string) {
  const href = buildAuthorizedExpenseReadonlyAttachmentUrl(url)
  if (!href) {
    return
  }
  window.open(href, '_blank', 'noopener')
}

export function downloadExpenseReadonlyAttachment(url?: string, fileName?: string) {
  const href = buildAuthorizedExpenseReadonlyAttachmentUrl(url)
  if (!href) {
    return
  }

  const anchor = document.createElement('a')
  anchor.href = href
  anchor.rel = 'noopener'
  if (typeof fileName === 'string' && fileName.trim()) {
    anchor.download = fileName.trim()
  }
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
}

export function isExpenseReadonlyAttachmentBlock(block: ProcessFormDesignBlock) {
  const controlType = getControlType(block)
  if (!['ATTACHMENT', 'IMAGE'].includes(controlType)) {
    return false
  }
  return String(block.fieldKey || '').trim() !== FIELD_INVOICE_ATTACHMENTS
}

function buildExpenseReadonlyAttachmentItem(
  block: ProcessFormDesignBlock,
  attachment: ExpenseAttachmentMeta,
  index: number,
  documentCode?: string
): ExpenseReadonlyAttachmentItem {
  const actionUrl = resolveExpenseReadonlyAttachmentActionUrl(documentCode, attachment)
  const previewable = isPreviewableExpenseReadonlyAttachment(attachment)
  const actionKind: ExpenseReadonlyAttachmentActionKind = actionUrl
    ? previewable
      ? 'preview'
      : 'download'
    : 'unavailable'

  return {
    id: attachment.attachmentId || `${String(block.fieldKey || 'attachment').trim() || 'attachment'}-${index}`,
    fileName: attachment.fileName,
    attachmentId: attachment.attachmentId,
    contentType: attachment.contentType,
    fileSize: attachment.fileSize,
    previewUrl: attachment.previewUrl,
    actionUrl,
    previewable,
    actionKind
  }
}

function resolveExpenseReadonlyAttachmentActionUrl(documentCode: string | undefined, attachment: ExpenseAttachmentMeta) {
  const normalizedDocumentCode = typeof documentCode === 'string' ? documentCode.trim() : ''
  const normalizedAttachmentId = typeof attachment.attachmentId === 'string' ? attachment.attachmentId.trim() : ''

  if (normalizedDocumentCode && normalizedAttachmentId) {
    return `/api/auth/expenses/${encodeURIComponent(normalizedDocumentCode)}/attachments/${encodeURIComponent(normalizedAttachmentId)}/content`
  }

  const previewUrl = typeof attachment.previewUrl === 'string' ? attachment.previewUrl.trim() : ''
  return previewUrl || ''
}

function isPreviewableExpenseReadonlyAttachment(attachment: ExpenseAttachmentMeta) {
  const contentType = String(attachment.contentType || '').trim().toLowerCase()
  const fileName = String(attachment.fileName || '').trim().toLowerCase()

  if (contentType === 'application/pdf' || fileName.endsWith('.pdf')) {
    return true
  }

  if (PREVIEWABLE_IMAGE_TYPES.has(contentType) || PREVIEWABLE_IMAGE_EXTENSIONS.test(fileName)) {
    return true
  }

  return false
}

function uniqueExpenseReadonlyAttachments(attachments: ExpenseAttachmentMeta[]) {
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

  return undefined
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}
