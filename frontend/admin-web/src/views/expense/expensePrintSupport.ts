import type { LocationQuery, Router } from 'vue-router'
import { expenseApi, type ExpenseApprovalLog, type ExpenseApprovalTask, type ExpenseDetailInstanceDetail, type ExpenseDocumentDetail, type ProcessFormDesignSchema } from '@/api'

export type ExpensePrintEnvironment = 'browser' | 'wecom' | 'feishu' | 'dingtalk'

export interface ExpenseDocumentPrintBundle {
  detail: ExpenseDocumentDetail
  expenseDetails: ExpenseDetailInstanceDetail[]
}

export interface ExpensePrintTimelineItem {
  key: string
  timestamp: string
  title: string
  description: string
}

const PRINT_POPUP_FEATURES = 'noopener,noreferrer'

export function isExpenseDetailPrintMode(query: LocationQuery) {
  return String(query.print || '') === '1'
}

export function normalizeExpensePrintDocumentCodes(raw: unknown) {
  const values = Array.isArray(raw) ? raw : [raw]
  return values
    .flatMap((item) => String(item || '').split(','))
    .map((item) => item.trim())
    .filter(Boolean)
}

export function detectExpensePrintEnvironment(userAgent = navigator?.userAgent || ''): ExpensePrintEnvironment {
  const normalized = userAgent.toLowerCase()
  if (normalized.includes('wxwork') || normalized.includes('wecom')) {
    return 'wecom'
  }
  if (normalized.includes('lark') || normalized.includes('feishu')) {
    return 'feishu'
  }
  if (normalized.includes('dingtalk')) {
    return 'dingtalk'
  }
  return 'browser'
}

export function buildExpenseDetailPrintHref(router: Router, documentCode: string) {
  return router.resolve({
    name: 'expense-document-print',
    params: { documentCode }
  }).href
}

export function buildExpenseBatchPrintHref(router: Router, documentCodes: string[], source = 'payment-pending') {
  return router.resolve({
    name: 'expense-document-batch-print',
    query: {
      documentCodes: documentCodes.join(','),
      source
    }
  }).href
}

export function openExpensePrintWindow(href: string) {
  const openedWindow = window.open(href, '_blank', PRINT_POPUP_FEATURES)
  if (openedWindow) {
    try {
      openedWindow.opener = null
    } catch {
      // Ignore opener assignment failures in strict browser contexts.
    }
  }
  return openedWindow
}

export async function loadExpenseDocumentPrintBundle(documentCode: string): Promise<ExpenseDocumentPrintBundle> {
  const detailRes = await expenseApi.getDetail(documentCode)
  const detail = detailRes.data
  const expenseDetails = await Promise.all(
    (detail.expenseDetails || []).map(async (item) => {
      const res = await expenseApi.getExpenseDetail(detail.documentCode, item.detailNo)
      return res.data
    })
  )
  return {
    detail,
    expenseDetails
  }
}

export function collectPrintSchemas(bundles: ExpenseDocumentPrintBundle[]) {
  return bundles.flatMap((bundle) => [
    bundle.detail.formSchemaSnapshot,
    ...bundle.expenseDetails.map((item) => item.schemaSnapshot)
  ]).filter(Boolean) as ProcessFormDesignSchema[]
}

export function resolvePrintExpenseDetailTypeLabel(detailType?: string, fallback?: string) {
  if (detailType === 'ENTERPRISE_TRANSACTION') return '企业往来'
  if (detailType === 'NORMAL_REIMBURSEMENT') return '普通报销'
  return fallback || '费用明细'
}

export function buildPrintTimelineItems(detail: ExpenseDocumentDetail): ExpensePrintTimelineItem[] {
  const logItems = (detail.actionLogs || [])
    .filter(shouldDisplayTimelineLog)
    .map((log, index) => ({
      key: `log-${log.id ?? index}`,
      timestamp: log.createdAt || '',
      title: timelineTitle(log, detail),
      description: timelineDescription(log)
    }))
  return logItems.concat(buildPendingTimelineItems(detail.currentTasks || []))
}

