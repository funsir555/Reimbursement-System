import { ElMessage } from 'element-plus'
import { watch, type ComputedRef, type Ref } from 'vue'
import {
  expenseApi,
  expenseApprovalApi,
  expenseCreateApi,
  type ExpenseCreateTemplateDetail,
  type ExpenseCreateTemplateSummary,
  type ExpenseDetailInstance,
  type ExpenseDocumentEditContext,
  type ProcessFormDesignSchema
} from '@/api'
import type { ExpenseCreateDraft } from './useExpenseCreateDraftPersistence'

type PageMode = 'create' | 'resubmit' | 'modify'
type AsyncStatus = 'idle' | 'loading' | 'success' | 'empty' | 'error'

type RouteLike = {
  query: Record<string, unknown>
}

type RouterLike = {
  replace: (to: string | Record<string, unknown>) => Promise<unknown> | unknown
}

type UseExpenseCreateBootstrapOptions = {
  route: RouteLike
  router: RouterLike
  pageMode: ComputedRef<PageMode>
  editingDocumentCode: ComputedRef<string>
  modifyingTaskId: ComputedRef<number>
  emptySchema: ProcessFormDesignSchema
  loading: Ref<boolean>
  templates: Ref<ExpenseCreateTemplateSummary[]>
  templateListStatus: Ref<AsyncStatus>
  templateListErrorMessage: Ref<string>
  templateDetailStatus: Ref<Exclude<AsyncStatus, 'empty'>>
  templateDetailErrorMessage: Ref<string>
  selectedTemplateCode: Ref<string>
  templateDetail: Ref<ExpenseCreateTemplateDetail | null>
  currentDraftKey: Ref<string>
  expenseDetails: Ref<ExpenseDetailInstance[]>
  formValues: Record<string, unknown>
  applyEditContextPermissions: (context: ExpenseDocumentEditContext | null) => void
  applyTemplateDetail: (detail: ExpenseCreateTemplateDetail) => void
  resetFormValues: () => void
  restoreManualApproverSelections: (source?: Record<string, string[]>) => void
  persistDraft: (options?: { includeTemplateDetail?: boolean }) => void
  readDraft: () => ExpenseCreateDraft | null
  restoreResubmitDraftState: (templateCode: string) => void
  runWithFormHydration: (apply: () => void) => void
  cloneDetail: (detail: ExpenseDetailInstance) => ExpenseDetailInstance
  cloneRecord: (value: Record<string, unknown>) => Record<string, unknown>
  resolveErrorMessage: (error: unknown, fallback: string) => string
}

