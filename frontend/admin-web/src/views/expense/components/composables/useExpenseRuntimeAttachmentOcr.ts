import { ElMessage, type UploadFile, type UploadUserFile } from 'element-plus'
import type { ComputedRef, Ref } from 'vue'
import {
  expenseCreateApi,
  type ExpenseAttachmentMeta,
  type ProcessFormDesignBlock,
  type ProcessFormDesignSchema
} from '@/api'
import {
  FIELD_INVOICE_ATTACHMENTS,
  resolveInvoiceOcrTotal,
  syncInvoiceAmountWithOcr
} from '@/views/expense/expenseDetailRuntime'

const INVOICE_ATTACHMENT_ALLOWED_EXTENSIONS = new Set(['.pdf', '.png', '.jpg', '.jpeg'])
const INVOICE_ATTACHMENT_ALLOWED_MIME_TYPES = new Set(['application/pdf', 'image/png', 'image/jpeg'])
const INVOICE_ATTACHMENT_INVALID_MESSAGE = '发票附件仅支持 PDF、PNG、JPG、JPEG 文件'
const INVOICE_ATTACHMENT_DEFAULT_ACCEPT = '.pdf,.png,.jpg,.jpeg'
const DEFAULT_ATTACHMENT_MAX_COUNT = 1
const DEFAULT_ATTACHMENT_MAX_SIZE_MB = 1
const BYTES_PER_MB = 1024 * 1024

export function resolveAttachmentMaxCount(block: ProcessFormDesignBlock) {
  const parsed = Number(block.props.maxCount)
  if (Number.isFinite(parsed) && parsed > 0) {
    return parsed
  }
  return DEFAULT_ATTACHMENT_MAX_COUNT
}

export function buildAttachmentCountExceededMessage(block: ProcessFormDesignBlock) {
  return `文件数量超出 ${resolveAttachmentMaxCount(block)} 个`
}

export function resolveAttachmentMaxSizeMb(block: ProcessFormDesignBlock) {
  const parsed = Number(block.props.maxSizeMb)
  if (Number.isFinite(parsed) && parsed > 0) {
    return parsed
  }
  return DEFAULT_ATTACHMENT_MAX_SIZE_MB
}

export function buildAttachmentSizeExceededMessage(block: ProcessFormDesignBlock) {
  return `文件大小超出 ${resolveAttachmentMaxSizeMb(block)}MB`
}

