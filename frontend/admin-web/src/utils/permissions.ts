type StoredUserLike = {
  permissionCodes?: string[]
}

export const EXPENSE_CREATE_ENTRY_PERMISSION_CODES = [
  'expense:create:view',
  'expense:create:create',
  'expense:create:submit'
] as const

const FALLBACK_PATHS: Array<{ path: string; permissionCodes: string[] }> = [
  { path: '/dashboard', permissionCodes: ['dashboard:menu', 'dashboard:view'] },
  { path: '/profile', permissionCodes: ['profile:menu', 'profile:view'] },
  { path: '/expense/create', permissionCodes: [...EXPENSE_CREATE_ENTRY_PERMISSION_CODES] },
  { path: '/expense/list', permissionCodes: ['expense:menu', 'expense:list:view'] },
  { path: '/archives/agents', permissionCodes: ['agents:menu', 'agents:view'] },
  { path: '/archives/invoices', permissionCodes: ['archives:menu', 'archives:invoices:view'] },
  {
    path: '/settings?tab=companyAccounts',
    permissionCodes: ['settings:company_accounts:view']
  },
  {
    path: '/settings?tab=organization',
    permissionCodes: [
      'settings:menu',
      'settings:organization:view',
      'settings:employees:view',
      'settings:roles:view',
      'settings:companies:view'
    ]
  }
]

export function readStoredUser(): StoredUserLike | null {
  const raw = localStorage.getItem('user')
  if (!raw) return null

  try {
    return JSON.parse(raw) as StoredUserLike
  } catch {
    return null
  }
}

export function getPermissionCodes(source?: StoredUserLike | string[] | null): string[] {
  if (Array.isArray(source)) {
    return source.filter(Boolean)
  }

  if (source?.permissionCodes) {
    return source.permissionCodes.filter(Boolean)
  }

  return (readStoredUser()?.permissionCodes || []).filter(Boolean)
}

export function hasPermission(permissionCode: string, source?: StoredUserLike | string[] | null): boolean {
  return getPermissionCodes(source).includes(permissionCode)
}

export function hasAnyPermission(permissionCodes: string[], source?: StoredUserLike | string[] | null): boolean {
  const ownedCodes = getPermissionCodes(source)
  return permissionCodes.some((code) => ownedCodes.includes(code))
}

export function resolveFirstAccessiblePath(source?: StoredUserLike | string[] | null): string {
  const ownedCodes = getPermissionCodes(source)
  const matched = FALLBACK_PATHS.find((item) => hasAnyPermission(item.permissionCodes, ownedCodes))
  return matched?.path || '/login'
}
