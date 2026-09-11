import { beforeEach, describe, expect, it, vi } from 'vitest'

const routerMocks = vi.hoisted(() => ({
  financeCompany: {
    ensureInitialized: vi.fn(),
    currentCompanyHasActiveAccountSet: true,
    isCurrentModuleEnabled: vi.fn(() => true)
  }
}))

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
    companyId: 'COMPANY_A',
    permissionCodes: ['expense:process_management:view', 'expense:voucher_generation:view']
  })),
  resolveFirstAccessiblePath: vi.fn(() => '/dashboard')
}))

vi.mock('@/stores/financeCompany', () => ({
  useFinanceCompanyStore: () => routerMocks.financeCompany
}))

import router from '@/router'
import ArchiveAgentView from '@/views/archives/ArchiveAgentView.vue'
import ExpenseDocumentBatchPrintView from '@/views/expense/ExpenseDocumentBatchPrintView.vue'
import ExpensePaymentOrdersView from '@/views/expense/ExpensePaymentOrdersView.vue'
import FinanceOpeningBalanceView from '@/views/finance/FinanceOpeningBalanceView.vue'
import FinanceCloseLedgerView from '@/views/finance/FinanceCloseLedgerView.vue'
import FinanceHomeView from '@/views/finance/FinanceHomeView.vue'
import FinanceLedgerReportView from '@/views/finance/FinanceLedgerReportView.vue'
import FinancePeriodTransferView from '@/views/finance/FinancePeriodTransferView.vue'
import FinancePostVoucherView from '@/views/finance/FinancePostVoucherView.vue'
import FinanceSystemEnableView from '@/views/finance/FinanceSystemEnableView.vue'
import FinanceSystemManagementView from '@/views/finance/FinanceSystemManagementView.vue'
import ProcessManagementView from '@/views/process/ProcessManagementView.vue'
import ExpenseVoucherGenerationView from '@/views/expense/ExpenseVoucherGenerationView.vue'
import { ElMessage } from 'element-plus'

