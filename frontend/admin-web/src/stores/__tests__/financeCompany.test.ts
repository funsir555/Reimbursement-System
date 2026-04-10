import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useFinanceCompanyStore } from '@/stores/financeCompany'

const mocks = vi.hoisted(() => ({
  financeContextApi: {
    getMeta: vi.fn()
  }
}))

vi.mock('@/api', () => ({
  financeContextApi: mocks.financeContextApi
}))

function buildMeta() {
  return {
    companyOptions: [
      {
        companyId: 'COMPANY202603260001',
        companyCode: 'COMP202603260001',
        companyName: '广州远智教育科技有限公司',
        hasActiveAccountSet: false,
        label: 'COMP202603260001 - 广州远智教育科技有限公司',
        value: 'COMPANY202603260001'
      },
      {
        companyId: 'COMPANY202604050001',
        companyCode: 'COMP202604050001',
        companyName: '广州市黄埔区远智自学考试辅导中心',
        hasActiveAccountSet: true,
        label: 'COMP202604050001 - 广州市黄埔区远智自学考试辅导中心',
        value: 'COMPANY202604050001'
      }
    ],
    currentUserCompanyId: 'COMPANY202603260001',
    defaultCompanyId: 'COMPANY202604050001'
  }
}

describe('financeCompany store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    mocks.financeContextApi.getMeta.mockReset()
    mocks.financeContextApi.getMeta.mockResolvedValue({ data: buildMeta() })
  })

  it('falls back to backend default company when the stored company has no active account set', async () => {
    localStorage.setItem('finance-current-company-id', 'COMPANY202603260001')
    const store = useFinanceCompanyStore()

    await store.ensureInitialized('COMPANY202603260001')

    expect(store.currentCompanyId).toBe('COMPANY202604050001')
    expect(localStorage.getItem('finance-current-company-id')).toBe('COMPANY202604050001')
  })

  it('keeps the stored company when it still has an active account set', async () => {
    const meta = buildMeta()
    meta.companyOptions[0].hasActiveAccountSet = true
    meta.defaultCompanyId = 'COMPANY202604050001'
    localStorage.setItem('finance-current-company-id', 'COMPANY202603260001')
    mocks.financeContextApi.getMeta.mockResolvedValueOnce({ data: meta })

    const store = useFinanceCompanyStore()
    await store.ensureInitialized('COMPANY202603260001')

    expect(store.currentCompanyId).toBe('COMPANY202603260001')
  })
})
