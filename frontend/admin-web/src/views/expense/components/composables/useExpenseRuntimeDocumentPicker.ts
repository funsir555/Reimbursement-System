import { computed, nextTick, reactive, type ComputedRef, type Ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  expenseApi,
  type ExpenseDocumentPickerGroup,
  type ExpenseDocumentPickerItem,
  type ProcessFormDesignBlock
} from '@/api'
import { normalizeBusinessComponentAllowedTemplateTypes } from '@/views/process/formDesignerHelper'
import {
  cloneDocumentRecord,
  createDocumentPickerDialogState,
  documentBlockHintFromBusinessCode,
  documentRelationTypeFromBusinessCode,
  formatDocumentAmount,
  isDocumentBusinessCode,
  isRelatedDocumentBusinessCode,
  isWriteOffDocumentBusinessCode,
  mergeDocumentRecord,
  normalizeRelatedDocumentValues,
  normalizeWriteOffDocumentValues,
  resolveDocumentPickerActiveTemplateType,
  resolveTemplateTypeLabel,
  toDocumentRecord,
  toRelatedDocumentValue,
  toWriteOffDocumentValue,
  writeOffSourceKindLabel,
  type RuntimeDocumentRecord
} from '../expenseRuntimeDocumentPickerShared'
import { compareMoney, subtractMoney } from '@/utils/money'

