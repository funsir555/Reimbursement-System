import { defineComponent, nextTick, reactive } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import FinanceCloseLedgerView from '@/views/finance/FinanceCloseLedgerView.vue'

const financeCompanyStore = reactive({
  currentCompanyId: 'COMP-001',
  currentCompanyName: '广州分公司',
  refreshContext: vi.fn()
})

const financePeriodStore = reactive({
  currentYear: 2026,
  currentPeriod: 4,
  currentYearPeriod: 202604,
  currentMonthText: '2026-04',
  hasPeriodContext: true,
  syncWithCompany: vi.fn()
})

const mocks = vi.hoisted(() => ({
  closeLedgerApi: {
    getMeta: vi.fn(),
    reconcile: vi.fn(),
    validate: vi.fn(),
    close: vi.fn()
  },
  message: {
    success: vi.fn(),
    warning: vi.fn(),
    error: vi.fn()
  }
}))

vi.mock('@/api', () => ({
  closeLedgerApi: mocks.closeLedgerApi
}))

vi.mock('@/stores/financeCompany', () => ({
  useFinanceCompanyStore: () => financeCompanyStore
}))

vi.mock('@/stores/financePeriod', () => ({
  useFinancePeriodStore: () => financePeriodStore
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: mocks.message
  }
})

const ButtonStub = defineComponent({
  props: {
    disabled: { type: Boolean, default: false }
  },
  emits: ['click'],
  template: '<button type="button" :disabled="disabled" @click="$emit(\'click\', $event)"><slot /></button>'
})

async function mountView() {
  const wrapper = mount(FinanceCloseLedgerView, {
    global: {
      stubs: {
        'el-card': defineComponent({ template: '<div><slot /></div>' }),
        'el-button': ButtonStub,
        'el-input': defineComponent({
          props: {
            modelValue: { type: String, default: '' }
          },
          emits: ['update:modelValue'],
          template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
        })
      }
    }
  })
  await flushPromises()
  await nextTick()
  return wrapper
}

async function clickButton(wrapper: Awaited<ReturnType<typeof mountView>>, label: string) {
  const buttons = wrapper.findAll('button')
  const target = buttons.find((item) => item.text().trim() === label) || buttons.find((item) => item.text().includes(label))
  expect(target, `button ${label} should exist`).toBeTruthy()
  await target!.trigger('click')
  await flushPromises()
}

function buildMeta(overrides: Record<string, unknown> = {}) {
  return {
    companyId: 'COMP-001',
    companyName: '广州分公司',
    iyear: 2026,
    iperiod: 4,
    iyperiod: 202604,
    periodLabel: '2026-04',
    status: 'OPEN',
    statusLabel: '未结账',
    postStatus: 'FULLY_POSTED',
    postStatusLabel: '已全部记账',
    unpostedVoucherCount: 0,
    reviewedVoucherCount: 0,
    errorVoucherCount: 0,
    postedVoucherCount: 8,
    fixedAssetClosed: true,
    fixedAssetStatusLabel: '固定资产已完成期间结账',
    ...overrides
  }
}

