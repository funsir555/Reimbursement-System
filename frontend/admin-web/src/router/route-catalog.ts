import type { RouteRecordRaw } from 'vue-router'
import type { AppRouteMetaKey } from './route-meta'
import { resolveRouteMeta } from './route-meta'
import LoginView from '../views/LoginView.vue'

type RouteComponent = NonNullable<RouteRecordRaw['component']>

// 用一份轻量定义同时生成 Vue Router 路由记录和“路由名 -> 绝对路径”索引，
// 可以避免同一套路由信息维护两份后发生不一致。
interface AppRouteDefinition {
  path: string
  name?: string
  component?: RouteComponent
  redirect?: string
  props?: RouteRecordRaw['props']
  metaKey?: AppRouteMetaKey
  children?: AppRouteDefinition[]
}

const placeholderView = () => import('../views/PlaceholderView.vue')

// 这份目录就是前端“页面地图”：定义每个路径对应哪个页面，
// 以及该页面后续要套用哪套 meta 规则来驱动登录、菜单和权限判断。
export const routeCatalog: AppRouteDefinition[] = [
  {
    // 登录页：未登录用户的公开入口页面。
    path: '/login',
    name: 'login',
    component: LoginView,
    metaKey: 'login'
  },
  {
    path: '/expense/documents/:documentCode/print',
    name: 'expense-document-print',
    component: () => import('../views/expense/ExpenseDocumentBatchPrintView.vue'),
    metaKey: 'expense-document-print'
  },
  {
    path: '/expense/documents/print',
    name: 'expense-document-batch-print',
    component: () => import('../views/expense/ExpenseDocumentBatchPrintView.vue'),
    metaKey: 'expense-document-batch-print'
  },
  {
    // 主框架页：登录后的大多数业务页面都挂在 MainLayout 下面。
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        // 首页与个人中心：用户登录后最先接触到的入口区域。
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('../views/DashboardView.vue'),
        metaKey: 'dashboard'
      },
      {
        path: 'dashboard/pending-repayments',
        name: 'dashboard-pending-repayments',
        component: () => import('../views/dashboard/DashboardOutstandingDocumentsView.vue'),
        props: { kind: 'LOAN' },
        metaKey: 'dashboard-pending-repayments'
      },
      {
        path: 'dashboard/pending-prepay-writeoffs',
        name: 'dashboard-pending-prepay-writeoffs',
        component: () => import('../views/dashboard/DashboardOutstandingDocumentsView.vue'),
        props: { kind: 'PREPAY_REPORT' },
        metaKey: 'dashboard-pending-prepay-writeoffs'
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('../views/profile/PersonalCenterView.vue'),
        metaKey: 'profile'
      },
      // 报销工作台：报销创建、查看、审批、付款、凭证生成等主业务页面。
      {
        path: 'expense/create',
        name: 'expense-create',
        component: () => import('../views/expense/ExpenseCreateView.vue'),
        metaKey: 'expense-create'
      },
      {
        path: 'expense/documents/:documentCode/resubmit',
        name: 'expense-document-resubmit',
        component: () => import('../views/expense/ExpenseCreateView.vue'),
        metaKey: 'expense-document-resubmit'
      },
      {
        path: 'expense/approval/tasks/:taskId/modify',
        name: 'expense-approval-task-modify',
        component: () => import('../views/expense/ExpenseCreateView.vue'),
        metaKey: 'expense-approval-task-modify'
      },
      {
        path: 'expense/create/details/:detailNo',
        name: 'expense-create-detail-edit',
        component: () => import('../views/expense/ExpenseDetailEditView.vue'),
        metaKey: 'expense-create-detail-edit'
      },
      {
        path: 'expense/list',
        name: 'expense-list',
        component: () => import('../views/expense/ExpenseListView.vue'),
        metaKey: 'expense-list'
      },
      {
        path: 'expense/approval',
        name: 'expense-approval',
        component: () => import('../views/expense/ExpenseApprovalView.vue'),
        metaKey: 'expense-approval'
      },
      {
        path: 'expense/documents/:documentCode',
        name: 'expense-document-detail',
        component: () => import('../views/expense/ExpenseDocumentDetailView.vue'),
        metaKey: 'expense-document-detail'
      },
      {
        path: 'expense/documents/:documentCode/details/:detailNo',
        name: 'expense-document-expense-detail',
        component: () => import('../views/expense/ExpenseDetailReadonlyView.vue'),
        metaKey: 'expense-document-expense-detail'
      },
      {
        path: 'expense/payment/orders',
        name: 'expense-payment-orders',
        component: () => import('../views/expense/ExpensePaymentOrdersView.vue'),
        metaKey: 'expense-payment-orders'
      },
      {
        path: 'expense/payment/bank-link',
        name: 'expense-bank-link',
        component: () => import('../views/expense/ExpenseBankLinkView.vue'),
        metaKey: 'expense-bank-link'
      },
      {
        path: 'expense/documents',
        name: 'expense-documents',
        component: () => import('../views/expense/ExpenseDocumentsView.vue'),
        metaKey: 'expense-documents'
      },
      {
        path: 'expense/voucher-generation',
        name: 'expense-voucher-generation',
        component: () => import('../views/expense/ExpenseVoucherGenerationView.vue'),
        metaKey: 'expense-voucher-generation'
      },
      {
        path: 'expense/workbench/process-management',
        name: 'expense-workbench-process-management',
        component: () => import('../views/process/ProcessManagementView.vue'),
        metaKey: 'expense-workbench-process-management'
      },
      {
        path: 'expense/workbench/process-management/create/:templateType',
        name: 'expense-workbench-process-management-create',
        component: () => import('../views/process/ProcessTemplateCreateView.vue'),
        metaKey: 'expense-workbench-process-management-create'
      },
      {
        path: 'expense/workbench/process-management/edit/:templateType/:id',
        name: 'expense-workbench-process-management-edit',
        component: () => import('../views/process/ProcessTemplateCreateView.vue'),
        metaKey: 'expense-workbench-process-management-edit'
      },
      {
        path: 'expense/workbench/process-management/flow-designer/create',
        name: 'expense-workbench-process-flow-create',
        component: () => import('../views/process/ProcessFlowDesignerView.vue'),
        metaKey: 'expense-workbench-process-flow-create'
      },
      {
        path: 'expense/workbench/process-management/flow-designer/:id',
        name: 'expense-workbench-process-flow-edit',
        component: () => import('../views/process/ProcessFlowDesignerView.vue'),
        metaKey: 'expense-workbench-process-flow-edit'
      },
      {
        path: 'expense/workbench/process-management/form-designer/create',
        name: 'expense-workbench-process-form-create',
        component: () => import('../views/process/ProcessFormDesignerView.vue'),
        metaKey: 'expense-workbench-process-form-create'
      },
      {
        path: 'expense/workbench/process-management/form-designer/:id',
        name: 'expense-workbench-process-form-edit',
        component: () => import('../views/process/ProcessFormDesignerView.vue'),
        metaKey: 'expense-workbench-process-form-edit'
      },
      {
        path: 'expense/workbench/process-management/expense-detail-designer/create',
        name: 'expense-workbench-process-expense-detail-create',
        component: () => import('../views/process/ProcessFormDesignerView.vue'),
        metaKey: 'expense-workbench-process-expense-detail-create'
      },
      {
        path: 'expense/workbench/process-management/expense-detail-designer/:id',
        name: 'expense-workbench-process-expense-detail-edit',
        component: () => import('../views/process/ProcessFormDesignerView.vue'),
        metaKey: 'expense-workbench-process-expense-detail-edit'
      },
      {
        path: 'expense/workbench/budget-management',
        name: 'expense-workbench-budget-management',
        component: placeholderView,
        metaKey: 'expense-workbench-budget-management'
      },
      // 财务模块：总账、固定资产、财务档案、财务系统管理等页面。
      {
        path: 'finance/general-ledger/new-voucher',
        name: 'finance-new-voucher',
        component: () => import('../views/finance/FinanceNewVoucherView.vue'),
        props: { pageMode: 'create' },
        metaKey: 'finance-new-voucher'
      },
      {
        path: 'finance/general-ledger/query-voucher',
        name: 'finance-query-voucher',
        component: () => import('../views/finance/FinanceQueryVoucherView.vue'),
        metaKey: 'finance-query-voucher'
      },
      {
        path: 'finance/general-ledger/query-voucher/:voucherNo',
        name: 'finance-query-voucher-detail',
        component: () => import('../views/finance/FinanceNewVoucherView.vue'),
        props: (route) => ({
          pageMode: 'detail',
          voucherNo: Array.isArray(route.params.voucherNo) ? route.params.voucherNo[0] || '' : String(route.params.voucherNo || '')
        }),
        metaKey: 'finance-query-voucher-detail'
      },
      {
        path: 'finance/general-ledger/review-voucher',
        name: 'finance-review-voucher',
        component: () => import('../views/finance/FinanceReviewVoucherView.vue'),
        metaKey: 'finance-review-voucher'
      },
      {
        path: 'finance/general-ledger/review-voucher/:voucherNo',
        name: 'finance-review-voucher-detail',
        component: () => import('../views/finance/FinanceNewVoucherView.vue'),
        props: (route) => ({
          pageMode: 'review',
          voucherNo: Array.isArray(route.params.voucherNo) ? route.params.voucherNo[0] || '' : String(route.params.voucherNo || '')
        }),
        metaKey: 'finance-review-voucher-detail'
      },
      {
        path: 'finance/general-ledger/post-voucher',
        name: 'finance-post-voucher',
        component: placeholderView,
        metaKey: 'finance-post-voucher'
      },
      {
        path: 'finance/general-ledger/close-ledger',
        name: 'finance-close-ledger',
        component: placeholderView,
        metaKey: 'finance-close-ledger'
      },
      {
        path: 'finance/general-ledger/balance-sheet',
        name: 'finance-ledger-balance-sheet',
        component: placeholderView,
        metaKey: 'finance-ledger-balance-sheet'
      },
      {
        path: 'finance/general-ledger/detail-ledger',
        name: 'finance-detail-ledger',
        component: placeholderView,
        metaKey: 'finance-detail-ledger'
      },
      {
        path: 'finance/general-ledger/general-ledger',
        name: 'finance-general-ledger-book',
        component: placeholderView,
        metaKey: 'finance-general-ledger-book'
      },
      {
        path: 'finance/general-ledger/project-detail-ledger',
        name: 'finance-project-detail-ledger',
        component: placeholderView,
        metaKey: 'finance-project-detail-ledger'
      },
      {
        path: 'finance/general-ledger/supplier-detail-ledger',
        name: 'finance-supplier-detail-ledger',
        component: placeholderView,
        metaKey: 'finance-supplier-detail-ledger'
      },
      {
        path: 'finance/general-ledger/customer-detail-ledger',
        name: 'finance-customer-detail-ledger',
        component: placeholderView,
        metaKey: 'finance-customer-detail-ledger'
      },
      {
        path: 'finance/general-ledger/personal-detail-ledger',
        name: 'finance-personal-detail-ledger',
        component: placeholderView,
        metaKey: 'finance-personal-detail-ledger'
      },
      {
        path: 'finance/general-ledger/quantity-amount-detail-ledger',
        name: 'finance-quantity-amount-detail-ledger',
        component: placeholderView,
        metaKey: 'finance-quantity-amount-detail-ledger'
      },
      {
        path: 'finance/fixed-assets',
        name: 'finance-fixed-assets',
        component: () => import('../views/finance/FinanceFixedAssetsView.vue'),
        metaKey: 'finance-fixed-assets'
      },
      {
        path: 'finance/reports/balance-sheet',
        name: 'finance-reports-balance-sheet',
        component: placeholderView,
        metaKey: 'finance-reports-balance-sheet'
      },
      {
        path: 'finance/reports/income-statement',
        name: 'finance-reports-income-statement',
        component: placeholderView,
        metaKey: 'finance-reports-income-statement'
      },
      {
        path: 'finance/reports/cash-flow',
        name: 'finance-reports-cash-flow',
        component: placeholderView,
        metaKey: 'finance-reports-cash-flow'
      },
      {
        path: 'finance/system-management',
        name: 'finance-system-management',
        component: () => import('../views/finance/FinanceSystemManagementView.vue'),
        metaKey: 'finance-system-management'
      },
      {
        path: 'finance/archives/customers',
        name: 'finance-archives-customers',
        component: () => import('../views/finance/FinanceCustomerArchiveView.vue'),
        metaKey: 'finance-archives-customers'
      },
      {
        path: 'finance/archives/suppliers',
        name: 'finance-archives-suppliers',
        component: () => import('../views/finance/FinanceSupplierArchiveView.vue'),
        metaKey: 'finance-archives-suppliers'
      },
      {
        path: 'finance/archives/employees',
        name: 'finance-archives-employees',
        component: () => import('../views/finance/FinanceEmployeeArchiveView.vue'),
        metaKey: 'finance-archives-employees'
      },
      {
        path: 'finance/archives/departments',
        name: 'finance-archives-departments',
        component: () => import('../views/finance/FinanceDepartmentArchiveView.vue'),
        metaKey: 'finance-archives-departments'
      },
      {
        path: 'finance/archives/account-subjects',
        name: 'finance-archives-account-subjects',
        component: () => import('../views/finance/FinanceAccountSubjectArchiveView.vue'),
        metaKey: 'finance-archives-account-subjects'
      },
      {
        path: 'finance/archives/projects',
        name: 'finance-archives-projects',
        component: () => import('../views/finance/FinanceProjectArchiveView.vue'),
        metaKey: 'finance-archives-projects'
      },
      // 档案中心：发票、档案 Agent、账簿等档案类页面。
      {
        path: 'archives/invoices',
        name: 'archives-invoices',
        component: () => import('../views/invoice/InvoiceListView.vue'),
        metaKey: 'archives-invoices'
      },
      {
        path: 'archives/agents',
        name: 'archives-agents',
        component: () => import('../views/archives/ArchiveAgentView.vue'),
        metaKey: 'archives-agents'
      },
      {
        path: 'archives/account-books',
        name: 'archives-account-books',
        component: placeholderView,
        metaKey: 'archives-account-books'
      },
      // 系统设置：组织、角色、同步配置等系统级管理页面。
      {
        path: 'settings',
        name: 'settings',
        component: () => import('../views/settings/SystemSettingsView.vue'),
        metaKey: 'settings'
      }
    ]
  }
]

