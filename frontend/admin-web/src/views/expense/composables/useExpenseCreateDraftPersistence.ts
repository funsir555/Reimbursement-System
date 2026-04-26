import { nextTick, onBeforeUnmount, ref, watch, type Ref } from 'vue'
import type { ExpenseCreateTemplateDetail, ExpenseDetailInstance } from '@/api'

export type ExpenseCreateDraft = {
  templateCode: string
  formValues: Record<string, unknown>
  expenseDetails: ExpenseDetailInstance[]
  manualApproverSelections: Record<string, string[]>
  templateDetail?: ExpenseCreateTemplateDetail
}

const DRAFT_PREFIX = 'expense-create-draft:'

type UseExpenseCreateDraftPersistenceOptions = {
  currentDraftKey: Ref<string>
  selectedTemplateCode: Ref<string>
  templateDetail: Ref<ExpenseCreateTemplateDetail | null>
  formValues: Record<string, unknown>
  expenseDetails: Ref<ExpenseDetailInstance[]>
  manualApproverSelections: Record<string, string[]>
  applyTemplateDetail: (detail: ExpenseCreateTemplateDetail) => void
  restoreManualApproverSelections: (source?: Record<string, string[]>) => void
  cloneDetail: (detail: ExpenseDetailInstance) => ExpenseDetailInstance
  cloneRecord: (value: Record<string, unknown>) => Record<string, unknown>
  cloneValue: <T>(value: T) => T
  cloneManualApproverSelections: () => Record<string, string[]>
}

export function useExpenseCreateDraftPersistence(options: UseExpenseCreateDraftPersistenceOptions) {
  const isFormHydrating = ref(false)
  const formHydrationVersion = ref(0)

  let draftPersistTimer: ReturnType<typeof setTimeout> | undefined
  let formHydrationToken = 0
  let isUnmounted = false

  watch(
    [options.formValues, options.expenseDetails, options.manualApproverSelections],
    () => {
      schedulePersistDraft()
    },
    { deep: true }
  )

  watch(options.templateDetail, (nextValue) => {
    if (!nextValue) {
      return
    }
    persistDraft({ includeTemplateDetail: true })
  })

  onBeforeUnmount(() => {
    isUnmounted = true
    clearDraftPersistTimer()
  })

  function storageKey() {
    return `${DRAFT_PREFIX}${options.currentDraftKey.value}`
  }

  function draftStorage() {
    if (typeof window === 'undefined' || typeof window.sessionStorage === 'undefined') {
      return null
    }
    return window.sessionStorage
  }

  function clearDraftPersistTimer() {
    if (!draftPersistTimer) {
      return
    }
    clearTimeout(draftPersistTimer)
    draftPersistTimer = undefined
  }

  function readDraft(): ExpenseCreateDraft | null {
    if (!options.currentDraftKey.value) {
      return null
    }
    const storage = draftStorage()
    if (!storage) {
      return null
    }
    const raw = storage.getItem(storageKey())
    if (!raw) {
      return null
    }
    try {
      return JSON.parse(raw) as ExpenseCreateDraft
    } catch {
      storage.removeItem(storageKey())
      return null
    }
  }

  function restoreResubmitDraftState(templateCode: string) {
    const draft = readDraft()
    if (!draft || draft.templateCode !== templateCode) {
      options.restoreManualApproverSelections(undefined)
      return
    }
    if (draft.templateDetail?.templateCode === templateCode) {
      options.applyTemplateDetail(draft.templateDetail)
    }
    runWithFormHydration(() => {
      Object.assign(options.formValues, options.cloneRecord(draft.formValues || {}))
      options.expenseDetails.value = Array.isArray(draft.expenseDetails)
        ? draft.expenseDetails.map(options.cloneDetail)
        : []
      options.restoreManualApproverSelections(draft.manualApproverSelections)
    })
  }

  function bumpFormHydrationVersion() {
    formHydrationVersion.value += 1
  }

  function runWithFormHydration(apply: () => void) {
    const token = ++formHydrationToken
    isFormHydrating.value = true
    apply()
    bumpFormHydrationVersion()
    void nextTick(() => {
      if (token === formHydrationToken) {
        isFormHydrating.value = false
      }
    })
  }

  function schedulePersistDraft() {
    if (
      isUnmounted ||
      !options.currentDraftKey.value ||
      !options.selectedTemplateCode.value ||
      !options.templateDetail.value
    ) {
      return
    }
    clearDraftPersistTimer()
    draftPersistTimer = setTimeout(() => {
      draftPersistTimer = undefined
      if (isUnmounted) {
        return
      }
      persistDraft()
    }, 120)
  }

  function persistDraft(persistOptions: { includeTemplateDetail?: boolean } = {}) {
    const storage = draftStorage()
    if (
      isUnmounted ||
      !storage ||
      !options.currentDraftKey.value ||
      !options.selectedTemplateCode.value ||
      !options.templateDetail.value
    ) {
      return
    }
    const currentDraft = readDraft()
    const payload: ExpenseCreateDraft = {
      templateCode: options.selectedTemplateCode.value,
      formValues: options.cloneRecord(options.formValues),
      expenseDetails: options.expenseDetails.value.map(options.cloneDetail),
      manualApproverSelections: options.cloneManualApproverSelections(),
      templateDetail: persistOptions.includeTemplateDetail
        ? options.cloneValue(options.templateDetail.value)
        : currentDraft?.templateDetail
    }
    storage.setItem(storageKey(), JSON.stringify(payload))
  }

  function clearDraft() {
    clearDraftPersistTimer()
    const storage = draftStorage()
    if (storage && options.currentDraftKey.value) {
      storage.removeItem(storageKey())
    }
  }

  return {
    clearDraft,
    formHydrationVersion,
    isFormHydrating,
    persistDraft,
    readDraft,
    restoreResubmitDraftState,
    runWithFormHydration
  }
}
