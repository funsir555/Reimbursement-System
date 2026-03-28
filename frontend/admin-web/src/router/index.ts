import { ElMessage } from 'element-plus'
import { createRouter, createWebHistory, type RouteLocationNormalized, type RouteRecordRaw } from 'vue-router'
import { authApi } from '@/api'
import { hasAnyPermission, readStoredUser, resolveFirstAccessiblePath } from '@/utils/permissions'
import LoginView from '../views/LoginView.vue'
import MainLayout from '../layouts/MainLayout.vue'

function placeholderRoute(
  path: string,
  name: string,
  title: string,
  description: string,
  permissionCode: string
): RouteRecordRaw {
  return {
    path,
    name,
    component: () => import('../views/PlaceholderView.vue'),
    meta: { title, description, permissionCode }
  }
}

const settingsPermissionCodes = [
  'settings:menu',
  'settings:organization:view',
  'settings:employees:view',
  'settings:roles:view',
  'settings:companies:view'
]

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { public: true }
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('../views/DashboardView.vue'),
        meta: { title: 'Dashboard', permissionCode: 'dashboard:view' }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('../views/profile/PersonalCenterView.vue'),
        meta: { title: 'Profile', permissionCode: 'profile:view' }
      },
      placeholderRoute('expense/create', 'expense-create', 'Create Expense', 'Create expense forms', 'expense:create:view'),
      {
        path: 'expense/list',
        name: 'expense-list',
        component: () => import('../views/expense/ExpenseListView.vue'),
        meta: { title: 'Expense List', permissionCode: 'expense:list:view' }
      },
      placeholderRoute('expense/approval', 'expense-approval', 'Expense Approval', 'Pending approvals', 'expense:approval:view'),
      placeholderRoute('expense/payment/bank-link', 'expense-bank-link', 'Bank Link', 'Bank payment integration', 'expense:payment:bank_link:view'),
      placeholderRoute('expense/documents', 'expense-documents', 'Expense Documents', 'Document query center', 'expense:documents:view'),
      placeholderRoute('expense/voucher-generation', 'expense-voucher-generation', 'Voucher Generation', 'Generate vouchers', 'expense:voucher_generation:view'),
      {
        path: 'expense/workbench/process-management',
        name: 'expense-workbench-process-management',
        component: () => import('../views/process/ProcessManagementView.vue'),
        meta: { title: 'Process Management', permissionCode: 'expense:process_management:view' }
      },
      {
        path: 'expense/workbench/process-management/create/:templateType',
        name: 'expense-workbench-process-management-create',
        component: () => import('../views/process/ProcessTemplateCreateView.vue'),
        meta: { title: 'Create Process Template', permissionCode: 'expense:process_management:view' }
      },
      {
        path: 'expense/workbench/process-management/edit/:templateType/:id',
        name: 'expense-workbench-process-management-edit',
        component: () => import('../views/process/ProcessTemplateCreateView.vue'),
        meta: { title: 'Edit Process Template', permissionCode: 'expense:process_management:view' }
      },
      {
        path: 'expense/workbench/process-management/flow-designer/create',
        name: 'expense-workbench-process-flow-create',
        component: () => import('../views/process/ProcessFlowDesignerView.vue'),
        meta: { title: 'Process Flow Designer', permissionCode: 'expense:process_management:view' }
      },
      {
        path: 'expense/workbench/process-management/flow-designer/:id',
        name: 'expense-workbench-process-flow-edit',
        component: () => import('../views/process/ProcessFlowDesignerView.vue'),
        meta: { title: 'Process Flow Designer', permissionCode: 'expense:process_management:view' }
      },
      placeholderRoute('expense/workbench/budget-management', 'expense-workbench-budget-management', 'Budget Management', 'Budget management', 'expense:budget_management:view'),
      {
        path: 'finance/general-ledger/new-voucher',
        name: 'finance-new-voucher',
        component: () => import('../views/finance/FinanceNewVoucherView.vue'),
        meta: { title: 'New Voucher', description: 'Create finance vouchers', permissionCode: 'finance:general_ledger:new_voucher:view' }
      },
      placeholderRoute('finance/general-ledger/query-voucher', 'finance-query-voucher', 'Query Voucher', 'Query vouchers', 'finance:general_ledger:query_voucher:view'),
      placeholderRoute('finance/general-ledger/review-voucher', 'finance-review-voucher', 'Review Voucher', 'Review vouchers', 'finance:general_ledger:review_voucher:view'),
      placeholderRoute('finance/general-ledger/balance-sheet', 'finance-ledger-balance-sheet', 'Ledger Balance Sheet', 'Ledger balance sheet', 'finance:general_ledger:balance_sheet:view'),
      placeholderRoute('finance/fixed-assets', 'finance-fixed-assets', 'Fixed Assets', 'Fixed assets', 'finance:fixed_assets:view'),
      placeholderRoute('finance/reports/balance-sheet', 'finance-reports-balance-sheet', 'Balance Sheet Report', 'Balance sheet report', 'finance:reports:balance_sheet:view'),
      placeholderRoute('finance/reports/income-statement', 'finance-reports-income-statement', 'Income Statement', 'Income statement', 'finance:reports:income_statement:view'),
      placeholderRoute('finance/reports/cash-flow', 'finance-reports-cash-flow', 'Cash Flow', 'Cash flow report', 'finance:reports:cash_flow:view'),
      placeholderRoute('finance/archives/customers', 'finance-archives-customers', 'Customer Archive', 'Customer archive', 'finance:archives:customers:view'),
      placeholderRoute('finance/archives/suppliers', 'finance-archives-suppliers', 'Supplier Archive', 'Supplier archive', 'finance:archives:suppliers:view'),
      placeholderRoute('finance/archives/employees', 'finance-archives-employees', 'Employee Archive', 'Employee archive', 'finance:archives:employees:view'),
      placeholderRoute('finance/archives/departments', 'finance-archives-departments', 'Department Archive', 'Department archive', 'finance:archives:departments:view'),
      {
        path: 'archives/invoices',
        name: 'archives-invoices',
        component: () => import('../views/invoice/InvoiceListView.vue'),
        meta: { title: 'Invoice Archive', permissionCode: 'archives:invoices:view' }
      },
      placeholderRoute('archives/account-books', 'archives-account-books', 'Account Books', 'Account books', 'archives:account_books:view'),
      {
        path: 'settings',
        name: 'settings',
        component: () => import('../views/settings/SystemSettingsView.vue'),
        meta: { title: 'System Settings', permissionCodes: settingsPermissionCodes }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

function routePermissionCodes(to: RouteLocationNormalized): string[] {
  const permissionCode = typeof to.meta.permissionCode === 'string' ? [to.meta.permissionCode] : []
  const permissionCodes = Array.isArray(to.meta.permissionCodes)
    ? to.meta.permissionCodes.filter((item): item is string => typeof item === 'string')
    : []
  return [...permissionCode, ...permissionCodes]
}

async function ensureCurrentUser() {
  const cachedUser = readStoredUser()
  if (cachedUser?.permissionCodes?.length) {
    return cachedUser
  }

  const res = await authApi.getCurrentUser()
  localStorage.setItem('user', JSON.stringify(res.data))
  return res.data
}

router.beforeEach(async (to) => {
  if (to.meta.public) {
    return true
  }

  const token = localStorage.getItem('token')
  if (!token) {
    return '/login'
  }

  const requiredCodes = routePermissionCodes(to)
  if (!requiredCodes.length) {
    return true
  }

  try {
    const currentUser = await ensureCurrentUser()
    if (hasAnyPermission(requiredCodes, currentUser)) {
      return true
    }

    const fallbackPath = resolveFirstAccessiblePath(currentUser)
    if (fallbackPath !== to.fullPath) {
      ElMessage.warning('当前账号没有访问该页面的权限')
      return fallbackPath
    }
    return true
  } catch {
    return '/login'
  }
})

export default router
