import { defineComponent, nextTick, reactive } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import FinancePostVoucherView from '@/views/finance/FinancePostVoucherView.vue'

const financeCompanyStore = reactive({
  currentCompanyId: 'COMP-001',
  currentCompanyName: '广州分公司'
})

const financePeriodStore = reactive({
  currentYear: 2026,
  currentPeriod: 4,
  currentYearPeriod: 202604,
  currentMonthText: '2026-04',
  hasPeriodContext: true
})

const mocks = vi.hoisted(() => ({
  postVoucherApi: {
    getMeta: vi.fn(),
    runPosting: vi.fn(),
    getTaskStatus: vi.fn()
  },
  message: {
    success: vi.fn(),
    warning: vi.fn(),
    error: vi.fn()
  }
}))

vi.mock('@/api', () => ({
  postVoucherApi: mocks.postVoucherApi
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
  const wrapper = mount(FinancePostVoucherView, {
    global: {
      stubs: {
        'el-card': defineComponent({ template: '<div><slot /></div>' }),
        'el-button': ButtonStub,
        'el-progress': defineComponent({
          props: { percentage: { type: Number, default: 0 } },
          template: '<div class="progress-stub">{{ percentage }}</div>'
        })
      }
    }
  })
  await flushPromises()
  await nextTick()
  return wrapper
}

function buildMeta(overrides: Record<string, unknown> = {}) {
  return {
    companyId: 'COMP-001',
    companyName: '广州分公司',
    iyear: 2026,
    iperiod: 4,
    iyperiod: 202604,
    periodLabel: '2026-04',
    status: 'NOT_POSTED',
    statusLabel: '未记账',
    canPost: true,
    blockedReason: '',
    unpostedVoucherCount: 0,
    unpostedSampleVoucherNos: [],
    errorVoucherCount: 0,
    errorSampleVoucherNos: [],
    reviewableVoucherCount: 3,
    postedVoucherCount: 1,
    lastTaskNo: '',
    lastTaskStatus: '',
    lastTaskMessage: '',
    ...overrides
  }
}

describe('FinancePostVoucherView', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.clearAllMocks()
    mocks.postVoucherApi.getMeta.mockResolvedValue({ data: buildMeta() })
    mocks.postVoucherApi.runPosting.mockResolvedValue({
      data: {
        taskNo: 'FPV202604270001',
        taskType: 'finance_post_voucher_run',
        businessType: 'finance_post_voucher',
        status: 'PENDING',
        message: '记账任务已提交'
      }
    })
    mocks.postVoucherApi.getTaskStatus.mockResolvedValue({
      data: {
        taskNo: 'FPV202604270001',
        taskType: 'finance_post_voucher_run',
        businessType: 'finance_post_voucher',
        status: 'SUCCESS',
        progress: 100,
        resultMessage: '记账完成',
        periodStatus: 'FULLY_POSTED',
        periodStatusLabel: '已全部记账',
        postedVoucherCount: 4,
        reviewableVoucherCount: 0,
        finished: true
      }
    })
  })

  it('loads posting meta with current company and global period by default', async () => {
    const wrapper = await mountView()

    expect(wrapper.text()).toContain('广州分公司')
    expect(wrapper.text()).toContain('2026-04')
    expect(mocks.postVoucherApi.getMeta).toHaveBeenCalledWith({
      companyId: 'COMP-001',
      iyear: 2026,
      iperiod: 4
    })
  })

  it('blocks posting when unposted vouchers exist and does not submit task', async () => {
    mocks.postVoucherApi.getMeta.mockResolvedValueOnce({
      data: buildMeta({
        canPost: false,
        blockedReason: '当前期间存在未审核凭证',
        unpostedVoucherCount: 2,
        unpostedSampleVoucherNos: ['记-0003', '记-0005']
      })
    })

    const wrapper = await mountView()
    const buttons = wrapper.findAll('button')
    await buttons[1]?.trigger('click')

    expect(mocks.message.warning).toHaveBeenCalledWith('当前期间存在 2 张未审核凭证，不能继续记账：记-0003、记-0005')
    expect(mocks.postVoucherApi.runPosting).not.toHaveBeenCalled()
  })

  it('blocks posting when error vouchers exist and does not submit task', async () => {
    mocks.postVoucherApi.getMeta.mockResolvedValueOnce({
      data: buildMeta({
        canPost: false,
        blockedReason: '当前期间存在错误凭证',
        errorVoucherCount: 1,
        errorSampleVoucherNos: ['记-0012']
      })
    })

    const wrapper = await mountView()
    const buttons = wrapper.findAll('button')
    await buttons[1]?.trigger('click')

    expect(mocks.message.warning).toHaveBeenCalledWith('当前期间存在 1 张错误凭证，不能继续记账：记-0012')
    expect(mocks.postVoucherApi.runPosting).not.toHaveBeenCalled()
  })

  it('submits posting task and refreshes progress by polling task status', async () => {
    const wrapper = await mountView()
    const buttons = wrapper.findAll('button')

    await buttons[1]?.trigger('click')
    await flushPromises()

    expect(mocks.postVoucherApi.runPosting).toHaveBeenCalledWith({
      companyId: 'COMP-001',
      iyear: 2026,
      iperiod: 4
    })
    expect(mocks.postVoucherApi.getTaskStatus).toHaveBeenCalledWith('FPV202604270001')
    expect(wrapper.text()).toContain('记账进度')
    expect(wrapper.text()).toContain('记账完成')
    expect(mocks.message.success).toHaveBeenCalledWith('记账完成')
  })
})