export function useExpenseRuntimeDocumentPicker(params: {
  blocks: ComputedRef<ProcessFormDesignBlock[]>
  formData: Ref<Record<string, unknown>>
  businessCode: (block: ProcessFormDesignBlock) => string
  prepareDocumentPickerOpen: () => void
  resolveErrorMessage: (error: unknown, fallback: string) => string
  toOptionalMoney: (value: unknown) => string | undefined
}) {
  const {
    blocks,
    formData,
    businessCode,
    prepareDocumentPickerOpen,
    resolveErrorMessage,
    toOptionalMoney
  } = params

  const documentPickerDialog = reactive(createDocumentPickerDialogState())

  const documentPickerTitle = computed(() =>
    documentPickerDialog.relationType === 'WRITEOFF' ? '选择核销单据' : '选择关联单据'
  )

  function isRelatedDocumentBlock(block: ProcessFormDesignBlock) {
    return isRelatedDocumentBusinessCode(businessCode(block))
  }

  function isWriteOffDocumentBlock(block: ProcessFormDesignBlock) {
    return isWriteOffDocumentBusinessCode(businessCode(block))
  }

  function isDocumentBusinessBlock(block: ProcessFormDesignBlock) {
    return isDocumentBusinessCode(businessCode(block))
  }

  function documentBlockHint(block: ProcessFormDesignBlock) {
    return documentBlockHintFromBusinessCode(businessCode(block))
  }

  function documentAllowedTemplateTypes(block: ProcessFormDesignBlock) {
    return normalizeBusinessComponentAllowedTemplateTypes(
      businessCode(block),
      block.props.allowedTemplateTypes
    )
  }

  function documentRecords(block: ProcessFormDesignBlock) {
    const rawValue = formData.value[block.fieldKey]
    return isWriteOffDocumentBlock(block)
      ? normalizeWriteOffDocumentValues(rawValue)
      : normalizeRelatedDocumentValues(rawValue)
  }

  function openDocumentPicker(block: ProcessFormDesignBlock) {
    prepareDocumentPickerOpen()
    Object.assign(documentPickerDialog, {
      ...createDocumentPickerDialogState(),
      visible: true,
      fieldKey: block.fieldKey,
      relationType: documentRelationTypeFromBusinessCode(businessCode(block))
    })

    documentRecords(block).forEach((item) => {
      if (!item.documentCode) {
        return
      }
      documentPickerDialog.selectedCodes.push(item.documentCode)
      documentPickerDialog.itemsByCode[item.documentCode] = cloneDocumentRecord(item)
    })

    void nextTick(() => loadDocumentPicker())
  }

  function closeDocumentPicker() {
    Object.assign(documentPickerDialog, createDocumentPickerDialogState())
  }

  async function loadDocumentPicker() {
    if (!documentPickerDialog.fieldKey) {
      return
    }
    const block = blocks.value.find((item) => item.fieldKey === documentPickerDialog.fieldKey)
    if (!block) {
      return
    }
    documentPickerDialog.loading = true
    try {
      const res = await expenseApi.getDocumentPicker({
        relationType: documentPickerDialog.relationType,
        templateTypes: documentAllowedTemplateTypes(block),
        keyword: documentPickerDialog.keyword || undefined
      })
      documentPickerDialog.groups = res.data.groups || []
      documentPickerDialog.activeTemplateType = resolveDocumentPickerActiveTemplateType(
        documentPickerDialog.groups,
        documentPickerDialog.activeTemplateType,
        documentPickerDialog.selectedCodes
      )
      documentPickerDialog.groups.forEach((group) => {
        group.items.forEach((item) => {
          documentPickerDialog.itemsByCode[item.documentCode] = mergeDocumentRecord(
            documentPickerDialog.itemsByCode[item.documentCode],
            toDocumentRecord(item)
          )
        })
      })
    } catch (error: unknown) {
      ElMessage.error(resolveErrorMessage(error, '加载可选单据失败'))
    } finally {
      documentPickerDialog.loading = false
    }
  }

  function isDocumentSelected(documentCode: string) {
    return documentPickerDialog.selectedCodes.includes(documentCode)
  }

  function selectedGroupCount(group: ExpenseDocumentPickerGroup) {
    return group.items.filter((item) => isDocumentSelected(item.documentCode)).length
  }

  function toggleDocumentSelection(item: ExpenseDocumentPickerItem) {
    const existingIndex = documentPickerDialog.selectedCodes.indexOf(item.documentCode)
    if (existingIndex >= 0) {
      documentPickerDialog.selectedCodes.splice(existingIndex, 1)
      return
    }
    documentPickerDialog.selectedCodes.push(item.documentCode)
    documentPickerDialog.itemsByCode[item.documentCode] = mergeDocumentRecord(
      documentPickerDialog.itemsByCode[item.documentCode],
      toDocumentRecord(item)
    )
  }

  function confirmDocumentPicker() {
    const block = blocks.value.find((item) => item.fieldKey === documentPickerDialog.fieldKey)
    if (!block) {
      closeDocumentPicker()
      return
    }

    const nextRecords = documentPickerDialog.selectedCodes
      .map((documentCode) => documentPickerDialog.itemsByCode[documentCode])
      .filter((item): item is RuntimeDocumentRecord => Boolean(item?.documentCode))
      .map((item) => cloneDocumentRecord(item))

    formData.value[block.fieldKey] = isWriteOffDocumentBlock(block)
      ? nextRecords.map((item) => toWriteOffDocumentValue(item))
      : nextRecords.map((item) => toRelatedDocumentValue(item))

    closeDocumentPicker()
  }

  function removeDocumentRecord(block: ProcessFormDesignBlock, documentCode: string) {
    const next = documentRecords(block).filter((item) => item.documentCode !== documentCode)
    formData.value[block.fieldKey] = isWriteOffDocumentBlock(block)
      ? next.map((item) => toWriteOffDocumentValue(item))
      : next.map((item) => toRelatedDocumentValue(item))
  }

  function updateWriteOffAmount(block: ProcessFormDesignBlock, documentCode: string, value: string) {
    const next = normalizeWriteOffDocumentValues(formData.value[block.fieldKey]).map((item) => {
      if (item.documentCode !== documentCode) {
        return item
      }
      const writeOffAmount = toOptionalMoney(value)
      const availableAmount = item.availableWriteOffAmount
      return {
        ...item,
        writeOffAmount,
        remainingAmount:
          !availableAmount || !writeOffAmount
            ? undefined
            : compareMoney(availableAmount, writeOffAmount) >= 0
              ? subtractMoney(availableAmount, writeOffAmount)
              : '0.00'
      }
    })
    formData.value[block.fieldKey] = next.map((item) => toWriteOffDocumentValue(item))
  }

  return {
    documentPickerDialog,
    documentPickerTitle,
    isRelatedDocumentBlock,
    isWriteOffDocumentBlock,
    isDocumentBusinessBlock,
    documentBlockHint,
    documentRecords,
    openDocumentPicker,
    closeDocumentPicker,
    loadDocumentPicker,
    isDocumentSelected,
    selectedGroupCount,
    toggleDocumentSelection,
    confirmDocumentPicker,
    removeDocumentRecord,
    updateWriteOffAmount,
    resolveTemplateTypeLabel,
    writeOffSourceKindLabel,
    formatAmount: formatDocumentAmount
  }
}
