import { computed, ref, watch, type ComputedRef, type Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  expenseApi,
  expenseApprovalApi,
  type ExpenseActionUserOption,
  type ExpenseDocumentDetail,
  type ExpenseDocumentNavigation
} from '@/api'
import { hasPermission } from '@/utils/permissions'
import type { ExpenseDetailActionItem } from '@/views/expense/expenseDetailActionMatrix'

type UserActionMode = 'transfer' | 'add-sign' | ''
type TaskActionMode = 'approve' | 'reject' | ''
type ApprovableTask = NonNullable<ExpenseDocumentDetail['currentTasks']>[number]

type RejectTargetOption = {
  nodeKey: string
  nodeName: string
  optionLabel: string
  isSubmitter?: boolean
}

type UseExpenseDocumentDetailActionOwnerOptions = {
  detail: Ref<ExpenseDocumentDetail | null>
  navigation: Ref<ExpenseDocumentNavigation>
  approvableTasks: ComputedRef<ApprovableTask[]>
  rejectTargetOptions: ComputedRef<RejectTargetOption[]>
  permissionCodes: ComputedRef<string[]>
  buildReturnToQuery: (extraQuery?: Record<string, string>) => Record<string, string>
  loadDetail: () => Promise<void>
  handlePrint: () => void
  navigateDetail: (documentCode?: string) => Promise<void>
  refreshAfterAction: (nextDetail?: ExpenseDocumentDetail) => Promise<void>
  resolveErrorMessage: (error: unknown, fallback: string) => string
}

