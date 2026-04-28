import { ElMessage } from 'element-plus'
import { defineStore } from 'pinia'
import { useFinanceCompanyStore } from './financeCompany'

const STORAGE_KEY = 'finance-current-period-map'

type PersistedPeriod = {
  year: number
  period: number
}

type PersistedPeriodMap = Record<string, PersistedPeriod>

type PeriodWindow = {
  startYear: number
  startMonth: number
  endYear: number
  endMonth: number
}

function normalizeText(value?: string | null) {
  const text = String(value || '').trim()
  return text || ''
}

function toPositiveInteger(value?: number | null) {
  const numeric = Number(value)
  return Number.isInteger(numeric) && numeric > 0 ? numeric : 0
}

function buildYearPeriod(year: number, period: number) {
  return year > 0 && period > 0 ? year * 100 + period : 0
}

function compareYearMonth(leftYear: number, leftMonth: number, rightYear: number, rightMonth: number) {
  return buildYearPeriod(leftYear, leftMonth) - buildYearPeriod(rightYear, rightMonth)
}

function readPersistedMap() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return {} as PersistedPeriodMap
    const parsed = JSON.parse(raw) as PersistedPeriodMap
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    localStorage.removeItem(STORAGE_KEY)
    return {} as PersistedPeriodMap
  }
}

function writePersistedMap(map: PersistedPeriodMap) {
  if (!Object.keys(map).length) {
    localStorage.removeItem(STORAGE_KEY)
    return
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(map))
}

