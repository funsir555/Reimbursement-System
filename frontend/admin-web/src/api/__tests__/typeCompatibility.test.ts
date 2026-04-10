import type {
  ExpenseDocumentDetail,
  ExpenseDocumentSubmitPayload,
  ExpensePaymentOrder,
  ExpenseSummary,
  FinanceVoucherDetail,
  FixedAssetCard,
  PageResult,
  ProcessFlowDetail,
  UserProfile
} from '@/api'
import { describe, expect, it } from 'vitest'

describe('@/api type compatibility', () => {
  it('keeps representative domain types resolvable from @/api', () => {
    const expensePage: PageResult<ExpenseSummary> = {
      total: 1,
      page: 1,
      pageSize: 20,
      items: []
    }

    const submitPayload: ExpenseDocumentSubmitPayload = {
      templateCode: 'TPL-001',
      formData: {},
      expenseDetails: []
    }

    const summary: ExpenseDocumentDetail['status'] = 'DRAFT'
    const voucherNo: FinanceVoucherDetail['voucherNo'] = '记-001'
    const flowName: ProcessFlowDetail['flowName'] = 'Flow'
    const assetName: FixedAssetCard['assetName'] = 'Asset'
    const paymentOrderCode: ExpensePaymentOrder['documentCode'] = 'PAY-001'
    const userName: UserProfile['name'] = 'Tester'

    expect(expensePage.items).toHaveLength(0)
    expect(submitPayload.templateCode).toBe('TPL-001')
    expect(summary).toBe('DRAFT')
    expect(voucherNo).toBe('记-001')
    expect(flowName).toBe('Flow')
    expect(assetName).toBe('Asset')
    expect(paymentOrderCode).toBe('PAY-001')
    expect(userName).toBe('Tester')
  })
})
