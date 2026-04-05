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

  it('builds preview items from attachment metadata and marks pdf/image types', () => {
    const [pdfItem, imageItem] = buildExpenseInvoicePreviewItems({
      formData: {
        invoiceAttachments: [
          {
            attachmentId: 'ATT-001',
            fileName: 'hotel.pdf',
            contentType: 'application/pdf',
            previewUrl: '/api/auth/expenses/attachments/ATT-001/content'
          },
          {
            attachmentId: 'ATT-002',
            fileName: 'taxi.png',
            contentType: 'image/png',
            previewUrl: '/api/auth/expenses/attachments/ATT-002/content'
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
      isPreviewable: true
    })
    expect(imageItem).toMatchObject({
      attachmentId: 'ATT-002',
      fileName: 'taxi.png',
      previewKind: 'image',
      isPdf: false,
      isImage: true,
      isPreviewable: true
    })
  })

  it('keeps legacy file names as fallback-only preview items', () => {
    const [item] = buildExpenseInvoicePreviewItems({
      formData: {
        invoiceAttachments: ['legacy-only.pdf']
      }
    })

    expect(item.fileName).toBe('legacy-only.pdf')
    expect(item.previewKind).toBe('pdf')
    expect(item.isPreviewable).toBe(false)
  })

  it('normalizes attachment objects before collecting invoice attachments', () => {
    const attachments = collectExpenseInvoiceAttachments(undefined, {
      invoiceAttachments: [
        {
          id: 'ATT-009',
          name: 'meal.jpg',
          mimeType: 'image/jpeg',
          url: '/api/auth/expenses/attachments/ATT-009/content'
        }
      ]
    })

    expect(attachments).toEqual([{
      attachmentId: 'ATT-009',
      fileName: 'meal.jpg',
      contentType: 'image/jpeg',
      fileSize: undefined,
      previewUrl: '/api/auth/expenses/attachments/ATT-009/content'
    }])
  })

  it('appends token to preview url when local token exists', () => {
    const getItemSpy = vi.spyOn(Storage.prototype, 'getItem').mockReturnValue('token-001')

    expect(buildAuthorizedAttachmentPreviewUrl('/api/auth/expenses/attachments/ATT-001/content'))
      .toBe('/api/auth/expenses/attachments/ATT-001/content?token=token-001')

    getItemSpy.mockRestore()
  })
})
