import {
  addMoney,
  isZeroMoney,
  normalizeMoneyValue,
  type MoneyInputValue
} from './money'

export type VoucherAmountField = 'md' | 'mc'

export type VoucherAmountLike = {
  md?: MoneyInputValue
  mc?: MoneyInputValue
}

export function normalizeSignedVoucherMoney(value?: MoneyInputValue) {
  const normalized = normalizeMoneyValue(value, { allowNegative: true, fallback: '' })
  return normalized || undefined
}

export function positiveVoucherMoney(value?: MoneyInputValue) {
  const normalized = normalizeSignedVoucherMoney(value)
  if (!normalized || isZeroMoney(normalized) || normalized.startsWith('-')) {
    return '0.00'
  }
  return normalized
}

export function negativeVoucherMoneyAsAbsolute(value?: MoneyInputValue) {
  const normalized = normalizeSignedVoucherMoney(value)
  if (!normalized || isZeroMoney(normalized) || !normalized.startsWith('-')) {
    return '0.00'
  }
  return normalized.slice(1)
}

export function resolveVoucherEffectiveDebit(row?: VoucherAmountLike | null) {
  if (!row) {
    return '0.00'
  }
  return addMoney(
    positiveVoucherMoney(row.md),
    negativeVoucherMoneyAsAbsolute(row.mc)
  )
}

export function resolveVoucherEffectiveCredit(row?: VoucherAmountLike | null) {
  if (!row) {
    return '0.00'
  }
  return addMoney(
    positiveVoucherMoney(row.mc),
    negativeVoucherMoneyAsAbsolute(row.md)
  )
}

export function hasVoucherEffectiveDebit(row?: VoucherAmountLike | null) {
  return !isZeroMoney(resolveVoucherEffectiveDebit(row))
}

export function hasVoucherEffectiveCredit(row?: VoucherAmountLike | null) {
  return !isZeroMoney(resolveVoucherEffectiveCredit(row))
}

export function sumVoucherEffectiveDebit(rows: VoucherAmountLike[]) {
  return rows.reduce((total, row) => addMoney(total, resolveVoucherEffectiveDebit(row)), '0.00')
}

export function sumVoucherEffectiveCredit(rows: VoucherAmountLike[]) {
  return rows.reduce((total, row) => addMoney(total, resolveVoucherEffectiveCredit(row)), '0.00')
}

export function resolveVoucherBalanceGap(rows: VoucherAmountLike[]) {
  return addMoney(sumVoucherEffectiveDebit(rows), negateVoucherMoney(sumVoucherEffectiveCredit(rows)))
}

export function resolveVoucherAutoBalanceValue(
  rows: VoucherAmountLike[],
  rowIndex: number,
  field: VoucherAmountField
) {
  const rowsWithoutTarget = rows.map((row, index) => {
    if (index !== rowIndex) {
      return row
    }
    return {
      ...row,
      [field]: ''
    }
  })
  const gap = resolveVoucherBalanceGap(rowsWithoutTarget)
  return field === 'mc' ? gap : negateVoucherMoney(gap)
}

export function negateVoucherMoney(value?: MoneyInputValue) {
  const normalized = normalizeMoneyValue(value, { allowNegative: true, fallback: '0.00' })
  if (isZeroMoney(normalized)) {
    return '0.00'
  }
  return normalized.startsWith('-') ? normalized.slice(1) : `-${normalized}`
}