export const useFinancePeriodStore = defineStore('financePeriod', {
  state: () => ({
    initialized: false,
    companyId: '',
    currentYear: 0,
    currentPeriod: 0,
    currentYearPeriod: 0,
    periodStartYear: 0,
    periodStartMonth: 0,
    periodEndYear: 0,
    periodEndMonth: 0
  }),
  getters: {
    hasAvailableRange: (state) => state.periodStartYear > 0 && state.periodStartMonth > 0 && state.periodEndYear > 0 && state.periodEndMonth > 0,
    hasPeriodContext: (state) => state.currentYear > 0 && state.currentPeriod > 0,
    currentMonthText: (state) =>
      state.currentYear > 0 && state.currentPeriod > 0
        ? `${state.currentYear}-${String(state.currentPeriod).padStart(2, '0')}`
        : '',
    yearOptions: (state) => {
      if (state.periodStartYear <= 0 || state.periodEndYear <= 0) {
        return [] as number[]
      }
      const result: number[] = []
      for (let year = state.periodStartYear; year <= state.periodEndYear; year += 1) {
        result.push(year)
      }
      return result
    },
    monthOptions: (state) => {
      if (state.currentYear <= 0) {
        return [] as number[]
      }
      const startMonth = state.currentYear === state.periodStartYear ? state.periodStartMonth : 1
      const endMonth = state.currentYear === state.periodEndYear ? state.periodEndMonth : 12
      if (startMonth <= 0 || endMonth <= 0 || endMonth < startMonth) {
        return [] as number[]
      }
      const result: number[] = []
      for (let month = startMonth; month <= endMonth; month += 1) {
        result.push(month)
      }
      return result
    }
  },
  actions: {
    ensureInitialized(silent = true) {
      this.initialized = true
      this.syncWithCompany(undefined, silent)
    },
    syncWithCompany(nextCompanyId?: string, silent = true) {
      const financeCompany = useFinanceCompanyStore()
      const companyId = normalizeText(nextCompanyId || financeCompany.currentCompanyId)
      if (!companyId) {
        this.clearCurrent()
        return false
      }

      const option = financeCompany.findCompanyOption(companyId)
      const window = this.resolveWindow(option)
      if (!window) {
        this.companyId = companyId
        this.currentYear = 0
        this.currentPeriod = 0
        this.currentYearPeriod = 0
        this.periodStartYear = 0
        this.periodStartMonth = 0
        this.periodEndYear = 0
        this.periodEndMonth = 0
        this.deletePersistedPeriod(companyId)
        return false
      }

      const stored = readPersistedMap()[companyId]
      const resolved = this.resolvePeriodWithinWindow(stored?.year, stored?.period, window)
      const usedFallback = !stored || stored.year !== resolved.year || stored.period !== resolved.period

      this.companyId = companyId
      this.currentYear = resolved.year
      this.currentPeriod = resolved.period
      this.currentYearPeriod = buildYearPeriod(resolved.year, resolved.period)
      this.periodStartYear = window.startYear
      this.periodStartMonth = window.startMonth
      this.periodEndYear = window.endYear
      this.periodEndMonth = window.endMonth
      this.persistPeriod(companyId, resolved.year, resolved.period)

      if (usedFallback && stored && !silent) {
        ElMessage.warning(`当前公司会计期间已重置为启用年月 ${resolved.year}-${String(resolved.period).padStart(2, '0')}`)
      }
      return true
    },
    switchPeriod(nextYear: number, nextPeriod: number) {
      if (!this.hasAvailableRange || !this.companyId) {
        ElMessage.warning('当前公司未创建账套，无法切换会计期间')
        return false
      }
      const year = toPositiveInteger(nextYear)
      const period = toPositiveInteger(nextPeriod)
      if (!this.isWithinWindow(year, period)) {
        const fallback = this.resolvePeriodWithinWindow(undefined, undefined, {
          startYear: this.periodStartYear,
          startMonth: this.periodStartMonth,
          endYear: this.periodEndYear,
          endMonth: this.periodEndMonth
        })
        this.currentYear = fallback.year
        this.currentPeriod = fallback.period
        this.currentYearPeriod = buildYearPeriod(fallback.year, fallback.period)
        this.persistPeriod(this.companyId, fallback.year, fallback.period)
        ElMessage.warning(`会计期间超出账套可用范围，已重置为启用年月 ${fallback.year}-${String(fallback.period).padStart(2, '0')}`)
        return false
      }

      this.currentYear = year
      this.currentPeriod = period
      this.currentYearPeriod = buildYearPeriod(year, period)
      this.persistPeriod(this.companyId, year, period)
      return true
    },
    reset() {
      this.initialized = false
      this.clearCurrent()
      localStorage.removeItem(STORAGE_KEY)
    },
    clearCurrent() {
      this.companyId = ''
      this.currentYear = 0
      this.currentPeriod = 0
      this.currentYearPeriod = 0
      this.periodStartYear = 0
      this.periodStartMonth = 0
      this.periodEndYear = 0
      this.periodEndMonth = 0
    },
    resolveWindow(option?: {
      hasActiveAccountSet?: boolean
      enabledYear?: number
      enabledPeriod?: number
      periodStartYear?: number
      periodStartMonth?: number
      periodEndYear?: number
      periodEndMonth?: number
    }) {
      if (!option?.hasActiveAccountSet) {
        return null
      }
      const enabledYear = toPositiveInteger(option.enabledYear)
      const enabledPeriod = toPositiveInteger(option.enabledPeriod)
      const startYear = toPositiveInteger(option.periodStartYear) || enabledYear
      const startMonth = toPositiveInteger(option.periodStartMonth) || enabledPeriod
      const endYear = toPositiveInteger(option.periodEndYear) || enabledYear
      const endMonth = toPositiveInteger(option.periodEndMonth) || enabledPeriod
      if (!enabledYear || !enabledPeriod || !startYear || !startMonth || !endYear || !endMonth) {
        return null
      }
      return {
        startYear,
        startMonth,
        endYear,
        endMonth
      } satisfies PeriodWindow
    },
    resolvePeriodWithinWindow(year: number | undefined, period: number | undefined, window: PeriodWindow) {
      const normalizedYear = toPositiveInteger(year)
      const normalizedPeriod = toPositiveInteger(period)
      if (this.isWithinWindow(normalizedYear, normalizedPeriod, window)) {
        return { year: normalizedYear, period: normalizedPeriod }
      }
      return { year: window.startYear, period: window.startMonth }
    },
    isWithinWindow(year: number, period: number, window?: PeriodWindow) {
      const effectiveWindow = window || {
        startYear: this.periodStartYear,
        startMonth: this.periodStartMonth,
        endYear: this.periodEndYear,
        endMonth: this.periodEndMonth
      }
      if (!year || !period || period < 1 || period > 12) {
        return false
      }
      return compareYearMonth(year, period, effectiveWindow.startYear, effectiveWindow.startMonth) >= 0
        && compareYearMonth(year, period, effectiveWindow.endYear, effectiveWindow.endMonth) <= 0
    },
    persistPeriod(companyId: string, year: number, period: number) {
      const map = readPersistedMap()
      map[companyId] = { year, period }
      writePersistedMap(map)
    },
    deletePersistedPeriod(companyId: string) {
      const map = readPersistedMap()
      if (!map[companyId]) {
        return
      }
      delete map[companyId]
      writePersistedMap(map)
    }
  }
})