export function useExpenseDocumentDetailActionOwner(
  options: UseExpenseDocumentDetailActionOwnerOptions
) {
  const route = useRoute()
  const router = useRouter()

  const commentDialogVisible = ref(false)
  const commentSubmitting = ref(false)
  const commentFileInput = ref<HTMLInputElement | null>(null)
  const commentForm = ref({
    comment: '',
    attachmentFileNames: [] as string[]
  })

  const taskActionDialogVisible = ref(false)
  const taskActionMode = ref<TaskActionMode>('')
  const taskActionSubmitting = ref(false)
  const taskActionForm = ref({
    comment: '',
    targetNodeKey: ''
  })

  const manualApproverSubmitting = ref(false)
  const manualApproverForm = ref({
    userIds: [] as number[]
  })

  const userActionDialogVisible = ref(false)
  const userActionMode = ref<UserActionMode>('')
  const userActionSubmitting = ref(false)
  const userOptionsLoading = ref(false)
  const userOptions = ref<ExpenseActionUserOption[]>([])
  const userActionForm = ref({
    targetUserId: undefined as number | undefined,
    remark: ''
  })

  const taskActionDialogTitle = computed(() =>
    taskActionMode.value === 'approve' ? '通过审批' : '驳回审批'
  )
  const taskActionDialogConfirm = computed(() =>
    taskActionMode.value === 'approve' ? '通过' : '驳回'
  )
  const taskActionDialogPlaceholder = computed(() =>
    taskActionMode.value === 'approve' ? '请输入审批意见（可空）' : '请输入驳回原因'
  )
  const userActionDialogTitle = computed(() =>
    userActionMode.value === 'transfer' ? '转交审批任务' : '发起前加签'
  )
  const userActionDialogLabel = computed(() =>
    userActionMode.value === 'transfer' ? '转交给' : '加签人'
  )
  const userActionDialogConfirm = computed(() =>
    userActionMode.value === 'transfer' ? '确认转交' : '确认加签'
  )
  const userActionDialogPlaceholder = computed(() =>
    userActionMode.value === 'transfer' ? '可选填写转交说明' : '可选填写加签说明'
  )

  watch(options.detail, () => {
    manualApproverForm.value = {
      userIds: []
    }
  })

  function openTaskActionDialog(action: 'approve' | 'reject') {
    taskActionMode.value = action
    taskActionForm.value = {
      comment: action === 'approve' ? '通过' : '驳回',
      targetNodeKey:
        action === 'reject' && options.rejectTargetOptions.value.length ? '__SUBMITTER__' : ''
    }
    taskActionDialogVisible.value = true
  }

  function closeTaskActionDialog() {
    taskActionDialogVisible.value = false
    taskActionMode.value = ''
    taskActionForm.value = {
      comment: '',
      targetNodeKey: ''
    }
  }

  async function handleTaskAction(action: 'approve' | 'reject') {
    if (!options.detail.value || !options.approvableTasks.value.length) {
      return
    }
    const permissionCode =
      action === 'approve' ? 'expense:approval:approve' : 'expense:approval:reject'
    if (!hasPermission(permissionCode, options.permissionCodes.value)) {
      ElMessage.warning('当前账号没有处理该审批的权限')
      return
    }
    openTaskActionDialog(action)
  }

  async function submitTaskAction() {
    const action = taskActionMode.value
    const task = options.approvableTasks.value[0]
    if (!action || !task) {
      return
    }
    taskActionSubmitting.value = true
    try {
      const api = action === 'approve' ? expenseApprovalApi.approve : expenseApprovalApi.reject
      const payload = {
        comment: taskActionForm.value.comment || '',
        ...(action === 'reject'
        && taskActionForm.value.targetNodeKey
        && taskActionForm.value.targetNodeKey !== '__SUBMITTER__'
          ? { targetNodeKey: taskActionForm.value.targetNodeKey }
          : {})
      }
      const res = await api(task.id, payload)
      closeTaskActionDialog()
      await options.refreshAfterAction(res.data)
      ElMessage.success(action === 'approve' ? '审批已通过' : '审批已驳回')
    } catch (error: unknown) {
      ElMessage.error(
        options.resolveErrorMessage(error, action === 'approve' ? '审批通过失败' : '审批驳回失败')
      )
    } finally {
      taskActionSubmitting.value = false
    }
  }

  async function submitManualApproverSelection() {
    const documentCode = options.detail.value?.documentCode || ''
    const nodeKey = options.detail.value?.manualApproverSelectionNodeKey || ''
    if (!documentCode || !nodeKey) {
      return
    }
    if (!manualApproverForm.value.userIds.length) {
      ElMessage.warning('请至少选择一位审批人')
      return
    }
    manualApproverSubmitting.value = true
    try {
      await expenseApi.submitManualApproverSelection(documentCode, {
        nodeKey,
        userIds: manualApproverForm.value.userIds
      })
      ElMessage.success('手动审批人已提交')
      await options.loadDetail()
    } catch (error: unknown) {
      ElMessage.error(options.resolveErrorMessage(error, '提交手动审批人失败'))
    } finally {
      manualApproverSubmitting.value = false
    }
  }

  async function handleActionClick(action: ExpenseDetailActionItem) {
    if (action.disabled) {
      ElMessage.warning(action.reason || '当前动作暂不可用')
      return
    }

    switch (action.key) {
      case 'resubmit':
        await openResubmitPage()
        return
      case 'recall':
        await handleRecall()
        return
      case 'print':
        options.handlePrint()
        return
      case 'download':
        ElMessage.info('功能建设中')
        return
      case 'comment':
        openCommentDialog()
        return
      case 'remind':
        await handleRemind()
        return
      case 'approve':
        await handleTaskAction('approve')
        return
      case 'reject':
        await handleTaskAction('reject')
        return
      case 'prev':
        await options.navigateDetail(options.navigation.value.prevDocumentCode)
        return
      case 'next':
        await options.navigateDetail(options.navigation.value.nextDocumentCode)
        return
      case 'modify':
        await openModifyPage()
        return
      case 'add-sign':
      case 'transfer':
        await openUserActionDialog(action.key)
        return
    }
  }

  async function handleRecall() {
    if (!options.detail.value) {
      return
    }
    try {
      await ElMessageBox.confirm(
        '召回后会回到草稿编辑页，并沿用当前单号重新提交，确认继续吗？',
        '召回单据',
        {
          type: 'warning',
          confirmButtonText: '确认召回',
          cancelButtonText: '取消'
        }
      )
      await expenseApi.recall(options.detail.value.documentCode)
      ElMessage.success('单据已召回，正在进入重提编辑页')
      await router.push({
        name: 'expense-document-resubmit',
        params: { documentCode: options.detail.value.documentCode },
        query: options.buildReturnToQuery()
      })
    } catch (error: unknown) {
      if (error === 'cancel' || String(error).includes('cancel')) {
        return
      }
      ElMessage.error(options.resolveErrorMessage(error, '召回单据失败'))
    }
  }

  async function handleRemind() {
    if (!options.detail.value) {
      return
    }
    try {
      const { value } = await ElMessageBox.prompt('可选填写催办备注', '催办审批', {
        inputType: 'textarea',
        inputPlaceholder: '例如：这笔单据今天需要完成处理',
        confirmButtonText: '发送催办',
        cancelButtonText: '取消'
      })
      const res = await expenseApi.remind(options.detail.value.documentCode, { remark: value || '' })
      await options.refreshAfterAction(res.data)
      ElMessage.success('已向当前审批人发送催办')
    } catch (error: unknown) {
      if (error === 'cancel' || String(error).includes('cancel')) {
        return
      }
      ElMessage.error(options.resolveErrorMessage(error, '催办失败'))
    }
  }

  function openCommentDialog() {
    commentForm.value = {
      comment: '',
      attachmentFileNames: []
    }
    commentDialogVisible.value = true
  }

  async function submitComment() {
    if (!options.detail.value) {
      return
    }
    if (!commentForm.value.comment.trim() && commentForm.value.attachmentFileNames.length === 0) {
      ElMessage.warning('请先输入评论或添加附件名')
      return
    }
    commentSubmitting.value = true
    try {
      const res = await expenseApi.comment(options.detail.value.documentCode, {
        comment: commentForm.value.comment.trim(),
        attachmentFileNames: commentForm.value.attachmentFileNames
      })
      commentDialogVisible.value = false
      await options.refreshAfterAction(res.data)
      ElMessage.success('评论已发布')
    } catch (error: unknown) {
      ElMessage.error(options.resolveErrorMessage(error, '发表评论失败'))
    } finally {
      commentSubmitting.value = false
    }
  }

  function pickCommentFiles() {
    commentFileInput.value?.click()
  }

  function handleCommentFileChange(event: Event) {
    const target = event.target as HTMLInputElement
    const files = Array.from(target.files || [])
    if (files.length === 0) {
      return
    }
    const merged = new Set([
      ...commentForm.value.attachmentFileNames,
      ...files.map((file) => file.name).filter(Boolean)
    ])
    commentForm.value.attachmentFileNames = Array.from(merged)
    target.value = ''
  }

  function removeCommentAttachment(name: string) {
    commentForm.value.attachmentFileNames = commentForm.value.attachmentFileNames.filter(
      (item) => item !== name
    )
  }

  async function openModifyPage() {
    const task = options.approvableTasks.value[0]
    if (!task) {
      ElMessage.warning('当前没有可修改的待办任务')
      return
    }
    await router.push({
      name: 'expense-approval-task-modify',
      params: { taskId: task.id },
      query: options.buildReturnToQuery()
    })
  }

  async function openUserActionDialog(actionKey: 'add-sign' | 'transfer') {
    const task = options.approvableTasks.value[0]
    if (!task) {
      ElMessage.warning('当前没有可处理的待办任务')
      return
    }
    userActionMode.value = actionKey
    userActionForm.value = {
      targetUserId: undefined,
      remark: ''
    }
    userActionDialogVisible.value = true
    await loadActionUsers('')
  }

  function closeUserActionDialog() {
    userActionDialogVisible.value = false
    userActionMode.value = ''
    userActionForm.value = {
      targetUserId: undefined,
      remark: ''
    }
  }

  async function loadActionUsers(keyword: string) {
    userOptionsLoading.value = true
    try {
      const res = await expenseApprovalApi.listActionUsers(keyword)
      userOptions.value = res.data
      return res.data
    } finally {
      userOptionsLoading.value = false
    }
  }

  async function submitUserAction() {
    const task = options.approvableTasks.value[0]
    if (!task) {
      ElMessage.warning('当前没有可处理的待办任务')
      return
    }
    if (!userActionForm.value.targetUserId) {
      ElMessage.warning('请先选择目标处理人')
      return
    }
    userActionSubmitting.value = true
    try {
      const payload = {
        targetUserId: userActionForm.value.targetUserId,
        remark: userActionForm.value.remark.trim()
      }
      const res = userActionMode.value === 'transfer'
        ? await expenseApprovalApi.transfer(task.id, payload)
        : await expenseApprovalApi.addSign(task.id, payload)
      closeUserActionDialog()
      await options.refreshAfterAction(res.data)
      ElMessage.success(userActionMode.value === 'transfer' ? '审批任务已转交' : '已发起加签')
    } catch (error: unknown) {
      ElMessage.error(
        options.resolveErrorMessage(
          error,
          userActionMode.value === 'transfer' ? '转交审批失败' : '加签失败'
        )
      )
    } finally {
      userActionSubmitting.value = false
    }
  }

  async function openResubmitPage() {
    const documentCode = options.detail.value?.documentCode || String(route.params.documentCode || '')
    if (!documentCode) {
      ElMessage.warning('缺少单据编码，无法打开编辑页')
      return
    }
    await router.push({
      path: `/expense/documents/${encodeURIComponent(documentCode)}/resubmit`,
      query: options.buildReturnToQuery(options.detail.value?.status === 'DRAFT' ? { entry: 'draft' } : {})
    })
  }

  return {
    commentDialogVisible,
    commentSubmitting,
    commentFileInput,
    commentForm,
    taskActionDialogVisible,
    taskActionMode,
    taskActionSubmitting,
    taskActionForm,
    manualApproverSubmitting,
    manualApproverForm,
    userActionDialogVisible,
    userActionMode,
    userActionSubmitting,
    userOptionsLoading,
    userOptions,
    userActionForm,
    taskActionDialogTitle,
    taskActionDialogConfirm,
    taskActionDialogPlaceholder,
    userActionDialogTitle,
    userActionDialogLabel,
    userActionDialogConfirm,
    userActionDialogPlaceholder,
    closeTaskActionDialog,
    closeUserActionDialog,
    submitManualApproverSelection,
    handleActionClick,
    submitTaskAction,
    submitComment,
    pickCommentFiles,
    handleCommentFileChange,
    removeCommentAttachment,
    loadActionUsers,
    submitUserAction
  }
}
