import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, type ComputedRef, type Ref } from 'vue'
import type {
  ExpenseCreateTemplateDetail,
  ExpenseDetailInstance,
  ProcessFormDesignBlock,
  ProcessFormDesignSchema
} from '@/api'
import { formatMoney } from '@/utils/money'
import {
  buildExpenseDetailAmountValidationMessage,
  buildExpenseDetailFormData,
  enrichExpenseDetailInstance,
  isExpenseDetailBlockReadOnly,
  isExpenseDetailBlockVisible,
  resolveBusinessScenario,
  resolveExpenseDetailAmount,
  validateExpenseDetailAmountRules
} from '../expenseDetailRuntime'
import { validateRuntimeRequiredValues } from '@/views/process/pmValidation'

type RouteLike = {
  fullPath: string
}

type RouterLike = {
  push: (to: string | Record<string, unknown>) => Promise<unknown> | unknown
}

type UseExpenseCreateExpenseDetailsOwnerOptions = {
  route: RouteLike
  router: RouterLike
  templateDetail: Ref<ExpenseCreateTemplateDetail | null>
  expenseDetails: Ref<ExpenseDetailInstance[]>
  currentDraftKey: Ref<string>
  selectedTemplateCode: Ref<string>
  emptySchema: ProcessFormDesignSchema
  isReportTemplate: ComputedRef<boolean>
  persistDraft: (options?: { includeTemplateDetail?: boolean }) => void
  cloneRecord: (value: Record<string, unknown>) => Record<string, unknown>
  isRecord: (value: unknown) => value is Record<string, unknown>
  resolveErrorMessage: (error: unknown, fallback: string) => string
}

