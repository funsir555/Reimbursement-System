import { describe, expect, it } from 'vitest'
import {
  buildFinanceNewVoucherToolbarGroups,
  type FinanceNewVoucherToolbarState
} from '../composables/buildFinanceNewVoucherToolbarGroups'

function state(overrides: Partial<FinanceNewVoucherToolbarState> = {}): FinanceNewVoucherToolbarState {
  return {
    reviewMode: false,
    detailRoute: false,
    editingExisting: false,
    createLockedLast: false,
    createEditingNew: false,
    detailEditable: undefined,
    canReviewVoucher: false,
    canUnreviewVoucher: false,
    canMarkVoucherError: false,
    canEditExisting: false,
    isVoidedVoucher: false,
    currentVoucherNo: '',
    readonlyMode: false,
    currentPeriodClosed: false,
    voucherStatus: undefined,
    ...overrides
  }
}

function actionKeys(input: FinanceNewVoucherToolbarState) {
  return buildFinanceNewVoucherToolbarGroups(input).flatMap((group) => group.actions.map((action) => action.key))
}

describe('new voucher toolbar groups', () => {
  it('keeps create shortcuts and only exposes clear while editing a new voucher', () => {
    const groups = buildFinanceNewVoucherToolbarGroups(state({ createEditingNew: true }))
    expect(groups.map((group) => group.key)).toEqual(['primary', 'edit', 'actions', 'tools'])
    expect(groups.flatMap((group) => group.actions).find((action) => action.key === 'save')?.shortcut).toBe('F6')
    expect(actionKeys(state({ createEditingNew: true }))).toContain('clear')
    expect(actionKeys(state())).not.toContain('clear')
  })

  it('preserves review permissions, error label and voided action choice', () => {
    const groups = buildFinanceNewVoucherToolbarGroups(state({
      reviewMode: true,
      isVoidedVoucher: true,
      voucherStatus: 'ERROR',
      currentVoucherNo: 'V-001'
    }))
    const actions = groups.flatMap((group) => group.actions)
    expect(groups.map((group) => group.key)).toEqual(['review-primary', 'review-actions'])
    expect(actions.find((action) => action.key === 'review')?.disabled).toBe(true)
    expect(actions.find((action) => action.key === 'markError')?.label).toBe('取消错误')
    expect(actions.find((action) => action.key === 'restore')?.disabled).toBe(true)
    expect(actions.map((action) => action.key)).not.toContain('void')
  })

  it('preserves modify and row-edit conditions for locked or closed vouchers', () => {
    const readOnly = state({ detailRoute: true, detailEditable: true, canEditExisting: true,
      currentVoucherNo: 'V-001', readonlyMode: true })
    expect(actionKeys(readOnly)).toContain('modify')
    expect(actionKeys(readOnly)).not.toContain('save')
    expect(actionKeys(readOnly)).not.toContain('insert')
    expect(actionKeys({ ...readOnly, currentPeriodClosed: true })).toEqual(
      expect.arrayContaining(['save', 'insert', 'delete'])
    )
  })
})