describe('router process management routes', () => {
  beforeEach(async () => {
    vi.clearAllMocks()
    localStorage.clear()
    localStorage.setItem('token', 'test-token')
    routerMocks.financeCompany.ensureInitialized.mockResolvedValue(undefined)
    routerMocks.financeCompany.isCurrentModuleEnabled.mockReturnValue(true)
    routerMocks.financeCompany.currentCompanyHasActiveAccountSet = true
    await router.replace('/dashboard').catch(() => undefined)
  })

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

  it('maps batch print route to ExpenseDocumentBatchPrintView', async () => {
    const route = router.getRoutes().find((item) => item.name === 'expense-document-batch-print')

    expect(route).toBeTruthy()
    expect(route?.meta.permissionCodes).toEqual([
      'expense:list:view',
      'expense:approval:view',
      'expense:documents:view',
      'expense:payment:payment_order:view'
    ])

    const loader = route?.components?.default as (() => Promise<{ default: unknown }>) | undefined
    expect(loader).toBeTypeOf('function')

    const module = await loader!()
    expect(module.default).toBe(ExpenseDocumentBatchPrintView)
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

  it('registers opening balance, post voucher, close ledger, period transfer and ledger reports as real views', async () => {
    const openingRoute = router.getRoutes().find((item) => item.name === 'finance-opening-balance')
    const postRoute = router.getRoutes().find((item) => item.name === 'finance-post-voucher')
    const closeRoute = router.getRoutes().find((item) => item.name === 'finance-close-ledger')
    const periodTransferRoute = router.getRoutes().find((item) => item.name === 'finance-period-transfer')
    const balanceRoute = router.getRoutes().find((item) => item.name === 'finance-ledger-balance-sheet')
    const sequenceRoute = router.getRoutes().find((item) => item.name === 'finance-sequence-ledger')

    expect(openingRoute?.path).toBe('/finance/general-ledger/opening-balance')
    expect(postRoute?.path).toBe('/finance/general-ledger/post-voucher')
    expect(closeRoute?.path).toBe('/finance/general-ledger/close-ledger')
    expect(periodTransferRoute?.path).toBe('/finance/general-ledger/period-transfer')
    expect(balanceRoute?.path).toBe('/finance/general-ledger/balance-sheet')
    expect(sequenceRoute?.path).toBe('/finance/general-ledger/sequence-ledger')

    const openingModule = await (openingRoute?.components?.default as (() => Promise<{ default: unknown }>))()
    const postModule = await (postRoute?.components?.default as (() => Promise<{ default: unknown }>))()
    const closeModule = await (closeRoute?.components?.default as (() => Promise<{ default: unknown }>))()
    const periodTransferModule = await (periodTransferRoute?.components?.default as (() => Promise<{ default: unknown }>))()
    const balanceModule = await (balanceRoute?.components?.default as (() => Promise<{ default: unknown }>))()
    const sequenceModule = await (sequenceRoute?.components?.default as (() => Promise<{ default: unknown }>))()

    expect(openingModule.default).toBe(FinanceOpeningBalanceView)
    expect(postModule.default).toBe(FinancePostVoucherView)
    expect(closeModule.default).toBe(FinanceCloseLedgerView)
    expect(periodTransferModule.default).toBe(FinancePeriodTransferView)
    expect(balanceModule.default).toBe(FinanceLedgerReportView)
    expect(sequenceModule.default).toBe(FinanceLedgerReportView)
  })

  it('registers the finance home route and keeps /finance as a redirect to it', async () => {
    const financeRootRoute = router.getRoutes().find((item) => item.path === '/finance')
    const financeHomeRoute = router.getRoutes().find((item) => item.name === 'finance-home')

    expect(financeRootRoute?.redirect).toBe('/finance/home')
    expect(financeHomeRoute?.path).toBe('/finance/home')

    const financeHomeModule = await (financeHomeRoute?.components?.default as (() => Promise<{ default: unknown }>))()
    expect(financeHomeModule.default).toBe(FinanceHomeView)
  })

  it('keeps the legacy finance system path as a redirect to the new account set child route', () => {
    const route = router.getRoutes().find((item) => item.name === 'finance-system-management')

    expect(route?.path).toBe('/finance/system-management')
    expect(route?.redirect).toBe('/finance/system-management/new-account-set')
    expect(route?.meta.permissionCode).toBe('finance:system_management:view')
  })

  it('maps the new finance system child routes to their intended views', async () => {
    const newAccountSetRoute = router.getRoutes().find((item) => item.name === 'finance-system-management-new-account-set')
    const systemEnableRoute = router.getRoutes().find((item) => item.name === 'finance-system-management-system-enable')

    expect(newAccountSetRoute?.path).toBe('/finance/system-management/new-account-set')
    expect(systemEnableRoute?.path).toBe('/finance/system-management/system-enable')

    const newAccountSetModule = await (newAccountSetRoute?.components?.default as (() => Promise<{ default: unknown }>))()
    const systemEnableModule = await (systemEnableRoute?.components?.default as (() => Promise<{ default: unknown }>))()

    expect(newAccountSetModule.default).toBe(FinanceSystemManagementView)
    expect(systemEnableModule.default).toBe(FinanceSystemEnableView)
  })

  it('blocks finance navigation when the current company has an account set but the module is disabled', async () => {
    routerMocks.financeCompany.isCurrentModuleEnabled.mockReturnValue(false)

    await router.push('/finance/fixed-assets').catch(() => undefined)

    expect(router.currentRoute.value.fullPath).toBe('/dashboard')
    expect(routerMocks.financeCompany.ensureInitialized).toHaveBeenCalledWith('COMPANY_A')
    expect(ElMessage.warning).toHaveBeenCalledWith('系统未启用')
  })
})
