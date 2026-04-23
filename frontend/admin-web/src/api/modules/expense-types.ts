// 这里定义 expense-types.ts 相关数据结构。
// 页面与 API 封装会依赖这些类型来约定字段。
// 如果改错，最容易影响列表、表单和接口联调。

import type { MoneyValue } from './core'
import type { ProcessFlowNode, ProcessFlowRoute } from './process-flow-types'
import type { ProcessFormDesignSchema } from './process-form-types'
import type { ProcessFormOption } from './process-template-types'

// 这是 ExpenseSummary 的数据结构。
export interface ExpenseSummary {
  documentCode: string
  no: string
  type: string
  reason: string
  documentTitle?: string
  documentReason?: string
  submitterName?: string
  submitterDeptName?: string
  templateName?: string
  templateType?: string
  templateTypeLabel?: string
  currentNodeName?: string
  documentStatus?: string
  documentStatusLabel?: string
  amount: MoneyValue
  outstandingAmount?: MoneyValue
  date: string
  status: string
  submittedAt?: string
  draftDeletable?: boolean
  paymentDate?: string
  paymentCompanyName?: string
  payeeName?: string
  counterpartyName?: string
  undertakeDepartmentNames?: string[]
  tagNames?: string[]
}

export interface ExpenseApprovalTask {
  id: number
  documentCode: string
  nodeKey: string
  nodeName: string
  nodeType: string
  assigneeUserId: number
  assigneeName: string
  status: string
  taskBatchNo: string
  approvalMode?: string
  taskKind?: string
  sourceTaskId?: number
  actionComment?: string
  createdAt?: string
  handledAt?: string
}

export interface ExpenseApprovalLog {
  id: number
  documentCode: string
  nodeKey?: string
  nodeName?: string
  actionType: string
  actorUserId?: number
  actorName?: string
  actionComment?: string
  payload: Record<string, unknown>
  createdAt?: string
}

export interface ExpenseApprovalNodeStatus {
  nodeKey: string
  nodeName?: string
  nodeType?: string
  status: string
  statusLabel?: string
  assigneeNames?: string[]
  occurredAt?: string
  description?: string
}

export interface ExpenseManualApproverSelectionPayload {
  nodeKey: string
  userIds: number[]
}

export interface ExpenseApprovalTimelineItem {
  key: string
  nodeKey?: string
  nodeName?: string
  nodeType?: string
  status?: string
  statusLabel?: string
  title: string
  description?: string
  timestamp?: string
  pending?: boolean
  future?: boolean
  attachmentNames?: string[]
}

export interface ExpenseDocumentCommentPayload {
  comment?: string
  attachmentFileNames?: string[]
}

export interface ExpenseDocumentReminderPayload {
  remark?: string
}

export interface ExpenseDocumentNavigation {
  prevDocumentCode?: string
  nextDocumentCode?: string
}

// 这是 ExpenseDocumentDetail 的数据结构。
export interface ExpenseDocumentDetail {
  documentCode: string
  documentTitle: string
  documentReason?: string
  status: string
  statusLabel: string
  totalAmount: MoneyValue
  submitterUserId?: number
  submitterName?: string
  templateName?: string
  templateType?: string
  currentNodeKey?: string
  currentNodeName?: string
  currentTaskType?: string
  submittedAt?: string
  finishedAt?: string
  templateSnapshot: Record<string, unknown>
  formSchemaSnapshot: ProcessFormDesignSchema
  formData: Record<string, unknown>
  flowSnapshot: {
    flowName?: string
    flowDescription?: string
    nodes?: ProcessFlowNode[]
    routes?: ProcessFlowRoute[]
    [key: string]: unknown
  }
  companyOptions: ProcessFormOption[]
  departmentOptions: ProcessFormOption[]
  expenseDetails: ExpenseDetailInstanceSummary[]
  currentTasks: ExpenseApprovalTask[]
  actionLogs: ExpenseApprovalLog[]
  approvalNodeStatuses?: ExpenseApprovalNodeStatus[]
  approvalTimeline?: ExpenseApprovalTimelineItem[]
  relatedDocumentBindings?: ExpenseDocumentRelationBinding[]
  writeOffDocumentBindings?: ExpenseDocumentWriteOffBinding[]
  manualApproverSelectionPending?: boolean
  manualApproverSelectionNodeKey?: string
  manualApproverSelectionNodeName?: string
  manualApproverOptions?: ProcessFormOption[]
  bankPayment?: ExpenseDocumentBankPayment
  bankReceipts?: ExpenseDocumentBankReceipt[]
}

