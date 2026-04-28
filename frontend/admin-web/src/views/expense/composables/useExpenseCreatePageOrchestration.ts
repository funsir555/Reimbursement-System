import { ElMessage } from 'element-plus'
import { computed, type ComputedRef, type Ref } from 'vue'
import {
  expenseApi,
  expenseApprovalApi,
  expenseCreateApi,
  type ExpenseCreateTemplateDetail,
  type ExpenseDocumentEditContext,
  type ExpenseDocumentSubmitPayload,
  type ExpenseDocumentSubmitResult,
  type ExpenseDocumentUpdatePayload,
  type ExpenseManualApproverPreview,
  type ProcessFormDesignSchema
} from '@/api'
import { hasPermission, resolveFirstAccessiblePath } from '@/utils/permissions'
import { validateExpenseRuntimeSchema } from '@/views/process/pmValidation'

type PageMode = 'create' | 'resubmit' | 'modify'

type RouteLike = {
  query: Record<string, unknown>
}

type RouterLike = {
  back: () => Promise<unknown> | unknown
  push: (to: string | Record<string, unknown>) => Promise<unknown> | unknown
  replace: (to: string | Record<string, unknown>) => Promise<unknown> | unknown
}

type RuntimeEditorLike = {
  validateBeforeSubmit?: () => boolean
}

type UseExpenseCreatePageOrchestrationOptions = {
  route: RouteLike
  router: RouterLike
  pageMode: ComputedRef<PageMode>
  permissionCodes: Ref<string[]>
  currentDraftKey: Ref<string>
  selectedTemplateCode: Ref<string>
  templateDetail: Ref<ExpenseCreateTemplateDetail | null>
  editingDocumentCode: ComputedRef<string>
  modifyingTaskId: ComputedRef<number>
  emptySchema: ProcessFormDesignSchema
  runtimeEditorRef: Ref<RuntimeEditorLike | null>
  expenseDetailsCount: ComputedRef<number>
  isReportTemplate: ComputedRef<boolean>
  isDraftEditEntry: ComputedRef<boolean>
  useServerDraftSave: ComputedRef<boolean>
  savingDraft: Ref<boolean>
  submitting: Ref<boolean>
  resetCreateSelectionState: () => void
  clearDraft: () => void
  persistDraft: (options?: { includeTemplateDetail?: boolean }) => void
  buildDocumentUpdatePayload: () => ExpenseDocumentUpdatePayload
  applyEditContextState: (context: ExpenseDocumentEditContext) => void
  restoreManualApproverSelections: (source?: Record<string, string[]>) => void
  cloneManualApproverSelections: () => Record<string, string[]>
  validateExpenseDetailAmountValues: () => string
  validateExpenseDetailRequiredValues: () => string
  validateExpenseDetailBusinessScenarios: () => string
  resolveErrorMessage: (error: unknown, fallback: string) => string
  loadManualApproverPreview: () => Promise<ExpenseManualApproverPreview | null>
  openManualApproverDialog: (preview: ExpenseManualApproverPreview) => void
}

