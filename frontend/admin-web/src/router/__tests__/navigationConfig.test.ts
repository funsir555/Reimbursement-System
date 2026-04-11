import { describe, expect, it } from 'vitest'
import { DASHBOARD_RECENT_MODULE_REGISTRY, FALLBACK_NAVIGATION_TARGETS, MAIN_NAVIGATION_MENU, filterVisibleNavigationMenu } from '@/router/navigation-config'
import { getRoutePathByName } from '@/router/route-catalog'
import { getRouteMenuPermissionCodes, resolvePlaceholderDescription, resolvePlaceholderTitle, resolveRouteMeta, resolveRouteTabTitle } from '@/router/route-meta'

describe('navigation configuration', () => {
  it('keeps route meta and menu nodes aligned for payment orders', () => {
    const expenseGroup = MAIN_NAVIGATION_MENU.find((item) => item.index === '/expense')
    const paymentGroup = expenseGroup?.children?.find((item) => item.index === '/expense/payment')
    const paymentOrders = paymentGroup?.children?.find((item) => item.index === '/expense/payment/orders')
    const paymentMeta = resolveRouteMeta('expense-payment-orders')

    expect(paymentOrders?.title).toBe(paymentMeta.menuTitle)
    expect(paymentOrders?.permissionCodes).toEqual(getRouteMenuPermissionCodes(paymentMeta))
  })

  it('filters menu visibility and fallback order from the same permission source', () => {
    const visibleMenu = filterVisibleNavigationMenu(MAIN_NAVIGATION_MENU, ['finance:system_management:view'])
    const financeGroup = visibleMenu.find((item) => item.index === '/finance')

    expect(financeGroup).toBeTruthy()
    expect(fallbackMatch('settings:roles:view')).toBe('/settings?tab=organization')
    expect(fallbackMatch('settings:company_accounts:view')).toBe('/settings?tab=companyAccounts')
  })

  it('resolves finance tab titles and placeholder copy from route meta', () => {
    expect(resolveRouteTabTitle({ tabTitle: '财务系统管理', title: '财务系统管理' })).toBe('财务系统管理')
    expect(resolvePlaceholderTitle(resolveRouteMeta('finance-review-voucher'))).toBe('审核凭证')
    expect(resolvePlaceholderDescription(resolveRouteMeta('finance-review-voucher'))).toBe('审核总账凭证')
  })

  it('derives dashboard recent modules from shared navigation metadata', () => {
    const voucherGeneration = DASHBOARD_RECENT_MODULE_REGISTRY.find(
      (item) => item.path === getRoutePathByName('expense-voucher-generation')
    )

    expect(voucherGeneration?.label).toBe('凭证生成')
    expect(voucherGeneration?.permissionCodes).toEqual(getRouteMenuPermissionCodes(resolveRouteMeta('expense-voucher-generation')))
  })
})

function fallbackMatch(permissionCode: string): string | undefined {
  return FALLBACK_NAVIGATION_TARGETS.find((item) => item.permissionCodes.includes(permissionCode))?.path
}