function joinRoutePath(parentPath: string, path: string): string {
  if (path.startsWith('/')) {
    return path
  }
  if (!parentPath || parentPath === '/') {
    return `/${path}`.replace(/\/+/g, '/')
  }
  return `${parentPath.replace(/\/$/, '')}/${path}`.replace(/\/+/g, '/')
}

function buildAbsolutePathIndex(definitions: AppRouteDefinition[], parentPath = ''): Record<string, string> {
  return definitions.reduce<Record<string, string>>((accumulator, definition) => {
    const currentPath = joinRoutePath(parentPath, definition.path)
    if (definition.name) {
      accumulator[definition.name] = currentPath
    }
    if (definition.children?.length) {
      Object.assign(accumulator, buildAbsolutePathIndex(definition.children, currentPath))
    }
    return accumulator
  }, {})
}

export const ROUTE_PATH_BY_NAME = buildAbsolutePathIndex(routeCatalog)

export function getRoutePathByName(name: string): string {
  const path = ROUTE_PATH_BY_NAME[name]
  if (!path) {
    throw new Error(`Unknown route name: ${name}`)
  }
  return path
}

function buildRoutes(definitions: AppRouteDefinition[]): RouteRecordRaw[] {
  // 把轻量路由定义转换成 Vue Router 真正运行时使用的结构。
  return definitions.map((definition) => {
    const route = {
      path: definition.path
    } as RouteRecordRaw

    if (definition.name) {
      route.name = definition.name
    }
    if (definition.component) {
      route.component = definition.component
    }
    if (definition.redirect) {
      route.redirect = definition.redirect
    }
    if (definition.props) {
      route.props = definition.props
    }
    if (definition.metaKey) {
      route.meta = resolveRouteMeta(definition.metaKey) as RouteRecordRaw['meta']
    }
    if (definition.children?.length) {
      route.children = buildRoutes(definition.children)
    }

    return route
  })
}

export function buildRouteRecords(): RouteRecordRaw[] {
  return buildRoutes(routeCatalog)
}
