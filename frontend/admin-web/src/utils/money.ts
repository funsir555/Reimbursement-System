export type MoneyInputValue = string | number | null | undefined

const TWO_DECIMAL_PATTERN = /^-?(0|[1-9]\d*)(\.\d{0,2})?$/

function normalizeRaw(value: MoneyInputValue): string {
  if (value === null || value === undefined) {
    return ''
  }
  return String(value).trim().replace(/,/g, '')
}

export function isMoneyText(value: MoneyInputValue, allowNegative = false) {
  const text = normalizeRaw(value)
  if (!text) {
    return false
  }
  if (!allowNegative && text.startsWith('-')) {
    return false
  }
  return TWO_DECIMAL_PATTERN.test(text)
}

export function normalizeMoneyValue(value: MoneyInputValue, options: { allowNegative?: boolean; fallback?: string } = {}) {
  const { allowNegative = false, fallback = '' } = options
  const text = normalizeRaw(value)
  if (!text) {
    return fallback
  }
  if (!isMoneyText(text, allowNegative)) {
    throw new Error(allowNegative ? '请输入合法金额，最多保留两位小数' : '请输入合法的非负金额，最多保留两位小数')
  }
  return centsToMoney(toMoneyCents(text))
}

export function toMoneyCents(value: MoneyInputValue) {
  const text = normalizeRaw(value)
  if (!text) {
    return 0n
  }
  const negative = text.startsWith('-')
  const normalized = negative ? text.slice(1) : text
  const [wholePart, decimalPart = ''] = normalized.split('.')
  const centsText = `${wholePart || '0'}${decimalPart.padEnd(2, '0').slice(0, 2)}`
  const cents = BigInt(centsText || '0')
  return negative ? -cents : cents
}

export function centsToMoney(value: bigint) {
  const negative = value < 0n
  const absolute = negative ? -value : value
  const whole = absolute / 100n
  const cents = absolute % 100n
  const text = `${whole}.${cents.toString().padStart(2, '0')}`
  return negative ? `-${text}` : text
}

export function addMoney(...values: MoneyInputValue[]) {
  const total = values.reduce((sum, current) => sum + toMoneyCents(current), 0n)
  return centsToMoney(total)
}

export function subtractMoney(left: MoneyInputValue, right: MoneyInputValue) {
  return centsToMoney(toMoneyCents(left) - toMoneyCents(right))
}

export function absMoney(value: MoneyInputValue) {
  const cents = toMoneyCents(value)
  return centsToMoney(cents < 0n ? -cents : cents)
}

export function compareMoney(left: MoneyInputValue, right: MoneyInputValue) {
  const gap = toMoneyCents(left) - toMoneyCents(right)
  if (gap === 0n) {
    return 0
  }
  return gap > 0n ? 1 : -1
}

export function formatMoney(value: MoneyInputValue) {
  const normalized = normalizeMoneyValue(value, { fallback: '0.00', allowNegative: true })
  const negative = normalized.startsWith('-')
  const plain = negative ? normalized.slice(1) : normalized
  const [wholePart, decimalPart = '00'] = plain.split('.')
  const groupedWhole = wholePart.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return `${negative ? '-' : ''}${groupedWhole}.${decimalPart}`
}

export function isZeroMoney(value: MoneyInputValue) {
  return toMoneyCents(value) === 0n
}
