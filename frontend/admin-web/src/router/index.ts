import { ElMessage } from 'element-plus'
import { createRouter, createWebHistory, type RouteLocationNormalized, type RouteRecordRaw } from 'vue-router'
import { authApi } from '@/api'
import {
  EXPENSE_CREATE_ENTRY_PERMISSION_CODES,
  hasAnyPermission,
  readStoredUser,
  resolveFirstAccessiblePath
} from '@/utils/permissions'
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
  'settings:companies:view',
  'settings:company_accounts:view'
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
        path: 'dashboard/pending-repayments',
        name: 'dashboard-pending-repayments',
        component: () => import('../views/dashboard/DashboardOutstandingDocumentsView.vue'),
        props: { kind: 'LOAN' },
        meta: { title: '待还款', permissionCode: 'dashboard:view' }
      },
      {
        path: 'dashboard/pending-prepay-writeoffs',
        name: 'dashboard-pending-prepay-writeoffs',
        component: () => import('../views/dashboard/DashboardOutstandingDocumentsView.vue'),
        props: { kind: 'PREPAY_REPORT' },
        meta: { title: '待预付核销', permissionCode: 'dashboard:view' }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('../views/profile/PersonalCenterView.vue'),
        meta: { title: 'Profile', permissionCode: 'profile:view' }
      },
      {
        path: 'expense/create',
        name: 'expense-create',
        component: () => import('../views/expense/ExpenseCreateView.vue'),
        meta: { title: 'Create Expense', permissionCodes: [...EXPENSE_CREATE_ENTRY_PERMISSION_CODES] }
      },
      {
        path: 'expense/documents/:documentCode/resubmit',
        name: 'expense-document-resubmit',
        component: () => import('../views/expense/ExpenseCreateView.vue'),
        meta: { title: 'Resubmit Expense', permissionCodes: ['expense:list:view', 'expense:create:create', 'expense:create:submit'] }
      },
      {
        path: 'expense/approval/tasks/:taskId/modify',
        name: 'expense-approval-task-modify',
        component: () => import('../views/expense/ExpenseCreateView.vue'),
        meta: { title: 'Modify Expense Approval', permissionCodes: ['expense:approval:view', 'expense:approval:approve'] }
      },
      {
        path: 'expense/create/details/:detailNo',
        name: 'expense-create-detail-edit',
        component: () => import('../views/expense/ExpenseDetailEditView.vue'),
        meta: {
          title: 'Edit Expense Detail',
          permissionCodes: [...EXPENSE_CREATE_ENTRY_PERMISSION_CODES, 'expense:approval:view', 'expense:approval:approve']
        }
      },
      {
        path: 'expense/list',
        name: 'expense-list',
        component: () => import('../views/expense/ExpenseListView.vue'),
        meta: { title: 'Expense List', permissionCode: 'expense:list:view' }
      },
      {
        path: 'expense/approval',
        name: 'expense-approval',
        component: () => import('../views/expense/ExpenseApprovalView.vue'),
        meta: { title: 'Expense Approval', permissionCode: 'expense:approval:view' }
      },
      {
        path: 'expense/documents/:documentCode',
        name: 'expense-document-detail',
        component: () => import('../views/expense/ExpenseDocumentDetailView.vue'),
        meta: { title: 'Expense Document Detail', permissionCodes: ['expense:list:view', 'expense:approval:view', 'expense:documents:view'] }
      },
      {
        path: 'expense/documents/:documentCode/details/:detailNo',
        name: 'expense-document-expense-detail',
        component: () => import('../views/expense/ExpenseDetailReadonlyView.vue'),
        meta: { title: 'Expense Detail', permissionCodes: ['expense:list:view', 'expense:approval:view', 'expense:documents:view'] }
      },
      {
        path: 'expense/payment/orders',
        name: 'expense-payment-orders',
        component: () => import('../views/expense/ExpensePaymentOrdersView.vue'),
        meta: {
          title: '\u4ed8\u6b3e\u5355',
          tabTitle: '\u4ed8\u6b3e\u5355',
          permissionCode: 'expense:payment:payment_order:view'
        }
      },
      {
        path: 'expense/payment/bank-link',
        name: 'expense-bank-link',
        component: () => import('../views/expense/ExpenseBankLinkView.vue'),
        meta: {
          title: '閾朵紒鐩磋繛',
          tabTitle: '閾朵紒鐩磋繛',
          permissionCode: 'expense:payment:bank_link:view'
        }
      },
      {
        path: 'expense/documents',
        name: 'expense-documents',
        component: () => import('../views/expense/ExpenseDocumentsView.vue'),
        meta: { title: '閸楁洘宓侀弻銉嚄', tabTitle: '閸楁洘宓侀弻銉嚄', description: '缂佺喍绔撮弻銉嚄瀹稿弶褰佹禍銈呭礋閹诡喖鑻熼弻銉ф箙濞翠胶鈻奸悩鑸碘偓浣碘偓浣姐€冮崡鏇炴彥閻撗傜瑢鐎光剝澹掔紒鎾寸亯', permissionCode: 'expense:documents:view' }
      },
      {
        path: 'expense/voucher-generation',
        name: 'expense-voucher-generation',
        component: () => import('../views/expense/ExpenseVoucherGenerationView.vue'),
        meta: {
          title: '凭证生成',
          tabTitle: '凭证生成',
          description: '报销凭证生成工作台',
          permissionCode: 'expense:voucher_generation:view'
        }
      },
      {
        path: 'expense/workbench/process-management',
        name: 'expense-workbench-process-management',
        component: () => import('../views/process/ProcessManagementView.vue'),
        meta: {
          title: '流程管理',
          tabTitle: '流程管理',
          description: '流程管理工作台',
          permissionCode: 'expense:process_management:view'
        }
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
      {
        path: 'expense/workbench/process-management/form-designer/create',
        name: 'expense-workbench-process-form-create',
        component: () => import('../views/process/ProcessFormDesignerView.vue'),
        meta: { title: 'Process Form Designer', permissionCode: 'expense:process_management:view' }
      },
      {
        path: 'expense/workbench/process-management/form-designer/:id',
        name: 'expense-workbench-process-form-edit',
        component: () => import('../views/process/ProcessFormDesignerView.vue'),
        meta: { title: 'Process Form Designer', permissionCode: 'expense:process_management:view' }
      },
      {
        path: 'expense/workbench/process-management/expense-detail-designer/create',
        name: 'expense-workbench-process-expense-detail-create',
        component: () => import('../views/process/ProcessFormDesignerView.vue'),
        meta: { title: 'Expense Detail Designer', permissionCode: 'expense:process_management:view' }
      },
      {
        path: 'expense/workbench/process-management/expense-detail-designer/:id',
        name: 'expense-workbench-process-expense-detail-edit',
        component: () => import('../views/process/ProcessFormDesignerView.vue'),
        meta: { title: 'Expense Detail Designer', permissionCode: 'expense:process_management:view' }
      },
      placeholderRoute('expense/workbench/budget-management', 'expense-workbench-budget-management', 'Budget Management', 'Budget management', 'expense:budget_management:view'),
      {
        path: 'finance/general-ledger/new-voucher',
        name: 'finance-new-voucher',
        component: () => import('../views/finance/FinanceNewVoucherView.vue'),
        props: { pageMode: 'create' },
        meta: {
          title: '新建凭证',
          tabTitle: '新建凭证',
          description: '新建总账凭证',
          permissionCode: 'finance:general_ledger:new_voucher:view'
        }
      },
      {
        path: 'finance/general-ledger/query-voucher',
        name: 'finance-query-voucher',
        component: () => import('../views/finance/FinanceQueryVoucherView.vue'),
        meta: {
          title: '查询凭证',
          tabTitle: '查询凭证',
          description: '查询总账凭证',
          permissionCode: 'finance:general_ledger:query_voucher:view'
        }
      },
      {
        path: 'finance/general-ledger/query-voucher/:voucherNo',
        name: 'finance-query-voucher-detail',
        component: () => import('../views/finance/FinanceNewVoucherView.vue'),
        props: (route) => ({
          pageMode: 'detail',
          voucherNo: Array.isArray(route.params.voucherNo) ? route.params.voucherNo[0] || '' : String(route.params.voucherNo || '')
        }),
        meta: {
          title: '凭证详情',
          tabTitle: '凭证详情',
          description: '查看和修改凭证详情',
          permissionCode: 'finance:general_ledger:query_voucher:view'
        }
      },
      {
        ...placeholderRoute('finance/general-ledger/review-voucher', 'finance-review-voucher', '瀹℃牳鍑瘉', '瀹℃牳鎬昏处鍑瘉', 'finance:general_ledger:review_voucher:view'),
        meta: {
          title: '瀹℃牳鍑瘉',
          tabTitle: '瀹℃牳鍑瘉',
          description: '瀹℃牳鎬昏处鍑瘉',
          permissionCode: 'finance:general_ledger:review_voucher:view'
        }
      },
      {
        ...placeholderRoute('finance/general-ledger/balance-sheet', 'finance-ledger-balance-sheet', '总账余额表', '查看总账余额表', 'finance:general_ledger:balance_sheet:view'),
        meta: {
          title: '总账余额表',
          tabTitle: '总账余额表',
          description: '查看总账余额表',
          permissionCode: 'finance:general_ledger:balance_sheet:view'
        }
      },
      {
        ...placeholderRoute('finance/general-ledger/detail-ledger', 'finance-detail-ledger', '明细账', '明细账功能建设中', 'finance:general_ledger:detail_ledger:view'),
        meta: {
          title: '明细账',
          tabTitle: '明细账',
          description: '明细账功能建设中',
          permissionCode: 'finance:general_ledger:detail_ledger:view'
        }
      },
      {
        ...placeholderRoute('finance/general-ledger/general-ledger', 'finance-general-ledger-book', '总分类账', '总分类账功能建设中', 'finance:general_ledger:general_ledger:view'),
        meta: {
          title: '总分类账',
          tabTitle: '总分类账',
          description: '总分类账功能建设中',
          permissionCode: 'finance:general_ledger:general_ledger:view'
        }
      },
      {
        ...placeholderRoute('finance/general-ledger/project-detail-ledger', 'finance-project-detail-ledger', '项目明细账', '项目明细账功能建设中', 'finance:general_ledger:project_detail_ledger:view'),
        meta: {
          title: '项目明细账',
          tabTitle: '项目明细账',
          description: '项目明细账功能建设中',
          permissionCode: 'finance:general_ledger:project_detail_ledger:view'
        }
      },
      {
        ...placeholderRoute('finance/general-ledger/supplier-detail-ledger', 'finance-supplier-detail-ledger', '供应商明细账', '供应商明细账功能建设中', 'finance:general_ledger:supplier_detail_ledger:view'),
        meta: {
          title: '供应商明细账',
          tabTitle: '供应商明细账',
          description: '供应商明细账功能建设中',
          permissionCode: 'finance:general_ledger:supplier_detail_ledger:view'
        }
      },
      {
        ...placeholderRoute('finance/general-ledger/customer-detail-ledger', 'finance-customer-detail-ledger', '客户明细账', '客户明细账功能建设中', 'finance:general_ledger:customer_detail_ledger:view'),
        meta: {
          title: '客户明细账',
          tabTitle: '客户明细账',
          description: '客户明细账功能建设中',
          permissionCode: 'finance:general_ledger:customer_detail_ledger:view'
        }
      },
      {
        ...placeholderRoute('finance/general-ledger/personal-detail-ledger', 'finance-personal-detail-ledger', '个人明细账', '个人明细账功能建设中', 'finance:general_ledger:personal_detail_ledger:view'),
        meta: {
          title: '个人明细账',
          tabTitle: '个人明细账',
          description: '个人明细账功能建设中',
          permissionCode: 'finance:general_ledger:personal_detail_ledger:view'
        }
      },
      {
        ...placeholderRoute('finance/general-ledger/quantity-amount-detail-ledger', 'finance-quantity-amount-detail-ledger', '数量金额明细账', '数量金额明细账功能建设中', 'finance:general_ledger:quantity_amount_detail_ledger:view'),
        meta: {
          title: '数量金额明细账',
          tabTitle: '数量金额明细账',
          description: '数量金额明细账功能建设中',
          permissionCode: 'finance:general_ledger:quantity_amount_detail_ledger:view'
        }
      },
      {
        path: 'finance/fixed-assets',
        name: 'finance-fixed-assets',
        component: () => import('../views/finance/FinanceFixedAssetsView.vue'),
        meta: {
          title: '固定资产',
          tabTitle: '固定资产',
          description: '固定资产业务工作台',
          permissionCode: 'finance:fixed_assets:view'
        }
      },
      {
        ...placeholderRoute('finance/reports/balance-sheet', 'finance-reports-balance-sheet', 'Balance Sheet Report', 'Balance sheet report', 'finance:reports:balance_sheet:view'),
        meta: {
          title: '资产负债表',
          tabTitle: '资产负债表',
          description: '查看资产负债表',
          permissionCode: 'finance:reports:balance_sheet:view'
        }
      },
      {
        ...placeholderRoute('finance/reports/income-statement', 'finance-reports-income-statement', 'Income Statement', 'Income statement', 'finance:reports:income_statement:view'),
        meta: {
          title: '利润表',
          tabTitle: '利润表',
          description: '查看利润表',
          permissionCode: 'finance:reports:income_statement:view'
        }
      },
      {
        ...placeholderRoute('finance/reports/cash-flow', 'finance-reports-cash-flow', 'Cash Flow', 'Cash flow report', 'finance:reports:cash_flow:view'),
        meta: {
          title: '现金流量表',
          tabTitle: '现金流量表',
          description: '查看现金流量表',
          permissionCode: 'finance:reports:cash_flow:view'
        }
      },
      {
        path: 'finance/system-management',
        name: 'finance-system-management',
        component: () => import('../views/finance/FinanceSystemManagementView.vue'),
        meta: {
          title: '??????',
          tabTitle: '??????',
          description: '???????????',
          permissionCode: 'finance:system_management:view'
        }
      },
      {
        path: 'finance/archives/customers',
        name: 'finance-archives-customers',
        component: () => import('../views/finance/FinanceCustomerArchiveView.vue'),
        meta: { title: '客户档案', tabTitle: '客户档案', description: '客户主数据维护', permissionCode: 'finance:archives:customers:view' }
      },
      {
        path: 'finance/archives/suppliers',
        name: 'finance-archives-suppliers',
        component: () => import('../views/finance/FinanceSupplierArchiveView.vue'),
        meta: { title: '供应商档案', tabTitle: '供应商档案', description: '供应商主数据维护', permissionCode: 'finance:archives:suppliers:view' }
      },
      {
        path: 'finance/archives/employees',
        name: 'finance-archives-employees',
        component: () => import('../views/finance/FinanceEmployeeArchiveView.vue'),
        meta: {
          title: '员工档案',
          tabTitle: '员工档案',
          description: '员工主数据查询',
          permissionCode: 'finance:archives:employees:view'
        }
      },
      {
        ...placeholderRoute('finance/archives/departments', 'finance-archives-departments', 'Department Archive', 'Department archive', 'finance:archives:departments:view'),
        meta: {
          title: '部门档案',
          tabTitle: '部门档案',
          description: '部门主数据维护',
          permissionCode: 'finance:archives:departments:view'
        }
      },
      {
        path: 'finance/archives/account-subjects',
        name: 'finance-archives-account-subjects',
        component: () => import('../views/finance/FinanceAccountSubjectArchiveView.vue'),
        meta: {
          title: '会计科目',
          tabTitle: '会计科目',
          description: '会计科目主数据维护',
          permissionCode: 'finance:archives:account_subjects:view'
        }
      },
      {
        path: 'finance/archives/projects',
        name: 'finance-archives-projects',
        component: () => import('../views/finance/FinanceProjectArchiveView.vue'),
        meta: {
          title: '项目档案',
          tabTitle: '项目档案',
          description: '项目分类与项目主目录维护',
          permissionCode: 'finance:archives:projects:view'
        }
      },
      {
        path: 'archives/invoices',
        name: 'archives-invoices',
        component: () => import('../views/invoice/InvoiceListView.vue'),
        meta: { title: 'Invoice Archive', permissionCode: 'archives:invoices:view' }
      },
      {
        path: 'archives/agents',
        name: 'archives-agents',
        component: () => import('../views/archives/ArchiveAgentView.vue'),
        meta: {
          title: 'Agent 工作台',
          tabTitle: 'Agent',
          description: '个人可配置的自动化 Agent 平台',
          permissionCode: 'agents:view'
        }
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
      ElMessage.warning('当前没有该页面访问权限，已跳转到可访问的首页。')
      return fallbackPath
    }
    return true
  } catch {
    return '/login'
  }
})

export default router




