import { computed, nextTick, reactive, ref, watch, type ComputedRef, type Ref } from 'vue'
import type { FinanceVoucherMeta, FinanceVoucherOption } from '@/api'
import { isZeroMoney, normalizeMoneyValue } from '@/utils/money'
import { showBusinessWarning } from '@/utils/businessWarning'
import { buildDepartmentTreeOptions, filterDepartmentTreeNode } from '@/utils/departmentTree'
import { resolveVoucherCashFlowAmount } from '@/utils/financeVoucherAmounts'

type VoucherEntryRowLike = {
  localId: string
  cdigest?: string
  ccode?: string
  ccodeName?: string
  cdeptId?: string
  cpersonId?: string
  ccusId?: string
  csupId?: string
  citemClass?: string
  citemId?: string
  cashFlowItemId?: number
  cashFlowItemName?: string
  cashFlowSubjectCode?: string
  cashFlowSubjectName?: string
  cashFlowAmount?: string
  md?: string
  mc?: string
}

type VoucherAssistCapability = {
  department: boolean
  employee: boolean
  customer: boolean
  supplier: boolean
  project: boolean
  lockedProjectClassCode?: string
}

type CashFlowEditorLine<TEntry extends VoucherEntryRowLike> = {
  rowIndex: number
  row: TEntry
  rowLabel: string
  amountMismatch: boolean
  subjectMismatch: boolean
}

type UseFinanceNewVoucherAssistCashflowOwnerOptions<TEntry extends VoucherEntryRowLike> = {
  voucherMeta: Ref<FinanceVoucherMeta | null>
  entries: Ref<TEntry[]> | ComputedRef<TEntry[]>
  selectedRow: ComputedRef<TEntry>
  selectedRowIndex: Ref<number>
  isReadonlyMode: ComputedRef<boolean>
  selectRow: (index: number) => void
}

