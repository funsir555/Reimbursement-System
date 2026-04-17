import type { RouteLocationNormalizedLoaded, RouteLocationNormalized, RouteMeta } from 'vue-router'
import { EXPENSE_CREATE_ENTRY_PERMISSION_CODES } from '@/utils/permissionConstants'

export const SETTINGS_ROUTE_PERMISSION_CODES = [
  'settings:menu',
  'settings:organization:view',
  'settings:employees:view',
  'settings:roles:view',
  'settings:companies:view',
  'settings:company_accounts:view'
] as const

export const SETTINGS_ORGANIZATION_FALLBACK_PERMISSION_CODES = [
  'settings:menu',
  'settings:organization:view',
  'settings:employees:view',
  'settings:roles:view',
  'settings:companies:view'
] as const

export type RecentModuleIconKey =
  | 'Plus'
  | 'Tickets'
  | 'Select'
  | 'Search'
  | 'Wallet'
  | 'Coin'
  | 'DocumentChecked'
  | 'Connection'

export interface AppRouteRecentModuleMeta {
  label: string
  description: string
  iconKey: RecentModuleIconKey
}

export interface AppRouteMetaDefinition {
  [key: string]: unknown
  title?: string
  tabTitle?: string
  menuTitle?: string
  description?: string
  placeholderTitle?: string
  placeholderDescription?: string
  permissionCode?: string
  permissionCodes?: string[]
  menuPermissionCodes?: string[]
  public?: boolean
  recentModule?: AppRouteRecentModuleMeta
}

declare module 'vue-router' {
  interface RouteMeta extends AppRouteMetaDefinition {}
}

type RouteMetaLike =
  | AppRouteMetaDefinition
  | RouteMeta
  | RouteLocationNormalized['meta']
  | RouteLocationNormalizedLoaded['meta']
  | null
  | undefined

function routeMeta(definition: AppRouteMetaDefinition): AppRouteMetaDefinition {
  return definition
}

