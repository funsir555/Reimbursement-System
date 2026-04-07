import { describe, expect, it } from 'vitest'
import { getPermissionCodes, hasAnyPermission, resolveFirstAccessiblePath } from '@/utils/permissions'

describe('permissions helpers', () => {
  it('resolves company account settings as the first accessible path', () => {
    expect(resolveFirstAccessiblePath(['settings:company_accounts:view'])).toBe(
      '/settings?tab=companyAccounts'
    )
  })

  it('keeps helper behavior for plain permission arrays', () => {
    const codes = getPermissionCodes(['settings:company_accounts:view', 'settings:companies:view'])

    expect(codes).toEqual(['settings:company_accounts:view', 'settings:companies:view'])
    expect(hasAnyPermission(['settings:company_accounts:view'], codes)).toBe(true)
  })
})
