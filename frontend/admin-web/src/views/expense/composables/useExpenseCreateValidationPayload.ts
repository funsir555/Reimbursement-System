import type { ComputedRef, Ref } from 'vue'
import type { ExpenseDetailInstance, ExpenseDocumentUpdatePayload, ProcessFormDesignBlock } from '@/api'
import { normalizeRuntimeAmountControlValues } from '../expenseDetailRuntime'

type UseExpenseCreateValidationPayloadOptions = {
  formValues: Record<string, unknown>
  blocks: ComputedRef<ProcessFormDesignBlock[]>
  totalAmount: ComputedRef<string>
  expenseDetails: Ref<ExpenseDetailInstance[]>
  manualApproverSelections: Record<string, string[]>
  cloneRecord: (value: Record<string, unknown>) => Record<string, unknown>
  cloneDetail: (detail: ExpenseDetailInstance) => ExpenseDetailInstance
}

export function useExpenseCreateValidationPayload(options: UseExpenseCreateValidationPayloadOptions) {
  function cloneManualApproverSelections() {
    return Object.fromEntries(
      Object.entries(options.manualApproverSelections).map(([nodeKey, userIds]) => [nodeKey, [...userIds]])
    )
  }

  function normalizeManualApproverSelections() {
    return Object.fromEntries(
      Object.entries(options.manualApproverSelections)
        .map(([nodeKey, userIds]) => [
          nodeKey,
          userIds
            .map((item) => Number(item))
            .filter((item) => Number.isFinite(item) && item > 0)
        ] as const)
        .filter(([, userIds]) => userIds.length > 0)
    )
  }

  function buildDocumentUpdatePayload(): ExpenseDocumentUpdatePayload {
    const payload: ExpenseDocumentUpdatePayload = {
      formData: {
        ...normalizeRuntimeAmountControlValues(options.blocks.value, options.cloneRecord(options.formValues)),
        __totalAmount: options.totalAmount.value
      },
      expenseDetails: options.expenseDetails.value.map(options.cloneDetail)
    }
    const manualSelections = normalizeManualApproverSelections()
    if (Object.keys(manualSelections).length > 0) {
      payload.manualApproverSelections = manualSelections
    }
    return payload
  }

  return {
    buildDocumentUpdatePayload,
    cloneManualApproverSelections
  }
}
