import { defineStore } from 'pinia'
import type { RouteLocationNormalizedLoaded } from 'vue-router'
import { resolveRouteTabTitle } from '@/router/route-meta'
import { FINANCE_HOME_PATH } from '@/utils/financeHomeModules'

export interface FinanceWorkspaceTab {
  path: string
  title: string
  closable: boolean
  pinned: boolean
  kind?: 'home' | 'page'
}

export type FinanceWorkspaceCloseGuard = () => boolean | Promise<boolean>
export type FinanceWorkspaceActionGuard = () => boolean | Promise<boolean>

type FinanceWorkspaceGuardEntry = {
  close?: FinanceWorkspaceCloseGuard
  periodSwitch?: FinanceWorkspaceActionGuard
  createVoucherTakeover?: FinanceWorkspaceActionGuard
}

const guardRegistry = new Map<string, FinanceWorkspaceGuardEntry>()

function ensureGuardEntry(path: string) {
  const targetPath = String(path || '')
  if (!targetPath) {
    return null
  }
  const existing = guardRegistry.get(targetPath)
  if (existing) {
    return existing
  }
  const created: FinanceWorkspaceGuardEntry = {}
  guardRegistry.set(targetPath, created)
  return created
}

function cleanupGuardEntry(path: string) {
  const targetPath = String(path || '')
  if (!targetPath) {
    return
  }
  const entry = guardRegistry.get(targetPath)
  if (!entry) {
    return
  }
  if (entry.close || entry.periodSwitch || entry.createVoucherTakeover) {
    return
  }
  guardRegistry.delete(targetPath)
}

