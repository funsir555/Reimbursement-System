import { computed, type Ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FinanceVoucherMeta, FinanceVoucherSavePayload } from '@/api'
import type { FinanceVoucherEntryRow } from './useFinanceNewVoucherRowOwner'
import { isZeroMoney } from '@/utils/money'
import {
  hasVoucherEffectiveCredit,
  hasVoucherEffectiveDebit,
  normalizeSignedVoucherMoney,
  resolveVoucherBalanceGap,
  resolveVoucherEffectiveCredit,
  resolveVoucherEffectiveDebit,
  sumVoucherEffectiveCredit,
  sumVoucherEffectiveDebit
} from '@/utils/financeVoucherAmounts'

type VoucherFormStateLike = {
  companyId: string
  iyear?: number
  iyperiod?: number
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

type UseFinanceNewVoucherValidationPayloadOptions = {
  form: VoucherFormStateLike
  voucherMeta: Ref<FinanceVoucherMeta | null>
  validationErrors: Ref<string[]>
  entryFieldMaxLength: Record<string, number>
  entryFieldLabels: Record<string, string>
  validateEntrySelection: (row: FinanceVoucherEntryRow, rowNo: number, errors: string[]) => void
  toOptionalString: (value?: string | null) => string | undefined
  toOptionalMoney: (value?: string | null) => string | undefined
  toOptionalDecimal: (value?: number | null, precision?: number) => number | undefined
}

export function useFinanceNewVoucherValidationPayload(options: UseFinanceNewVoucherValidationPayloadOptions) {
  const effectiveRows = computed(() => options.form.entries.filter((item) => !isEntryBlank(item)))
  const totalDebit = computed(() => sumVoucherEffectiveDebit(effectiveRows.value))
  const totalCredit = computed(() => sumVoucherEffectiveCredit(effectiveRows.value))
  const balanceGap = computed(() => resolveVoucherBalanceGap(effectiveRows.value))

  function buildPayload(includeBlankRows = false): FinanceVoucherSavePayload {
    const note = options.toOptionalString(options.form.ctext2 || options.form.ctext1) || ''
    const entries = (includeBlankRows ? options.form.entries : effectiveRows.value).map((item, index) => ({
      inid: index + 1,
      cdigest: options.toOptionalString(item.cdigest) || '',
      ccode: item.ccode || '',
      cdeptId: options.toOptionalString(item.cdeptId),
      cpersonId: options.toOptionalString(item.cpersonId),
      ccusId: options.toOptionalString(item.ccusId),
      csupId: options.toOptionalString(item.csupId),
      citemClass: options.toOptionalString(item.citemClass),
      citemId: options.toOptionalString(item.citemId),
      cashFlowItemId: item.cashFlowItemId,
      cashFlowItemName: options.toOptionalString(item.cashFlowItemName),
      cexchName: options.toOptionalString(item.cexchName) || options.voucherMeta.value?.defaultCurrencyName || options.voucherMeta.value?.defaultCurrency || '人民币',
      currencyCode: options.toOptionalString(item.currencyCode) || options.voucherMeta.value?.defaultCurrencyCode || options.voucherMeta.value?.defaultCurrency || 'CNY',
      nfrat: options.toOptionalDecimal(item.nfrat, 2),
      md: normalizeMoneyField(item.md),
      mc: normalizeMoneyField(item.mc),
      ndS: options.toOptionalDecimal(item.ndS, 6),
      ncS: options.toOptionalDecimal(item.ncS, 6)
    }))

    return {
      companyId: options.form.companyId,
      iyear: options.form.iyear,
      iyperiod: options.form.iyperiod,
      iperiod: options.form.iperiod,
      csign: options.form.csign,
      inoId: options.form.inoId,
      dbillDate: options.form.dbillDate,
      idoc: options.form.idoc,
      cbill: options.form.cbill,
      ctext1: '',
      ctext2: note,
      entries
    }
  }

  function validateEntryLength(row: FinanceVoucherEntryRow, rowNo: number, errors: string[]) {
    ;(Object.entries(options.entryFieldMaxLength) as Array<[keyof typeof options.entryFieldMaxLength, number]>).forEach(([fieldKey, maxLength]) => {
      const value = row[fieldKey as keyof FinanceVoucherEntryRow]
      if (typeof value !== 'string') {
        return
      }
      const normalized = value.trim()
      if (normalized.length > maxLength) {
        errors.push(`第 ${rowNo} 行${options.entryFieldLabels[fieldKey]}最多 ${maxLength} 个字符`)
      }
    })
  }

  function validateVoucher(showToast = false) {
    const errors: string[] = []
    const entries = effectiveRows.value

    if (!options.form.companyId) errors.push('当前公司未设置')
    if (!options.form.dbillDate) errors.push('请选择制单日期')
    if (!options.form.csign) errors.push('请选择凭证类别')
    if (!options.form.iperiod || options.form.iperiod < 1 || options.form.iperiod > 12) errors.push('会计期间必须在 1 到 12 之间')
    if (entries.length < 2) errors.push('至少需要两条有效分录')

    entries.forEach((row, index) => {
      const rowNo = index + 1
      if (!(row.cdigest || '').trim()) errors.push(`第 ${rowNo} 行摘要不能为空`)
      if (!row.ccode) errors.push(`第 ${rowNo} 行请选择科目`)
      validateEntryLength(row, rowNo, errors)
      options.validateEntrySelection(row, rowNo, errors)
      if (hasVoucherEffectiveDebit(row) && hasVoucherEffectiveCredit(row)) errors.push(`第 ${rowNo} 行借贷不能同时填写`)
      if (!hasVoucherEffectiveDebit(row) && !hasVoucherEffectiveCredit(row)) errors.push(`第 ${rowNo} 行借方或贷方至少填写一项`)
      if ((row.nfrat ?? 1) <= 0) errors.push(`第 ${rowNo} 行汇率必须大于 0`)
    })

    if (entries.length >= 2 && !isZeroMoney(balanceGap.value)) errors.push('借方合计必须等于贷方合计')

    options.validationErrors.value = Array.from(new Set(errors))
    if (showToast) {
      options.validationErrors.value.length
        ? ElMessage.warning(options.validationErrors.value[0])
        : ElMessage.success('凭证校验通过，借贷已平衡')
    }
    return options.validationErrors.value.length === 0
  }

  function isEntryBlank(row: FinanceVoucherEntryRow) {
    return !(row.cdigest || '').trim()
      && !row.ccode
      && !row.cdeptId
      && !row.cpersonId
      && !row.ccusId
      && !row.csupId
      && !row.citemClass
      && !row.citemId
      && !row.cashFlowItemId
      && !normalizeMoneyField(row.md)
      && !normalizeMoneyField(row.mc)
      && !row.ndS
      && !row.ncS
  }

  function buildSnapshot() {
    return JSON.stringify(buildPayload(true))
  }

  function normalizeMoneyField(value?: string) {
    const normalized = normalizeSignedVoucherMoney(value)
    if (!normalized || isZeroMoney(normalized)) {
      return undefined
    }
    return normalized
  }

  return {
    effectiveRows,
    totalDebit,
    totalCredit,
    balanceGap,
    buildPayload,
    validateVoucher,
    isEntryBlank,
    buildSnapshot,
    normalizeMoneyField,
    resolveRowEffectiveDebit: resolveVoucherEffectiveDebit,
    resolveRowEffectiveCredit: resolveVoucherEffectiveCredit
  }
}
