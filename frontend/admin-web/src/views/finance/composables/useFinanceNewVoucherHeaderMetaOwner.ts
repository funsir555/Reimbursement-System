import { computed, watch, type ComputedRef, type Ref } from 'vue'
import { ElMessage } from 'element-plus'
import { financeApi, type FinanceVoucherMeta } from '@/api'
import type { FinanceVoucherEntryRow } from './useFinanceNewVoucherRowOwner'
import type { FinanceVoucherOption } from '@/api'

type FinanceCompanyLike = {
  currentCompanyId?: string
  currentCompanyName?: string
  companyOptions?: Array<{ companyId: string; companyName: string; hasActiveAccountSet?: boolean }>
  currentCompanyHasActiveAccountSet?: boolean
}

type VoucherFormStateLike = {
  companyId: string
  iperiod: number
  csign: string
  inoId?: number
  dbillDate: string
  idoc: number
  cbill: string
  ctext1?: string
  ctext2?: string
  entries: FinanceVoucherEntryRow[]
}

type UseFinanceNewVoucherHeaderMetaOwnerOptions = {
  form: VoucherFormStateLike
  financeCompany: FinanceCompanyLike
  voucherMeta: Ref<FinanceVoucherMeta | null>
  selectedRow: ComputedRef<FinanceVoucherEntryRow>
  currentAssistCapability: ComputedRef<{ lockedProjectClassCode?: string }>
  voucherHeaderLocked: ComputedRef<boolean>
  loading: Ref<boolean>
  initializing: Ref<boolean>
  resolveAccountLabel: (code?: string, accountName?: string) => string
  resetLeafSubjectHistory: (rows: FinanceVoucherEntryRow[], accountOptions?: FinanceVoucherOption[]) => void
  resolveErrorMessage: (error: unknown, fallback: string) => string
}

export function useFinanceNewVoucherHeaderMetaOwner(options: UseFinanceNewVoucherHeaderMetaOwnerOptions) {
  const currentCompanyOption = computed(() =>
    options.financeCompany.companyOptions?.find((item) => item.companyId === (options.financeCompany.currentCompanyId || options.form.companyId))
  )

  const currentRowLabel = computed(() => {
    if (!options.selectedRow.value?.ccode) return ''
    return options.resolveAccountLabel(options.selectedRow.value.ccode, options.selectedRow.value.ccodeName)
  })

  const currentCompanyName = computed(() =>
    options.financeCompany.currentCompanyName
      || currentCompanyOption.value?.companyName
      || resolveCompanyName(options.form.companyId)
  )

  const currentCompanyHasActiveAccountSet = computed(() => {
    if (typeof options.financeCompany.currentCompanyHasActiveAccountSet === 'boolean') {
      return options.financeCompany.currentCompanyHasActiveAccountSet
    }
    if (typeof currentCompanyOption.value?.hasActiveAccountSet === 'boolean') {
      return currentCompanyOption.value.hasActiveAccountSet
    }
    return Boolean(options.financeCompany.currentCompanyId || options.form.companyId)
  })

  const voucherNoticeItems = computed<Array<{ level: 'warning' | 'danger' | 'info'; text: string }>>(() => {
    const notices: Array<{ level: 'warning' | 'danger' | 'info'; text: string }> = []
    if (!options.financeCompany.currentCompanyId) {
      notices.push({ level: 'warning', text: '当前未选择财务公司，请先选择公司后再录入凭证。' })
      return notices
    }
    if (!currentCompanyHasActiveAccountSet.value) {
      notices.push({ level: 'warning', text: '当前公司未创建账套，请切换公司或先建账。' })
      return notices
    }
    if (!options.voucherMeta.value) {
      return notices
    }
    if (!options.voucherMeta.value.accountOptions?.length) {
      notices.push({ level: 'danger', text: '当前公司账套已启用，但暂无会计科目数据，请检查账套初始化结果。' })
    }
    if (!options.voucherMeta.value.customerOptions?.length) {
      notices.push({ level: 'info', text: '当前公司暂无客户档案数据。' })
    }
    if (!options.voucherMeta.value.supplierOptions?.length) {
      notices.push({ level: 'info', text: '当前公司暂无供应商档案数据。' })
    }
    if (!options.voucherMeta.value.projectClassOptions?.length || !options.voucherMeta.value.projectOptions?.length) {
      notices.push({ level: 'info', text: '当前公司暂无项目档案数据。' })
    }
    const lockedProjectClassCode = options.currentAssistCapability.value.lockedProjectClassCode
    if (lockedProjectClassCode && !(options.voucherMeta.value.projectClassOptions || []).some((item) => item.value === lockedProjectClassCode)) {
      notices.push({ level: 'warning', text: `当前科目挂载的项目分类【${lockedProjectClassCode}】不存在或当前不可用，请先维护项目档案。` })
    }
    return notices
  })

  const remarkText = computed({
    get: () => options.form.ctext2 || options.form.ctext1 || '',
    set: (value: string) => {
      options.form.ctext1 = ''
      options.form.ctext2 = value
    }
  })

  const voucherNoInput = computed({
    get: () => (options.form.inoId === undefined || options.form.inoId === null ? '' : String(options.form.inoId)),
    set: (value: string) => {
      if (options.voucherHeaderLocked.value) return
      const digits = String(value || '').replace(/\D/g, '')
      options.form.inoId = digits ? Number(digits) : undefined
    }
  })

  watch(() => options.form.dbillDate, (value) => {
    if (options.initializing.value || options.voucherHeaderLocked.value) return
    const nextPeriod = inferPeriod(value)
    if (nextPeriod) options.form.iperiod = nextPeriod
  })

  watch(() => [options.form.dbillDate, options.form.csign] as const, async () => {
    if (options.initializing.value || options.loading.value || !options.voucherMeta.value || options.voucherHeaderLocked.value) return
    await refreshSuggestedVoucherNo()
  })

  async function refreshSuggestedVoucherNo() {
    try {
      const companyId = options.financeCompany.currentCompanyId || options.form.companyId
      const res = await financeApi.getVoucherMeta({ companyId, billDate: options.form.dbillDate, csign: options.form.csign })
      options.voucherMeta.value = res.data
      options.resetLeafSubjectHistory(options.form.entries, res.data.accountOptions)
      options.form.companyId = companyId || ''
      options.form.inoId = res.data.suggestedVoucherNo
      if (!options.form.cbill) options.form.cbill = res.data.defaultMaker
    } catch (error: unknown) {
      ElMessage.error(options.resolveErrorMessage(error, '刷新凭证编号失败'))
    }
  }

  function resolveCompanyName(companyId?: string) {
    if (!companyId) return '未设置'
    const matched = options.voucherMeta.value?.companyOptions.find((item) => item.value === companyId)
    return matched?.name || companyId
  }

  function inferPeriod(value: string) {
    const month = Number(value?.split('-')?.[1])
    return Number.isFinite(month) && month >= 1 && month <= 12 ? month : undefined
  }

  return {
    currentCompanyOption,
    currentRowLabel,
    currentCompanyName,
    currentCompanyHasActiveAccountSet,
    voucherNoticeItems,
    remarkText,
    voucherNoInput,
    refreshSuggestedVoucherNo
  }
}
