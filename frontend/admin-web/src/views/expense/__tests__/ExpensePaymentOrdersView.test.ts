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
    replace: vi.fn(),
    resolve: vi.fn()
  },
  expensePaymentApi: {
    listOrders: vi.fn(),
    startTask: vi.fn(),
    completeTask: vi.fn(),
    markException: vi.fn(),
    submitOrderExport: vi.fn(),
    rejectTasks: vi.fn()
  },
  elMessage: {
    success: vi.fn(),
    error: vi.fn(),
    info: vi.fn(),
    warning: vi.fn()
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
  template: "<button type=\"button\" :disabled=\"disabled\" @click=\"$emit('click', $event)\"><slot /></button>"
})

const InputStub = defineComponent({
  props: {
    modelValue: {
      type: [String, Number],
      default: ''
    }
  },
  emits: ['update:modelValue'],
  template: "<input :value=\"modelValue\" @input=\"$emit('update:modelValue', $event.target.value)\" />"
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
    return () => h('div', {}, [
      slots.header ? h('div', { class: 'table-column-header' }, slots.header()) : null,
      ...(rows?.value || []).map((row: Record<string, unknown>, index: number) => h(
        'div',
        { key: `${String(index)}-${props.prop}` },
        slots.default ? slots.default({ row }) : [h('span', {}, props.prop ? String(row[props.prop] ?? '') : '')]
      ))
    ])
  }
})

