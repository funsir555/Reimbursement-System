function padPeriodNumber(value: number) {
  return String(value).padStart(2, '0')
}

export function formatFinancePeriodMonthEnd(year?: number, period?: number) {
  const resolvedYear = Number(year || 0)
  const resolvedPeriod = Number(period || 0)
  if (!Number.isInteger(resolvedYear) || resolvedYear <= 0) {
    return ''
  }
  if (!Number.isInteger(resolvedPeriod) || resolvedPeriod < 1 || resolvedPeriod > 12) {
    return ''
  }
  const lastDay = new Date(resolvedYear, resolvedPeriod, 0).getDate()
  return `${resolvedYear}-${padPeriodNumber(resolvedPeriod)}-${padPeriodNumber(lastDay)}`
}