export function useExpenseCreatePageOrchestration(options: UseExpenseCreatePageOrchestrationOptions) {
  const submitButtonLabel = computed(() => {
    if (options.pageMode.value === 'resubmit') {
      return options.isDraftEditEntry.value ? '提交审批单' : '重新提交审批单'
    }
    if (options.pageMode.value === 'modify') {
      return '保存修改'
    }
    return '提交审批单'
  })

  const backButtonLabel = computed(() => {
    if (options.pageMode.value === 'create') {
      return '返回我的报销'
    }
    return '返回单据详情'
  })

  function reselectTemplate() {
    if (options.pageMode.value !== 'create') {
      return
    }
    options.clearDraft()
    options.resetCreateSelectionState()
    void options.router.replace({ name: 'expense-create', query: {} })
  }

  function resolveReturnToPath() {
    return typeof options.route.query.returnTo === 'string' && options.route.query.returnTo.trim()
      ? options.route.query.returnTo.trim()
      : ''
  }

  async function navigateBackWithFallback(fallbackPath = '') {
    const returnTo = resolveReturnToPath()
    if (returnTo) {
      await options.router.push(returnTo)
      return
    }
    if (window.history.length > 1) {
      await options.router.back()
      return
    }
    if (fallbackPath) {
      await options.router.push(fallbackPath)
      return
    }
    await options.router.push('/expense/list')
  }

  function goBack() {
    if (options.pageMode.value === 'create') {
      reselectTemplate()
      return
    }
    void navigateBackWithFallback(
      options.editingDocumentCode.value
        ? `/expense/documents/${encodeURIComponent(options.editingDocumentCode.value)}`
        : ''
    )
  }

  function goBackToList() {
    if (hasPermission('expense:list:view', options.permissionCodes.value)) {
      void options.router.push('/expense/list')
      return
    }
    const fallbackPath = resolveFirstAccessiblePath(options.permissionCodes.value)
    if (fallbackPath && fallbackPath !== '/expense/create') {
      void options.router.push(fallbackPath)
      return
    }
    void options.router.push('/dashboard')
  }

  function validateBeforePersist() {
    if (!options.currentDraftKey.value || !options.selectedTemplateCode.value || !options.templateDetail.value) {
      ElMessage.warning('当前页面尚未准备好，暂时无法保存草稿')
      return false
    }
    const expenseDetailAmountIssue = options.validateExpenseDetailAmountValues()
    if (expenseDetailAmountIssue) {
      ElMessage.warning(expenseDetailAmountIssue)
      return false
    }
    return true
  }

  async function saveDraftManually() {
    if (!validateBeforePersist()) {
      return
    }
    options.persistDraft({ includeTemplateDetail: true })
    if (options.pageMode.value === 'create') {
      await createServerDraft()
      return
    }
    if (!options.useServerDraftSave.value) {
      ElMessage.success('草稿已保存')
      return
    }
    await saveExistingServerDraft()
  }

  async function createServerDraft() {
    options.savingDraft.value = true
    try {
      const payload: ExpenseDocumentSubmitPayload = {
        templateCode: options.selectedTemplateCode.value,
        ...options.buildDocumentUpdatePayload()
      }
      const response = await expenseCreateApi.createDraft(payload)
      options.clearDraft()
      ElMessage.success('草稿已保存')
      await options.router.replace(
        `/expense/documents/${encodeURIComponent(response.data.documentCode)}/resubmit?entry=draft`
      )
    } catch (error: unknown) {
      ElMessage.error(options.resolveErrorMessage(error, '保存草稿失败'))
    } finally {
      options.savingDraft.value = false
    }
  }

  async function saveExistingServerDraft() {
    options.savingDraft.value = true
    try {
      const manualSelections = options.cloneManualApproverSelections()
      const response = await expenseApi.saveDraft(
        options.editingDocumentCode.value,
        options.buildDocumentUpdatePayload()
      )
      options.applyEditContextState(response.data)
      options.restoreManualApproverSelections(manualSelections)
      options.persistDraft({ includeTemplateDetail: true })
      ElMessage.success('草稿已保存')
    } catch (error: unknown) {
      ElMessage.error(options.resolveErrorMessage(error, '保存草稿失败'))
    } finally {
      options.savingDraft.value = false
    }
  }

  function validateBeforeSubmit() {
    if (!options.selectedTemplateCode.value || !options.templateDetail.value) {
      ElMessage.warning('请先选择模板')
      return false
    }
    const runtimeSchemaIssues = validateExpenseRuntimeSchema(options.templateDetail.value.schema || options.emptySchema)
    if (runtimeSchemaIssues.length) {
      ElMessage.warning(runtimeSchemaIssues[0])
      return false
    }
    if (options.runtimeEditorRef.value?.validateBeforeSubmit && !options.runtimeEditorRef.value.validateBeforeSubmit()) {
      return false
    }
    if (options.isReportTemplate.value) {
      if (options.expenseDetailsCount.value === 0) {
        ElMessage.warning('报销单提交前至少需要 1 份费用明细')
        return false
      }
      if (options.expenseDetailsCount.value > 10) {
        ElMessage.warning('费用明细最多只能添加 10 份')
        return false
      }
    }
    const expenseDetailRequiredIssue = options.validateExpenseDetailRequiredValues()
    if (expenseDetailRequiredIssue) {
      ElMessage.warning(expenseDetailRequiredIssue)
      return false
    }
    const expenseDetailScenarioIssue = options.validateExpenseDetailBusinessScenarios()
    if (expenseDetailScenarioIssue) {
      ElMessage.warning(expenseDetailScenarioIssue)
      return false
    }
    const expenseDetailAmountIssue = options.validateExpenseDetailAmountValues()
    if (expenseDetailAmountIssue) {
      ElMessage.warning(expenseDetailAmountIssue)
      return false
    }
    return true
  }

  async function submitDocument() {
    if (!validateBeforeSubmit()) {
      return
    }
    if (options.pageMode.value === 'modify') {
      await performSubmit()
      return
    }
    try {
      const preview = await options.loadManualApproverPreview()
      if (preview?.manualNodes?.length) {
        options.openManualApproverDialog(preview)
        return
      }
    } catch (error: unknown) {
      ElMessage.error(options.resolveErrorMessage(error, submitFailedMessage()))
      return
    }
    await performSubmit()
  }

  async function confirmSubmitAfterManualSelection() {
    if (!validateBeforeSubmit()) {
      return
    }
    await performSubmit()
  }

  async function performSubmit() {
    options.submitting.value = true
    try {
      const payload = options.buildDocumentUpdatePayload()
      if (options.pageMode.value === 'create') {
        const response = await expenseCreateApi.submit({
          templateCode: options.selectedTemplateCode.value,
          ...payload
        })
        await handleSubmitSuccess(response.data, '审批单已提交')
        return
      }
      if (options.pageMode.value === 'resubmit') {
        const response = await expenseApi.resubmit(options.editingDocumentCode.value, payload)
        await handleSubmitSuccess(response.data, options.isDraftEditEntry.value ? '草稿已提交' : '审批单已重新提交')
        return
      }
      const response = await expenseApprovalApi.modify(options.modifyingTaskId.value, payload)
      options.clearDraft()
      ElMessage.success('审批单已更新')
      await options.router.push(`/expense/documents/${encodeURIComponent(response.data.documentCode)}`)
    } catch (error: unknown) {
      ElMessage.error(options.resolveErrorMessage(error, submitFailedMessage()))
    } finally {
      options.submitting.value = false
    }
  }

  async function handleSubmitSuccess(result: ExpenseDocumentSubmitResult, message: string) {
    options.clearDraft()
    ElMessage.success(message)
    await options.router.push(`/expense/documents/${encodeURIComponent(result.documentCode)}`)
  }

  function submitFailedMessage() {
    if (options.pageMode.value === 'resubmit') {
      return options.isDraftEditEntry.value ? '提交审批单失败' : '重新提交审批单失败'
    }
    if (options.pageMode.value === 'modify') {
      return '保存审批修改失败'
    }
    return '提交审批单失败'
  }

  return {
    backButtonLabel,
    confirmSubmitAfterManualSelection,
    goBack,
    goBackToList,
    reselectTemplate,
    saveDraftManually,
    submitButtonLabel,
    submitDocument
  }
}
