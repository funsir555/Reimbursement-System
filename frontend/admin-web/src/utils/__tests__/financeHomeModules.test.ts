import { beforeEach, describe, expect, it } from 'vitest'
import { recordFinanceHomeVisit, resolveFinanceHomeModules } from '@/utils/financeHomeModules'

describe('financeHomeModules', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('falls back to the first accessible finance leaf modules when there is no recent history', () => {
    localStorage.setItem('user', JSON.stringify({
      userId: 7,
      username: 'finance-user',
      permissionCodes: [
        'finance:general_ledger:new_voucher:view',
        'finance:general_ledger:query_voucher:view',
        'finance:system_management:view'
      ]
    }))

    expect(resolveFinanceHomeModules().map((item) => item.path)).toEqual([
      '/finance/general-ledger/new-voucher',
      '/finance/general-ledger/query-voucher',
      '/finance/system-management/new-account-set',
      '/finance/system-management/system-enable'
    ])
  })

  it('keeps recent finance visits user-scoped and ordered by most recent first', () => {
    localStorage.setItem('user', JSON.stringify({
      userId: 18,
      username: 'finance-admin',
      permissionCodes: [
        'finance:general_ledger:new_voucher:view',
        'finance:general_ledger:query_voucher:view',
        'finance:general_ledger:review_voucher:view'
      ]
    }))

    recordFinanceHomeVisit('/finance/general-ledger/query-voucher')
    recordFinanceHomeVisit('/finance/general-ledger/review-voucher')

    expect(resolveFinanceHomeModules().slice(0, 3).map((item) => item.path)).toEqual([
      '/finance/general-ledger/review-voucher',
      '/finance/general-ledger/query-voucher',
      '/finance/general-ledger/new-voucher'
    ])

    localStorage.setItem('user', JSON.stringify({
      userId: 19,
      username: 'other-user',
      permissionCodes: [
        'finance:general_ledger:new_voucher:view',
        'finance:general_ledger:query_voucher:view'
      ]
    }))

    expect(resolveFinanceHomeModules().map((item) => item.path)).toEqual([
      '/finance/general-ledger/new-voucher',
      '/finance/general-ledger/query-voucher'
    ])
  })
})
