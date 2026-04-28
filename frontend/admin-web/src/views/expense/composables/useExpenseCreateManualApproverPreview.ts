import { computed, ref, type ComputedRef } from 'vue'
import {
  expenseApi,
  expenseCreateApi,
  type ExpenseDocumentSubmitPayload,
  type ExpenseDocumentUpdatePayload,
  type ExpenseManualApproverPreview
} from '@/api'

type PageMode = 'create' | 'resubmit' | 'modify'

type UseExpenseCreateManualApproverPreviewOptions = {
  pageMode: ComputedRef<PageMode>
  selectedTemplateCode: ComputedRef<string>
  editingDocumentCode: ComputedRef<string>
  manualApproverSelections: Record<string, string[]>
  buildDocumentUpdatePayload: () => ExpenseDocumentUpdatePayload
}

export function useExpenseCreateManualApproverPreview(options: UseExpenseCreateManualApproverPreviewOptions) {
  const dialogVisible = ref(false)
  const previewResult = ref<ExpenseManualApproverPreview | null>(null)

  const approvalTimelineItems = computed(() => previewResult.value?.approvalTimeline || [])
  const manualNodes = computed(() => previewResult.value?.manualNodes || [])
  const canConfirmSubmit = computed(() =>
    manualNodes.value.every((node) => (options.manualApproverSelections[node.nodeKey] || []).length > 0)
  )

  async function loadPreview() {
    const payload = options.buildDocumentUpdatePayload()
    if (options.pageMode.value === 'create') {
      const createPayload: ExpenseDocumentSubmitPayload = {
        templateCode: options.selectedTemplateCode.value,
        ...payload
      }
      const response = await expenseCreateApi.previewManualApprovers(createPayload)
      return response.data
    }
    if (options.pageMode.value === 'resubmit' && options.editingDocumentCode.value) {
      const response = await expenseApi.previewManualApprovers(options.editingDocumentCode.value, payload)
      return response.data
    }
    return null
  }

  function openDialog(preview: ExpenseManualApproverPreview) {
    previewResult.value = preview
    preview.manualNodes.forEach((node) => {
      if (!node.nodeKey || !Array.isArray(node.selectedUserIds) || node.selectedUserIds.length === 0) {
        return
      }
      options.manualApproverSelections[node.nodeKey] = node.selectedUserIds
        .map((item) => String(item || '').trim())
        .filter(Boolean)
    })
    dialogVisible.value = true
  }

  function closeDialog() {
    dialogVisible.value = false
  }

  function updateNodeSelection(nodeKey: string, userIds: Array<string | number>) {
    const normalizedNodeKey = String(nodeKey || '').trim()
    if (!normalizedNodeKey) {
      return
    }
    options.manualApproverSelections[normalizedNodeKey] = userIds
      .map((item) => String(item || '').trim())
      .filter(Boolean)
  }

  return {
    dialogVisible,
    previewResult,
    approvalTimelineItems,
    manualNodes,
    canConfirmSubmit,
    loadPreview,
    openDialog,
    closeDialog,
    updateNodeSelection
  }
}
