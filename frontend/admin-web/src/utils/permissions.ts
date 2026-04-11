import { FALLBACK_NAVIGATION_TARGETS } from '@/router/navigation-config'
import { EXPENSE_CREATE_ENTRY_PERMISSION_CODES } from './permissionConstants'

type StoredUserLike = {
  permissionCodes?: string[]
}

export { EXPENSE_CREATE_ENTRY_PERMISSION_CODES }

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
  const matched = FALLBACK_NAVIGATION_TARGETS.find((item) => hasAnyPermission(item.permissionCodes, ownedCodes))
  return matched?.path || '/login'
}