export const ROUTE_META_REGISTRY = {
  login: routeMeta({
    public: true
  }),
  dashboard: routeMeta({
    title: '首页',
    menuTitle: '首页',
    permissionCode: 'dashboard:view'
  }),
  'dashboard-pending-repayments': routeMeta({
    title: '待还款',
    permissionCode: 'dashboard:view',
    recentModule: {
      label: '待还款',
      description: '处理借款单待核销余额',
      iconKey: 'Wallet'
    }
  }),
  'dashboard-pending-prepay-writeoffs': routeMeta({
    title: '待预付核销',
    permissionCode: 'dashboard:view',
    recentModule: {
      label: '待核销',
      description: '处理预付未到票单据的待核销余额',
      iconKey: 'Coin'
    }
  }),
  profile: routeMeta({
    title: '个人中心',
    menuTitle: '个人中心',
    permissionCode: 'profile:view',
    menuPermissionCodes: ['profile:menu', 'profile:view']
  }),
  'expense-create': routeMeta({
    title: '新建报销',
    menuTitle: '新建报销',
    permissionCodes: [...EXPENSE_CREATE_ENTRY_PERMISSION_CODES],
    recentModule: {
      label: '新建报销',
      description: '快速发起新的报销、申请、借款或合同单',
      iconKey: 'Plus'
    }
  }),
  'expense-document-resubmit': routeMeta({
    title: '重新提交报销',
    permissionCodes: ['expense:list:view', 'expense:create:create', 'expense:create:submit']
  }),
  'expense-approval-task-modify': routeMeta({
    title: '审批改单',
    permissionCodes: ['expense:approval:view', 'expense:approval:approve']
  }),
  'expense-create-detail-edit': routeMeta({
    title: '编辑费用明细',
    permissionCodes: [...EXPENSE_CREATE_ENTRY_PERMISSION_CODES, 'expense:approval:view', 'expense:approval:approve']
  }),
  'expense-list': routeMeta({
    title: '我的报销',
    menuTitle: '我的报销',
    permissionCode: 'expense:list:view',
    recentModule: {
      label: '我的报销',
      description: '查看本人单据、草稿和驳回记录',
      iconKey: 'Tickets'
    }
  }),
  'expense-approval': routeMeta({
    title: '待我审批',
    menuTitle: '待我审批',
    permissionCode: 'expense:approval:view',
    recentModule: {
      label: '待我审批',
      description: '处理当前待办审批任务',
      iconKey: 'Select'
    }
  }),
  'expense-document-detail': routeMeta({
    title: '报销单详情',
    permissionCodes: ['expense:list:view', 'expense:approval:view', 'expense:documents:view']
  }),
  'expense-document-batch-print': routeMeta({
    title: '???????',
    permissionCodes: ['expense:list:view', 'expense:approval:view', 'expense:documents:view', 'expense:payment:payment_order:view']
  }),
  'expense-document-expense-detail': routeMeta({
    title: '费用明细',
    permissionCodes: ['expense:list:view', 'expense:approval:view', 'expense:documents:view']
  }),
  'expense-payment-orders': routeMeta({
    title: '付款单',
    tabTitle: '付款单',
    menuTitle: '付款单',
    description: '付款单工作台',
    permissionCode: 'expense:payment:payment_order:view'
  }),
  'expense-bank-link': routeMeta({
    title: '银企直连',
    tabTitle: '银企直连',
    menuTitle: '银企直连',
    description: '银企直连配置',
    permissionCode: 'expense:payment:bank_link:view'
  }),
  'expense-documents': routeMeta({
    title: '单据查询',
    tabTitle: '单据查询',
    menuTitle: '单据查询',
    description: '统一查询已提交单据与流程状态',
    permissionCode: 'expense:documents:view',
    recentModule: {
      label: '单据查询',
      description: '统一查询已提交单据与流程状态',
      iconKey: 'Search'
    }
  }),
  'expense-voucher-generation': routeMeta({
    title: '凭证生成',
    tabTitle: '凭证生成',
    menuTitle: '凭证生成',
    description: '报销凭证生成工作台',
    permissionCode: 'expense:voucher_generation:view',
    recentModule: {
      label: '凭证生成',
      description: '批量推送审批通过单据到总账',
      iconKey: 'DocumentChecked'
    }
  }),
  'expense-workbench-process-management': routeMeta({
    title: '流程管理',
    tabTitle: '流程管理',
    menuTitle: '流程管理',
    description: '流程管理工作台',
    permissionCode: 'expense:process_management:view',
    recentModule: {
      label: '流程管理',
      description: '维护模板、流程、表单与费用明细设计',
      iconKey: 'Connection'
    }
  }),
  'expense-workbench-process-management-create': routeMeta({
    title: '新建流程模板',
    permissionCode: 'expense:process_management:view'
  }),
  'expense-workbench-process-management-edit': routeMeta({
    title: '编辑流程模板',
    permissionCode: 'expense:process_management:view'
  }),
  'expense-workbench-process-flow-create': routeMeta({
    title: '流程设计器',
    permissionCode: 'expense:process_management:view'
  }),
  'expense-workbench-process-flow-edit': routeMeta({
    title: '流程设计器',
    permissionCode: 'expense:process_management:view'
  }),
  'expense-workbench-process-form-create': routeMeta({
    title: '表单设计器',
    permissionCode: 'expense:process_management:view'
  }),
  'expense-workbench-process-form-edit': routeMeta({
    title: '表单设计器',
    permissionCode: 'expense:process_management:view'
  }),
  'expense-workbench-process-expense-detail-create': routeMeta({
    title: '费用明细设计器',
    permissionCode: 'expense:process_management:view'
  }),
  'expense-workbench-process-expense-detail-edit': routeMeta({
    title: '费用明细设计器',
    permissionCode: 'expense:process_management:view'
  }),
  'expense-workbench-budget-management': routeMeta({
    title: '预算管理',
    tabTitle: '预算管理',
    menuTitle: '预算管理',
    description: '预算管理',
    placeholderTitle: '预算管理',
    placeholderDescription: '预算管理功能建设中',
    permissionCode: 'expense:budget_management:view'
  }),
  'finance-new-voucher': routeMeta({
    title: '新建凭证',
    tabTitle: '新建凭证',
    menuTitle: '新建凭证',
    description: '新建总账凭证',
    permissionCode: 'finance:general_ledger:new_voucher:view'
  }),
  'finance-query-voucher': routeMeta({
    title: '查询凭证',
    tabTitle: '查询凭证',
    menuTitle: '查询凭证',
    description: '查询总账凭证',
    permissionCode: 'finance:general_ledger:query_voucher:view'
  }),
  'finance-query-voucher-detail': routeMeta({
    title: '凭证详情',
    tabTitle: '凭证详情',
    description: '查看和修改凭证详情',
    permissionCode: 'finance:general_ledger:query_voucher:view'
  }),
  'finance-review-voucher': routeMeta({
    title: '审核凭证',
    tabTitle: '审核凭证',
    menuTitle: '审核凭证',
    description: '审核总账凭证',
    permissionCode: 'finance:general_ledger:review_voucher:view'
  }),
  'finance-post-voucher': routeMeta({
    title: '记账',
    tabTitle: '记账',
    menuTitle: '记账',
    description: '总账记账功能建设中',
    placeholderTitle: '记账',
    placeholderDescription: '总账记账功能建设中',
    permissionCode: 'finance:general_ledger:post_voucher:view'
  }),
  'finance-close-ledger': routeMeta({
    title: '结账',
    tabTitle: '结账',
    menuTitle: '结账',
    description: '总账结账功能建设中',
    placeholderTitle: '结账',
    placeholderDescription: '总账结账功能建设中',
    permissionCode: 'finance:general_ledger:close_ledger:view'
  }),
  'finance-review-voucher-detail': routeMeta({
    title: '审核凭证详情',
    tabTitle: '审核凭证详情',
    description: '查看和审核凭证详情',
    permissionCode: 'finance:general_ledger:review_voucher:view'
  }),
  'finance-ledger-balance-sheet': routeMeta({
    title: '余额表',
    tabTitle: '余额表',
    menuTitle: '余额表',
    description: '查看余额表',
    placeholderTitle: '余额表',
    placeholderDescription: '查看余额表',
    permissionCode: 'finance:general_ledger:balance_sheet:view'
  }),
  'finance-detail-ledger': routeMeta({
    title: '明细账',
    tabTitle: '明细账',
    menuTitle: '明细账',
    description: '明细账功能建设中',
    placeholderTitle: '明细账',
    placeholderDescription: '明细账功能建设中',
    permissionCode: 'finance:general_ledger:detail_ledger:view'
  }),
  'finance-general-ledger-book': routeMeta({
    title: '总分类账',
    tabTitle: '总分类账',
    menuTitle: '总分类账',
    description: '总分类账功能建设中',
    placeholderTitle: '总分类账',
    placeholderDescription: '总分类账功能建设中',
    permissionCode: 'finance:general_ledger:general_ledger:view'
  }),
  'finance-project-detail-ledger': routeMeta({
    title: '项目明细账',
    tabTitle: '项目明细账',
    menuTitle: '项目明细账',
    description: '项目明细账功能建设中',
    placeholderTitle: '项目明细账',
    placeholderDescription: '项目明细账功能建设中',
    permissionCode: 'finance:general_ledger:project_detail_ledger:view'
  }),
  'finance-supplier-detail-ledger': routeMeta({
    title: '供应商明细账',
    tabTitle: '供应商明细账',
    menuTitle: '供应商明细账',
    description: '供应商明细账功能建设中',
    placeholderTitle: '供应商明细账',
    placeholderDescription: '供应商明细账功能建设中',
    permissionCode: 'finance:general_ledger:supplier_detail_ledger:view'
  }),
  'finance-customer-detail-ledger': routeMeta({
    title: '客户明细账',
    tabTitle: '客户明细账',
    menuTitle: '客户明细账',
    description: '客户明细账功能建设中',
    placeholderTitle: '客户明细账',
    placeholderDescription: '客户明细账功能建设中',
    permissionCode: 'finance:general_ledger:customer_detail_ledger:view'
  }),
  'finance-personal-detail-ledger': routeMeta({
    title: '个人明细账',
    tabTitle: '个人明细账',
    menuTitle: '个人明细账',
    description: '个人明细账功能建设中',
    placeholderTitle: '个人明细账',
    placeholderDescription: '个人明细账功能建设中',
    permissionCode: 'finance:general_ledger:personal_detail_ledger:view'
  }),
  'finance-quantity-amount-detail-ledger': routeMeta({
    title: '数量金额明细账',
    tabTitle: '数量金额明细账',
    menuTitle: '数量金额明细账',
    description: '数量金额明细账功能建设中',
    placeholderTitle: '数量金额明细账',
    placeholderDescription: '数量金额明细账功能建设中',
    permissionCode: 'finance:general_ledger:quantity_amount_detail_ledger:view'
  }),
  'finance-fixed-assets': routeMeta({
    title: '固定资产',
    tabTitle: '固定资产',
    menuTitle: '固定资产',
    description: '固定资产业务工作台',
    permissionCode: 'finance:fixed_assets:view'
  }),
  'finance-reports-balance-sheet': routeMeta({
    title: '资产负债表',
    tabTitle: '资产负债表',
    menuTitle: '资产负债表',
    description: '查看资产负债表',
    placeholderTitle: '资产负债表',
    placeholderDescription: '查看资产负债表',
    permissionCode: 'finance:reports:balance_sheet:view'
  }),
  'finance-reports-income-statement': routeMeta({
    title: '利润表',
    tabTitle: '利润表',
    menuTitle: '利润表',
    description: '查看利润表',
    placeholderTitle: '利润表',
    placeholderDescription: '查看利润表',
    permissionCode: 'finance:reports:income_statement:view'
  }),
  'finance-reports-cash-flow': routeMeta({
    title: '现金流量表',
    tabTitle: '现金流量表',
    menuTitle: '现金流量表',
    description: '查看现金流量表',
    placeholderTitle: '现金流量表',
    placeholderDescription: '查看现金流量表',
    permissionCode: 'finance:reports:cash_flow:view'
  }),
  'finance-system-management': routeMeta({
    title: '财务系统管理',
    tabTitle: '财务系统管理',
    menuTitle: '财务系统管理',
    description: '财务系统管理工作台',
    permissionCode: 'finance:system_management:view'
  }),
  'finance-archives-customers': routeMeta({
    title: '客户档案',
    tabTitle: '客户档案',
    menuTitle: '客户档案',
    description: '客户主数据维护',
    permissionCode: 'finance:archives:customers:view'
  }),
  'finance-archives-suppliers': routeMeta({
    title: '供应商档案',
    tabTitle: '供应商档案',
    menuTitle: '供应商档案',
    description: '供应商主数据维护',
    permissionCode: 'finance:archives:suppliers:view'
  }),
  'finance-archives-employees': routeMeta({
    title: '员工档案',
    tabTitle: '员工档案',
    menuTitle: '员工档案',
    description: '员工主数据查询',
    permissionCode: 'finance:archives:employees:view'
  }),
  'finance-archives-departments': routeMeta({
    title: '部门档案',
    tabTitle: '部门档案',
    menuTitle: '部门档案',
    description: '部门主数据维护',
    placeholderTitle: '部门档案',
    placeholderDescription: '部门主数据维护',
    permissionCode: 'finance:archives:departments:view'
  }),
  'finance-archives-account-subjects': routeMeta({
    title: '会计科目',
    tabTitle: '会计科目',
    menuTitle: '会计科目',
    description: '会计科目主数据维护',
    permissionCode: 'finance:archives:account_subjects:view'
  }),
  'finance-archives-projects': routeMeta({
    title: '项目档案',
    tabTitle: '项目档案',
    menuTitle: '项目档案',
    description: '项目分类与项目主目录维护',
    permissionCode: 'finance:archives:projects:view'
  }),
  'archives-invoices': routeMeta({
    title: '发票管理',
    menuTitle: '发票管理',
    permissionCode: 'archives:invoices:view'
  }),
  'archives-agents': routeMeta({
    title: 'Agent 工作台',
    tabTitle: 'Agent',
    menuTitle: 'Agent',
    description: '个人可配置的自动化 Agent 平台',
    permissionCode: 'agents:view',
    menuPermissionCodes: ['agents:menu', 'agents:view']
  }),
  'archives-account-books': routeMeta({
    title: '账套管理',
    menuTitle: '账套管理',
    description: '账套管理',
    placeholderTitle: '账套管理',
    placeholderDescription: '账套管理功能建设中',
    permissionCode: 'archives:account_books:view'
  }),
  settings: routeMeta({
    title: '系统设置',
    menuTitle: '系统设置',
    permissionCodes: [...SETTINGS_ROUTE_PERMISSION_CODES],
    menuPermissionCodes: [...SETTINGS_ROUTE_PERMISSION_CODES]
  })
} satisfies Record<string, AppRouteMetaDefinition>

