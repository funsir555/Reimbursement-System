import { getRoutePathByName } from './route-catalog'
import {
  type AppRouteRecentModuleMeta,
  type RecentModuleIconKey,
  SETTINGS_ORGANIZATION_FALLBACK_PERMISSION_CODES,
  resolveRouteMenuTitle,
  resolveRouteMeta,
  getRouteMenuPermissionCodes
} from './route-meta'

export type NavigationIconKey = 'House' | 'Wallet' | 'Coin' | 'FolderOpened' | 'Agent' | 'Setting'

export interface NavigationMenuNode {
  key: string
  index: string
  title: string
  iconKey?: NavigationIconKey
  permissionCodes: string[]
  children?: NavigationMenuNode[]
}

export interface NavigationFallbackTarget {
  path: string
  permissionCodes: string[]
}

export interface DashboardRecentModuleItem {
  path: string
  label: string
  description: string
  iconKey: RecentModuleIconKey
  permissionCodes: string[]
}

function mergePermissionCodes(items: string[][]): string[] {
  return Array.from(new Set(items.flat().filter(Boolean)))
}

function createRouteMenuNode(routeName: string): NavigationMenuNode {
  const meta = resolveRouteMeta(routeName as never)
  return {
    key: routeName,
    index: getRoutePathByName(routeName),
    title: resolveRouteMenuTitle(meta),
    permissionCodes: getRouteMenuPermissionCodes(meta)
  }
}

function createMenuGroup(index: string, title: string, children: NavigationMenuNode[], iconKey?: NavigationIconKey): NavigationMenuNode {
  return {
    key: index,
    index,
    title,
    iconKey,
    permissionCodes: mergePermissionCodes(children.map((item) => item.permissionCodes)),
    children
  }
}

function createFallbackTarget(path: string, permissionCodes: string[]): NavigationFallbackTarget {
  return { path, permissionCodes }
}

function createRouteFallback(routeName: string, pathOverride?: string): NavigationFallbackTarget {
  const meta = resolveRouteMeta(routeName as never)
  return createFallbackTarget(pathOverride || getRoutePathByName(routeName), getRouteMenuPermissionCodes(meta))
}

function createRecentModuleItem(routeName: string): DashboardRecentModuleItem {
  const meta = resolveRouteMeta(routeName as never)
  const recentModule = meta.recentModule as AppRouteRecentModuleMeta | undefined
  if (!recentModule) {
    throw new Error(`Route ${routeName} is missing recent module metadata`)
  }
  return {
    path: getRoutePathByName(routeName),
    label: recentModule.label,
    description: recentModule.description,
    iconKey: recentModule.iconKey,
    permissionCodes: getRouteMenuPermissionCodes(meta)
  }
}

