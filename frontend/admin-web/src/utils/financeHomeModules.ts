import { MAIN_NAVIGATION_MENU, type NavigationMenuNode } from '@/router/navigation-config'
import { hasAnyPermission, readStoredUser } from '@/utils/permissions'

export type FinanceHomeModuleItem = {
  path: string
  label: string
  permissionCodes: string[]
}

export const FINANCE_HOME_PATH = '/finance/home'

const STORAGE_KEY_PREFIX = 'finance-home:recent-routes:'
const MAX_RECENT_ITEMS = 10
const FINANCE_GROUP_INDEX = '/finance'

const FINANCE_HOME_MODULE_REGISTRY = collectFinanceLeafModules()
const REGISTRY_BY_PATH = new Map(FINANCE_HOME_MODULE_REGISTRY.map((item) => [item.path, item]))

export function recordFinanceHomeVisit(path: string) {
  if (typeof window === 'undefined') {
    return
  }
  const normalizedPath = String(path || '')
  if (!REGISTRY_BY_PATH.has(normalizedPath)) {
    return
  }
  const nextPaths = [
    normalizedPath,
    ...readRecentFinancePaths().filter((item) => item !== normalizedPath)
  ].slice(0, MAX_RECENT_ITEMS)
  window.localStorage.setItem(storageKey(), JSON.stringify(nextPaths))
}

export function resolveFinanceHomeModules(source?: { permissionCodes?: string[] } | string[] | null, limit = MAX_RECENT_ITEMS) {
  const recentItems = readRecentFinancePaths()
    .map((path) => REGISTRY_BY_PATH.get(path))
    .filter((item): item is FinanceHomeModuleItem => Boolean(item))
    .filter((item) => hasAnyPermission(item.permissionCodes, source))

  if (recentItems.length >= limit) {
    return recentItems.slice(0, limit)
  }

  const fallbackItems = FINANCE_HOME_MODULE_REGISTRY
    .filter((item) => !recentItems.some((recentItem) => recentItem.path === item.path))
    .filter((item) => hasAnyPermission(item.permissionCodes, source))

  return [...recentItems, ...fallbackItems].slice(0, limit)
}

function collectFinanceLeafModules() {
  const financeGroup = MAIN_NAVIGATION_MENU.find((item) => item.index === FINANCE_GROUP_INDEX)
  if (!financeGroup?.children?.length) {
    return [] as FinanceHomeModuleItem[]
  }

  const result: FinanceHomeModuleItem[] = []
  walkLeafNodes(financeGroup.children, result)
  return result
}

function walkLeafNodes(nodes: NavigationMenuNode[], result: FinanceHomeModuleItem[]) {
  nodes.forEach((node) => {
    if (node.children?.length) {
      walkLeafNodes(node.children, result)
      return
    }
    if (!node.index || node.index === FINANCE_HOME_PATH) {
      return
    }
    result.push({
      path: node.index,
      label: node.title,
      permissionCodes: node.permissionCodes
    })
  })
}

function readRecentFinancePaths() {
  if (typeof window === 'undefined') {
    return []
  }
  try {
    const raw = window.localStorage.getItem(storageKey())
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

function storageKey() {
  const currentUser = readStoredUser() as { userId?: string | number; username?: string } | null
  const userKey = currentUser?.userId || currentUser?.username || 'anonymous'
  return `${STORAGE_KEY_PREFIX}${userKey}`
}
