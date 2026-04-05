export type ExpenseDetailActionKey =
  | 'recall'
  | 'print'
  | 'comment'
  | 'download'
  | 'remind'
  | 'approve'
  | 'reject'
  | 'prev'
  | 'next'
  | 'modify'
  | 'add-sign'
  | 'transfer'

export type ExpenseDetailActionItem = {
  key: ExpenseDetailActionKey
  label: string
  primary?: boolean
  type?: 'primary' | 'success' | 'danger' | 'warning'
  disabled?: boolean
  reason?: string
}

export type ExpenseDetailStatusBucket = 'pending' | 'exception' | 'terminal' | 'other'

export type ResolveExpenseDetailActionsInput = {
  statusBucket: ExpenseDetailStatusBucket
  isSubmitter: boolean
  isActiveApprover: boolean
  isFlowParticipant: boolean
  canComment: boolean
  canApprovalView: boolean
  prevDocumentCode?: string
  nextDocumentCode?: string
}

const EXCEPTION_DISABLED_REASON = '异常单据需由发起人召回后重提，当前审批处理动作已锁定'

function primaryAction(
  key: ExpenseDetailActionKey,
  label: string,
  type: ExpenseDetailActionItem['type'] = 'primary',
  disabled = false,
  reason = ''
): ExpenseDetailActionItem {
  return { key, label, primary: true, type, disabled, reason }
}

function secondaryAction(
  key: ExpenseDetailActionKey,
  label: string,
  disabled = false,
  reason = ''
): ExpenseDetailActionItem {
  return { key, label, disabled, reason }
}

export function resolveExpenseDetailActions(input: ResolveExpenseDetailActionsInput): ExpenseDetailActionItem[] {
  const viewerActions = (allowCommentAction: boolean): ExpenseDetailActionItem[] => [
    secondaryAction('print', '打印'),
    ...(allowCommentAction ? [secondaryAction('comment', '评论')] : []),
    secondaryAction('download', '下载')
  ]

  if (input.isSubmitter) {
    if (input.statusBucket === 'pending') {
      return [
        primaryAction('recall', '召回'),
        ...viewerActions(input.canComment),
        secondaryAction('remind', '催办')
      ]
    }
    if (input.statusBucket === 'exception') {
      return [primaryAction('recall', '召回')]
    }
    if (input.statusBucket === 'terminal') {
      return viewerActions(input.canComment)
    }
  }

  if (input.statusBucket === 'pending' && input.isActiveApprover) {
    return [
      primaryAction('approve', '通过', 'success'),
      primaryAction('reject', '驳回', 'danger'),
      secondaryAction('prev', '上一单', !input.prevDocumentCode),
      secondaryAction('next', '下一单', !input.nextDocumentCode),
      secondaryAction('modify', '修改'),
      secondaryAction('print', '打印'),
      secondaryAction('add-sign', '加签'),
      secondaryAction('transfer', '转交'),
      ...(input.canComment ? [secondaryAction('comment', '评论')] : []),
      secondaryAction('download', '下载')
    ]
  }

  if (input.statusBucket === 'exception' && input.canApprovalView && input.isFlowParticipant) {
    return [
      primaryAction('approve', '通过', 'success', true, EXCEPTION_DISABLED_REASON),
      primaryAction('reject', '驳回', 'danger', true, EXCEPTION_DISABLED_REASON),
      secondaryAction('prev', '上一单', !input.prevDocumentCode),
      secondaryAction('next', '下一单', !input.nextDocumentCode),
      secondaryAction('modify', '修改', true, EXCEPTION_DISABLED_REASON),
      secondaryAction('print', '打印'),
      secondaryAction('add-sign', '加签', true, EXCEPTION_DISABLED_REASON),
      secondaryAction('transfer', '转交', true, EXCEPTION_DISABLED_REASON),
      ...(input.canComment ? [secondaryAction('comment', '评论')] : []),
      secondaryAction('download', '下载')
    ]
  }

  if (['pending', 'exception', 'terminal'].includes(input.statusBucket)) {
    return viewerActions(input.canComment)
  }

  return []
}

export function resolveDisabledExpenseDetailActionHint(actionItems: ExpenseDetailActionItem[]): string {
  return actionItems.find((item) => item.disabled && item.reason)?.reason || ''
}
