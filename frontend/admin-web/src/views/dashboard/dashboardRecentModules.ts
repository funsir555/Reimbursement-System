import { DASHBOARD_RECENT_MODULE_REGISTRY } from '@/router/navigation-config'
import { hasAnyPermission } from '@/utils/permissions'

type PermissionSource = { permissionCodes?: string[] } | string[] | null | undefined

export type { DashboardRecentModuleItem } from '@/router/navigation-config'

const STORAGE_KEY = 'dashboard:recent-modules'
const MAX_ITEMS = 8
export const DASHBOARD_DATA_CHANGED_EVENT = 'dashboard:data-changed'

const REGISTRY_BY_PATH = new Map(DASHBOARD_RECENT_MODULE_REGISTRY.map((item) => [item.path, item]))

export function recordRecentModuleVisit(path: string) {
  if (typeof window === 'undefined' || !REGISTRY_BY_PATH.has(path)) {
    return
  }
  const next = [path, ...readRecentModulePaths().filter((item) => item !== path)].slice(0, MAX_ITEMS)
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
}

export function resolveRecentModules(source?: PermissionSource, limit = 6) {
  const recentItems = readRecentModulePaths()
    .map((path) => REGISTRY_BY_PATH.get(path))
    .filter((item): item is NonNullable<typeof item> => Boolean(item))
    .filter((item) => hasAnyPermission(item.permissionCodes, source))

  if (recentItems.length >= limit) {
    return recentItems.slice(0, limit)
  }

  const fallbackItems = DASHBOARD_RECENT_MODULE_REGISTRY
    .filter((item) => !recentItems.some((recentItem) => recentItem.path === item.path))
    .filter((item) => hasAnyPermission(item.permissionCodes, source))

  return [...recentItems, ...fallbackItems].slice(0, limit)
}

export function notifyDashboardDataChanged() {
  if (typeof window === 'undefined') {
    return
  }
  window.dispatchEvent(new CustomEvent(DASHBOARD_DATA_CHANGED_EVENT))
}

function readRecentModulePaths() {
  if (typeof window === 'undefined') {
    return []
  }
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) {
      return []
    }
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed)
      ? parsed.filter((item): item is string => typeof item === 'string' && REGISTRY_BY_PATH.has(item))
      : []
  } catch {
    return []
  }
}