export interface ExpenseDocumentBankPayment {
  bankProvider?: string
  bankChannel?: string
  companyBankAccountName?: string
  paymentStatusCode?: string
  paymentStatusLabel?: string
  manualPaid?: boolean
  paidAt?: string
  receiptStatusLabel?: string
  receiptReceivedAt?: string
  bankFlowNo?: string
  bankOrderNo?: string
  lastErrorMessage?: string
}

export interface ExpenseDocumentBankReceipt {
  attachmentId?: string
  fileName: string
  contentType?: string
  fileSize?: number
  previewUrl?: string
  receivedAt?: string
}

export interface ExpenseDetailInstance {
  detailNo?: string
  detailDesignCode?: string
  detailType?: string
  enterpriseMode?: string
  expenseTypeCode?: string
  businessSceneMode?: string
  detailTitle?: string
  sortOrder?: number
  formData: Record<string, unknown>
}

export interface ExpenseDetailInstanceSummary {
  detailNo: string
  detailDesignCode?: string
  detailType: string
  detailTypeLabel: string
  enterpriseMode?: string
  enterpriseModeLabel?: string
  detailTitle?: string
  sortOrder?: number
  createdAt?: string
}

export interface ExpenseDetailInstanceDetail {
  documentCode: string
  detailNo: string
  detailDesignCode?: string
  detailType: string
  detailTypeLabel: string
  enterpriseMode?: string
  enterpriseModeLabel?: string
  expenseTypeCode?: string
  businessSceneMode?: string
  detailTitle?: string
  sortOrder?: number
  schemaSnapshot: ProcessFormDesignSchema
  formData: Record<string, unknown>
  createdAt?: string
  updatedAt?: string
}

export interface ExpenseRelatedDocumentValue {
  documentCode: string
  documentTitle?: string
  templateType?: string
  templateTypeLabel?: string
  templateName?: string
  status?: string
  statusLabel?: string
}

export interface ExpenseWriteOffDocumentValue extends ExpenseRelatedDocumentValue {
  writeOffSourceKind?: string
  availableWriteOffAmount?: MoneyValue
  writeOffAmount?: MoneyValue
  remainingAmount?: MoneyValue
  effectiveStatus?: string
}

export interface ExpenseDocumentRelationBinding {
  direction: 'OUTBOUND' | 'INBOUND'
  fieldKey?: string
  documentCode: string
  documentTitle?: string
  templateType?: string
  templateTypeLabel?: string
  templateName?: string
  status?: string
  statusLabel?: string
  submitterName?: string
}

export interface ExpenseDocumentWriteOffBinding extends ExpenseDocumentRelationBinding {
  writeOffSourceKind?: string
  requestedAmount?: MoneyValue
  effectiveAmount?: MoneyValue
  remainingAmount?: MoneyValue
  effectiveStatus?: string
  effectiveStatusLabel?: string
}

export interface ExpenseDocumentPickerItem {
  documentCode: string
  documentTitle?: string
  templateType: string
  templateTypeLabel: string
  templateName?: string
  status: string
  statusLabel: string
  totalAmount: MoneyValue
  availableWriteOffAmount?: MoneyValue
  writeOffSourceKind?: string
}

export interface ExpenseDocumentPickerGroup {
  templateType: string
  templateTypeLabel: string
  total: number
  page: number
  pageSize: number
  items: ExpenseDocumentPickerItem[]
}

// 这是 ExpenseDocumentPickerResult 的数据结构。
export interface ExpenseDocumentPickerResult {
  relationType: 'RELATED' | 'WRITEOFF'
  groups: ExpenseDocumentPickerGroup[]
}
