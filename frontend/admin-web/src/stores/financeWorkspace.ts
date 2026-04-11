import { defineStore } from 'pinia'
import type { RouteLocationNormalizedLoaded } from 'vue-router'
import { resolveRouteTabTitle } from '@/router/route-meta'

export interface FinanceWorkspaceTab {
  path: string
  title: string
}

function resolveTabTitle(route: RouteLocationNormalizedLoaded): string {
  const tabTitle = resolveRouteTabTitle(route)
  if (tabTitle) {
    return tabTitle
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
