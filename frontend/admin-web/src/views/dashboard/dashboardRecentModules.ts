import { hasAnyPermission } from '@/utils/permissions'

type PermissionSource = { permissionCodes?: string[] } | string[] | null | undefined

export interface DashboardRecentModuleItem {
  path: string
  label: string
  description: string
  iconKey: string
  permissionCodes: string[]
}

const STORAGE_KEY = 'dashboard:recent-modules'
const MAX_ITEMS = 8
export const DASHBOARD_DATA_CHANGED_EVENT = 'dashboard:data-changed'

export const DASHBOARD_RECENT_MODULE_REGISTRY: DashboardRecentModuleItem[] = [
  {
    path: '/expense/create',
    label: '新建报销',
    description: '快速发起新的报销、申请、借款或合同单',
    iconKey: 'Plus',
    permissionCodes: ['expense:create:view', 'expense:create:create', 'expense:create:submit']
  },
  {
    path: '/expense/list',
    label: '我的报销',
    description: '查看本人单据、草稿和驳回记录',
    iconKey: 'Tickets',
    permissionCodes: ['expense:list:view']
  },
  {
    path: '/expense/approval',
    label: '待我审批',
    description: '处理当前待办审批任务',
    iconKey: 'Select',
    permissionCodes: ['expense:approval:view']
  },
  {
    path: '/expense/documents',
    label: '单据查询',
    description: '统一查询已提交单据与流程状态',
    iconKey: 'Search',
    permissionCodes: ['expense:documents:view']
  },
  {
    path: '/dashboard/pending-repayments',
    label: '待还款',
    description: '处理借款单待核销余额',
    iconKey: 'Wallet',
    permissionCodes: ['dashboard:view']
  },
  {
    path: '/dashboard/pending-prepay-writeoffs',
    label: '待核销',
    description: '处理预付未到票单据的待核销余额',
    iconKey: 'Coin',
    permissionCodes: ['dashboard:view']
  },
  {
    path: '/expense/voucher-generation',
    label: '凭证生成',
    description: '批量推送审批通过单据到总账',
    iconKey: 'DocumentChecked',
    permissionCodes: ['expense:voucher_generation:view']
  },
  {
    path: '/expense/workbench/process-management',
    label: '流程管理',
    description: '维护模板、流程、表单与费用明细设计',
    iconKey: 'Connection',
    permissionCodes: ['expense:process_management:view']
  }
]

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
    .filter((item): item is DashboardRecentModuleItem => Boolean(item))
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