const DialogStub = defineComponent({
  props: {
    modelValue: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue'],
  template: '<div v-if="modelValue"><slot /><slot name="footer" /></div>'
})

const PaginationStub = defineComponent({
  props: {
    currentPage: {
      type: Number,
      default: 1
    },
    pageSize: {
      type: Number,
      default: 10
    },
    total: {
      type: Number,
      default: 0
    }
  },
  emits: ['update:current-page', 'update:page-size'],
  template: `
    <div>
      <button data-testid="pagination-next" type="button" @click="$emit('update:current-page', currentPage + 1)">next</button>
      <button data-testid="pagination-prev" type="button" @click="$emit('update:current-page', currentPage - 1)">prev</button>
      <span>{{ total }}</span>
    </div>
  `
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
        'el-pagination': PaginationStub,
        'el-dialog': DialogStub,
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
    mocks.router.resolve.mockImplementation(({ query }: { query?: Record<string, string> }) => ({
      href: `/expense/documents/print?documentCodes=${query?.documentCodes || ''}&source=${query?.source || ''}`
    }))

    const pendingRows = Array.from({ length: 11 }, (_, index) => buildOrder({
      taskId: index + 1,
      documentCode: `DOC-PAY-${String(index + 1).padStart(3, '0')}`,
      documentTitle: `付款单-${index + 1}`
    }))

    mocks.expensePaymentApi.listOrders.mockImplementation((status?: string) => {
      const map: Record<string, unknown[]> = {
        PENDING_PAYMENT: pendingRows,
        PAYING: [buildOrder({ taskId: 21, documentCode: 'DOC-PAY-021', paymentStatusCode: 'PAYING', paymentStatusLabel: '支付中' })],
        PAYMENT_COMPLETED: [buildOrder({ taskId: 31, documentCode: 'DOC-PAY-031', paymentStatusCode: 'PAYMENT_COMPLETED', paymentStatusLabel: '已支付' })],
        PAYMENT_FINISHED: [buildOrder({ taskId: 41, documentCode: 'DOC-PAY-041', paymentStatusCode: 'PAYMENT_FINISHED', paymentStatusLabel: '已完成', bankFlowNo: 'BF-041', paidAt: '2026-04-06 09:30:00', receiptStatusLabel: '已获取回单' })],
        PAYMENT_EXCEPTION: [buildOrder({ taskId: 51, documentCode: 'DOC-PAY-051', paymentStatusCode: 'PAYMENT_EXCEPTION', paymentStatusLabel: '支付异常', receiptStatusLabel: '回单查询失败' })]
      }
      return Promise.resolve({ data: status ? map[status] || [] : [] })
    })
    mocks.expensePaymentApi.startTask.mockResolvedValue({ data: {} })
    mocks.expensePaymentApi.completeTask.mockResolvedValue({ data: {} })
    mocks.expensePaymentApi.markException.mockResolvedValue({ data: {} })
    mocks.expensePaymentApi.submitOrderExport.mockResolvedValue({ data: { taskNo: 'TASK-001' } })
    mocks.expensePaymentApi.rejectTasks.mockResolvedValue({ data: true })
    mocks.elMessageBox.confirm.mockResolvedValue(undefined)
    mocks.elMessageBox.prompt.mockResolvedValue({ value: '批量驳回原因' })
    vi.stubGlobal('open', vi.fn(() => ({ opener: null })))
  })

  it('renders pending rows with a default floating bar and disables actions before selection', async () => {
    const wrapper = await mountView()

    expect(wrapper.text()).toContain('待支付')
    expect(wrapper.text()).toContain('支付中')
    expect(wrapper.text()).toContain('已支付')
    expect(wrapper.text()).toContain('已完成')
    expect(wrapper.text()).toContain('支付异常')
    expect(wrapper.text()).toContain('付款单工作台')
    expect(wrapper.find('[data-testid="expense-payment-select-row-1"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="expense-payment-floating-bar"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="expense-payment-floating-bar"]').text()).toContain('未选择单据')
    expect(wrapper.get('[data-testid="expense-payment-floating-bar"]').text()).toContain('请先勾选待支付单据，再进行批量操作')
    expect(wrapper.text()).toContain('发起支付')
    expect(wrapper.text()).toContain('下载')
    expect(wrapper.text()).toContain('手动已支付')
    expect(wrapper.text()).toContain('打印')
    expect(wrapper.text()).toContain('驳回')
    expect(wrapper.get('[data-testid="expense-payment-bulk-start"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-testid="expense-payment-bulk-download"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-testid="expense-payment-bulk-manual-paid"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-testid="expense-payment-bulk-print"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-testid="expense-payment-bulk-reject"]').attributes('disabled')).toBeDefined()
  })

  it('keeps selected tasks when paging inside pending tab', async () => {
    const wrapper = await mountView()

    await wrapper.get('[data-testid="expense-payment-select-row-1"]').setValue(true)
    await flushPromises()
    await wrapper.get('[data-testid="pagination-next"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('DOC-PAY-011')
    expect(wrapper.get('[data-testid="expense-payment-floating-bar"]').text()).toContain('已选 1 项')
    expect(wrapper.get('[data-testid="expense-payment-bulk-download"]').attributes('disabled')).toBeUndefined()
  })

  it('opens the shared batch print page for selected pending payment documents', async () => {
    const wrapper = await mountView()

    await wrapper.get('[data-testid="expense-payment-select-row-1"]').setValue(true)
    await flushPromises()
    await wrapper.get('[data-testid="expense-payment-select-row-2"]').setValue(true)
    await flushPromises()
    await wrapper.get('[data-testid="expense-payment-bulk-print"]').trigger('click')

    expect(mocks.router.resolve).toHaveBeenCalledWith({
      name: 'expense-document-batch-print',
      query: {
        documentCodes: 'DOC-PAY-001,DOC-PAY-002',
        source: 'payment-pending'
      }
    })
    expect(window.open).toHaveBeenCalledWith(
      '/expense/documents/print?documentCodes=DOC-PAY-001,DOC-PAY-002&source=payment-pending',
      '_blank',
      'noopener,noreferrer'
    )
  })

  it('submits export from the start payment dialog and clears selection', async () => {
    const wrapper = await mountView()

    await wrapper.get('[data-testid="expense-payment-select-row-1"]').setValue(true)
    await flushPromises()
    await wrapper.get('[data-testid="expense-payment-bulk-start"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('导出支付单')
    expect(wrapper.text()).toContain('银企直连支付')
    await wrapper.get('[data-testid="expense-payment-start-option-export"]').trigger('click')
    await flushPromises()

    expect(mocks.expensePaymentApi.submitOrderExport).toHaveBeenCalledWith([1])
    expect(mocks.elMessage.success).toHaveBeenCalledWith('下载任务已提交，请到下载中心查看进度')
    expect(wrapper.find('[data-testid="expense-payment-floating-bar"]').text()).toContain('未选择单据')
    expect(wrapper.text()).not.toContain('导出支付单')
    expect(mocks.expensePaymentApi.startTask).not.toHaveBeenCalled()
  })

  it('keeps bank-link payment as a static placeholder in the start payment dialog', async () => {
    const wrapper = await mountView()

    await wrapper.get('[data-testid="expense-payment-select-row-1"]').setValue(true)
    await flushPromises()
    await wrapper.get('[data-testid="expense-payment-bulk-start"]').trigger('click')
    await flushPromises()

    await wrapper.get('[data-testid="expense-payment-start-option-bank-link"]').trigger('click')
    await flushPromises()

    expect(mocks.elMessage.info).toHaveBeenCalledWith('银企直连支付功能建设中')
    expect(mocks.expensePaymentApi.submitOrderExport).not.toHaveBeenCalled()
  })

  it('submits export for selected pending payment tasks', async () => {
    const wrapper = await mountView()

    await wrapper.get('[data-testid="expense-payment-select-row-1"]').setValue(true)
    await flushPromises()
    await wrapper.get('[data-testid="expense-payment-bulk-download"]').trigger('click')
    await flushPromises()

    expect(mocks.expensePaymentApi.submitOrderExport).toHaveBeenCalledWith([1])
    expect(mocks.elMessage.success).toHaveBeenCalledWith('下载任务已提交，请到下载中心查看进度')
  })

  it('rejects selected pending payment tasks with a textarea prompt', async () => {
    const wrapper = await mountView()

    await wrapper.get('[data-testid="expense-payment-select-row-1"]').setValue(true)
    await flushPromises()
    await wrapper.get('[data-testid="expense-payment-bulk-reject"]').trigger('click')
    await flushPromises()

    expect(mocks.elMessageBox.prompt).toHaveBeenCalled()
    expect(mocks.expensePaymentApi.rejectTasks).toHaveBeenCalledWith([1], { comment: '批量驳回原因' })
    expect(mocks.elMessage.success).toHaveBeenCalledWith('付款任务已驳回')
  })

  it('clears selection after switching away from pending tab and keeps other tabs row actions', async () => {
    const wrapper = await mountView()

    await wrapper.get('[data-testid="expense-payment-select-row-1"]').setValue(true)
    await flushPromises()
    await wrapper.get('[data-testid="expense-payment-stat-paid"]').trigger('click')

    expect(mocks.router.replace).toHaveBeenCalledWith({
      path: '/expense/payment/orders',
      query: { tab: 'paid' }
    })

    mocks.route.query.tab = 'paid'
    await flushPromises()

    expect(wrapper.find('[data-testid="expense-payment-floating-bar"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('查看')
    expect(wrapper.find('[data-testid="expense-payment-select-row-1"]').exists()).toBe(false)
  })
})
