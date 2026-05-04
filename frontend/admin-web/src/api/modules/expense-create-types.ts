// 这里定义 expense-create-types.ts 相关数据结构。
// 页面与 API 封装会依赖这些类型来约定字段。
// 如果改错，最容易影响列表、表单和接口联调。

import type { ExpenseApprovalTimelineItem, ExpenseDetailInstance } from './expense-types'
import type { ProcessCustomArchiveDetail } from './process-archive-types'
import type { ProcessFlowNode, ProcessFlowRoute } from './process-flow-types'
import type { ProcessFormDesignSchema } from './process-form-types'
import type { ProcessFormOption } from './process-template-types'

export interface ExpenseCreateTemplateSummary {
  templateCode: string
  templateName: string
  templateType: string
  templateTypeLabel: string
  categoryCode: string
  categoryName?: string
  formDesignCode?: string
}

export interface ExpenseCreateVendorOption {
  value: string
  label: string
  secondaryLabel?: string
  cVenCode: string
  cVenName: string
  cVenAbbName?: string
}

export interface ExpenseCreateVendorOptionsParams {
  keyword?: string
  includeDisabled?: boolean
  paymentCompanyId?: string
}

export interface ExpenseCreatePayeeOption {
  value: string
  label: string
  sourceType: string
  sourceCode: string
  secondaryLabel?: string
}

export interface ExpenseCreatePayeeAccountOption {
  value: string
  label: string
  sourceType: string
  ownerCode: string
  ownerName: string
  bankName?: string
  accountName?: string
  accountNoMasked?: string
  secondaryLabel?: string
}

export interface ExpenseCreatePayeeOptionsParams {
  keyword?: string
  personalOnly?: boolean
}

export type ExpenseCreatePayeeAccountLinkageMode = 'EMPLOYEE' | 'ENTERPRISE'

export interface ExpenseCreatePayeeAccountOptionsParams {
  keyword?: string
  linkageMode?: ExpenseCreatePayeeAccountLinkageMode
  payeeName?: string
  counterpartyCode?: string
  paymentCompanyId?: string
}

export interface ExpenseCreateTemplateDetail {
  templateCode: string
  templateName: string
  templateType: string
  templateTypeLabel: string
  categoryCode: string
  templateDescription?: string
  formDesignCode?: string
  approvalFlowCode?: string
  flowName?: string
  formName?: string
  schema: ProcessFormDesignSchema
  flowSnapshot: {
    flowName?: string
    flowDescription?: string
    nodes?: ProcessFlowNode[]
    routes?: ProcessFlowRoute[]
    [key: string]: unknown
  }
  sharedArchives: ProcessCustomArchiveDetail[]
  expenseDetailDesignCode?: string
  expenseDetailDesignName?: string
  expenseDetailType?: string
  expenseDetailTypeLabel?: string
  expenseDetailModeDefault?: string
  expenseDetailSchema: ProcessFormDesignSchema
  expenseDetailSharedArchives: ProcessCustomArchiveDetail[]
  companyOptions: ProcessFormOption[]
  departmentOptions: ProcessFormOption[]
  userOptions: ProcessFormOption[]
  currentUserCompanyId?: string
  currentUserCompanyName?: string
  currentUserDeptId?: string
  currentUserDeptName?: string
  currentUserDeptIds?: string[]
  currentUserDeptNames?: string[]
}

// 这是 ExpenseDocumentEditContext 的数据结构。
export interface ExpenseDocumentEditContext extends ExpenseCreateTemplateDetail {
  editMode: string
  documentCode: string
  taskId?: number
  taskNodeKey?: string
  allowEditFormModule?: boolean
  allowEditPayAccount?: boolean
  formData: Record<string, unknown>
  expenseDetails: ExpenseDetailInstance[]
}

export interface ExpenseDocumentSubmitPayload {
  templateCode: string
  formData: Record<string, unknown>
  expenseDetails?: ExpenseDetailInstance[]
  manualApproverSelections?: Record<string, number[]>
}

export interface ExpenseDocumentSubmitResult {
  id: number
  documentCode: string
  status: string
}

export interface ExpenseManualApproverPreviewNode {
  nodeKey: string
  nodeName?: string
  nodeType?: string
  required?: boolean
  candidateOptions: ProcessFormOption[]
  selectedUserIds: number[]
}

export interface ExpenseManualApproverPreview {
  approvalTimeline: ExpenseApprovalTimelineItem[]
  manualNodes: ExpenseManualApproverPreviewNode[]
}

// 这是 ExpenseDocumentUpdatePayload 的数据结构。
export interface ExpenseDocumentUpdatePayload {
  formData: Record<string, unknown>
  expenseDetails?: ExpenseDetailInstance[]
  manualApproverSelections?: Record<string, number[]>
}

export interface ExpenseAttachmentMeta {
  attachmentId?: string
  fileName: string
  contentType?: string
  fileSize?: number
  previewUrl?: string
  ocr?: ExpenseAttachmentOcrSnapshot
}

export interface ExpenseAttachmentOcrSnapshot {
  status: string
  providerCode?: string
  providerName?: string
  requestId?: string
  recognizedAt?: string
  invoiceCode?: string
  invoiceNumber?: string
  invoiceDate?: string
  invoiceType?: string
  sellerName?: string
  totalAmount?: number
  taxAmount?: number
  message?: string
}

export type ExpenseAttachmentOcrResult = ExpenseAttachmentOcrSnapshot
