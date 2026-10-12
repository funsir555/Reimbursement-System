import { centsToMoney, normalizeMoneyValue, toMoneyCents } from '@/utils/money'

type BalanceTreeRow<T> = {
  mb: string | number
  draftBalance: string
  balanceDirection?: string
  children?: T[]
}

export function recalculateTreeBalances<T extends BalanceTreeRow<T>>(rows: T[]): void {
  for (const row of rows) {
    recalculateNode(row)
  }
}

function recalculateNode<T extends BalanceTreeRow<T>>(row: T): bigint {
  if (!row.children?.length) {
    row.mb = normalizeMoneyValue(row.mb ?? row.draftBalance, { allowNegative: true, fallback: '0.00' })
    const amount = toMoneyCents(row.mb)
    return isDebitDirection(row.balanceDirection) ? amount : -amount
  }
  const signed = row.children.reduce((sum, child) => sum + recalculateNode(child), 0n)
  row.mb = centsToMoney(isDebitDirection(row.balanceDirection) ? signed : -signed)
  row.draftBalance = row.mb
  return signed
}

function isDebitDirection(direction?: string): boolean {
  return !direction || direction.toUpperCase().includes('DEBIT') || direction.includes('借')
}
