import api, {
  expenseApi,
  expenseApprovalApi,
  expenseCreateApi,
  expensePaymentApi,
  processApi,
  request,
  systemSettingsApi
} from '@/api'
import { describe, expect, it } from 'vitest'

describe('@/api barrel compatibility', () => {
  it('re-exports the legacy request entrypoints', () => {
    expect(api).toBe(request)
    expect(typeof request).toBe('function')
  })

  it('keeps expense and core api groups available from @/api', () => {
    expect(expenseApi).toBeDefined()
    expect(expenseCreateApi).toBeDefined()
    expect(expenseApprovalApi).toBeDefined()
    expect(expensePaymentApi).toBeDefined()
    expect(processApi).toBeDefined()
    expect(systemSettingsApi).toBeDefined()

    expect(typeof expenseApi.list).toBe('function')
    expect(typeof expenseCreateApi.listTemplates).toBe('function')
    expect(typeof expenseApprovalApi.listPending).toBe('function')
    expect(typeof expensePaymentApi.listOrders).toBe('function')
    expect(typeof processApi.getOverview).toBe('function')
    expect(typeof systemSettingsApi.getBootstrap).toBe('function')
  })
})
