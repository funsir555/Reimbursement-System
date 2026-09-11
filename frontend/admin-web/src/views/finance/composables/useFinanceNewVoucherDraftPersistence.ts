import { onBeforeUnmount, onDeactivated, watch, type ComputedRef, type Ref } from 'vue'
import type { FinanceVoucherSavePayload } from '@/api'

type UseFinanceNewVoucherDraftPersistenceOptions = {
  pageMode: ComputedRef<'create' | 'detail' | 'review'>
  draftPersistenceEnabled: ComputedRef<boolean>
  initializing: Ref<boolean>
  voucherMetaReady: ComputedRef<boolean>
  hasUnsavedChanges: ComputedRef<boolean>
  hasDraft: Ref<boolean>
  currentCompanyId: () => string
  buildPayload: () => FinanceVoucherSavePayload
  buildSnapshot: () => string
  writeDraft: (draft: FinanceVoucherSavePayload, companyId?: string) => void
  clearDraft: (companyId?: string) => void
  debounceMs?: number
}

const DEFAULT_DEBOUNCE_MS = 180

export function useFinanceNewVoucherDraftPersistence(options: UseFinanceNewVoucherDraftPersistenceOptions) {
  let persistTimer: ReturnType<typeof setTimeout> | undefined
  let persistenceSuppressed = false

  const debounceMs = options.debounceMs ?? DEFAULT_DEBOUNCE_MS

  watch(
    () => [
      options.pageMode.value,
      options.initializing.value,
      options.voucherMetaReady.value,
      options.hasUnsavedChanges.value,
      options.hasDraft.value,
      options.currentCompanyId(),
      options.buildSnapshot()
    ] as const,
    () => {
      handleDraftPersistence()
    },
    { immediate: true }
  )

  onDeactivated(() => {
    flushDraft()
  })

  onBeforeUnmount(() => {
    flushDraft()
    clearPersistTimer()
  })

  function handleDraftPersistence() {
    if (
      options.pageMode.value !== 'create'
      || !options.draftPersistenceEnabled.value
      || options.initializing.value
      || !options.voucherMetaReady.value
    ) {
      clearPersistTimer()
      return
    }

    const companyId = options.currentCompanyId()
    if (!companyId) {
      clearPersistTimer()
      return
    }
    if (persistenceSuppressed) {
      clearPersistTimer()
      return
    }

    const payload = options.buildPayload()
    const shouldPersist =
      options.hasUnsavedChanges.value ||
      (options.hasDraft.value && hasMeaningfulDraftContent(payload))

    if (!shouldPersist) {
      clearPersistTimer()
      options.clearDraft(companyId)
      return
    }

    schedulePersist(payload, companyId)
  }

  function schedulePersist(payload: FinanceVoucherSavePayload, companyId: string) {
    clearPersistTimer()
    persistTimer = setTimeout(() => {
      persistTimer = undefined
      options.writeDraft(payload, companyId)
    }, debounceMs)
  }

  function flushDraft() {
    if (
      options.pageMode.value !== 'create'
      || !options.draftPersistenceEnabled.value
      || options.initializing.value
      || !options.voucherMetaReady.value
    ) {
      return
    }
    const companyId = options.currentCompanyId()
    if (!companyId) {
      return
    }
    if (persistenceSuppressed) {
      clearPersistTimer()
      return
    }
    const payload = options.buildPayload()
    if (options.hasUnsavedChanges.value || (options.hasDraft.value && hasMeaningfulDraftContent(payload))) {
      clearPersistTimer()
      options.writeDraft(payload, companyId)
      return
    }
    clearPersistTimer()
    options.clearDraft(companyId)
  }

  function clearPersistTimer() {
    if (!persistTimer) {
      return
    }
    clearTimeout(persistTimer)
    persistTimer = undefined
  }

  function suppressPersistence() {
    persistenceSuppressed = true
    clearPersistTimer()
  }

  function resumePersistence() {
    persistenceSuppressed = false
  }

  function discardDraft(companyId = options.currentCompanyId()) {
    suppressPersistence()
    options.clearDraft(companyId)
  }

  return {
    suppressPersistence,
    resumePersistence,
    discardDraft
  }
}

function hasMeaningfulDraftContent(payload: FinanceVoucherSavePayload | null | undefined) {
  if (!payload) {
    return false
  }

  if (Number(payload.idoc || 0) > 0) {
    return true
  }

  if (trimToNull(payload.ctext1) || trimToNull(payload.ctext2)) {
    return true
  }

  return Array.isArray(payload.entries) && payload.entries.some((entry) => {
    return Boolean(
      trimToNull(entry.cdigest) ||
      trimToNull(entry.ccode) ||
      trimToNull(entry.cdeptId) ||
      trimToNull(entry.cpersonId) ||
      trimToNull(entry.ccusId) ||
      trimToNull(entry.csupId) ||
      trimToNull(entry.citemClass) ||
      trimToNull(entry.citemId) ||
      trimToNull(entry.md) ||
      trimToNull(entry.mc) ||
      trimToNull(entry.ndS) ||
      trimToNull(entry.ncS) ||
      entry.cashFlowItemId !== undefined && entry.cashFlowItemId !== null ||
      trimToNull(entry.cashFlowItemName) ||
      trimToNull(entry.cashFlowSubjectCode) ||
      trimToNull(entry.cashFlowSubjectName) ||
      trimToNull(entry.cashFlowAmount)
    )
  })
}

function trimToNull(value: unknown) {
  const normalized = String(value ?? '').trim()
  return normalized || ''
}