export const MAIN_NAVIGATION_MENU: NavigationMenuNode[] = [
  {
    key: 'dashboard',
    index: getRoutePathByName('dashboard'),
    title: resolveRouteMenuTitle(resolveRouteMeta('dashboard')),
    iconKey: 'House',
    permissionCodes: ['dashboard:menu', 'dashboard:view']
  },
  createMenuGroup(
    '/expense',
    '报销管理',
    [
      createRouteMenuNode('expense-create'),
      createRouteMenuNode('expense-list'),
      createRouteMenuNode('expense-approval'),
      createMenuGroup(
        '/expense/payment',
        '支付',
        [
          createRouteMenuNode('expense-payment-orders'),
          createRouteMenuNode('expense-bank-link')
        ]
      ),
      createRouteMenuNode('expense-documents'),
      createRouteMenuNode('expense-voucher-generation'),
      createMenuGroup(
        '/expense/workbench',
        '管理工作台',
        [
          createRouteMenuNode('expense-workbench-process-management'),
          createRouteMenuNode('expense-workbench-budget-management')
        ]
      )
    ],
    'Wallet'
  ),
  createMenuGroup(
    '/finance',
    '财务管理',
    [
      createMenuGroup(
        '/finance/general-ledger',
        '总账',
        [
          createRouteMenuNode('finance-new-voucher'),
          createRouteMenuNode('finance-query-voucher'),
          createRouteMenuNode('finance-review-voucher'),
          createRouteMenuNode('finance-post-voucher'),
          createRouteMenuNode('finance-close-ledger'),
          createRouteMenuNode('finance-ledger-balance-sheet'),
          createRouteMenuNode('finance-detail-ledger'),
          createRouteMenuNode('finance-general-ledger-book'),
          createRouteMenuNode('finance-project-detail-ledger'),
          createRouteMenuNode('finance-supplier-detail-ledger'),
          createRouteMenuNode('finance-customer-detail-ledger'),
          createRouteMenuNode('finance-personal-detail-ledger'),
          createRouteMenuNode('finance-quantity-amount-detail-ledger')
        ]
      ),
      createRouteMenuNode('finance-fixed-assets'),
      createMenuGroup(
        '/finance/reports',
        '财务报表',
        [
          createRouteMenuNode('finance-reports-balance-sheet'),
          createRouteMenuNode('finance-reports-income-statement'),
          createRouteMenuNode('finance-reports-cash-flow')
        ]
      ),
      createMenuGroup(
        '/finance/archives',
        '会计档案',
        [
          createRouteMenuNode('finance-archives-customers'),
          createRouteMenuNode('finance-archives-suppliers'),
          createRouteMenuNode('finance-archives-employees'),
          createRouteMenuNode('finance-archives-departments'),
          createRouteMenuNode('finance-archives-account-subjects'),
          createRouteMenuNode('finance-archives-projects')
        ]
      ),
      createRouteMenuNode('finance-system-management')
    ],
    'Coin'
  ),
  createMenuGroup(
    '/archives',
    '电子档案',
    [
      createRouteMenuNode('archives-invoices'),
      createRouteMenuNode('archives-account-books')
    ],
    'FolderOpened'
  ),
  {
    key: 'archives-agents',
    index: getRoutePathByName('archives-agents'),
    title: resolveRouteMenuTitle(resolveRouteMeta('archives-agents')),
    iconKey: 'Agent',
    permissionCodes: getRouteMenuPermissionCodes(resolveRouteMeta('archives-agents'))
  },
  {
    key: 'settings',
    index: getRoutePathByName('settings'),
    title: resolveRouteMenuTitle(resolveRouteMeta('settings')),
    iconKey: 'Setting',
    permissionCodes: getRouteMenuPermissionCodes(resolveRouteMeta('settings'))
  }
]

export const FALLBACK_NAVIGATION_TARGETS: NavigationFallbackTarget[] = [
  createFallbackTarget('/dashboard', ['dashboard:menu', 'dashboard:view']),
  createFallbackTarget('/profile', ['profile:menu', 'profile:view']),
  createRouteFallback('expense-create'),
  createRouteFallback('expense-list'),
  createFallbackTarget('/archives/agents', ['agents:menu', 'agents:view']),
  createRouteFallback('archives-invoices'),
  createFallbackTarget('/settings?tab=companyAccounts', ['settings:company_accounts:view']),
  createFallbackTarget('/settings?tab=organization', [...SETTINGS_ORGANIZATION_FALLBACK_PERMISSION_CODES])
]

export const DASHBOARD_RECENT_MODULE_REGISTRY: DashboardRecentModuleItem[] = [
  createRecentModuleItem('expense-create'),
  createRecentModuleItem('expense-list'),
  createRecentModuleItem('expense-approval'),
  createRecentModuleItem('expense-documents'),
  createRecentModuleItem('dashboard-pending-repayments'),
  createRecentModuleItem('dashboard-pending-prepay-writeoffs'),
  createRecentModuleItem('expense-voucher-generation'),
  createRecentModuleItem('expense-workbench-process-management')
]

export function matchesAnyPermission(requiredCodes: string[], ownedCodes: string[]): boolean {
  return requiredCodes.some((code) => ownedCodes.includes(code))
}

export function filterVisibleNavigationMenu(source: NavigationMenuNode[], ownedCodes: string[]): NavigationMenuNode[] {
  return source
    .map((node) => {
      if (!node.children?.length) {
        return matchesAnyPermission(node.permissionCodes, ownedCodes) ? node : null
      }

      const visibleChildren = filterVisibleNavigationMenu(node.children, ownedCodes)
      if (!visibleChildren.length) {
        return null
      }

      return {
        ...node,
        children: visibleChildren,
        permissionCodes: mergePermissionCodes(visibleChildren.map((child) => child.permissionCodes))
      }
    })
    .filter((node): node is NavigationMenuNode => Boolean(node))
}
