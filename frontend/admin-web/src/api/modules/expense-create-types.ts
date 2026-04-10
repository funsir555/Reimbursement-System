import type { ExpenseDetailInstance } from './expense-types'
import type { ProcessCustomArchiveDetail } from './process-archive-types'
import type { ProcessFormDesignSchema } from './process-form-types'
import type { ProcessFormOption } from './process-template-types'

export interface ExpenseCreateTemplateSummary {
  templateCode: string
  templateName: string
  templateType: string
  templateTypeLabel: string
  categoryCode: string
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
  currentUserDeptId?: string
  currentUserDeptName?: string
}

export interface ExpenseDocumentEditContext extends ExpenseCreateTemplateDetail {
  editMode: string
  documentCode: string
  taskId?: number
  formData: Record<string, unknown>
  expenseDetails: ExpenseDetailInstance[]
}

export interface ExpenseDocumentSubmitPayload {
  templateCode: string
  formData: Record<string, unknown>
  expenseDetails?: ExpenseDetailInstance[]
}

export interface ExpenseDocumentSubmitResult {
  id: number
  documentCode: string
  status: string
}

export interface ExpenseDocumentUpdatePayload {
  formData: Record<string, unknown>
  expenseDetails?: ExpenseDetailInstance[]
}

export interface ExpenseAttachmentMeta {
  attachmentId?: string
  fileName: string
  contentType?: string
  fileSize?: number
  previewUrl?: string
}