describe('FinanceCloseLedgerView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    financeCompanyStore.refreshContext.mockResolvedValue(undefined)
    financePeriodStore.syncWithCompany.mockReturnValue(true)

    mocks.closeLedgerApi.getMeta.mockResolvedValue({ data: buildMeta() })
    mocks.closeLedgerApi.reconcile.mockResolvedValue({
      data: {
        passed: true,
        summaryMessage: '总账对账通过',
        differenceSubjectCount: 0,
        differenceAssistCount: 0,
        missingAssistCount: 0,
        illegalAssistCount: 0,
        differenceSubjects: [],
        differenceAssistKeys: [],
        missingAssistSubjects: [],
        illegalAssistMessages: []
      }
    })
    mocks.closeLedgerApi.validate.mockResolvedValue({
      data: {
        passed: true,
        generalPassed: true,
        externalPassed: true,
        alreadyClosed: false,
        reconcilePassed: true,
        postStatus: 'FULLY_POSTED',
        postStatusLabel: '已全部记账',
        blockingReasons: [],
        generalChecks: [
          { code: 'no_reviewed', label: '没有未记账凭证才能结账', passed: true, message: '当前期间不存在已审核未记账凭证' }
        ],
        externalChecks: [
          { code: 'fixed_assets', label: '固定资产期间结账', passed: true, message: '固定资产已完成期间结账' }
        ]
      }
    })
    mocks.closeLedgerApi.close.mockResolvedValue({
      data: buildMeta({
        status: 'CLOSED',
        statusLabel: '已结账',
        closeNote: '月结完成'
      })
    })
  })

  it('loads close-ledger meta with current company and global period', async () => {
    const wrapper = await mountView()

    expect(wrapper.text()).toContain('广州分公司')
    expect(wrapper.text()).toContain('2026-04')
    expect(mocks.closeLedgerApi.getMeta).toHaveBeenCalledWith({
      companyId: 'COMP-001',
      iyear: 2026,
      iperiod: 4
    })
  })

  it('blocks moving forward when reconcile fails', async () => {
    mocks.closeLedgerApi.reconcile.mockResolvedValueOnce({
      data: {
        passed: false,
        summaryMessage: '总账对账未通过',
        differenceSubjectCount: 1,
        differenceAssistCount: 0,
        missingAssistCount: 0,
        illegalAssistCount: 0,
        differenceSubjects: ['1001/CNY'],
        differenceAssistKeys: [],
        missingAssistSubjects: [],
        illegalAssistMessages: []
      }
    })
    const wrapper = await mountView()

    await clickButton(wrapper, '下一步')
    await clickButton(wrapper, '下一步')

    expect(wrapper.text()).toContain('总账对账未通过')
    expect(mocks.message.warning).toHaveBeenCalled()
    expect(mocks.closeLedgerApi.validate).not.toHaveBeenCalled()
  })

  it('submits close after reconcile and validation pass, then refreshes company and period context', async () => {
    const wrapper = await mountView()

    await clickButton(wrapper, '下一步')
    await clickButton(wrapper, '下一步')
    await clickButton(wrapper, '下一步')
    await clickButton(wrapper, '下一步')
    await clickButton(wrapper, '结账')

    expect(mocks.closeLedgerApi.reconcile).toHaveBeenCalledWith({
      companyId: 'COMP-001',
      iyear: 2026,
      iperiod: 4
    })
    expect(mocks.closeLedgerApi.validate).toHaveBeenCalledWith({
      companyId: 'COMP-001',
      iyear: 2026,
      iperiod: 4
    })
    expect(mocks.closeLedgerApi.close).toHaveBeenCalledWith({
      companyId: 'COMP-001',
      iyear: 2026,
      iperiod: 4,
      closeNote: undefined
    })
    expect(financeCompanyStore.refreshContext).toHaveBeenCalledWith('COMP-001')
    expect(financePeriodStore.syncWithCompany).toHaveBeenCalledWith('COMP-001', true)
    expect(mocks.message.success).toHaveBeenCalledWith('结账成功')
  })

  it('shows empty-period messaging and allows validation without posted state', async () => {
    mocks.closeLedgerApi.getMeta.mockResolvedValueOnce({
      data: buildMeta({
        postStatus: 'NOT_POSTED',
        postStatusLabel: '未记账',
        unpostedVoucherCount: 0,
        reviewedVoucherCount: 0,
        errorVoucherCount: 0,
        postedVoucherCount: 0
      })
    })
    mocks.closeLedgerApi.validate.mockResolvedValueOnce({
      data: {
        passed: true,
        generalPassed: true,
        externalPassed: true,
        alreadyClosed: false,
        reconcilePassed: true,
        postStatus: 'NOT_POSTED',
        postStatusLabel: '未记账',
        blockingReasons: [],
        generalChecks: [
          { code: 'no_reviewed', label: '没有未记账凭证才能结账', passed: true, message: '当前期间无凭证，视为已满足记账前置条件' }
        ],
        externalChecks: [
          { code: 'fixed_assets', label: '固定资产期间结账', passed: true, message: '固定资产已完成期间结账' }
        ]
      }
    })

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('本期无凭证')
    await clickButton(wrapper, '下一步')
    await clickButton(wrapper, '下一步')
    await clickButton(wrapper, '下一步')

    expect(mocks.message.success).toHaveBeenCalledWith('本期无凭证，将按零发生额结转到下一期间')
  })
})
