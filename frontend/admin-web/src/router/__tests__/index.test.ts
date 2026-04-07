import { describe, expect, it, vi } from 'vitest'

vi.mock('element-plus', () => ({
  ElMessage: {
    warning: vi.fn()
  }
}))

vi.mock('@/api', () => ({
  authApi: {
    getCurrentUser: vi.fn()
  },
  archiveAgentApi: {
    getMeta: vi.fn(),
    list: vi.fn(),
    getDetail: vi.fn(),
    listRuns: vi.fn(),
    getRunDetail: vi.fn()
  }
}))

vi.mock('@/utils/permissions', () => ({
  EXPENSE_CREATE_ENTRY_PERMISSION_CODES: ['expense:create:create', 'expense:create:submit'],
  hasAnyPermission: vi.fn(() => true),
  readStoredUser: vi.fn(() => ({
    permissionCodes: ['expense:process_management:view', 'expense:voucher_generation:view']
  })),
  resolveFirstAccessiblePath: vi.fn(() => '/dashboard')
}))

import router from '@/router'
import ArchiveAgentView from '@/views/archives/ArchiveAgentView.vue'
import ExpensePaymentOrdersView from '@/views/expense/ExpensePaymentOrdersView.vue'
import ProcessManagementView from '@/views/process/ProcessManagementView.vue'
import ExpenseVoucherGenerationView from '@/views/expense/ExpenseVoucherGenerationView.vue'

describe('router process management routes', () => {
  it('maps process management route back to ProcessManagementView', async () => {
    const route = router.getRoutes().find((item) => item.name === 'expense-workbench-process-management')

    expect(route).toBeTruthy()
    expect(route?.meta.permissionCode).toBe('expense:process_management:view')

    const loader = route?.components?.default as (() => Promise<{ default: unknown }>) | undefined
    expect(loader).toBeTypeOf('function')

    const module = await loader!()
    expect(module.default).toBe(ProcessManagementView)
  })

  it('keeps voucher generation route on ExpenseVoucherGenerationView', async () => {
    const route = router.getRoutes().find((item) => item.name === 'expense-voucher-generation')

    expect(route).toBeTruthy()
    expect(route?.meta.permissionCode).toBe('expense:voucher_generation:view')

    const loader = route?.components?.default as (() => Promise<{ default: unknown }>) | undefined
    expect(loader).toBeTypeOf('function')

    const module = await loader!()
    expect(module.default).toBe(ExpenseVoucherGenerationView)
  })

  it('maps payment orders route to ExpensePaymentOrdersView', async () => {
    const route = router.getRoutes().find((item) => item.name === 'expense-payment-orders')

    expect(route).toBeTruthy()
    expect(route?.meta.permissionCode).toBe('expense:payment:payment_order:view')

    const loader = route?.components?.default as (() => Promise<{ default: unknown }>) | undefined
    expect(loader).toBeTypeOf('function')

    const module = await loader!()
    expect(module.default).toBe(ExpensePaymentOrdersView)
  })

  it('maps archive agent route to ArchiveAgentView', async () => {
    const route = router.getRoutes().find((item) => item.name === 'archives-agents')

    expect(route).toBeTruthy()
    expect(route?.meta.permissionCode).toBe('agents:view')

    const loader = route?.components?.default as (() => Promise<{ default: unknown }>) | undefined
    expect(loader).toBeTypeOf('function')

    const module = await loader!()
    expect(module.default).toBe(ArchiveAgentView)
  })
})