export function useExpenseCreateExpenseDetailsOwner(options: UseExpenseCreateExpenseDetailsOwnerOptions) {
  const canAddExpenseDetail = computed(() => Boolean(options.templateDetail.value?.expenseDetailDesignCode))

  function cloneDetail(detail: ExpenseDetailInstance): ExpenseDetailInstance {
    return enrichExpenseDetailInstance(
      {
        ...detail,
        formData: options.cloneRecord(detail.formData || {})
      },
      options.templateDetail.value?.expenseDetailModeDefault,
      options.templateDetail.value?.expenseDetailSchema
    )
  }

  function addExpenseDetail() {
    if (!options.templateDetail.value?.expenseDetailDesignCode) {
      ElMessage.warning('当前模板未绑定费用明细表单')
      return
    }
    if (options.expenseDetails.value.length >= 10) {
      ElMessage.warning('费用明细最多只能添加 10 份')
      return
    }

    const sortOrder = options.expenseDetails.value.length + 1
    const detailNo = `D${String(sortOrder).padStart(3, '0')}`
    const detail = enrichExpenseDetailInstance(
      {
        detailNo,
        detailDesignCode: options.templateDetail.value.expenseDetailDesignCode,
        detailType: options.templateDetail.value.expenseDetailType,
        enterpriseMode: '',
        detailTitle: `费用明细 ${sortOrder}`,
        sortOrder,
        formData: buildExpenseDetailFormData(
          options.templateDetail.value.expenseDetailSchema,
          options.templateDetail.value.expenseDetailType,
          {},
          options.templateDetail.value.expenseDetailModeDefault
        )
      },
      options.templateDetail.value.expenseDetailModeDefault,
      options.templateDetail.value.expenseDetailSchema
    )
    options.expenseDetails.value = [...options.expenseDetails.value, detail]
    options.persistDraft()
    editExpenseDetail(detailNo)
  }

  function editExpenseDetail(detailNo: string) {
    if (!detailNo || !options.currentDraftKey.value || !options.selectedTemplateCode.value) {
      return
    }
    options.persistDraft({ includeTemplateDetail: true })
    void options.router.push({
      name: 'expense-create-detail-edit',
      params: { detailNo },
      query: {
        draftKey: options.currentDraftKey.value,
        templateCode: options.selectedTemplateCode.value,
        returnTo: options.route.fullPath
      }
    })
  }

  async function removeExpenseDetail(detailNo: string) {
    const target = options.expenseDetails.value.find((item) => item.detailNo === detailNo)
    if (!target) {
      return
    }
    try {
      await ElMessageBox.confirm(`确认删除“${target.detailTitle || detailNo}”吗？`, '删除费用明细', {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
      })
      options.expenseDetails.value = options.expenseDetails.value
        .filter((item) => item.detailNo !== detailNo)
        .map((item, index) => ({
          ...item,
          sortOrder: index + 1
        }))
      options.persistDraft()
    } catch (error: unknown) {
      if (error === 'cancel' || String(error).includes('cancel')) {
        return
      }
      ElMessage.error(options.resolveErrorMessage(error, '删除费用明细失败'))
    }
  }

  function expenseDetailAmountText(detail: ExpenseDetailInstance) {
    const amount =
      resolveExpenseDetailAmount(
        options.isRecord(detail.formData) ? detail.formData : {},
        String(detail.detailType || ''),
        String(
          detail.businessSceneMode
            || detail.enterpriseMode
            || options.templateDetail.value?.expenseDetailModeDefault
            || ''
        ),
        options.templateDetail.value?.expenseDetailSchema
      ) || '0.00'
    return `金额：¥ ${formatMoney(amount)}`
  }

  function validateExpenseDetailBusinessScenarios() {
    if (!options.isReportTemplate.value || options.templateDetail.value?.expenseDetailType !== 'ENTERPRISE_TRANSACTION') {
      return ''
    }
    for (const detail of options.expenseDetails.value) {
      const resolvedScenario = resolveBusinessScenario(
        options.isRecord(detail.formData) ? detail.formData : {},
        options.templateDetail.value?.expenseDetailType,
        options.templateDetail.value?.expenseDetailModeDefault,
        options.templateDetail.value?.expenseDetailSchema
      )
      if (!resolvedScenario) {
        return `请先为“${detail.detailTitle || detail.detailNo}”选择业务场景`
      }
    }
    return ''
  }

  function validateExpenseDetailAmountValues() {
    if (!options.isReportTemplate.value) {
      return ''
    }
    const detailType = options.templateDetail.value?.expenseDetailType
    const defaultBusinessScenario = options.templateDetail.value?.expenseDetailModeDefault
    const detailSchema = options.templateDetail.value?.expenseDetailSchema
    for (const detail of options.expenseDetails.value) {
      const detailFormData = options.isRecord(detail.formData) ? detail.formData : {}
      const issue = validateExpenseDetailAmountRules(
        detailFormData,
        detailType || String(detail.detailType || ''),
        String(detail.businessSceneMode || detail.enterpriseMode || defaultBusinessScenario || ''),
        detailSchema
      )
      if (issue) {
        return buildExpenseDetailAmountValidationMessage(issue, detail.detailTitle || detail.detailNo || '未命名明细')
      }
    }
    return ''
  }

  function validateExpenseDetailRequiredValues() {
    const detailSchema = options.templateDetail.value?.expenseDetailSchema || options.emptySchema
    const detailType = options.templateDetail.value?.expenseDetailType
    const defaultBusinessScenario = options.templateDetail.value?.expenseDetailModeDefault
    for (const detail of options.expenseDetails.value) {
      const detailFormData = options.isRecord(detail.formData) ? detail.formData : {}
      const issues = validateRuntimeRequiredValues(detailSchema, detailFormData, {
        shouldValidateBlock: (block: ProcessFormDesignBlock) => (
          isExpenseDetailBlockVisible(block, detailFormData, detailType, defaultBusinessScenario, detailSchema)
            && !isExpenseDetailBlockReadOnly(block)
        )
      })
      if (issues.length) {
        return `请先完善费用明细“${detail.detailTitle || detail.detailNo || '未命名明细'}”：${issues[0]}`
      }
    }
    return ''
  }

  return {
    addExpenseDetail,
    canAddExpenseDetail,
    cloneDetail,
    editExpenseDetail,
    expenseDetailAmountText,
    removeExpenseDetail,
    validateExpenseDetailAmountValues,
    validateExpenseDetailBusinessScenarios,
    validateExpenseDetailRequiredValues
  }
}
