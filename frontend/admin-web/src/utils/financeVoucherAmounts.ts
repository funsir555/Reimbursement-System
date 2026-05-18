import {
  addMoney,
  isZeroMoney,
  normalizeMoneyValue,
  type MoneyInputValue
} from './money'

export type VoucherAmountField = 'md' | 'mc'
export type VoucherActualDirection = 'DEBIT' | 'CREDIT'

export type VoucherAmountLike = {
  md?: MoneyInputValue
  mc?: MoneyInputValue
}

export function normalizeSignedVoucherMoney(value?: MoneyInputValue) {
  const normalized = normalizeMoneyValue(value, { allowNegative: true, fallback: '' })
  return normalized || undefined
}

export function hasVoucherFieldAmount(field: VoucherAmountField, value?: MoneyInputValue) {
  return resolveVoucherFieldActualDirection(field, value) !== null
}

export function resolveVoucherFieldActualDirection(
  field: VoucherAmountField,
  value?: MoneyInputValue
): VoucherActualDirection | null {
  const normalized = normalizeSignedVoucherMoney(value)
  if (!normalized || isZeroMoney(normalized)) {
    return null
  }
  if (field === 'md') {
    return normalized.startsWith('-') ? 'CREDIT' : 'DEBIT'
  }
  return normalized.startsWith('-') ? 'DEBIT' : 'CREDIT'
}

export function resolveVoucherFieldDisplayAmount(field: VoucherAmountField, value?: MoneyInputValue) {
  if (!hasVoucherFieldAmount(field, value)) {
    return '0.00'
  }
  const normalized = normalizeSignedVoucherMoney(value)
  return normalized?.startsWith('-') ? normalized.slice(1) : normalized || '0.00'
}

export function resolveOppositeVoucherAmountField(field: VoucherAmountField): VoucherAmountField {
  return field === 'md' ? 'mc' : 'md'
}

export function encodeVoucherFieldAmount(
  field: VoucherAmountField,
  actualDirection: VoucherActualDirection,
  displayAmount?: MoneyInputValue
) {
  const normalizedAmount = normalizeSignedVoucherMoney(displayAmount)
  if (!normalizedAmount || isZeroMoney(normalizedAmount)) {
    return ''
  }
  const absoluteAmount = normalizedAmount.startsWith('-') ? normalizedAmount.slice(1) : normalizedAmount
  if (field === 'md') {
    return actualDirection === 'DEBIT' ? absoluteAmount : `-${absoluteAmount}`
  }
  return actualDirection === 'CREDIT' ? absoluteAmount : `-${absoluteAmount}`
}

export function toggleVoucherAmountDirection(
  field: VoucherAmountField,
  value?: MoneyInputValue
) {
  const currentDirection = resolveVoucherFieldActualDirection(field, value)
  const nextField = resolveOppositeVoucherAmountField(field)
  if (!currentDirection) {
    return {
      md: '',
      mc: '',
      nextField,
      changed: false
    }
  }
  const nextDirection: VoucherActualDirection = currentDirection === 'DEBIT' ? 'CREDIT' : 'DEBIT'
  const nextValue = encodeVoucherFieldAmount(nextField, nextDirection, resolveVoucherFieldDisplayAmount(field, value))
  return {
    md: nextField === 'md' ? nextValue : '',
    mc: nextField === 'mc' ? nextValue : '',
    nextField,
    changed: true
  }
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

export function resolveVoucherCashFlowAmount(row?: VoucherAmountLike | null) {
  const debit = resolveVoucherEffectiveDebit(row)
  if (!isZeroMoney(debit)) {
    return debit
  }
  const credit = resolveVoucherEffectiveCredit(row)
  if (!isZeroMoney(credit)) {
    return credit
  }
  return ''
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
