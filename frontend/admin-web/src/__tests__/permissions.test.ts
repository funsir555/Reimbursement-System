import { beforeEach, describe, expect, it } from 'vitest'
import {
  getPermissionCodes,
  hasAnyPermission,
  hasPermission,
  readStoredUser,
  resolveFirstAccessiblePath
} from '@/utils/permissions'

describe('permissions utilities', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('returns explicit permission arrays directly', () => {
    expect(getPermissionCodes(['dashboard:view', '', 'profile:view'])).toEqual([
      'dashboard:view',
      'profile:view'
    ])
  })

  it('falls back to stored user permissions', () => {
    localStorage.setItem('user', JSON.stringify({ permissionCodes: ['archives:invoices:view'] }))

    expect(readStoredUser()).toEqual({ permissionCodes: ['archives:invoices:view'] })
    expect(getPermissionCodes()).toEqual(['archives:invoices:view'])
  })

  it('checks single and multiple permissions', () => {
    const source = { permissionCodes: ['dashboard:view', 'profile:view'] }

    expect(hasPermission('dashboard:view', source)).toBe(true)
    expect(hasPermission('settings:organization:view', source)).toBe(false)
    expect(hasAnyPermission(['expense:list:view', 'profile:view'], source)).toBe(true)
    expect(hasAnyPermission(['expense:list:view', 'settings:organization:view'], source)).toBe(false)
  })

  it('resolves the first accessible fallback route', () => {
    expect(resolveFirstAccessiblePath({ permissionCodes: ['archives:invoices:view'] })).toBe('/archives/invoices')
    expect(resolveFirstAccessiblePath({ permissionCodes: ['settings:roles:view'] })).toBe('/settings?tab=organization')
    expect(resolveFirstAccessiblePath({ permissionCodes: [] })).toBe('/login')
  })
})