export function useFinanceNewVoucherAssistCashflowOwner<TEntry extends VoucherEntryRowLike>(
  options: UseFinanceNewVoucherAssistCashflowOwnerOptions<TEntry>
) {
  const lastValidLeafSubjectByRow = reactive<Record<string, { code: string; name?: string } | undefined>>({})
  const leafSubjectWarningVisible = ref(false)
  const cashFlowDialogVisible = ref(false)
  const cashFlowDialogRowIndex = ref<number | null>(null)

  const accountOptionMap = computed(
    () => new Map((options.voucherMeta.value?.accountOptions || []).map((item) => [item.value, item] as const))
  )
  const cashFlowOptionMap = computed(
    () => new Map((options.voucherMeta.value?.cashFlowOptions || []).map((item) => [item.value, item] as const))
  )
  const cashAccountOptions = computed(() => {
    const optionList = (options.voucherMeta.value?.accountOptions || []).filter((item) => Number(item.bcash || 0) === 1)
    const selectedValues = options.entries.value.flatMap((row) => [normalizeText(row.cashFlowSubjectCode), normalizeText(row.ccode)])
    return appendDisplayOptions(optionList, selectedValues)
  })
  const cashFlowEditorLines = computed<CashFlowEditorLine<TEntry>[]>(() =>
    options.entries.value
      .map((row, rowIndex) => buildCashFlowEditorLine(row, rowIndex))
      .filter((item): item is CashFlowEditorLine<TEntry> => Boolean(item))
  )
  const selectedAccountOption = computed(() => {
    const code = options.selectedRow.value?.ccode
    return code ? accountOptionMap.value.get(code) : undefined
  })
  const currentAssistCapability = computed(() => resolveAssistCapability(selectedAccountOption.value))
  const assistDisabledState = computed(() => ({
    department: options.isReadonlyMode.value || !currentAssistCapability.value.department,
    employee: options.isReadonlyMode.value || !currentAssistCapability.value.employee,
    customer: options.isReadonlyMode.value || !currentAssistCapability.value.customer,
    supplier: options.isReadonlyMode.value || !currentAssistCapability.value.supplier,
    projectClass:
      options.isReadonlyMode.value
      || !currentAssistCapability.value.project
      || Boolean(currentAssistCapability.value.lockedProjectClassCode),
    project: options.isReadonlyMode.value || !currentAssistCapability.value.project
  }))
  const projectClassOptionsForDisplay = computed(() =>
    appendDisplayOption(
      options.voucherMeta.value?.projectClassOptions || [],
      currentAssistCapability.value.lockedProjectClassCode || options.selectedRow.value?.citemClass
    )
  )
  const departmentTreeOptions = computed(() =>
    buildDepartmentTreeOptions(options.voucherMeta.value?.departmentOptions || [])
  )
  const filteredProjectOptions = computed(() => {
    const projectClassCode = currentAssistCapability.value.lockedProjectClassCode || options.selectedRow.value?.citemClass
    const allOptions = options.voucherMeta.value?.projectOptions || []
    const filtered = !projectClassCode
      ? allOptions
      : allOptions.filter((item) => item.parentValue === projectClassCode)
    return appendDisplayOption(filtered, options.selectedRow.value?.citemId)
  })

  watch(
    () =>
      [
        options.selectedRowIndex.value,
        options.selectedRow.value?.ccode,
        options.selectedRow.value?.citemClass,
        options.selectedRow.value?.citemId,
        options.voucherMeta.value?.accountOptions,
        options.voucherMeta.value?.projectOptions,
        options.isReadonlyMode.value
      ] as const,
    () => {
      syncSelectedRowAssistState()
    }
  )

  function buildOptionValueSet(optionList?: FinanceVoucherOption[]) {
    return new Set((optionList || []).map((item) => item.value).filter((value): value is string => Boolean(value)))
  }

  function normalizeText(value?: string | null) {
    const text = String(value || '').trim()
    return text || undefined
  }

  function normalizeCashFlowAmount(value?: string | null) {
    return normalizeMoneyValue(value, { fallback: '' }) || undefined
  }

  function isOptionEnabled(value?: number | null) {
    return Number(value || 0) === 1
  }

  function isLeafAccountOption(option?: FinanceVoucherOption | null) {
    return Number(option?.leafFlag || 0) === 1
  }

  function findAccountOptionByCode(code?: string | null, accountOptions = options.voucherMeta.value?.accountOptions || []) {
    const normalizedCode = normalizeText(code)
    return normalizedCode ? accountOptions.find((item) => item.value === normalizedCode) : undefined
  }

  function rememberLeafSubject(row: TEntry, option?: FinanceVoucherOption | null) {
    if (!option || !isLeafAccountOption(option)) {
      delete lastValidLeafSubjectByRow[row.localId]
      return
    }
    lastValidLeafSubjectByRow[row.localId] = {
      code: option.value,
      name: option.name
    }
  }

  function clearAssistSelections(row: TEntry) {
    row.cdeptId = ''
    row.cpersonId = ''
    row.ccusId = ''
    row.csupId = ''
    row.citemClass = ''
    row.citemId = ''
  }

  function clearRowCashFlow(row: TEntry) {
    row.cashFlowItemId = undefined
    row.cashFlowItemName = ''
    row.cashFlowSubjectCode = ''
    row.cashFlowSubjectName = ''
    row.cashFlowAmount = ''
  }

  function requiresRowCashFlow(row?: TEntry | null) {
    if (!row?.ccode) return false
    const account = findAccountOptionByCode(row.ccode)
    return Number(account?.bcash || 0) === 1 && (!isZeroMoney(row.md || '0') || !isZeroMoney(row.mc || '0'))
  }

  function resolveRowCashFlowSubjectCode(row?: TEntry | null) {
    return normalizeText(row?.ccode) || ''
  }

  function resolveRowCashFlowSubjectName(row?: TEntry | null) {
    if (!row?.ccode) {
      return ''
    }
    return findAccountOptionByCode(row.ccode)?.name || normalizeText(row.ccodeName) || ''
  }

  function resolveRowCashFlowAmount(row?: TEntry | null) {
    return resolveVoucherCashFlowAmount(row) || ''
  }

  function hasRowCashFlowDraft(row?: TEntry | null) {
    if (!row) {
      return false
    }
    return Boolean(
      row.cashFlowItemId
      || normalizeText(row.cashFlowItemName)
      || normalizeText(row.cashFlowSubjectCode)
      || normalizeText(row.cashFlowSubjectName)
      || normalizeCashFlowAmount(row.cashFlowAmount)
    )
  }

  function clearRowCashFlowSelection(row: TEntry) {
    row.cashFlowItemId = undefined
    row.cashFlowItemName = ''
  }

  function rowCashFlowSubjectMatchesVoucher(row?: TEntry | null) {
    if (!row) {
      return true
    }
    const cashFlowSubjectCode = normalizeText(row.cashFlowSubjectCode)
    if (!cashFlowSubjectCode) {
      return true
    }
    return cashFlowSubjectCode === resolveRowCashFlowSubjectCode(row)
  }

  function rowCashFlowAmountMatchesVoucher(row?: TEntry | null) {
    if (!row) {
      return true
    }
    const cashFlowAmount = normalizeCashFlowAmount(row.cashFlowAmount)
    if (!cashFlowAmount) {
      return true
    }
    return cashFlowAmount === resolveRowCashFlowAmount(row)
  }

  function syncRowCashFlowFromVoucher(row: TEntry, clearSelection = false) {
    if (!requiresRowCashFlow(row)) {
      clearRowCashFlow(row)
      return false
    }
    const nextSubjectCode = resolveRowCashFlowSubjectCode(row)
    const nextSubjectName = resolveRowCashFlowSubjectName(row)
    const nextAmount = resolveRowCashFlowAmount(row)
    const changed = normalizeText(row.cashFlowSubjectCode) !== nextSubjectCode
      || normalizeText(row.cashFlowSubjectName) !== normalizeText(nextSubjectName)
      || normalizeCashFlowAmount(row.cashFlowAmount) !== normalizeCashFlowAmount(nextAmount)
    if (changed && clearSelection) {
      clearRowCashFlowSelection(row)
    }
    row.cashFlowSubjectCode = nextSubjectCode
    row.cashFlowSubjectName = nextSubjectName
    row.cashFlowAmount = nextAmount
    return true
  }

  function buildCashFlowEditorLine(row: TEntry, rowIndex: number): CashFlowEditorLine<TEntry> | null {
    if (!requiresRowCashFlow(row) && !hasRowCashFlowDraft(row)) {
      return null
    }
    const digest = normalizeText(row.cdigest)
    return {
      rowIndex,
      row,
      rowLabel: digest ? `第 ${rowIndex + 1} 行 · ${digest}` : `第 ${rowIndex + 1} 行`,
      amountMismatch: !rowCashFlowAmountMatchesVoucher(row),
      subjectMismatch: !rowCashFlowSubjectMatchesVoucher(row)
    }
  }

  function syncRowAccountState(row: TEntry, accountOptions = options.voucherMeta.value?.accountOptions || []) {
    const option = findAccountOptionByCode(row.ccode, accountOptions)
    if (option?.name) {
      row.ccodeName = option.name
    } else if (!row.ccode) {
      row.ccodeName = ''
    }
    if (!row.ccode) {
      delete lastValidLeafSubjectByRow[row.localId]
      return
    }
    if (isLeafAccountOption(option)) {
      rememberLeafSubject(row, option)
      return
    }
    delete lastValidLeafSubjectByRow[row.localId]
  }

  function resetLeafSubjectHistory(rows: TEntry[], accountOptions = options.voucherMeta.value?.accountOptions || []) {
    Object.keys(lastValidLeafSubjectByRow).forEach((key) => {
      delete lastValidLeafSubjectByRow[key]
    })
    rows.forEach((row) => {
      syncRowAccountState(row, accountOptions)
    })
  }

  function focusSubjectField(row: TEntry) {
    if (typeof document === 'undefined') return
    const selector = `[data-subject-row-id="${row.localId}"]`
    const host = document.querySelector(selector)
    if (!(host instanceof HTMLElement)) return
    const focusTarget = host.matches('input,select,[tabindex]')
      ? host
      : host.querySelector<HTMLElement>('input,select,[tabindex],.el-select__wrapper')
    focusTarget?.focus?.()
  }

  async function restoreInvalidLeafSubject(row: TEntry, rowIndex = options.selectedRowIndex.value) {
    if (leafSubjectWarningVisible.value) {
      return false
    }
    const invalidLabel = resolveAccountLabel(row.ccode, row.ccodeName)
    const previousLeafSubject = lastValidLeafSubjectByRow[row.localId]
    leafSubjectWarningVisible.value = true
    try {
      await showBusinessWarning({
        title: '科目不可录入',
        message: `当前科目【${invalidLabel}】不是末级科目，不允许录入凭证，请重新选择末级科目。`,
        confirmButtonText: '返回科目'
      })
    } finally {
      leafSubjectWarningVisible.value = false
    }

    if (previousLeafSubject) {
      row.ccode = previousLeafSubject.code
      row.ccodeName = previousLeafSubject.name || findAccountOptionByCode(previousLeafSubject.code)?.name || ''
    } else {
      row.ccode = ''
      row.ccodeName = ''
    }
    clearAssistSelections(row)
    clearDisabledAssistFields(row, resolveAssistCapability(findAccountOptionByCode(row.ccode)))
    options.selectedRowIndex.value = Math.max(0, Math.min(rowIndex, options.entries.value.length - 1))
    await nextTick()
    focusSubjectField(row)
    return false
  }

  async function ensureSelectedRowUsesLeafSubject() {
    if (options.isReadonlyMode.value) return true
    const row = options.selectedRow.value
    if (!row?.ccode) return true
    const option = findAccountOptionByCode(row.ccode)
    if (!option || isLeafAccountOption(option)) {
      return true
    }
    return restoreInvalidLeafSubject(row, options.selectedRowIndex.value)
  }

  async function tryLeaveSubjectField(nextRowIndex = options.selectedRowIndex.value) {
    const canLeave = await ensureSelectedRowUsesLeafSubject()
    if (canLeave) {
      options.selectRow(nextRowIndex)
    }
    return canLeave
  }

  function resolveCashFlowDialogRowIndex(preferredIndex = options.selectedRowIndex.value) {
    const preferredRow = options.entries.value[preferredIndex]
    if (preferredRow && (requiresRowCashFlow(preferredRow) || hasRowCashFlowDraft(preferredRow))) {
      return preferredIndex
    }
    const matchedIndex = options.entries.value.findIndex((row) => requiresRowCashFlow(row) || hasRowCashFlowDraft(row))
    return matchedIndex >= 0 ? matchedIndex : null
  }

  function openCashFlowDialog(index = options.selectedRowIndex.value) {
    if (options.isReadonlyMode.value) {
      return false
    }
    const targetIndex = resolveCashFlowDialogRowIndex(index)
    if (targetIndex === null) {
      return false
    }
    const row = options.entries.value[targetIndex]
    if (!row) {
      return false
    }
    syncRowCashFlowFromVoucher(row, false)
    cashFlowDialogRowIndex.value = targetIndex
    cashFlowDialogVisible.value = true
    options.selectRow(targetIndex)
    return true
  }

  function closeCashFlowDialog() {
    cashFlowDialogVisible.value = false
    const rowIndex = cashFlowDialogRowIndex.value
    cashFlowDialogRowIndex.value = null
    if (rowIndex === null || options.isReadonlyMode.value) {
      return
    }
    const row = options.entries.value[rowIndex]
    if (!row) return
    nextTick(() => {
      options.selectRow(rowIndex)
      focusSubjectField(row)
    })
  }

  function confirmCashFlowSelection() {
    return cashFlowEditorLines.value.every((item) => !requiresRowCashFlow(item.row) || Boolean(item.row.cashFlowItemId))
  }

  function ensureRowCashFlowState(row: TEntry) {
    if (!requiresRowCashFlow(row)) {
      clearRowCashFlow(row)
      return false
    }
    if (!normalizeText(row.cashFlowSubjectCode) || !normalizeCashFlowAmount(row.cashFlowAmount)) {
      syncRowCashFlowFromVoucher(row, false)
    }
    if (row.cashFlowItemId) {
      const option = cashFlowOptionMap.value.get(String(row.cashFlowItemId))
      row.cashFlowItemName = option?.name || option?.label || row.cashFlowItemName || ''
      return false
    }
    return true
  }

  function resolveAssistCapability(option?: FinanceVoucherOption | null): VoucherAssistCapability {
    return {
      department: isOptionEnabled(option?.bdept),
      employee: isOptionEnabled(option?.bperson),
      customer: isOptionEnabled(option?.bcus),
      supplier: isOptionEnabled(option?.bsup),
      project: isOptionEnabled(option?.bitem),
      lockedProjectClassCode: normalizeText(option?.cassItem)
    }
  }

  function appendDisplayOption(optionList: FinanceVoucherOption[], value?: string, name?: string) {
    const normalizedValue = normalizeText(value)
    if (!normalizedValue || optionList.some((item) => item.value === normalizedValue)) {
      return optionList
    }
    return [
      ...optionList,
      {
        value: normalizedValue,
        code: normalizedValue,
        name,
        label: name ? `${normalizedValue}  ${name}` : normalizedValue
      }
    ]
  }

  function appendDisplayOptions(optionList: FinanceVoucherOption[], values: Array<string | undefined>) {
    return values.reduce(
      (result, value) => appendDisplayOption(result, value),
      optionList
    )
  }

  function clearDisabledAssistFields(row: TEntry, capability: VoucherAssistCapability) {
    if (!capability.department) row.cdeptId = ''
    if (!capability.employee) row.cpersonId = ''
    if (!capability.customer) row.ccusId = ''
    if (!capability.supplier) row.csupId = ''
    if (!capability.project) {
      row.citemClass = ''
      row.citemId = ''
      return
    }
    if (capability.lockedProjectClassCode && row.citemClass !== capability.lockedProjectClassCode) {
      row.citemClass = capability.lockedProjectClassCode
    }
  }

  function syncSelectedRowAssistState() {
    if (options.isReadonlyMode.value) return
    const row = options.selectedRow.value
    if (!row) return
    const capability = resolveAssistCapability(accountOptionMap.value.get(row.ccode || ''))
    clearDisabledAssistFields(row, capability)
    if (!row.citemId) return
    const project = (options.voucherMeta.value?.projectOptions || []).find((item) => item.value === row.citemId)
    if (!project) {
      row.citemId = ''
      return
    }
    const projectClassCode = capability.lockedProjectClassCode || row.citemClass
    if (projectClassCode && project.parentValue && project.parentValue !== projectClassCode) {
      row.citemId = ''
    }
  }

  function validateEntrySelection(row: TEntry, rowNo: number, errors: string[]) {
    const meta = options.voucherMeta.value
    if (!meta) {
      return
    }

    const departmentValues = buildOptionValueSet(meta.departmentOptions)
    const employeeValues = buildOptionValueSet(meta.employeeOptions)
    const customerValues = buildOptionValueSet(meta.customerOptions)
    const supplierValues = buildOptionValueSet(meta.supplierOptions)
    const projectClassValues = buildOptionValueSet(meta.projectClassOptions)
    const projectMap = new Map((meta.projectOptions || []).map((item) => [item.value, item] as const))
    const cashFlowValues = buildOptionValueSet(meta.cashFlowOptions)
    const account = row.ccode ? accountOptionMap.value.get(row.ccode) : undefined
    const capability = resolveAssistCapability(account)

    if (row.ccode && !account) {
      errors.push(`第 ${rowNo} 行科目不存在或当前不可用`)
    }
    if (account && !isLeafAccountOption(account)) {
      errors.push(`第 ${rowNo} 行科目【${formatVoucherOptionLabel(account)}】不是末级科目，不允许录入凭证`)
    }
    if (row.cdeptId && !departmentValues.has(row.cdeptId)) {
      errors.push(`第 ${rowNo} 行部门不存在或当前不可用`)
    }
    if (row.cpersonId && !employeeValues.has(row.cpersonId)) {
      errors.push(`第 ${rowNo} 行人员不存在或当前不可用`)
    }
    if (row.ccusId && !customerValues.has(row.ccusId)) {
      errors.push(`第 ${rowNo} 行客户不存在或当前不可用`)
    }
    if (row.csupId && !supplierValues.has(row.csupId)) {
      errors.push(`第 ${rowNo} 行供应商不存在或当前不可用`)
    }
    if (row.cdeptId && !capability.department) {
      errors.push(`第 ${rowNo} 行当前科目未启用部门辅助核算`)
    }
    if (row.cpersonId && !capability.employee) {
      errors.push(`第 ${rowNo} 行当前科目未启用人员辅助核算`)
    }
    if (row.ccusId && !capability.customer) {
      errors.push(`第 ${rowNo} 行当前科目未启用客户辅助核算`)
    }
    if (row.csupId && !capability.supplier) {
      errors.push(`第 ${rowNo} 行当前科目未启用供应商辅助核算`)
    }
    if (row.citemClass && !projectClassValues.has(row.citemClass)) {
      errors.push(`第 ${rowNo} 行项目分类不存在或当前不可用`)
    }
    if ((row.citemClass || row.citemId) && !capability.project) {
      errors.push(`第 ${rowNo} 行当前科目未启用项目辅助核算`)
    }
    if (capability.lockedProjectClassCode && row.citemClass && row.citemClass !== capability.lockedProjectClassCode) {
      errors.push(`第 ${rowNo} 行项目分类必须为科目挂载的项目分类【${capability.lockedProjectClassCode}】`)
    }
    if (row.citemId) {
      if (!row.citemClass) {
        errors.push(`第 ${rowNo} 行选择项目时必须同时选择项目分类`)
        return
      }
      const project = projectMap.get(row.citemId)
      if (!project) {
        errors.push(`第 ${rowNo} 行项目不存在或当前不可用`)
        return
      }
      if (project.parentValue && project.parentValue !== row.citemClass) {
        errors.push(`第 ${rowNo} 行项目分类与项目归属不匹配`)
      }
    }
    if (Number(account?.bcash || 0) === 1 && (!isZeroMoney(row.md || '0') || !isZeroMoney(row.mc || '0'))) {
      const expectedSubjectCode = resolveRowCashFlowSubjectCode(row)
      const expectedAmount = resolveRowCashFlowAmount(row)
      const enteredSubjectCode = normalizeText(row.cashFlowSubjectCode)
      const enteredAmount = normalizeCashFlowAmount(row.cashFlowAmount)
      if (enteredSubjectCode && enteredSubjectCode !== expectedSubjectCode) {
        errors.push(`第 ${rowNo} 行现金流量科目必须与凭证分录科目一致`)
      }
      if (enteredAmount && enteredAmount !== expectedAmount) {
        errors.push(`第 ${rowNo} 行现金流量金额必须与凭证分录金额一致`)
      }
      if (!row.cashFlowItemId) {
        errors.push(`第 ${rowNo} 行科目已启用现金管理，必须选择现金流量`)
      } else if (!cashFlowValues.has(String(row.cashFlowItemId))) {
        errors.push(`第 ${rowNo} 行现金流量不存在或当前不可用`)
      }
    } else if (hasRowCashFlowDraft(row)) {
      errors.push(`第 ${rowNo} 行当前分录不需要现金流量，请先清除已录入的现金流量信息`)
    }
  }

  function handleSubjectChange(index: number, value?: string | number) {
    options.selectRow(index)
    const row = options.entries.value[index]
    if (!row) return
    row.ccode = normalizeText(typeof value === 'number' ? String(value) : value) || ''
    const option = findAccountOptionByCode(row.ccode)
    row.ccodeName = option?.name || ''
    if (!row.ccode) {
      delete lastValidLeafSubjectByRow[row.localId]
      clearAssistSelections(row)
      clearRowCashFlow(row)
      return
    }
    if (isLeafAccountOption(option)) {
      rememberLeafSubject(row, option)
    }
    if (Number(option?.bcash || 0) !== 1) {
      clearRowCashFlow(row)
    } else {
      syncRowCashFlowFromVoucher(row, true)
    }
  }

  function handleSubjectFieldFocus(index: number) {
    if (index === options.selectedRowIndex.value) {
      options.selectRow(index)
      return
    }
    void tryLeaveSubjectField(index)
  }

  function handleSubjectDropdownVisibleChange(index: number, visible: boolean) {
    if (!visible) return
    handleSubjectFieldFocus(index)
  }

  function handleAssistFieldFocus() {
    void ensureSelectedRowUsesLeafSubject()
  }

  function handleCashFlowFieldFocus() {
    if (options.isReadonlyMode.value) return
    const row = options.selectedRow.value
    if (!row) {
      return
    }
    if (!requiresRowCashFlow(row) && !hasRowCashFlowDraft(row)) {
      const opened = openCashFlowDialog(options.selectedRowIndex.value)
      if (!opened) {
        return
      }
      return
    }
    ensureRowCashFlowState(row)
    openCashFlowDialog(options.selectedRowIndex.value)
  }

  function handleAmountValueChange(index: number) {
    if (options.isReadonlyMode.value) return
    const row = options.entries.value[index]
    if (!row) return
    if (!requiresRowCashFlow(row)) {
      clearRowCashFlow(row)
      return
    }
    syncRowCashFlowFromVoucher(row, true)
  }

  function handleAmountBlur(index: number) {
    if (options.isReadonlyMode.value) return
    const row = options.entries.value[index]
    if (!row) return
    if (requiresRowCashFlow(row)) {
      syncRowCashFlowFromVoucher(row, true)
    }
    if (ensureRowCashFlowState(row)) {
      openCashFlowDialog(index)
    }
  }

  function handleCashFlowSubjectChange(index: number, value?: string | number) {
    const row = options.entries.value[index]
    if (!row) {
      return
    }
    const subjectCode = normalizeText(typeof value === 'number' ? String(value) : value) || ''
    const option = cashAccountOptions.value.find((item) => item.value === subjectCode)
    row.cashFlowSubjectCode = subjectCode
    row.cashFlowSubjectName = option?.name || option?.label || ''
    clearRowCashFlowSelection(row)
  }

  function handleCashFlowAmountChange(index: number, value?: string) {
    const row = options.entries.value[index]
    if (!row) {
      return
    }
    row.cashFlowAmount = normalizeCashFlowAmount(value) || ''
    clearRowCashFlowSelection(row)
  }

  function handleCashFlowItemChange(index: number, value?: string | number) {
    const row = options.entries.value[index]
    if (!row) {
      return
    }
    const normalizedValue = normalizeText(typeof value === 'number' ? String(value) : value)
    if (!normalizedValue) {
      clearRowCashFlowSelection(row)
      return
    }
    const option = cashFlowOptionMap.value.get(normalizedValue)
    if (!option) {
      clearRowCashFlowSelection(row)
      return
    }
    row.cashFlowItemId = Number(option.value)
    row.cashFlowItemName = option.name || option.label || option.code || ''
  }

  function formatVoucherOptionLabel(option?: FinanceVoucherOption | null) {
    if (!option) return ''
    if (option.code && option.name) return `${option.code}  ${option.name}`
    if (option.name) return option.name
    if (option.code) return option.code
    return option.label || option.value
  }

  function resolveAccountLabel(code?: string, accountName?: string) {
    if (!code) return '当前行'
    const matched = options.voucherMeta.value?.accountOptions.find((item) => item.value === code)
    if (matched) return formatVoucherOptionLabel(matched)
    if (accountName) return `${code}  ${accountName}`
    return code
  }

  return {
    cashFlowDialogVisible,
    cashFlowDialogRowIndex,
    cashFlowEditorLines,
    cashAccountOptions,
    currentAssistCapability,
    assistDisabledState,
    projectClassOptionsForDisplay,
    departmentTreeOptions,
    filteredProjectOptions,
    requiresRowCashFlow,
    resetLeafSubjectHistory,
    ensureSelectedRowUsesLeafSubject,
    tryLeaveSubjectField,
    closeCashFlowDialog,
    confirmCashFlowSelection,
    ensureRowCashFlowState,
    validateEntrySelection,
    handleSubjectChange,
    handleSubjectFieldFocus,
    handleSubjectDropdownVisibleChange,
    handleAssistFieldFocus,
    handleCashFlowFieldFocus,
    handleAmountValueChange,
    handleAmountBlur,
    handleCashFlowSubjectChange,
    handleCashFlowAmountChange,
    handleCashFlowItemChange,
    filterDepartmentTreeNode,
    resolveAccountLabel
  }
}
