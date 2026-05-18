import { defineStore } from 'pinia'
import type { RouteLocationNormalizedLoaded } from 'vue-router'
import { resolveRouteTabTitle } from '@/router/route-meta'

export interface FinanceWorkspaceTab {
  path: string
  title: string
}

export type FinanceWorkspaceCloseGuard = () => boolean | Promise<boolean>

const closeGuardRegistry = new Map<string, FinanceWorkspaceCloseGuard>()

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
    registerCloseGuard(path: string, guard: FinanceWorkspaceCloseGuard) {
      const targetPath = String(path || '')
      if (!targetPath) {
        return
      }
      closeGuardRegistry.set(targetPath, guard)
    },
    unregisterCloseGuard(path: string) {
      const targetPath = String(path || '')
      if (!targetPath) {
        return
      }
      closeGuardRegistry.delete(targetPath)
    },
    async requestClose(path: string) {
      const targetPath = String(path || '')
      if (!targetPath) {
        return true
      }
      const guard = closeGuardRegistry.get(targetPath)
      if (!guard) {
        return true
      }
      return await guard()
    },
    close(path: string) {
      closeGuardRegistry.delete(path)
      this.tabs = this.tabs.filter((item) => item.path !== path)
      if (this.activePath === path) {
        this.activePath = this.tabs[this.tabs.length - 1]?.path || ''
      }
    },
    replaceTabPath(oldPath: string, nextPath: string, title?: string) {
      const sourcePath = String(oldPath || '')
      const targetPath = String(nextPath || '')
      if (!sourcePath || !targetPath || sourcePath === targetPath) {
        return
      }

      const sourceIndex = this.tabs.findIndex((item) => item.path === sourcePath)
      if (sourceIndex === -1) {
        return
      }

      const existingTargetIndex = this.tabs.findIndex((item) => item.path === targetPath)
      closeGuardRegistry.delete(sourcePath)

      if (existingTargetIndex !== -1) {
        if (title) {
          const targetTab = this.tabs[existingTargetIndex]
          if (!targetTab) {
            return
          }
          this.tabs[existingTargetIndex] = {
            ...targetTab,
            title
          }
        }
        this.tabs = this.tabs.filter((item) => item.path !== sourcePath)
        if (this.activePath === sourcePath) {
          this.activePath = targetPath
        }
        return
      }

      const sourceTab = this.tabs[sourceIndex]
      if (!sourceTab) {
        return
      }
      this.tabs[sourceIndex] = {
        path: targetPath,
        title: title || sourceTab.title || targetPath
      }
      if (this.activePath === sourcePath) {
        this.activePath = targetPath
      }
    },
    closeOthers(path: string) {
      this.tabs
        .filter((item) => item.path !== path)
        .forEach((item) => closeGuardRegistry.delete(item.path))
      this.tabs = this.tabs.filter((item) => item.path === path)
      this.activePath = path
    },
    closeToRight(path: string) {
      const index = this.tabs.findIndex((item) => item.path === path)
      if (index === -1) {
        return
      }
      this.tabs.slice(index + 1).forEach((item) => closeGuardRegistry.delete(item.path))
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
