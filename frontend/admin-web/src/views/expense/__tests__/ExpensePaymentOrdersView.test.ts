import { flushPromises, mount } from '@vue/test-utils'
import { computed, defineComponent, h, inject, provide, reactive } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ExpensePaymentOrdersView from '@/views/expense/ExpensePaymentOrdersView.vue'

const mocks = vi.hoisted(() => ({
  route: {
    path: '/expense/payment/orders',
    query: { tab: 'pending' as string }
  },
  router: {
    push: vi.fn(),
    replace: vi.fn()
  },
  expensePaymentApi: {
    listOrders: vi.fn(),
    startTask: vi.fn(),
    completeTask: vi.fn(),
    markException: vi.fn()
  },
  elMessage: {
    success: vi.fn(),
    error: vi.fn()
  },
  elMessageBox: {
    confirm: vi.fn(),
    prompt: vi.fn()
  }
}))

mocks.route = reactive({
  path: '/expense/payment/orders',
  query: { tab: 'pending' }
})

vi.mock('vue-router', () => ({
  useRoute: () => mocks.route,
  useRouter: () => mocks.router
}))

vi.mock('@/api', () => ({
  expensePaymentApi: mocks.expensePaymentApi
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage,
  ElMessageBox: mocks.elMessageBox
}))

vi.mock('@/utils/money', () => ({
  formatMoney: (value: unknown) => Number(value || 0).toFixed(2)
}))

const tableRowsKey = Symbol('tableRows')

const SimpleContainer = defineComponent({
  template: '<div><slot name="header" /><slot /><slot name="footer" /></div>'
})

const ButtonStub = defineComponent({
  props: {
    disabled: {
      type: Boolean,
      default: false
    }
  },
  emits: ['click'],
  template: '<button type="button" :disabled="disabled" @click="$emit(\'click\', $event)"><slot /></button>'
})

const InputStub = defineComponent({
  props: {
    modelValue: {
      type: [String, Number],
      default: ''
    }
  },
  emits: ['update:modelValue'],
  template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
})

const TableStub = defineComponent({
  props: {
    data: {
      type: Array,
      default: () => []
    }
  },
  setup(props, { slots }) {
    provide(tableRowsKey, computed(() => props.data))
    return () => h('div', {}, slots.default ? slots.default() : [])
  }
})

const TableColumnStub = defineComponent({
  props: {
    prop: {
      type: String,
      default: ''
    }
  },
  setup(props, { slots }) {
    const rows = inject<any>(tableRowsKey)
    return () => h(
      'div',
      {},
      (rows?.value || []).map((row: Record<string, unknown>, index: number) => h(
        'div',
        { key: `${String(index)}-${props.prop}` },
        slots.default ? slots.default({ row }) : [h('span', {}, props.prop ? String(row[props.prop] ?? '') : '')]
      ))
    )
  }
})

function buildOrder(overrides: Record<string, unknown>) {
  return {
    taskId: 1,
    documentCode: 'DOC-PAY-001',
    documentTitle: '付款单-001',
    submitterName: '张三',
    paymentCompanyName: '华南公司',
    amount: 100.5,
    paymentStatusCode: 'PENDING_PAYMENT',
    paymentStatusLabel: '待支付',
    receiptStatusLabel: '待查询回单',
    companyBankAccountName: '华南公司基本户',
    bankFlowNo: '',
    paidAt: '',
    ...overrides
  }
}

async function mountView() {
  const wrapper = mount(ExpensePaymentOrdersView, {
    global: {
      stubs: {
        'el-card': SimpleContainer,
        'el-tag': SimpleContainer,
        'el-button': ButtonStub,
        'el-input': InputStub,
        'el-table': TableStub,
        'el-table-column': TableColumnStub,
        'el-pagination': SimpleContainer,
        'el-icon': SimpleContainer
      },
      directives: {
        loading: () => undefined
      }
    }
  })
  await flushPromises()
  return wrapper
}

describe('ExpensePaymentOrdersView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.route.path = '/expense/payment/orders'
    mocks.route.query.tab = 'pending'

    mocks.expensePaymentApi.listOrders.mockImplementation((status?: string) => {
      const map: Record<string, unknown[]> = {
        PENDING_PAYMENT: [buildOrder({ taskId: 1 })],
        PAYING: [buildOrder({ taskId: 2, documentCode: 'DOC-PAY-002', paymentStatusCode: 'PAYING', paymentStatusLabel: '支付中' })],
        PAYMENT_COMPLETED: [buildOrder({ taskId: 3, documentCode: 'DOC-PAY-003', paymentStatusCode: 'PAYMENT_COMPLETED', paymentStatusLabel: '已支付' })],
        PAYMENT_FINISHED: [buildOrder({ taskId: 4, documentCode: 'DOC-PAY-004', paymentStatusCode: 'PAYMENT_FINISHED', paymentStatusLabel: '已完成', bankFlowNo: 'BF-004', paidAt: '2026-04-06 09:30:00', receiptStatusLabel: '已获取回单' })],
        PAYMENT_EXCEPTION: [buildOrder({ taskId: 5, documentCode: 'DOC-PAY-005', paymentStatusCode: 'PAYMENT_EXCEPTION', paymentStatusLabel: '支付异常', receiptStatusLabel: '回单查询失败' })]
      }
      return Promise.resolve({ data: status ? map[status] || [] : [] })
    })
    mocks.expensePaymentApi.startTask.mockResolvedValue({ data: {} })
    mocks.expensePaymentApi.completeTask.mockResolvedValue({ data: {} })
    mocks.expensePaymentApi.markException.mockResolvedValue({ data: {} })
    mocks.elMessageBox.confirm.mockResolvedValue(undefined)
    mocks.elMessageBox.prompt.mockResolvedValue({ value: '备注' })
  })

  it('renders all five payment status groups and the pending payment row', async () => {
    const wrapper = await mountView()

    expect(wrapper.text()).toContain('待支付')
    expect(wrapper.text()).toContain('支付中')
    expect(wrapper.text()).toContain('已支付')
    expect(wrapper.text()).toContain('已完成')
    expect(wrapper.text()).toContain('支付异常')
    expect(wrapper.text()).toContain('付款单工作台')
    expect(wrapper.text()).toContain('DOC-PAY-001')
    expect(wrapper.text()).toContain('发起支付')
  })

  it('starts a bank payment task from the pending tab', async () => {
    const wrapper = await mountView()
    const startButton = wrapper.findAll('button').find((item) => item.text() === '发起支付')

    expect(startButton).toBeTruthy()
    await startButton!.trigger('click')
    await flushPromises()

    expect(mocks.elMessageBox.confirm).toHaveBeenCalled()
    expect(mocks.expensePaymentApi.startTask).toHaveBeenCalledWith(1)
    expect(mocks.elMessage.success).toHaveBeenCalledWith('付款任务已推送至银行')
  })
})
