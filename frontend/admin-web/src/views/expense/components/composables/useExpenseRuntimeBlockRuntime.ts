import { computed, type ComputedRef, type Ref } from 'vue'
import { ElMessage } from 'element-plus'
import type {
  ProcessCustomArchiveDetail,
  ProcessFormDesignBlock,
  ProcessFormDesignSchema,
  ProcessFormOption
} from '@/api'
import {
  getBusinessComponentDefinition,
  getControlType,
  getOptionItems,
  getSharedArchiveCode
} from '@/views/process/formDesignerHelper'
import {
  applyExpenseDetailAmountInput,
  buildExpenseDetailAmountValidationMessage,
  isExpenseDetailBlockReadOnly,
  isExpenseDetailBlockVisible,
  validateExpenseDetailAmountRules
} from '@/views/expense/expenseDetailRuntime'
import {
  documentTitleMaxLength,
  validateRuntimeRequiredValues,
  validateRuntimeTitleValues
} from '@/views/process/pmValidation'

export function useExpenseRuntimeBlockRuntime(params: {
  schema: ComputedRef<ProcessFormDesignSchema>
  formData: Ref<Record<string, unknown>>
  sharedArchives: ComputedRef<ProcessCustomArchiveDetail[]>
  companyOptionsSource: ComputedRef<ProcessFormOption[]>
  departmentOptionsSource: ComputedRef<ProcessFormOption[]>
  detailType: ComputedRef<string>
  defaultBusinessScenario: ComputedRef<string>
  approvalEditMode: ComputedRef<boolean>
  allowEditFormModule: ComputedRef<boolean>
  allowEditPayAccount: ComputedRef<boolean>
}) {
  const {
    schema,
    formData,
    sharedArchives,
    companyOptionsSource,
    departmentOptionsSource,
    detailType,
    defaultBusinessScenario,
    approvalEditMode,
    allowEditFormModule,
    allowEditPayAccount
  } = params

  const blocks = computed(() => schema.value?.blocks || [])
  const visibleBlocks = computed(() =>
    detailType.value ? blocks.value.filter((block) => isVisible(block)) : blocks.value
  )
  const sharedArchiveMap = computed(
    () => new Map(sharedArchives.value.map((item) => [item.archiveCode, item]))
  )
  const companyOptions = computed(() => companyOptionsSource.value || [])
  const departmentOptions = computed(() => departmentOptionsSource.value || [])

  function handleAmountInput(block: ProcessFormDesignBlock, nextValue: string | number) {
    applyExpenseDetailAmountInput(
      formData.value,
      block.fieldKey,
      nextValue,
      detailType.value,
      defaultBusinessScenario.value,
      schema.value
    )
  }

  function validateBeforeSubmit() {
    const requiredIssues = validateRuntimeRequiredValues(schema.value, formData.value || {}, {
      shouldValidateBlock: (block) => isVisible(block) && !isReadOnly(block)
    })
    if (requiredIssues.length) {
      ElMessage.warning(requiredIssues[0])
      return false
    }

    const titleIssues = validateRuntimeTitleValues(schema.value, formData.value || {})
    if (titleIssues.length) {
      ElMessage.warning(titleIssues[0])
      return false
    }

    const amountIssue = validateExpenseDetailAmountRules(
      formData.value || {},
      detailType.value,
      defaultBusinessScenario.value,
      schema.value
    )
    if (amountIssue) {
      ElMessage.warning(buildExpenseDetailAmountValidationMessage(amountIssue))
      return false
    }

    return true
  }

  function controlType(block: ProcessFormDesignBlock) {
    return getControlType(block)
  }

  function optionItems(block: ProcessFormDesignBlock) {
    return getOptionItems(block)
  }

  function isVisible(block: ProcessFormDesignBlock) {
    if (!detailType.value) {
      return true
    }
    return isExpenseDetailBlockVisible(
      block,
      formData.value,
      detailType.value,
      defaultBusinessScenario.value,
      schema.value
    )
  }

  function isApprovalEditableBlock(block: ProcessFormDesignBlock) {
    if (!approvalEditMode.value) {
      return false
    }
    if (block.kind === 'BUSINESS_COMPONENT' && businessCode(block) === 'payee-account') {
      return Boolean(allowEditPayAccount.value)
    }
    return (
      Boolean(allowEditFormModule.value) &&
      String(block.permission?.fixedStages?.IN_APPROVAL || 'READONLY') === 'EDITABLE'
    )
  }

  function isReadOnly(block: ProcessFormDesignBlock) {
    if (approvalEditMode.value) {
      return !isApprovalEditableBlock(block)
    }
    return isExpenseDetailBlockReadOnly(block)
  }

  function placeholderOf(block: ProcessFormDesignBlock) {
    return String(block.props.placeholder || `请输入${block.label}`)
  }

  function businessCode(block: ProcessFormDesignBlock) {
    return (
      getBusinessComponentDefinition(String(block.props.componentCode || ''))?.code ||
      String(block.props.componentCode || '')
    )
  }

  function findBusinessFieldKeys(code: string) {
    return blocks.value
      .filter((block) => block.kind === 'BUSINESS_COMPONENT' && businessCode(block) === code)
      .map((block) => block.fieldKey)
  }

  function departmentLabel(value: string) {
    return departmentOptions.value.find((item) => item.value === value)?.label || value
  }

  function sharedArchiveItems(block: ProcessFormDesignBlock) {
    return sharedArchiveMap.value.get(getSharedArchiveCode(block))?.items || []
  }

  return {
    blocks,
    visibleBlocks,
    companyOptions,
    departmentOptions,
    controlType,
    optionItems,
    documentTitleMaxLength,
    handleAmountInput,
    validateBeforeSubmit,
    isVisible,
    isReadOnly,
    placeholderOf,
    businessCode,
    findBusinessFieldKeys,
    departmentLabel,
    sharedArchiveItems
  }
}
