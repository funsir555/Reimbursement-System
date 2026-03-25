import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import MainLayout from '../layouts/MainLayout.vue'

function placeholderRoute(
  path: string,
  name: string,
  title: string,
  description: string
): RouteRecordRaw {
  return {
    path,
    name,
    component: () => import('../views/PlaceholderView.vue'),
    meta: { title, description }
  }
}

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
        meta: { title: '首页' }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('../views/profile/PersonalCenterView.vue'),
        meta: { title: '个人中心' }
      },
      placeholderRoute('expense/create', 'expense-create', '新建报销', '这里将承载报销单创建、附件上传与提交审批流程。'),
      {
        path: 'expense/list',
        name: 'expense-list',
        component: () => import('../views/expense/ExpenseListView.vue'),
        meta: { title: '我的报销' }
      },
      placeholderRoute('expense/approval', 'expense-approval', '待我审批', '这里将展示当前待你审批的报销单据与流转节点。'),
      placeholderRoute('expense/payment/bank-link', 'expense-bank-link', '银企直连', '这里将接入支付指令、付款回单和银企直连能力。'),
      placeholderRoute('expense/documents', 'expense-documents', '单据查询', '这里将集中查询报销单、审批单与相关业务单据。'),
      placeholderRoute('expense/voucher-generation', 'expense-voucher-generation', '凭证生成', '这里将根据报销结果自动生成财务凭证。'),
      {
        path: 'expense/workbench/process-management',
        name: 'expense-workbench-process-management',
        component: () => import('../views/process/ProcessManagementView.vue'),
        meta: { title: '流程管理' }
      },
      {
        path: 'expense/workbench/process-management/create/:templateType',
        name: 'expense-workbench-process-management-create',
        component: () => import('../views/process/ProcessTemplateCreateView.vue'),
        meta: { title: '新建单据模板' }
      },
      placeholderRoute('expense/workbench/budget-management', 'expense-workbench-budget-management', '预算管理', '这里将维护预算科目、额度和控制规则。'),
      placeholderRoute('finance/general-ledger/new-voucher', 'finance-new-voucher', '新建凭证', '这里将支持财务人员录入与生成总账凭证。'),
      placeholderRoute('finance/general-ledger/query-voucher', 'finance-query-voucher', '查询凭证', '这里将支持按期间、科目和状态查询凭证。'),
      placeholderRoute('finance/general-ledger/review-voucher', 'finance-review-voucher', '审核凭证', '这里将支持凭证审核、反审核和状态跟踪。'),
      placeholderRoute('finance/general-ledger/balance-sheet', 'finance-ledger-balance-sheet', '余额表', '这里将展示总账科目的余额表与期间数据。'),
      placeholderRoute('finance/fixed-assets', 'finance-fixed-assets', '固定资产', '这里将管理固定资产卡片、折旧和处置。'),
      placeholderRoute('finance/reports/balance-sheet', 'finance-reports-balance-sheet', '资产负债表', '这里将生成资产负债表。'),
      placeholderRoute('finance/reports/income-statement', 'finance-reports-income-statement', '利润表', '这里将生成利润表。'),
      placeholderRoute('finance/reports/cash-flow', 'finance-reports-cash-flow', '现金流量表', '这里将生成现金流量表。'),
      placeholderRoute('finance/archives/customers', 'finance-archives-customers', '客户档案', '这里将维护客户基础资料与往来信息。'),
      placeholderRoute('finance/archives/suppliers', 'finance-archives-suppliers', '供应商档案', '这里将维护供应商基础资料与结算信息。'),
      placeholderRoute('finance/archives/employees', 'finance-archives-employees', '员工档案', '这里将维护员工基础资料与财务归属信息。'),
      placeholderRoute('finance/archives/departments', 'finance-archives-departments', '部门档案', '这里将维护部门组织和财务归属关系。'),
      {
        path: 'archives/invoices',
        name: 'archives-invoices',
        component: () => import('../views/invoice/InvoiceListView.vue'),
        meta: { title: '发票管理' }
      },
      placeholderRoute('archives/account-books', 'archives-account-books', '账套管理', '这里将维护账套、期间和会计主体配置。'),
      placeholderRoute('settings', 'settings', '系统设置', '这里将维护系统级参数、基础配置与运行选项。')
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach((to, from, next) => {
  if (!to.meta.public) {
    const token = localStorage.getItem('token')
    if (!token) {
      return next('/login')
    }
  }
  next()
})

export default router
