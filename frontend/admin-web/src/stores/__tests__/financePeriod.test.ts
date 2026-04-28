import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { useFinancePeriodStore } from '@/stores/financePeriod'

const mocks = vi.hoisted(() => ({
  warningSpy: vi.fn()
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: {
      warning: mocks.warningSpy
    }
  }
})

describe('financePeriod store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    mocks.warningSpy.mockReset()
  })

  it('initializes from the current company enabled year and month', () => {
    const financeCompany = useFinanceCompanyStore()
    financeCompany.companyOptions = [{
      companyId: 'COMPANY_A',
      companyCode: '001',
      companyName: '测试公司A',
      label: '001 - 测试公司A',
      value: 'COMPANY_A',
      hasActiveAccountSet: true,
      enabledYear: 2026,
      enabledPeriod: 6,
      periodStartYear: 2026,
      periodStartMonth: 6,
      periodEndYear: 2026,
      periodEndMonth: 6
    }]
    financeCompany.currentCompanyId = 'COMPANY_A'

    const financePeriod = useFinancePeriodStore()
    financePeriod.ensureInitialized()

    expect(financePeriod.currentYear).toBe(2026)
    expect(financePeriod.currentPeriod).toBe(6)
    expect(financePeriod.currentYearPeriod).toBe(202606)
    expect(financePeriod.yearOptions).toEqual([2026])
    expect(financePeriod.monthOptions).toEqual([6])
  })

  it('falls back to the enabled period when persisted data is out of range', () => {
    localStorage.setItem('finance-current-period-map', JSON.stringify({
      COMPANY_A: { year: 2027, period: 1 }
    }))

    const financeCompany = useFinanceCompanyStore()
    financeCompany.companyOptions = [{
      companyId: 'COMPANY_A',
      companyCode: '001',
      companyName: '测试公司A',
      label: '001 - 测试公司A',
      value: 'COMPANY_A',
      hasActiveAccountSet: true,
      enabledYear: 2026,
      enabledPeriod: 6,
      periodStartYear: 2026,
      periodStartMonth: 6,
      periodEndYear: 2026,
      periodEndMonth: 6
    }]
    financeCompany.currentCompanyId = 'COMPANY_A'

    const financePeriod = useFinancePeriodStore()
    financePeriod.syncWithCompany('COMPANY_A', false)

    expect(financePeriod.currentYear).toBe(2026)
    expect(financePeriod.currentPeriod).toBe(6)
    expect(mocks.warningSpy).toHaveBeenCalledTimes(1)
  })

  it('rejects switching to a period outside the allowed window', () => {
    const financeCompany = useFinanceCompanyStore()
    financeCompany.companyOptions = [{
      companyId: 'COMPANY_A',
      companyCode: '001',
      companyName: '测试公司A',
      label: '001 - 测试公司A',
      value: 'COMPANY_A',
      hasActiveAccountSet: true,
      enabledYear: 2026,
      enabledPeriod: 6,
      periodStartYear: 2026,
      periodStartMonth: 6,
      periodEndYear: 2026,
      periodEndMonth: 6
    }]
    financeCompany.currentCompanyId = 'COMPANY_A'

    const financePeriod = useFinancePeriodStore()
    financePeriod.ensureInitialized()

    expect(financePeriod.switchPeriod(2026, 7)).toBe(false)
    expect(financePeriod.currentYearPeriod).toBe(202606)
    expect(mocks.warningSpy).toHaveBeenCalledTimes(1)
  })
})