export function useExpenseCreateBootstrap(options: UseExpenseCreateBootstrapOptions) {
  let pageSyncVersion = 0
  let templateListRequestVersion = 0
  let templateListLoadingPromise: Promise<void> | null = null

  watch(
    [
      options.pageMode,
      () => options.route.query.templateCode,
      () => options.route.query.draftKey,
      options.editingDocumentCode,
      options.modifyingTaskId
    ],
    () => {
      void syncPageState()
    },
    { immediate: true }
  )

  async function syncPageState() {
    const version = ++pageSyncVersion
    if (options.pageMode.value === 'create') {
      await syncCreatePage(version)
      return
    }
    await syncEditPage(version)
  }

  async function syncCreatePage(version: number) {
    void ensureTemplateListLoaded()

    const templateCode = typeof options.route.query.templateCode === 'string' ? options.route.query.templateCode : ''
    const draftKey = typeof options.route.query.draftKey === 'string' ? options.route.query.draftKey : ''
    if (!templateCode || !draftKey) {
      resetCreateSelectionState()
      return
    }

    options.templateDetailStatus.value = 'loading'
    options.templateDetailErrorMessage.value = ''
    options.currentDraftKey.value = draftKey
    options.selectedTemplateCode.value = templateCode
    await loadTemplateDetail(templateCode, true, version)
  }

  async function syncEditPage(version: number) {
    options.loading.value = true
    options.templateDetailStatus.value = 'idle'
    options.templateDetailErrorMessage.value = ''

    try {
      const context = await fetchEditContext()
      if (version !== pageSyncVersion) {
        return
      }
      options.selectedTemplateCode.value = context.templateCode
      applyEditContextState(context)
      if (options.pageMode.value === 'resubmit') {
        options.restoreResubmitDraftState(context.templateCode)
      } else {
        options.restoreManualApproverSelections(options.readDraft()?.manualApproverSelections)
      }
      options.persistDraft({ includeTemplateDetail: true })
    } catch (error: unknown) {
      if (version !== pageSyncVersion) {
        return
      }
      ElMessage.error(options.resolveErrorMessage(error, '加载审批单页面失败'))
    } finally {
      if (version === pageSyncVersion) {
        options.loading.value = false
      }
    }
  }

  async function chooseTemplate(templateCode: string) {
    await options.router.replace({
      name: 'expense-create',
      query: { templateCode, draftKey: createDraftKey(templateCode) }
    })
  }

  async function ensureTemplateListLoaded(force = false) {
    if (templateListLoadingPromise && !force) {
      return templateListLoadingPromise
    }
    if (!force && ['loading', 'success', 'empty'].includes(options.templateListStatus.value)) {
      return
    }

    const requestVersion = ++templateListRequestVersion
    options.templateListStatus.value = 'loading'
    options.templateListErrorMessage.value = ''

    templateListLoadingPromise = (async () => {
      try {
        const res = await expenseCreateApi.listTemplates()
        if (requestVersion !== templateListRequestVersion) {
          return
        }
        options.templates.value = res.data
        options.templateListStatus.value = res.data.length > 0 ? 'success' : 'empty'
      } catch (error: unknown) {
        if (requestVersion !== templateListRequestVersion) {
          return
        }
        options.templates.value = []
        options.templateListStatus.value = 'error'
        options.templateListErrorMessage.value = options.resolveErrorMessage(error, '加载单据模板失败，请稍后重试')
      } finally {
        if (requestVersion === templateListRequestVersion) {
          templateListLoadingPromise = null
        }
      }
    })()

    await templateListLoadingPromise
  }

  async function loadTemplateDetail(templateCode: string, useDraft: boolean, version = pageSyncVersion) {
    options.loading.value = true
    options.templateDetailStatus.value = 'loading'
    options.templateDetailErrorMessage.value = ''

    try {
      const res = await expenseCreateApi.getTemplateDetail(templateCode)
      if (version !== pageSyncVersion) {
        return
      }

      options.applyTemplateDetail(res.data)
      options.applyEditContextPermissions(null)
      options.resetFormValues()
      options.expenseDetails.value = []

      if (useDraft) {
        const draft = options.readDraft()
        if (draft && draft.templateCode === templateCode) {
          options.runWithFormHydration(() => {
            Object.assign(options.formValues, draft.formValues || {})
            options.expenseDetails.value = Array.isArray(draft.expenseDetails) ? draft.expenseDetails.map(options.cloneDetail) : []
            options.restoreManualApproverSelections(draft.manualApproverSelections)
          })
          if (draft.templateDetail) {
            options.applyTemplateDetail(draft.templateDetail)
          }
        }
      }

      options.templateDetailStatus.value = 'success'
      options.persistDraft({ includeTemplateDetail: true })
    } catch (error: unknown) {
      if (version !== pageSyncVersion) {
        return
      }
      options.templateDetail.value = null
      options.templateDetailStatus.value = 'error'
      options.templateDetailErrorMessage.value = options.resolveErrorMessage(error, '加载模板详情失败，请稍后重试')
      options.expenseDetails.value = []
      options.resetFormValues()
    } finally {
      if (version === pageSyncVersion) {
        options.loading.value = false
      }
    }
  }

  async function fetchEditContext() {
    if (options.pageMode.value === 'resubmit') {
      if (!options.editingDocumentCode.value) {
        throw new Error('缺少待重提单号')
      }
      const res = await expenseApi.getEditContext(options.editingDocumentCode.value)
      return res.data
    }
    if (!options.modifyingTaskId.value) {
      throw new Error('缺少待修改任务')
    }
    const res = await expenseApprovalApi.getModifyContext(options.modifyingTaskId.value)
    return res.data
  }

  function extractTemplateDetail(context: ExpenseDocumentEditContext): ExpenseCreateTemplateDetail {
    return {
      templateCode: context.templateCode,
      templateName: context.templateName,
      templateType: context.templateType,
      templateTypeLabel: context.templateTypeLabel,
      categoryCode: context.categoryCode,
      templateDescription: context.templateDescription,
      formDesignCode: context.formDesignCode,
      approvalFlowCode: context.approvalFlowCode,
      flowName: context.flowName,
      formName: context.formName,
      schema: context.schema || options.emptySchema,
      flowSnapshot: context.flowSnapshot || {},
      sharedArchives: context.sharedArchives || [],
      expenseDetailDesignCode: context.expenseDetailDesignCode,
      expenseDetailDesignName: context.expenseDetailDesignName,
      expenseDetailType: context.expenseDetailType,
      expenseDetailTypeLabel: context.expenseDetailTypeLabel,
      expenseDetailModeDefault: context.expenseDetailModeDefault,
      expenseDetailSchema: context.expenseDetailSchema || options.emptySchema,
      expenseDetailSharedArchives: context.expenseDetailSharedArchives || [],
      companyOptions: context.companyOptions || [],
      departmentOptions: context.departmentOptions || [],
      userOptions: context.userOptions || [],
      currentUserCompanyId: context.currentUserCompanyId,
      currentUserCompanyName: context.currentUserCompanyName,
      currentUserDeptId: context.currentUserDeptId,
      currentUserDeptName: context.currentUserDeptName
    }
  }

  function applyEditContextState(context: ExpenseDocumentEditContext) {
    options.selectedTemplateCode.value = context.templateCode
    options.currentDraftKey.value = buildEditDraftKey(context)
    options.applyTemplateDetail(extractTemplateDetail(context))
    options.applyEditContextPermissions(context)
    options.runWithFormHydration(() => {
      options.resetFormValues()
      Object.assign(options.formValues, options.cloneRecord(context.formData))
      options.expenseDetails.value = Array.isArray(context.expenseDetails) ? context.expenseDetails.map(options.cloneDetail) : []
    })
  }

  function createDraftKey(templateCode: string) {
    return `${templateCode}-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`
  }

  function buildEditDraftKey(context: ExpenseDocumentEditContext) {
    if (options.pageMode.value === 'resubmit') {
      return `resubmit-${context.documentCode}`
    }
    return `modify-${context.taskId || options.modifyingTaskId.value || context.documentCode}`
  }

  function resetCreateSelectionState() {
    options.selectedTemplateCode.value = ''
    options.currentDraftKey.value = ''
    options.templateDetail.value = null
    options.templateDetailStatus.value = 'idle'
    options.templateDetailErrorMessage.value = ''
    options.expenseDetails.value = []
    options.resetFormValues()
  }

  function retryLoadTemplates() {
    void ensureTemplateListLoaded(true)
  }

  function retryLoadSelectedTemplate() {
    if (!options.selectedTemplateCode.value) {
      return
    }
    const version = ++pageSyncVersion
    void loadTemplateDetail(options.selectedTemplateCode.value, true, version)
  }
  return {
    applyEditContextState,
    chooseTemplate,
    resetCreateSelectionState,
    retryLoadTemplates,
    retryLoadSelectedTemplate
  }
}