function createFinanceHomeTab(): FinanceWorkspaceTab {
  return {
    path: FINANCE_HOME_PATH,
    title: '财务管理',
    closable: false,
    pinned: true,
    kind: 'home'
  }
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
    activePath: '',
    cacheVersionByPath: {} as Record<string, number>
  }),
  getters: {
    isFinancePath: () => (path: string) => path === '/finance' || path.startsWith('/finance/'),
    cachedPaths: (state) => state.tabs.map((item) => item.path)
  },
  actions: {
    syncRoute(route: RouteLocationNormalizedLoaded) {
      if (!this.isFinancePath(route.path)) {
        return
      }

      this.ensureHomeTab()
      const path = route.fullPath
      this.ensureCacheVersion(path)
      if (path === '/finance' || path === FINANCE_HOME_PATH) {
        this.activePath = FINANCE_HOME_PATH
        return
      }
      const existing = this.tabs.find((item) => item.path === path)
      if (existing) {
        existing.title = resolveTabTitle(route)
      } else {
        this.tabs.push({
          path,
          title: resolveTabTitle(route),
          closable: true,
          pinned: false,
          kind: 'page'
        })
      }

      this.ensureHomeTabFirst()
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
      const entry = ensureGuardEntry(targetPath)
      if (!entry) {
        return
      }
      entry.close = guard
    },
    unregisterCloseGuard(path: string) {
      const targetPath = String(path || '')
      if (!targetPath) {
        return
      }
      const entry = guardRegistry.get(targetPath)
      if (!entry) {
        return
      }
      delete entry.close
      cleanupGuardEntry(targetPath)
    },
    async requestClose(path: string) {
      const targetPath = String(path || '')
      if (!targetPath) {
        return true
      }
      const guard = guardRegistry.get(targetPath)?.close
      if (!guard) {
        return true
      }
      return await guard()
    },
    registerPeriodSwitchGuard(path: string, guard: FinanceWorkspaceActionGuard) {
      const targetPath = String(path || '')
      if (!targetPath) {
        return
      }
      const entry = ensureGuardEntry(targetPath)
      if (!entry) {
        return
      }
      entry.periodSwitch = guard
    },
    unregisterPeriodSwitchGuard(path: string) {
      const targetPath = String(path || '')
      if (!targetPath) {
        return
      }
      const entry = guardRegistry.get(targetPath)
      if (!entry) {
        return
      }
      delete entry.periodSwitch
      cleanupGuardEntry(targetPath)
    },
    async requestPeriodSwitch() {
      for (const tab of this.tabs) {
        if (!tab.closable) {
          continue
        }
        const guard = guardRegistry.get(tab.path)?.periodSwitch
        if (!guard) {
          continue
        }
        const allowed = await guard()
        if (allowed === false) {
          return false
        }
      }
      return true
    },
    registerCreateVoucherTakeoverGuard(path: string, guard: FinanceWorkspaceActionGuard) {
      const targetPath = String(path || '')
      if (!targetPath) {
        return
      }
      const entry = ensureGuardEntry(targetPath)
      if (!entry) {
        return
      }
      entry.createVoucherTakeover = guard
    },
    unregisterCreateVoucherTakeoverGuard(path: string) {
      const targetPath = String(path || '')
      if (!targetPath) {
        return
      }
      const entry = guardRegistry.get(targetPath)
      if (!entry) {
        return
      }
      delete entry.createVoucherTakeover
      cleanupGuardEntry(targetPath)
    },
    async requestCreateVoucherTakeover() {
      for (const tab of this.tabs) {
        const guard = guardRegistry.get(tab.path)?.createVoucherTakeover
        if (!guard) {
          continue
        }
        const allowed = await guard()
        if (allowed === false) {
          return false
        }
      }
      return true
    },
    close(path: string) {
      if (this.isPinnedPath(path)) {
        return
      }
      this.invalidateCache(path)
      guardRegistry.delete(path)
      this.tabs = this.tabs.filter((item) => item.path !== path)
      if (this.activePath === path) {
        this.activePath = this.getLastAvailablePath()
      }
      this.ensureHomeTabFirst()
    },
    replaceTabPath(oldPath: string, nextPath: string, title?: string) {
      const sourcePath = String(oldPath || '')
      const targetPath = String(nextPath || '')
      if (!sourcePath || !targetPath || sourcePath === targetPath || this.isPinnedPath(sourcePath)) {
        return
      }

      const sourceIndex = this.tabs.findIndex((item) => item.path === sourcePath)
      if (sourceIndex === -1) {
        return
      }

      const existingTargetIndex = this.tabs.findIndex((item) => item.path === targetPath)
      this.ensureCacheVersion(targetPath)
      this.invalidateCache(sourcePath)
      guardRegistry.delete(sourcePath)

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
        this.ensureHomeTabFirst()
        return
      }

      const sourceTab = this.tabs[sourceIndex]
      if (!sourceTab) {
        return
      }
      this.tabs[sourceIndex] = {
        path: targetPath,
        title: title || sourceTab.title || targetPath,
        closable: sourceTab.closable,
        pinned: sourceTab.pinned,
        kind: sourceTab.kind
      }
      if (this.activePath === sourcePath) {
        this.activePath = targetPath
      }
      this.ensureHomeTabFirst()
    },
    closeOthers(path: string) {
      this.tabs
        .filter((item) => item.path !== path && item.closable)
        .forEach((item) => guardRegistry.delete(item.path))
      this.tabs = this.tabs.filter((item) => item.path === path || !item.closable)
      this.ensureHomeTabFirst()
      this.activePath = path
    },
    closeToRight(path: string) {
      const index = this.tabs.findIndex((item) => item.path === path)
      if (index === -1) {
        return
      }
      this.tabs
        .slice(index + 1)
        .filter((item) => item.closable)
        .forEach((item) => guardRegistry.delete(item.path))
      this.tabs = this.tabs.filter((item, itemIndex) => itemIndex <= index || !item.closable)
      this.ensureHomeTabFirst()
      if (!this.tabs.some((item) => item.path === this.activePath)) {
        this.activePath = path
      }
    },
    resetToHome() {
      this.ensureHomeTab()
      this.tabs
        .filter((item) => item.closable)
        .forEach((item) => {
          this.invalidateCache(item.path)
          guardRegistry.delete(item.path)
        })
      this.tabs = this.tabs.filter((item) => !item.closable)
      this.ensureHomeTabFirst()
      this.activePath = FINANCE_HOME_PATH
    },
    getNextPathAfterClose(path: string) {
      const index = this.tabs.findIndex((item) => item.path === path)
      if (index === -1) {
        return this.getLastAvailablePath()
      }

      return this.tabs[index + 1]?.path || this.tabs[index - 1]?.path || FINANCE_HOME_PATH
    },
    ensureHomeTab() {
      const existing = this.tabs.find((item) => item.path === FINANCE_HOME_PATH)
      if (!existing) {
        this.tabs.unshift(createFinanceHomeTab())
        return
      }
      existing.title = '财务管理'
      existing.closable = false
      existing.pinned = true
      existing.kind = 'home'
      this.ensureHomeTabFirst()
    },
    ensureHomeTabFirst() {
      const homeIndex = this.tabs.findIndex((item) => item.path === FINANCE_HOME_PATH)
      if (homeIndex <= 0) {
        return
      }
      const [homeTab] = this.tabs.splice(homeIndex, 1)
      if (!homeTab) {
        return
      }
      this.tabs.unshift(homeTab)
    },
    isPinnedPath(path: string) {
      return path === FINANCE_HOME_PATH || this.tabs.some((item) => item.path === path && item.pinned)
    },
    ensureCacheVersion(path: string) {
      const targetPath = String(path || '')
      if (!targetPath || Object.prototype.hasOwnProperty.call(this.cacheVersionByPath, targetPath)) {
        return
      }
      this.cacheVersionByPath = {
        ...this.cacheVersionByPath,
        [targetPath]: 0
      }
    },
    invalidateCache(path: string) {
      const targetPath = String(path || '')
      if (!targetPath) {
        return
      }
      const currentVersion = this.cacheVersionByPath[targetPath] ?? 0
      this.cacheVersionByPath = {
        ...this.cacheVersionByPath,
        [targetPath]: currentVersion + 1
      }
    },
    resolveViewCacheKey(path: string) {
      const targetPath = String(path || '')
      return `${targetPath}::${this.cacheVersionByPath[targetPath] ?? 0}`
    },
    getLastAvailablePath() {
      return this.tabs[this.tabs.length - 1]?.path || FINANCE_HOME_PATH
    }
  }
})
