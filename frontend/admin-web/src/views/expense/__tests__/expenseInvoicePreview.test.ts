import { describe, expect, it, vi } from 'vitest'
import {
  buildAuthorizedAttachmentPreviewUrl,
  buildExpenseInvoicePreviewItems,
  collectExpenseInvoiceAttachments,
  collectExpenseInvoiceFileNames
} from '@/views/expense/expenseInvoicePreview'

function createAttachmentBlock(fieldKey: string, controlType: 'ATTACHMENT' | 'IMAGE' = 'ATTACHMENT') {
  return {
    blockId: fieldKey,
    fieldKey,
    kind: 'CONTROL',
    label: fieldKey,
    span: 1,
    required: false,
    props: {
      controlType
    }
  } as const
}

describe('expenseInvoicePreview', () => {
  it('prefers invoiceAttachments over other attachment fields', () => {
    const fileNames = collectExpenseInvoiceFileNames({
      layoutMode: 'TWO_COLUMN',
      blocks: [
        createAttachmentBlock('otherAttachments'),
        createAttachmentBlock('otherImages', 'IMAGE')
      ]
    }, {
      invoiceAttachments: ['invoice-a.pdf'],
      otherAttachments: ['fallback-a.pdf'],
      otherImages: ['fallback-b.png']
    })

    expect(fileNames).toEqual(['invoice-a.pdf'])
  })

  it('builds preview items from attachment OCR snapshots instead of filename mock data', () => {
    const [pdfItem, imageItem] = buildExpenseInvoicePreviewItems({
      formData: {
        invoiceAttachments: [
          {
            attachmentId: 'ATT-001',
            fileName: 'hotel.pdf',
            contentType: 'application/pdf',
            previewUrl: '/api/auth/expenses/attachments/ATT-001/content',
            ocr: {
              status: 'SUCCESS',
              providerName: '阿里云',
              invoiceCode: '1234567890',
              invoiceNumber: '87654321',
              invoiceDate: '2026-04-19',
              invoiceType: '增值税电子普通发票',
              sellerName: '上海测试商户',
              totalAmount: 188.5,
              taxAmount: 10.68,
              message: '识别成功'
            }
          },
          {
            attachmentId: 'ATT-002',
            fileName: 'taxi.png',
            contentType: 'image/png',
            previewUrl: '/api/auth/expenses/attachments/ATT-002/content',
            ocr: {
              status: 'UNCONFIGURED',
              message: '未配置 OCR'
            }
          }
        ]
      }
    })

    expect(pdfItem).toMatchObject({
      attachmentId: 'ATT-001',
      fileName: 'hotel.pdf',
      previewKind: 'pdf',
      isPdf: true,
      isImage: false,
      isPreviewable: true,
      invoiceCode: '1234567890',
      sellerName: '上海测试商户',
      statusLabel: '已识别'
    })
    expect(imageItem).toMatchObject({
      attachmentId: 'ATT-002',
      fileName: 'taxi.png',
      previewKind: 'image',
      isPdf: false,
      isImage: true,
      isPreviewable: true,
      statusLabel: '未配置 OCR'
    })
  })

  it('keeps legacy file names as pending preview items without fake invoice values', () => {
    const [item] = buildExpenseInvoicePreviewItems({
      formData: {
        invoiceAttachments: ['legacy-only.pdf']
      }
    })

    expect(item.fileName).toBe('legacy-only.pdf')
    expect(item.previewKind).toBe('pdf')
    expect(item.isPreviewable).toBe(false)
    expect(item.statusLabel).toBe('待识别')
    expect(item.invoiceCode).toBe('--')
  })

  it('normalizes attachment objects before collecting invoice attachments', () => {
    const attachments = collectExpenseInvoiceAttachments(undefined, {
      invoiceAttachments: [
        {
          id: 'ATT-009',
          name: 'meal.jpg',
          mimeType: 'image/jpeg',
          url: '/api/auth/expenses/attachments/ATT-009/content',
          ocr: {
            status: 'SUCCESS',
            invoiceCode: '2345678901'
          }
        }
      ]
    })

    expect(attachments).toEqual([{
      attachmentId: 'ATT-009',
      fileName: 'meal.jpg',
      contentType: 'image/jpeg',
      fileSize: null,
      previewUrl: '/api/auth/expenses/attachments/ATT-009/content',
      ocr: {
        status: 'SUCCESS',
        providerCode: '',
        providerName: '',
        requestId: '',
        recognizedAt: '',
        invoiceCode: '2345678901',
        invoiceNumber: '',
        invoiceDate: '',
        invoiceType: '',
        sellerName: '',
        totalAmount: null,
        taxAmount: null,
        message: ''
      }
    }])
  })

  it('appends token to preview url when local token exists', () => {
    const getItemSpy = vi.spyOn(Storage.prototype, 'getItem').mockReturnValue('token-001')

    expect(buildAuthorizedAttachmentPreviewUrl('/api/auth/expenses/attachments/ATT-001/content'))
      .toBe('/api/auth/expenses/attachments/ATT-001/content?token=token-001')

    getItemSpy.mockRestore()
  })
})
