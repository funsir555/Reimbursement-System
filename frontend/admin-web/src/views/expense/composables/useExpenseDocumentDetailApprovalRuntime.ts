import { computed, type ComputedRef, type Ref } from 'vue'
import type {
  ExpenseApprovalNodeStatus,
  ExpenseApprovalTimelineItem,
  ExpenseDocumentDetail,
  ExpenseDocumentNavigation,
  ProcessFlowNode,
  ProcessFormOption
} from '@/api'
import {
  resolveDisabledExpenseDetailActionHint,
  resolveExpenseDetailActions,
  type ExpenseDetailActionItem
} from '@/views/expense/expenseDetailActionMatrix'

type RejectTargetOption = {
  nodeKey: string
  nodeName: string
  optionLabel: string
  isSubmitter?: boolean
}

type ApprovableTask = NonNullable<ExpenseDocumentDetail['currentTasks']>[number]

type UseExpenseDocumentDetailApprovalRuntimeOptions = {
  detail: Ref<ExpenseDocumentDetail | null>
  navigation: Ref<ExpenseDocumentNavigation>
  currentUserId: ComputedRef<number>
  canApprovalView: ComputedRef<boolean>
}

export function useExpenseDocumentDetailApprovalRuntime(
  options: UseExpenseDocumentDetailApprovalRuntimeOptions
) {
  const approvableTasks = computed<ApprovableTask[]>(() =>
    (options.detail.value?.currentTasks || []).filter(
      (task) => task.assigneeUserId === options.currentUserId.value && task.nodeType === 'APPROVAL'
    )
  )

  const currentApprovalNode = computed<ProcessFlowNode | null>(() => {
    const taskNodeKey = approvableTasks.value[0]?.nodeKey || options.detail.value?.currentNodeKey || ''
    if (!taskNodeKey) {
      return null
    }
    const flowNodes = Array.isArray(options.detail.value?.flowSnapshot?.nodes)
      ? options.detail.value?.flowSnapshot?.nodes || []
      : []
    return flowNodes.find((node) => node.nodeKey === taskNodeKey) || null
  })

  const currentApprovalSpecialSettings = computed(
    () =>
      new Set(
        Array.isArray(currentApprovalNode.value?.config?.specialSettings)
          ? currentApprovalNode.value?.config?.specialSettings || []
          : []
      )
  )

  const canModifyCurrentTask = computed(
    () =>
      currentApprovalSpecialSettings.value.has('ALLOW_EDIT_FORM_MODULE')
      || currentApprovalSpecialSettings.value.has('ALLOW_EDIT_PAY_ACCOUNT')
  )

  const rejectTargetOptions = computed<RejectTargetOption[]>(() => {
    if (!currentApprovalSpecialSettings.value.has('REJECT_TO_ANY_NODE')) {
      return []
    }
    const currentNodeKey = currentApprovalNode.value?.nodeKey || options.detail.value?.currentNodeKey || ''
    const baseOptions: RejectTargetOption[] = [
      {
        nodeKey: '__SUBMITTER__',
        nodeName: '驳回到提单人',
        optionLabel: formatRejectTargetLabel('驳回到提单人', options.detail.value?.submitterName),
        isSubmitter: true
      }
    ]
    const upstreamApprovalNodes = (options.detail.value?.approvalNodeStatuses || [])
      .filter(
        (item) =>
          item.nodeType === 'APPROVAL'
          && item.nodeKey !== currentNodeKey
          && item.status !== 'NOT_REACHED'
          && item.status !== 'PENDING'
          && item.status !== 'MANUAL_SELECTION_PENDING'
      )
      .map((item) => ({
        nodeKey: item.nodeKey,
        nodeName: item.nodeName || item.nodeKey,
        optionLabel: formatRejectTargetLabel(item.nodeName || item.nodeKey, item.assigneeNames)
      }))
    return [...baseOptions, ...upstreamApprovalNodes]
  })

  const isSubmitter = computed(
    () => options.detail.value?.submitterUserId === options.currentUserId.value
  )
  const isManualApproverSelectionPending = computed(() =>
    Boolean(options.detail.value?.manualApproverSelectionPending)
  )
  const canSubmitManualApproverSelection = computed(
    () => isSubmitter.value && isManualApproverSelectionPending.value
  )
  const manualApproverOptions = computed<ProcessFormOption[]>(
    () => options.detail.value?.manualApproverOptions || []
  )
  const isActiveApprover = computed(() => approvableTasks.value.length > 0)
  const canResubmitEdit = computed(() => {
    const status = options.detail.value?.status || ''
    return isSubmitter.value && (status === 'DRAFT' || status === 'REJECTED')
  })
  const isFlowParticipant = computed(() => {
    if (!options.detail.value) {
      return false
    }
    if (isSubmitter.value || isActiveApprover.value) {
      return true
    }
    const userId = options.currentUserId.value
    return options.detail.value.actionLogs.some((log) => {
      if (log.actorUserId === userId) {
        return true
      }
      const approverUserIds = Array.isArray(log.payload?.approverUserIds)
        ? log.payload.approverUserIds
        : []
      return approverUserIds.some((item) => Number(item) === userId)
    })
  })
  const canComment = computed(() => isSubmitter.value || isFlowParticipant.value)

  const statusBucket = computed<'pending' | 'exception' | 'terminal' | 'other'>(() => {
    const status = options.detail.value?.status || ''
    if (status === 'PENDING_APPROVAL') {
      return 'pending'
    }
    if (status === 'EXCEPTION') {
      return 'exception'
    }
    if (
      status === 'APPROVED'
      || status === 'COMPLETED'
      || status === 'PAID'
      || status === 'PENDING_PAYMENT'
      || status === 'PAYING'
      || status === 'PAYMENT_COMPLETED'
      || status === 'PAYMENT_FINISHED'
      || status === 'PAYMENT_EXCEPTION'
    ) {
      return 'terminal'
    }
    return 'other'
  })

  const approvalNodeStatuses = computed<ExpenseApprovalNodeStatus[]>(
    () => options.detail.value?.approvalNodeStatuses || []
  )
  const approvalTimelineItems = computed<ExpenseApprovalTimelineItem[]>(
    () => options.detail.value?.approvalTimeline || []
  )
  const actionItems = computed<ExpenseDetailActionItem[]>(() => {
    if (!options.detail.value) {
      return []
    }
    return resolveExpenseDetailActions({
      statusBucket: statusBucket.value,
      isSubmitter: isSubmitter.value,
      canResubmitEdit: canResubmitEdit.value,
      isActiveApprover: isActiveApprover.value,
      canModify: canModifyCurrentTask.value,
      isFlowParticipant: isFlowParticipant.value,
      canComment: canComment.value,
      canApprovalView: options.canApprovalView.value,
      prevDocumentCode: options.navigation.value.prevDocumentCode,
      nextDocumentCode: options.navigation.value.nextDocumentCode
    })
  })

  const secondaryActionItems = computed(() => actionItems.value.filter((item) => !item.primary))
  const primaryActionItems = computed(() => actionItems.value.filter((item) => item.primary))
  const disabledActionHint = computed(() => resolveDisabledExpenseDetailActionHint(actionItems.value))

  function approvalStatusLabel(status?: string) {
    const labels: Record<string, string> = {
      NOT_REACHED: '未到达',
      PENDING: '审批中',
      MANUAL_SELECTION_PENDING: '待手动选择审批人',
      APPROVED: '已通过',
      REJECTED: '已驳回',
      AUTO_SKIPPED: '已自动跳过',
      EXCEPTION: '异常',
      PAYMENT_PENDING: '待支付',
      PAYMENT_COMPLETED: '已支付',
      PAYMENT_EXCEPTION: '支付异常'
    }
    return labels[status || ''] || '处理中'
  }

  function approvalStatusTagType(status?: string) {
    switch (status) {
      case 'PENDING':
      case 'PAYMENT_PENDING':
      case 'MANUAL_SELECTION_PENDING':
        return 'warning'
      case 'APPROVED':
      case 'PAYMENT_COMPLETED':
        return 'success'
      case 'REJECTED':
      case 'EXCEPTION':
      case 'PAYMENT_EXCEPTION':
        return 'danger'
      case 'AUTO_SKIPPED':
        return 'info'
      default:
        return 'info'
    }
  }

  return {
    approvableTasks,
    rejectTargetOptions,
    isSubmitter,
    isManualApproverSelectionPending,
    canSubmitManualApproverSelection,
    manualApproverOptions,
    approvalNodeStatuses,
    approvalTimelineItems,
    actionItems,
    secondaryActionItems,
    primaryActionItems,
    disabledActionHint,
    approvalStatusLabel,
    approvalStatusTagType
  }
}

function formatRejectTargetLabel(nodeName: string, assigneeNames?: string[] | string) {
  const names = Array.isArray(assigneeNames)
    ? assigneeNames.filter((item) => Boolean(String(item || '').trim()))
    : [String(assigneeNames || '').trim()].filter(Boolean)
  if (!names.length) {
    return nodeName
  }
  return `${nodeName}（${names.join('、')}）`
}
