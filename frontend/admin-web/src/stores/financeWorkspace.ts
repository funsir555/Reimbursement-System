import { defineStore } from 'pinia'
import type { RouteLocationNormalizedLoaded } from 'vue-router'

export interface FinanceWorkspaceTab {
  path: string
  title: string
}

const FINANCE_ROUTE_TITLE_MAP: Record<string, string> = {
  'finance-new-voucher': '新建凭证',
  'finance-query-voucher': '查询凭证',
  'finance-query-voucher-detail': '凭证详情',
  'finance-review-voucher': '审核凭证',
  'finance-ledger-balance-sheet': '总账余额表',
  'finance-detail-ledger': '明细账',
  'finance-general-ledger-book': '总分类账',
  'finance-project-detail-ledger': '项目明细账',
  'finance-supplier-detail-ledger': '供应商明细账',
  'finance-customer-detail-ledger': '客户明细账',
  'finance-personal-detail-ledger': '个人明细账',
  'finance-quantity-amount-detail-ledger': '数量金额明细账',
  'finance-fixed-assets': '固定资产',
  'finance-reports-balance-sheet': '资产负债表',
  'finance-reports-income-statement': '利润表',
  'finance-reports-cash-flow': '现金流量表',
  'finance-system-management': '财务系统管理',
  'finance-archives-customers': '客户档案',
  'finance-archives-suppliers': '供应商档案',
  'finance-archives-employees': '员工档案',
  'finance-archives-departments': '部门档案',
  'finance-archives-account-subjects': '会计科目'
}

function resolveTabTitle(route: RouteLocationNormalizedLoaded) {
  if (typeof route.name === 'string' && FINANCE_ROUTE_TITLE_MAP[route.name]) {
    return FINANCE_ROUTE_TITLE_MAP[route.name]
  }

  const tabTitle = route.meta.tabTitle
  if (typeof tabTitle === 'string' && tabTitle.trim()) {
    return tabTitle
  }

  const title = route.meta.title
  if (typeof title === 'string' && title.trim()) {
    return title
  }

  return typeof route.name === 'string' ? route.name : route.fullPath
}

export const useFinanceWorkspaceStore = defineStore('financeWorkspace', {
  state: () => ({
    tabs: [] as FinanceWorkspaceTab[],
    activePath: ''
  }),
  getters: {
    isFinancePath: () => (path: string) => path.startsWith('/finance/'),
    cachedPaths: (state) => state.tabs.map((item) => item.path)
  },
  actions: {
    syncRoute(route: RouteLocationNormalizedLoaded) {
      if (!this.isFinancePath(route.path)) {
        return
      }

      const path = route.fullPath
      const existing = this.tabs.find((item) => item.path === path)
      if (existing) {
        existing.title = resolveTabTitle(route)
      } else {
        this.tabs.push({
          path,
          title: resolveTabTitle(route)
        })
      }

      this.activePath = path
    },
    activate(path: string) {
      this.activePath = path
    },
    close(path: string) {
      this.tabs = this.tabs.filter((item) => item.path !== path)
      if (this.activePath === path) {
        this.activePath = this.tabs[this.tabs.length - 1]?.path || ''
      }
    },
    closeOthers(path: string) {
      this.tabs = this.tabs.filter((item) => item.path === path)
      this.activePath = path
    },
    closeToRight(path: string) {
      const index = this.tabs.findIndex((item) => item.path === path)
      if (index === -1) {
        return
      }
      this.tabs = this.tabs.slice(0, index + 1)
      if (!this.tabs.some((item) => item.path === this.activePath)) {
        this.activePath = path
      }
    },
    getNextPathAfterClose(path: string) {
      const index = this.tabs.findIndex((item) => item.path === path)
      if (index === -1) {
        return this.tabs[this.tabs.length - 1]?.path || ''
      }

      return this.tabs[index + 1]?.path || this.tabs[index - 1]?.path || ''
    }
  }
})