export function useExpenseRuntimeAttachmentOcr(params: {
  formData: Ref<Record<string, unknown>>
  schema: ComputedRef<ProcessFormDesignSchema>
  detailType: ComputedRef<string>
  defaultBusinessScenario: ComputedRef<string>
  resolveErrorMessage: (error: unknown, fallback: string) => string
}) {
  const { formData, schema, detailType, defaultBusinessScenario, resolveErrorMessage } = params

  function uploadFileList(block: ProcessFormDesignBlock): UploadUserFile[] {
    return normalizeAttachments(formData.value[block.fieldKey]).map((item, index) => ({
      name: item.fileName,
      status: 'success',
      uid: index + 1
    }))
  }

  function uploadAccept(block: ProcessFormDesignBlock) {
    if (!isInvoiceAttachmentBlock(block)) {
      const accept = String(block.props.accept || '').trim()
      return accept || undefined
    }
    const accept = String(block.props.accept || '').trim()
    return accept || INVOICE_ATTACHMENT_DEFAULT_ACCEPT
  }

  async function handleFileChange(block: ProcessFormDesignBlock, uploadFile: UploadFile) {
    if (!uploadFile.raw) {
      return
    }

    const current = normalizeAttachments(formData.value[block.fieldKey])
    if (current.length >= resolveAttachmentMaxCount(block)) {
      ElMessage.warning(buildAttachmentCountExceededMessage(block))
      return
    }

    if (uploadFile.raw.size > resolveAttachmentMaxSizeMb(block) * BYTES_PER_MB) {
      ElMessage.warning(buildAttachmentSizeExceededMessage(block))
      return
    }

    if (isInvoiceAttachmentBlock(block) && !isAllowedInvoiceAttachmentFile(uploadFile.raw)) {
      ElMessage.warning(INVOICE_ATTACHMENT_INVALID_MESSAGE)
      return
    }

    try {
      const uploadRes = await expenseCreateApi.uploadAttachment(uploadFile.raw)
      const uploadedAttachment = attachResolvedOcr(
        uploadRes.data,
        isInvoiceAttachmentBlock(block)
          ? await resolveAttachmentOcrSnapshot(uploadRes.data)
          : undefined
      )
      const nextAttachments = [...current, uploadedAttachment]
      formData.value[block.fieldKey] = nextAttachments
      syncInvoiceAmountFromAttachments(block, current, nextAttachments)
    } catch (error: unknown) {
      ElMessage.error(resolveErrorMessage(error, '附件上传失败'))
    }
  }

  function handleFileExceed(block: ProcessFormDesignBlock) {
    ElMessage.warning(buildAttachmentCountExceededMessage(block))
  }

  function handleFileRemove(block: ProcessFormDesignBlock, uploadFile: UploadFile) {
    const current = normalizeAttachments(formData.value[block.fieldKey])
    const nextAttachments = current.filter((item, index) => {
      const fallbackUid = `legacy-${block.fieldKey}-${index}-${item.fileName}`
      const currentUid = item.attachmentId || fallbackUid
      if (uploadFile.uid !== undefined && String(uploadFile.uid) === currentUid) {
        return false
      }
      return item.fileName !== uploadFile.name
    })
    formData.value[block.fieldKey] = nextAttachments
    syncInvoiceAmountFromAttachments(block, current, nextAttachments)
  }

  function isInvoiceAttachmentBlock(block: ProcessFormDesignBlock) {
    if (block.fieldKey === FIELD_INVOICE_ATTACHMENTS) {
      return true
    }

    const tokens = normalizeAcceptTokens(block.props.accept)
    return tokens.length > 0 && tokens.every((token) => INVOICE_ATTACHMENT_ALLOWED_EXTENSIONS.has(token)) && tokens.includes('.pdf')
  }

  function isAllowedInvoiceAttachmentFile(file: File) {
    const mimeType = String(file.type || '').trim().toLowerCase()
    if (mimeType && INVOICE_ATTACHMENT_ALLOWED_MIME_TYPES.has(mimeType)) {
      return true
    }

    const extension = resolveFileExtension(file.name)
    return Boolean(extension && INVOICE_ATTACHMENT_ALLOWED_EXTENSIONS.has(extension))
  }

  async function resolveAttachmentOcrSnapshot(
    attachment: ExpenseAttachmentMeta
  ): Promise<ExpenseAttachmentMeta['ocr']> {
    const attachmentId = typeof attachment.attachmentId === 'string' ? attachment.attachmentId.trim() : ''
    if (!attachmentId) {
      return {
        status: 'FAILED',
        message: '附件上传成功，但未返回附件标识'
      }
    }

    try {
      const res = await expenseCreateApi.recognizeAttachmentOcr(attachmentId)
      return normalizeOcrSnapshot(res.data)
    } catch (error: unknown) {
      return {
        status: 'FAILED',
        message: resolveErrorMessage(error, 'OCR 识别失败，请稍后重试')
      }
    }
  }

  function attachResolvedOcr(
    attachment: ExpenseAttachmentMeta,
    snapshot?: ExpenseAttachmentMeta['ocr']
  ): ExpenseAttachmentMeta {
    if (!snapshot) {
      return attachment
    }
    return {
      ...attachment,
      ocr: snapshot
    }
  }

  function syncInvoiceAmountFromAttachments(
    block: ProcessFormDesignBlock,
    previousAttachments: ExpenseAttachmentMeta[],
    nextAttachments: ExpenseAttachmentMeta[]
  ) {
    if (!isInvoiceAttachmentBlock(block) || block.fieldKey !== FIELD_INVOICE_ATTACHMENTS) {
      return
    }
    syncInvoiceAmountWithOcr(
      formData.value,
      resolveInvoiceOcrTotal(previousAttachments),
      resolveInvoiceOcrTotal(nextAttachments),
      detailType.value,
      defaultBusinessScenario.value,
      schema.value
    )
  }

  function normalizeAttachments(value: unknown): ExpenseAttachmentMeta[] {
    if (Array.isArray(value)) {
      return value.flatMap((item) => normalizeAttachments(item))
    }

    if (typeof value === 'string') {
      const fileName = value.trim()
      return fileName ? [{ fileName }] : []
    }

    if (value && typeof value === 'object') {
      const record = value as Record<string, unknown>
      const fileName = firstNonBlank(record.fileName, record.name, record.label, record.value, record.url)
      if (!fileName) {
        return []
      }

      return [
        {
          attachmentId: firstNonBlank(record.attachmentId, record.id),
          fileName,
          contentType: firstNonBlank(record.contentType, record.mimeType, record.type),
          fileSize: toOptionalNumber(record.fileSize, record.size),
          previewUrl: firstNonBlank(record.previewUrl, record.fileUrl, record.url),
          ocr: normalizeOcrSnapshot(record.ocr)
        }
      ]
    }

    return []
  }

  function normalizeOcrSnapshot(value: unknown): ExpenseAttachmentMeta['ocr'] {
    if (!value || typeof value !== 'object' || Array.isArray(value)) {
      return undefined
    }

    const record = value as Record<string, unknown>
    const status = firstNonBlank(record.status)
    if (!status) {
      return undefined
    }

    return {
      status,
      providerCode: firstNonBlank(record.providerCode),
      providerName: firstNonBlank(record.providerName),
      requestId: firstNonBlank(record.requestId),
      recognizedAt: firstNonBlank(record.recognizedAt),
      invoiceCode: firstNonBlank(record.invoiceCode),
      invoiceNumber: firstNonBlank(record.invoiceNumber),
      invoiceDate: firstNonBlank(record.invoiceDate),
      invoiceType: firstNonBlank(record.invoiceType),
      sellerName: firstNonBlank(record.sellerName),
      totalAmount: toOptionalNumber(record.totalAmount),
      taxAmount: toOptionalNumber(record.taxAmount),
      message: firstNonBlank(record.message)
    }
  }

  function normalizeAcceptTokens(value: unknown) {
    return String(value || '')
      .split(',')
      .map((item) => item.trim().toLowerCase())
      .filter(Boolean)
  }

  function resolveFileExtension(fileName: string) {
    const normalized = String(fileName || '').trim().toLowerCase()
    const dotIndex = normalized.lastIndexOf('.')
    if (dotIndex < 0) {
      return ''
    }
    return normalized.slice(dotIndex)
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

  return {
    uploadFileList,
    uploadAccept,
    handleFileChange,
    handleFileExceed,
    handleFileRemove
  }
}
