import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useFinanceWorkspaceStore } from '@/stores/financeWorkspace'
import { FINANCE_HOME_PATH } from '@/utils/financeHomeModules'

function buildRoute(path: string, title = path) {
  return {
    path,
    fullPath: path,
    name: title,
    meta: {
      tabTitle: title
    }
  } as const
}

describe('financeWorkspace store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('injects the pinned finance home tab before other finance pages', () => {
    const store = useFinanceWorkspaceStore()

    store.syncRoute(buildRoute('/finance/general-ledger/new-voucher', '新建凭证') as never)

    expect(store.tabs.map((item) => item.path)).toEqual([
      FINANCE_HOME_PATH,
      '/finance/general-ledger/new-voucher'
    ])
    expect(store.tabs[0]).toMatchObject({
      title: '财务管理',
      closable: false,
      pinned: true,
      kind: 'home'
    })
    expect(store.tabs[1]).toMatchObject({
      title: '新建凭证',
      closable: true,
      pinned: false,
      kind: 'page'
    })
  })

  it('keeps the finance home tab when closing the last functional tab', () => {
    const store = useFinanceWorkspaceStore()

    store.syncRoute(buildRoute('/finance/general-ledger/new-voucher', '新建凭证') as never)

    expect(store.getNextPathAfterClose('/finance/general-ledger/new-voucher')).toBe(FINANCE_HOME_PATH)

    store.close('/finance/general-ledger/new-voucher')

    expect(store.tabs.map((item) => item.path)).toEqual([FINANCE_HOME_PATH])
    expect(store.activePath).toBe(FINANCE_HOME_PATH)
  })

  it('never removes the pinned finance home tab from closeOthers and closeToRight', () => {
    const store = useFinanceWorkspaceStore()

    store.syncRoute(buildRoute('/finance/general-ledger/new-voucher', '新建凭证') as never)
    store.syncRoute(buildRoute('/finance/general-ledger/query-voucher', '查询凭证') as never)
    store.syncRoute(buildRoute('/finance/system-management/new-account-set', '新建账套') as never)

    store.closeOthers('/finance/general-ledger/query-voucher')
    expect(store.tabs.map((item) => item.path)).toEqual([
      FINANCE_HOME_PATH,
      '/finance/general-ledger/query-voucher'
    ])

    store.syncRoute(buildRoute('/finance/general-ledger/new-voucher', '新建凭证') as never)
    store.syncRoute(buildRoute('/finance/system-management/new-account-set', '新建账套') as never)
    store.closeToRight('/finance/general-ledger/query-voucher')

    expect(store.tabs.map((item) => item.path)).toEqual([
      FINANCE_HOME_PATH,
      '/finance/general-ledger/query-voucher'
    ])
  })

  it('aggregates period-switch guards across closable finance tabs', async () => {
    const store = useFinanceWorkspaceStore()
    const guards = {
      create: false,
      detail: true
    }

    store.syncRoute(buildRoute('/finance/general-ledger/new-voucher', '新建凭证') as never)
    store.syncRoute(buildRoute('/finance/general-ledger/query-voucher/1', '凭证详情') as never)
    store.registerPeriodSwitchGuard('/finance/general-ledger/new-voucher', async () => guards.create)
    store.registerPeriodSwitchGuard('/finance/general-ledger/query-voucher/1', async () => guards.detail)

    await expect(store.requestPeriodSwitch()).resolves.toBe(false)

    guards.create = true
    await expect(store.requestPeriodSwitch()).resolves.toBe(true)
  })

  it('resets the workspace to the pinned finance home tab only', () => {
    const store = useFinanceWorkspaceStore()

    store.syncRoute(buildRoute('/finance/general-ledger/new-voucher', '新建凭证') as never)
    store.syncRoute(buildRoute('/finance/general-ledger/query-voucher', '查询凭证') as never)

    store.resetToHome()

    expect(store.tabs.map((item) => item.path)).toEqual([FINANCE_HOME_PATH])
    expect(store.activePath).toBe(FINANCE_HOME_PATH)
  })

  it('bumps the view cache key after closing a finance tab', () => {
    const store = useFinanceWorkspaceStore()

    store.syncRoute(buildRoute('/finance/general-ledger/new-voucher', '新建凭证') as never)
    const beforeCloseKey = store.resolveViewCacheKey('/finance/general-ledger/new-voucher')

    store.close('/finance/general-ledger/new-voucher')
    store.syncRoute(buildRoute('/finance/general-ledger/new-voucher', '新建凭证') as never)
    const reopenedKey = store.resolveViewCacheKey('/finance/general-ledger/new-voucher')

    expect(reopenedKey).not.toBe(beforeCloseKey)
  })
})