function timelineTitle(log: ExpenseApprovalLog, documentDetail?: ExpenseDocumentDetail | null) {
  const actorName = log.actorName || '审批人'
  const nodeName = asString(log.nodeName) || '节点'
  const actionMap: Record<string, string> = {
    SUBMIT: `${asString(documentDetail?.submitterName) || asString(log.actorName) || '提单人'} 提交单据`,
    RECALL: `${actorName} 召回单据`,
    RESUBMIT: `${actorName} 重新提交`,
    APPROVE: `${nodeName} ${actorName} 审批通过`,
    REJECT: `${nodeName} ${actorName} 审批驳回`,
    MODIFY: `${actorName} 修改单据`,
    COMMENT: `${actorName} 发表评论`,
    REMIND: `${actorName} 发起催办`,
    TRANSFER: `${actorName} 转交审批`,
    ADD_SIGN: `${actorName} 发起加签`,
    PAYMENT_START: `${actorName} 发起支付`,
    PAYMENT_COMPLETE: `${actorName} 确认已支付`,
    PAYMENT_EXCEPTION: `${actorName} 标记支付异常`,
    FINISH: '审批完成',
    EXCEPTION: '流程异常'
  }
  return actionMap[log.actionType] || log.actionType
}

function timelineDescription(log: ExpenseApprovalLog) {
  if (log.actionType === 'COMMENT') {
    return String(log.payload?.comment || log.actionComment || '')
  }
  if (['SUBMIT', 'APPROVE', 'REJECT', 'PAYMENT_COMPLETE', 'PAYMENT_EXCEPTION'].includes(log.actionType)) {
    const comment = asString(log.actionComment)
    return isRedundantTimelineComment(log.actionType, comment) ? '' : comment
  }
  const parts = [asString(log.actionComment)]
  if (log.nodeName && !['APPROVE', 'REJECT', 'RECALL', 'RESUBMIT', 'COMMENT'].includes(log.actionType)) {
    parts.unshift(log.nodeName)
  }
  if (log.actionType === 'TRANSFER' && log.payload?.targetUserName) {
    parts.push(`转交给 ${String(log.payload.targetUserName)}`)
  }
  if (log.actionType === 'ADD_SIGN' && log.payload?.targetUserName) {
    parts.push(`加签给 ${String(log.payload.targetUserName)}`)
  }
  return parts.filter(Boolean).join(' / ')
}

function buildPendingTimelineItems(tasks: ExpenseApprovalTask[]): ExpensePrintTimelineItem[] {
  const deduped = new Map<string, ExpensePrintTimelineItem>()
  tasks.forEach((task, index) => {
    const nodeName = asString(task.nodeName) || '节点'
    const assigneeName = asString(task.assigneeName) || '待分配处理人'
    const dedupeKey = `${asString(task.nodeKey) || 'pending'}::${assigneeName}`
    if (deduped.has(dedupeKey)) {
      return
    }
    deduped.set(dedupeKey, {
      key: `pending-${task.id ?? index}-${dedupeKey}`,
      timestamp: task.createdAt || '',
      title: task.nodeType === 'PAYMENT'
        ? `${assigneeName} 待支付`
        : `${nodeName} ${assigneeName} 审批中`,
      description: ''
    })
  })
  return Array.from(deduped.values())
}

function shouldDisplayTimelineLog(log: ExpenseApprovalLog) {
  return [
    'SUBMIT',
    'RECALL',
    'RESUBMIT',
    'APPROVE',
    'REJECT',
    'MODIFY',
    'COMMENT',
    'TRANSFER',
    'ADD_SIGN',
    'PAYMENT_START',
    'PAYMENT_COMPLETE',
    'PAYMENT_EXCEPTION',
    'FINISH',
    'EXCEPTION'
  ].includes(log.actionType)
}

function isRedundantTimelineComment(actionType: string, comment: string) {
  if (!comment) {
    return false
  }
  const normalized = comment.replace(/\s+/g, '')
  const redundantByAction: Record<string, string[]> = {
    APPROVE: ['通过', '审批通过', '同意'],
    REJECT: ['驳回', '审批驳回'],
    PAYMENT_COMPLETE: ['已支付', '确认已支付'],
    PAYMENT_EXCEPTION: ['支付异常', '标记支付异常']
  }
  return (redundantByAction[actionType] || []).includes(normalized)
}

function asString(value: unknown) {
  return typeof value === 'string' ? value.trim() : ''
}