export type AppRouteMetaKey = keyof typeof ROUTE_META_REGISTRY

function normalizePermissionCodes(codes: unknown): string[] {
  return Array.isArray(codes) ? codes.filter((item): item is string => typeof item === 'string') : []
}

function unwrapMeta(source: RouteMetaLike | { meta?: RouteMetaLike }): RouteMetaLike {
  if (source && typeof source === 'object' && 'meta' in source) {
    return (source as { meta?: RouteMetaLike }).meta ?? null
  }
  return (source as RouteMetaLike) ?? null
}

export function resolveRouteMeta(key: AppRouteMetaKey): AppRouteMetaDefinition {
  return ROUTE_META_REGISTRY[key]
}

export function getRoutePermissionCodes(source: RouteMetaLike | { meta?: RouteMetaLike }): string[] {
  const meta = unwrapMeta(source)
  if (!meta) {
    return []
  }
  const single = typeof meta.permissionCode === 'string' ? [meta.permissionCode] : []
  return [...single, ...normalizePermissionCodes(meta.permissionCodes)]
}

export function getRouteMenuPermissionCodes(source: RouteMetaLike | { meta?: RouteMetaLike }): string[] {
  const meta = unwrapMeta(source)
  if (!meta) {
    return []
  }
  const explicit = normalizePermissionCodes(meta.menuPermissionCodes)
  return explicit.length ? explicit : getRoutePermissionCodes(meta)
}

export function resolveRouteTitle(source: RouteMetaLike | { meta?: RouteMetaLike }): string {
  const meta = unwrapMeta(source)
  return (typeof meta?.title === 'string' && meta.title.trim()) ? meta.title : ''
}

export function resolveRouteTabTitle(source: RouteMetaLike | { meta?: RouteMetaLike }): string {
  const meta = unwrapMeta(source)
  if (typeof meta?.tabTitle === 'string' && meta.tabTitle.trim()) {
    return meta.tabTitle
  }
  return resolveRouteTitle(meta)
}

export function resolveRouteDescription(source: RouteMetaLike | { meta?: RouteMetaLike }): string {
  const meta = unwrapMeta(source)
  return (typeof meta?.description === 'string' && meta.description.trim()) ? meta.description : ''
}

export function resolveRouteMenuTitle(source: RouteMetaLike | { meta?: RouteMetaLike }): string {
  const meta = unwrapMeta(source)
  if (typeof meta?.menuTitle === 'string' && meta.menuTitle.trim()) {
    return meta.menuTitle
  }
  if (typeof meta?.tabTitle === 'string' && meta.tabTitle.trim()) {
    return meta.tabTitle
  }
  return resolveRouteTitle(meta)
}

export function resolvePlaceholderTitle(source: RouteMetaLike | { meta?: RouteMetaLike }): string {
  const meta = unwrapMeta(source)
  if (typeof meta?.placeholderTitle === 'string' && meta.placeholderTitle.trim()) {
    return meta.placeholderTitle
  }
  return resolveRouteTitle(meta) || '功能页面'
}

export function resolvePlaceholderDescription(source: RouteMetaLike | { meta?: RouteMetaLike }): string {
  const meta = unwrapMeta(source)
  if (typeof meta?.placeholderDescription === 'string' && meta.placeholderDescription.trim()) {
    return meta.placeholderDescription
  }
  return resolveRouteDescription(meta) || '功能建设中'
}
