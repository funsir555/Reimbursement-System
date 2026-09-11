import { computed, defineComponent, nextTick, reactive } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { ref } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import FinanceNewVoucherView from '@/views/finance/FinanceNewVoucherView.vue'

const routeState = reactive({
  name: 'finance-new-voucher',
  path: '/finance/general-ledger/new-voucher',
  fullPath: '/finance/general-ledger/new-voucher',
  params: {} as Record<string, unknown>
})

const mocks = vi.hoisted(() => ({
  financeApi: {
    getVoucherMeta: vi.fn(),
    listVouchers: vi.fn(),
    getVoucherDetail: vi.fn(),
    createVoucher: vi.fn(),
    updateVoucher: vi.fn(),
    reviewVoucher: vi.fn(),
    unreviewVoucher: vi.fn(),
    markVoucherError: vi.fn(),
    clearVoucherError: vi.fn(),
    restoreVoucher: vi.fn(),
    exportVouchers: vi.fn()
  },
  financeCompany: {
    currentCompanyId: 'COMPANY_A',
    currentCompanyName: '广州远智教育科技有限公司',
    currentCompanyLabel: '001  广州远智教育科技有限公司',
    currentCompanyHasActiveAccountSet: true,
    registerSwitchGuard: vi.fn(),
    unregisterSwitchGuard: vi.fn()
  },
  financePeriod: {
    currentYear: 2026,
    currentPeriod: 6,
    currentYearPeriod: 202606,
    hasPeriodContext: true
  },
  financeWorkspace: {
    registerCloseGuard: vi.fn(),
    unregisterCloseGuard: vi.fn(),
    registerPeriodSwitchGuard: vi.fn(),
    unregisterPeriodSwitchGuard: vi.fn(),
    registerCreateVoucherTakeoverGuard: vi.fn(),
    unregisterCreateVoucherTakeoverGuard: vi.fn(),
    requestCreateVoucherTakeover: vi.fn(async () => true),
    invalidateCache: vi.fn(),
    tabs: [{ path: '/finance/general-ledger/new-voucher', title: '新建凭证' }]
  },
  router: {
    push: vi.fn(),
    replace: vi.fn(),
    resolve: vi.fn((location: { params?: { voucherNo?: string } }) => ({
      fullPath: `/finance/general-ledger/query-voucher/${location.params?.voucherNo || ''}`
    }))
  },
  elMessage: {
    success: vi.fn(),
    warning: vi.fn(),
    error: vi.fn()
  },
  elMessageBox: {
    alert: vi.fn(),
    confirm: vi.fn()
  }
}))

const financeCompanyStore = reactive(mocks.financeCompany)
const financePeriodStore = reactive(mocks.financePeriod)
const mountedWrappers: Array<{ unmount: () => void }> = []

vi.mock('@/api', () => ({
  financeApi: mocks.financeApi
}))

vi.mock('@/stores/financeCompany', () => ({
  useFinanceCompanyStore: () => financeCompanyStore
}))

vi.mock('@/stores/financePeriod', () => ({
  useFinancePeriodStore: () => financePeriodStore
}))

vi.mock('@/stores/financeWorkspace', () => ({
  useFinanceWorkspaceStore: () => mocks.financeWorkspace
}))

vi.mock('vue-router', () => ({
  useRoute: () => routeState,
  useRouter: () => mocks.router
}))

vi.mock('@/utils/permissions', () => ({
  hasPermission: vi.fn(() => true),
  readStoredUser: vi.fn(() => ({ permissionCodes: ['finance:general_ledger:query_voucher:edit'] }))
}))

vi.mock('@/utils/money', () => ({
  absMoney: (value: string) => value.replace('-', ''),
  addMoney: (left: string, right: string) => String((Number(left || 0) + Number(right || 0)).toFixed(2)),
  formatMoney: (value: string | number) => String(value),
  isZeroMoney: (value: string | number | undefined) => Number(value || 0) === 0,
  normalizeMoneyValue: (value?: string, options?: { fallback?: string }) => value || options?.fallback || ''
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: mocks.elMessage,
    ElMessageBox: mocks.elMessageBox
  }
})

vi.mock('@element-plus/icons-vue', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@element-plus/icons-vue')>()
  return {
    ...actual,
    CircleClose: { template: '<span />' },
    Coin: { template: '<span />' },
    Delete: { template: '<span />' },
    DocumentCopy: { template: '<span />' },
    Download: { template: '<span />' },
    Edit: { template: '<span />' },
    Plus: { template: '<span />' },
    Printer: { template: '<span />' },
    RefreshLeft: { template: '<span />' },
    Search: { template: '<span />' },
    Select: { template: '<span />' },
    Tickets: { template: '<span />' },
    Top: { template: '<span />' },
    TrendCharts: { template: '<span />' },
    Tools: { template: '<span />' }
  }
})

const InputStub = defineComponent({
  props: {
    modelValue: { type: [String, Number], default: '' },
    placeholder: { type: String, default: '' },
    readonly: { type: Boolean, default: false },
    disabled: { type: Boolean, default: false }
  },
  emits: ['focus', 'update:modelValue'],
  template: '<input v-bind="$attrs" :value="modelValue" :placeholder="placeholder" :readonly="readonly" :disabled="disabled" @focus="$emit(\'focus\')" @input="$emit(\'update:modelValue\', $event.target.value)" />'
})

const SelectStub = defineComponent({
  props: {
    modelValue: { type: [String, Number], default: '' },
    disabled: { type: Boolean, default: false }
  },
  emits: ['change', 'focus', 'update:modelValue', 'visible-change'],
  template: '<select :value="modelValue" :disabled="disabled" @focus="$emit(\'focus\')" @change="$emit(\'update:modelValue\', $event.target.value); $emit(\'change\', $event.target.value)"><slot /></select>'
})

const TreeSelectStub = defineComponent({
  props: {
    modelValue: { type: [String, Number], default: '' },
    disabled: { type: Boolean, default: false },
    data: { type: Array, default: () => [] },
    filterNodeMethod: { type: Function, default: undefined }
  },
  emits: ['focus', 'update:modelValue'],
  setup(props) {
    const flattened = computed(() => {
      const result: Array<{ value: string | number; label: string }> = []
      const visit = (nodes: Array<any>) => {
        nodes.forEach((node) => {
          result.push({ value: node.value, label: node.label })
          if (Array.isArray(node.children) && node.children.length) {
            visit(node.children)
          }
        })
      }
      visit(props.data as Array<any>)
      return result
    })
    return { flattened }
  },
  template: '<select data-testid="department-tree-select" :value="modelValue" :disabled="disabled" @focus="$emit(\'focus\')" @change="$emit(\'update:modelValue\', $event.target.value)"><option v-for="item in flattened" :key="item.value" :value="item.value">{{ item.label }}</option></select>'
})

const OptionStub = defineComponent({
  props: { label: { type: String, default: '' }, value: { type: [String, Number], default: '' } },
  template: '<option :value="value">{{ label }}</option>'
})

const ButtonStub = defineComponent({
  emits: ['click'],
  template: '<button type="button" v-bind="$attrs" @click="$emit(\'click\', $event)"><slot /></button>'
})

const NumberStub = defineComponent({
  props: { modelValue: { type: [String, Number], default: '' }, disabled: { type: Boolean, default: false } },
  emits: ['update:modelValue'],
  template: '<input :value="modelValue" :disabled="disabled" @input="$emit(\'update:modelValue\', Number($event.target.value || 0))" />'
})

const MoneyInputStub = defineComponent({
  props: {
    modelValue: { type: String, default: '' },
    placeholder: { type: String, default: '' },
    readonly: { type: Boolean, default: false },
    disabled: { type: Boolean, default: false }
  },
  emits: ['update:modelValue', 'focus', 'blur', 'keydown'],
  template: '<input v-bind="$attrs" :value="modelValue" :placeholder="placeholder" :readonly="readonly" :disabled="disabled" @input="$emit(\'update:modelValue\', $event.target.value)" @focus="$emit(\'focus\')" @blur="$emit(\'blur\', $event)" @keydown="$emit(\'keydown\', $event)" />'
})

const mountOptions = {
  global: {
    stubs: {
      'el-button': ButtonStub,
      'el-input': InputStub,
      'el-select': SelectStub,
      'el-tree-select': TreeSelectStub,
      'el-option': OptionStub,
      'el-input-number': NumberStub,
      'el-date-picker': InputStub,
      'el-dialog': defineComponent({
        props: { modelValue: { type: Boolean, default: false } },
        template: '<div v-if="modelValue"><slot /><slot name="footer" /></div>'
      }),
      'el-empty': defineComponent({ template: '<div><slot /></div>' }),
      'el-radio-group': defineComponent({ props: { modelValue: { type: [String, Number], default: '' } }, emits: ['update:modelValue'], template: '<div><slot /></div>' }),
      'el-radio': defineComponent({ props: { label: { type: [String, Number], default: '' } }, template: '<label><input type=\"radio\" :value=\"label\" /><slot /></label>' }),
      'el-icon': true,
      'money-input': MoneyInputStub,
      'finance-customer-archive-dialog': defineComponent({ template: '<div />' }),
      'finance-supplier-archive-dialog': defineComponent({ template: '<div />' }),
      'finance-project-archive-dialog': defineComponent({ template: '<div />' })
    }
  }
}

function buildMeta() {
  return {
    companyOptions: [{ value: 'COMPANY_A', code: '001', name: '广州远智教育科技有限公司', label: '001  广州远智教育科技有限公司' }],
    departmentOptions: [
      { value: '10', code: 'D001', name: 'Finance Center', label: 'D001  Finance Center' },
      { value: '11', code: 'D002', name: 'Expense Admin', label: 'D002  Expense Admin', parentValue: '10' }
    ],
    employeeOptions: [{ value: '2', code: '2', name: '员工甲', label: '2  员工甲' }],
    voucherTypeOptions: [{ value: '记', label: '记账凭证' }],
    currencyOptions: [{ value: 'CNY', label: '人民币' }],
    accountOptions: [
      {
        value: '1001',
        code: '1001',
        name: '库存现金',
        label: '1001  库存现金',
        subjectCategory: 'ASSET',
        subjectCategoryLabel: '资产',
        leafFlag: 1,
        bcash: 1,
        bperson: 1,
        bcus: 1,
        bsup: 1,
        bdept: 1,
        bitem: 1,
        cassItem: '01'
      },
      {
        value: '6601',
        code: '6601',
        name: '管理费用',
        label: '6601  管理费用',
        subjectCategory: 'PROFIT',
        subjectCategoryLabel: '损益',
        leafFlag: 0,
        bperson: 0,
        bcus: 0,
        bsup: 0,
        bdept: 0,
        bitem: 0
      },
      {
        value: '560101',
        code: '560101',
        name: '办公费',
        label: '560101  办公费',
        parentValue: '6601',
        subjectCategory: 'PROFIT',
        subjectCategoryLabel: '损益',
        leafFlag: 1,
        bperson: 0,
        bcus: 0,
        bsup: 0,
        bdept: 0,
        bitem: 0
      },
      {
        value: '100201',
        code: '100201',
        name: '银行存款',
        label: '100201  银行存款',
        subjectCategory: 'ASSET',
        subjectCategoryLabel: '资产',
        leafFlag: 1,
        bperson: 0,
        bcus: 0,
        bsup: 0,
        bdept: 0,
        bitem: 0
      }
    ],
    cashFlowOptions: [
      { value: '101', code: '1001', name: '销售商品、提供劳务收到的现金', label: '1001  销售商品、提供劳务收到的现金' }
    ],
    customerOptions: [{ value: 'C00001', code: 'C00001', name: '华南客户', label: 'C00001  华南客户' }],
    supplierOptions: [{ value: 'V00001', code: 'V00001', name: '核心供应商', label: 'V00001  核心供应商' }],
    projectClassOptions: [{ value: '01', code: '01', name: '市场项目', label: '01  市场项目' }],
    projectOptions: [
      { value: '000001', code: '000001', name: '华南推广项目', label: '000001  华南推广项目', parentValue: '01' },
      { value: '000002', code: '000002', name: '华北推广项目', label: '000002  华北推广项目', parentValue: '02' }
    ],
    defaultCompanyId: 'COMPANY_A',
    defaultYear: 2026,
    defaultYearPeriod: 202604,
    defaultBillDate: '2026-04-05',
    defaultPeriod: 4,
    defaultVoucherType: '记',
    suggestedVoucherNo: 12,
    defaultMaker: '财务制单员',
    defaultAttachedDocCount: 0,
    defaultCurrency: 'CNY',
    defaultCurrencyCode: 'CNY',
    defaultCurrencyName: '人民币'
  }
}

function buildDetail(voucherNo = 'COMPANY_A~2026~4~记~12') {
  const isCurrentMonthVoucher = voucherNo === 'COMPANY_A~2026~6~记~18'
  return {
    voucherNo,
    displayVoucherNo: isCurrentMonthVoucher ? '记-0018' : '记-0012',
    companyId: 'COMPANY_A',
    iyear: 2026,
    iyperiod: isCurrentMonthVoucher ? 202606 : 202604,
    iperiod: isCurrentMonthVoucher ? 6 : 4,
    csign: '记',
    voucherTypeLabel: '记账凭证',
    inoId: isCurrentMonthVoucher ? 18 : 12,
    dbillDate: isCurrentMonthVoucher ? '2026-06-20' : '2026-04-05',
    idoc: 1,
    cbill: '财务制单员',
    checkerName: '',
    checkedAt: '',
    postedAt: '',
    ctext1: '',
    ctext2: '已有凭证',
    status: 'UNPOSTED',
    statusLabel: '未记账',
    editable: true,
    totalDebit: '100.00',
    totalCredit: '100.00',
    entries: [
      { inid: 1, cdigest: '摘要 A', ccode: '1001', ccodeName: '库存现金', currencyCode: 'CNY', cexchName: '人民币', md: '100.00', mc: '', cashFlowItemId: 101, cashFlowItemName: '销售商品、提供劳务收到的现金' },
      { inid: 2, cdigest: '摘要 B', ccode: '100201', ccodeName: '银行存款', currencyCode: 'CNY', cexchName: '人民币', md: '', mc: '100.00' }
    ]
  }
}

async function mountView(
  props: { pageMode?: 'create' | 'detail' | 'review'; voucherNo?: string } = {},
  options: { autoEnterCreateEditing?: boolean } = {}
) {
  const wrapper = mount(FinanceNewVoucherView, {
    ...mountOptions,
    attachTo: document.body,
    props
  })
  mountedWrappers.push(wrapper)
  await flushPromises()
  await nextTick()
  if ((props.pageMode ?? 'create') === 'create' && options.autoEnterCreateEditing !== false) {
    const hasSaveButton = wrapper.findAll('button').some((button) => button.text() === '保存')
    if (!hasSaveButton) {
      await wrapper.findAll('button').find((button) => button.text() === '新增')?.trigger('click')
      await flushPromises()
      await nextTick()
    }
  }
  return wrapper
}

describe('FinanceNewVoucherView', () => {
  afterEach(() => {
    while (mountedWrappers.length) {
      mountedWrappers.pop()?.unmount()
    }
    document.body.innerHTML = ''
  })

  beforeEach(() => {
    vi.clearAllMocks()
    sessionStorage.clear()
    routeState.name = 'finance-new-voucher'
    routeState.path = '/finance/general-ledger/new-voucher'
    routeState.fullPath = '/finance/general-ledger/new-voucher'
    routeState.params = {}
    financeCompanyStore.currentCompanyId = 'COMPANY_A'
    financeCompanyStore.currentCompanyName = '广州远智教育科技有限公司'
    financeCompanyStore.currentCompanyLabel = '001  广州远智教育科技有限公司'
    financeCompanyStore.currentCompanyHasActiveAccountSet = true
    mocks.financeWorkspace.requestCreateVoucherTakeover.mockResolvedValue(true)
    mocks.financeWorkspace.tabs = [{ path: '/finance/general-ledger/new-voucher', title: '新建凭证' }]
    financePeriodStore.currentYear = 2026
    financePeriodStore.currentPeriod = 6
    financePeriodStore.currentYearPeriod = 202606
    financePeriodStore.hasPeriodContext = true
    mocks.financeApi.getVoucherMeta.mockResolvedValue({ data: buildMeta() })
    mocks.financeApi.listVouchers.mockResolvedValue({
      data: {
        total: 2,
        items: [
          {
            voucherNo: 'COMPANY_A~2026~6~记~12',
            displayVoucherNo: '记-0012',
            companyId: 'COMPANY_A',
            iyear: 2026,
            iyperiod: 202606,
            iperiod: 6,
            csign: '记',
            voucherTypeLabel: '记账凭证',
            dbillDate: '2026-06-12',
            summary: '摘要一',
            cbill: '财务制单员',
            idoc: 1,
            status: 'UNPOSTED',
            statusLabel: '未记账',
            editable: true,
            entryCount: 2,
            totalDebit: '100.00',
            totalCredit: '100.00',
            inoId: 12
          },
          {
            voucherNo: 'COMPANY_A~2026~6~记~18',
            displayVoucherNo: '记-0018',
            companyId: 'COMPANY_A',
            iyear: 2026,
            iyperiod: 202606,
            iperiod: 6,
            csign: '记',
            voucherTypeLabel: '记账凭证',
            dbillDate: '2026-06-20',
            summary: '摘要二',
            cbill: '财务制单员',
            idoc: 1,
            status: 'UNPOSTED',
            statusLabel: '未记账',
            editable: true,
            entryCount: 2,
            totalDebit: '100.00',
            totalCredit: '100.00',
            inoId: 18
          }
        ]
      }
    })
    mocks.financeApi.getVoucherDetail.mockImplementation(async (_companyId: string, voucherNo: string) => ({
      data: buildDetail(voucherNo)
    }))
    mocks.financeApi.reviewVoucher.mockResolvedValue({
      data: {
        action: 'REVIEW',
        voucherNo: 'COMPANY_A~2026~4~记~12',
        status: 'REVIEWED',
        statusLabel: '已审核',
        checkerName: '审核人甲',
        nextVoucherNo: 'COMPANY_A~2026~4~记~13',
        lastVoucherOfMonth: false
      }
    })
    mocks.financeApi.unreviewVoucher.mockResolvedValue({
      data: {
        action: 'UNREVIEW',
    voucherNo: 'COMPANY_A~2026~4~记~12',
        status: 'UNPOSTED',
        statusLabel: '未记账'
      }
    })
    mocks.financeApi.markVoucherError.mockResolvedValue({
      data: {
        action: 'MARK_ERROR',
        voucherNo: 'COMPANY_A~2026~4~记~12',
        status: 'ERROR',
        statusLabel: '已标记错误'
      }
    })
    mocks.financeApi.clearVoucherError.mockResolvedValue({
      data: {
        action: 'CLEAR_ERROR',
        voucherNo: 'COMPANY_A~2026~4~记~12',
        status: 'UNPOSTED',
        statusLabel: '未记账'
      }
    })
    mocks.financeApi.restoreVoucher.mockResolvedValue({
      data: {
        action: 'RESTORE',
        voucherNo: 'COMPANY_A~2026~4~记~12',
        status: 'UNPOSTED',
        statusLabel: '未记账'
      }
    })
    mocks.financeApi.exportVouchers.mockResolvedValue(undefined)
  })

  it('loads voucher meta with the finance company context', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: { iyear?: number; iperiod?: number; iyperiod?: number; dbillDate?: string }
    }

    expect(mocks.financeApi.getVoucherMeta).toHaveBeenCalledWith({ companyId: 'COMPANY_A', billDate: '2026-06-30' })
    expect(mocks.financeApi.listVouchers).toHaveBeenCalledWith({
      companyId: 'COMPANY_A',
      billMonth: '2026-06',
      page: 1,
      pageSize: 500
    })
    expect(wrapper.text()).toContain('凭证编号')
    expect(wrapper.text()).toContain('备注')
    expect(vm.form.iyear).toBe(2026)
    expect(vm.form.iperiod).toBe(6)
    expect(vm.form.iyperiod).toBe(202606)
    expect(vm.form.dbillDate).toBe('2026-06-30')
  })

  it('opens create mode as a locked blank page when the current month has no vouchers', async () => {
    vi.useFakeTimers()
    try {
      mocks.financeApi.listVouchers.mockResolvedValue({ data: { total: 0, items: [] } })
      const wrapper = await mountView({ pageMode: 'create' }, { autoEnterCreateEditing: false })

      const voucherNoInput = wrapper.get('[data-testid="voucher-no-input"]')
      expect((voucherNoInput.element as HTMLInputElement).value).toBe('')
      expect(voucherNoInput.attributes('readonly')).toBeDefined()
      expect(wrapper.findAll('button').some((button) => button.text() === '保存')).toBe(false)

      await vi.advanceTimersByTimeAsync(220)
      expect(sessionStorage.getItem('finance-new-voucher-draft:COMPANY_A')).toBeNull()

      await wrapper.findAll('button').find((button) => button.text() === '新增')?.trigger('click')
      await flushPromises()

      expect(wrapper.find('[data-testid="voucher-no-input"]').exists()).toBe(true)
      expect(wrapper.findAll('button').some((button) => button.text() === '保存')).toBe(true)
    } finally {
      vi.useRealTimers()
    }
  })

  it('does not show the reset prompt when starting a new voucher from a locked create page', async () => {
    mocks.financeApi.listVouchers.mockResolvedValue({ data: { total: 0, items: [] } })
    const wrapper = await mountView({ pageMode: 'create' }, { autoEnterCreateEditing: false })

    await wrapper.findAll('button').find((button) => button.text() === '新增')?.trigger('click')
    await flushPromises()

    expect(mocks.elMessageBox.confirm).not.toHaveBeenCalledWith(
      '将清空当前录入内容并开始新的凭证，是否继续？',
      '新增凭证',
      expect.anything()
    )
    expect(wrapper.findAll('button').some((button) => button.text() === '保存')).toBe(true)
  })

  it('keeps the blank create-editing state when switching away and back without closing the tab', async () => {
    mocks.financeApi.listVouchers.mockResolvedValue({ data: { total: 0, items: [] } })
    const host = defineComponent({
      components: {
        FinanceNewVoucherView,
        DummyPane: defineComponent({ template: '<div data-testid="dummy-pane" />' })
      },
      setup() {
        const active = ref(true)
        const viewProps = { pageMode: 'create' as const }
        return { active, viewProps, FinanceNewVoucherView, DummyPane: defineComponent({ template: '<div data-testid="dummy-pane" />' }) }
      },
      template: '<KeepAlive><component :is="active ? FinanceNewVoucherView : DummyPane" v-bind="viewProps" /></KeepAlive>'
    })
    const wrapper = mount(host, {
      ...mountOptions,
      attachTo: document.body
    })
    mountedWrappers.push(wrapper)
    await flushPromises()
    await nextTick()

    expect(mocks.financeApi.getVoucherMeta).toHaveBeenCalledTimes(1)
    expect(wrapper.findAll('button').some((button) => button.text() === '保存')).toBe(false)

    await wrapper.findAll('button').find((button) => button.text() === '新增')?.trigger('click')
    await flushPromises()
    expect(wrapper.findAll('button').some((button) => button.text() === '保存')).toBe(true)

    ;(wrapper.vm as unknown as { active: boolean }).active = false
    await flushPromises()
    ;(wrapper.vm as unknown as { active: boolean }).active = true
    await flushPromises()

    expect(mocks.financeApi.getVoucherMeta).toHaveBeenCalledTimes(2)
    expect(wrapper.findAll('button').some((button) => button.text() === '保存')).toBe(true)
  })

  it('opens create mode with the current month last voucher in readonly state and exposes modify', async () => {
    const wrapper = await mountView({ pageMode: 'create' }, { autoEnterCreateEditing: false })
    const vm = wrapper.vm as unknown as {
      form: { inoId?: number; dbillDate?: string; entries: Array<{ cdigest: string }> }
    }

    expect(vm.form.inoId).toBe(18)
    expect(vm.form.dbillDate).toBe('2026-06-20')
    expect(vm.form.entries[0]?.cdigest).toBe('摘要 A')
    expect(wrapper.findAll('button').some((button) => button.text() === '修改')).toBe(true)
    expect(wrapper.findAll('button').some((button) => button.text() === '保存')).toBe(false)
  })

  it('allows selecting saved vouchers from a readonly create page dropdown', async () => {
    const wrapper = await mountView({ pageMode: 'create' }, { autoEnterCreateEditing: false })

    await wrapper.get('[data-testid="voucher-no-trigger"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="voucher-no-dropdown"]').exists()).toBe(true)
    expect(wrapper.findAll('[data-testid="voucher-no-option"]').map((option) => option.text())).toContain('记-00122026-06-12摘要一')
  })

  it('updates the loaded last voucher when modifying from the create page', async () => {
    mocks.financeApi.updateVoucher.mockResolvedValue({
      data: {
        voucherNo: 'COMPANY_A~2026~6~记~18',
        companyId: 'COMPANY_A',
        iyear: 2026,
        iyperiod: 202606,
        iperiod: 6,
        csign: '记',
        inoId: 18,
        entryCount: 2,
        totalDebit: '100.00',
        totalCredit: '100.00',
        status: 'UNPOSTED',
        checkedAt: null,
        postedAt: null
      }
    })
    const wrapper = await mountView({ pageMode: 'create' }, { autoEnterCreateEditing: false })
    const vm = wrapper.vm as unknown as {
      form: { entries: Array<{ cdigest: string }> }
    }

    await wrapper.findAll('button').find((button) => button.text() === '修改')?.trigger('click')
    await flushPromises()

    vm.form.entries[0].cdigest = '修改后的最后凭证'
    await flushPromises()
    await wrapper.findAll('button').find((button) => button.text() === '保存')?.trigger('click')
    await flushPromises()

    expect(mocks.financeApi.updateVoucher).toHaveBeenCalledWith(
      'COMPANY_A',
      'COMPANY_A~2026~6~记~18',
      expect.objectContaining({
        companyId: 'COMPANY_A',
        inoId: 18
      })
    )
    expect(mocks.financeApi.createVoucher).not.toHaveBeenCalled()
  })

  it('reloads saved voucher suggestions when the month or voucher type changes', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: { dbillDate?: string; csign?: string }
    }

    vm.form.dbillDate = '2026-07-15'
    await flushPromises()
    expect(mocks.financeApi.listVouchers).toHaveBeenCalledWith({
      companyId: 'COMPANY_A',
      billMonth: '2026-07',
      page: 1,
      pageSize: 500
    })

    vm.form.csign = '收'
    await flushPromises()
    expect(mocks.financeApi.listVouchers).toHaveBeenCalledTimes(3)
  })

  it('keeps manual voucher-number input editable without navigating away', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const voucherNoInput = wrapper.get('[data-testid="voucher-no-input"]')
    const vm = wrapper.vm as unknown as {
      form: { inoId?: number }
    }

    await voucherNoInput.setValue('18')
    await flushPromises()

    expect(vm.form.inoId).toBe(18)
    expect(mocks.router.replace).not.toHaveBeenCalled()
  })

  it('opens the voucher dropdown only from the dedicated trigger and shows a scrollable saved-voucher list', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const voucherNoInput = wrapper.get('[data-testid="voucher-no-input"]')

    await voucherNoInput.trigger('focus')
    await flushPromises()
    expect(wrapper.find('[data-testid="voucher-no-dropdown"]').exists()).toBe(false)

    await wrapper.get('[data-testid="voucher-no-trigger"]').trigger('click')
    await flushPromises()

    const dropdown = wrapper.get('[data-testid="voucher-no-dropdown"]')
    const optionTexts = wrapper.findAll('[data-testid="voucher-no-option"]').map((option) => option.text())

    expect(dropdown.exists()).toBe(true)
    expect(optionTexts).toContain('记-00122026-06-12摘要一')
    expect(optionTexts).toContain('记-00182026-06-20摘要二')
  })

  it('opens a saved voucher detail in a new tab without replacing the current create tab', async () => {
    const wrapper = await mountView({ pageMode: 'create' })

    await wrapper.get('[data-testid="voucher-no-trigger"]').trigger('click')
    await flushPromises()

    await wrapper.findAll('[data-testid="voucher-no-option"]')[1]?.trigger('click')
    await flushPromises()

    expect(mocks.router.push).toHaveBeenCalledWith({
      name: 'finance-query-voucher-detail',
      params: { voucherNo: 'COMPANY_A~2026~6~记~18' }
    })
    expect(mocks.financeWorkspace.requestCreateVoucherTakeover).not.toHaveBeenCalled()
  })

  it('registers a close guard for editable vouchers and prompts before closing when dirty', async () => {
    mocks.elMessageBox.confirm.mockResolvedValueOnce(undefined)
    sessionStorage.setItem('finance-new-voucher-draft:COMPANY_A', JSON.stringify({ companyId: 'COMPANY_A', entries: [] }))
    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: { entries: Array<{ cdigest: string }> }
    }

    expect(mocks.financeWorkspace.registerCloseGuard).toHaveBeenCalledWith(
      '/finance/general-ledger/new-voucher',
      expect.any(Function)
    )

    vm.form.entries[0].cdigest = '待关闭未保存'
    await flushPromises()

    const guard = mocks.financeWorkspace.registerCloseGuard.mock.calls.at(-1)?.[1] as (() => Promise<boolean>) | undefined
    expect(await guard?.()).toBe(true)
    expect(mocks.elMessageBox.confirm).toHaveBeenCalledWith(
      '当前凭证未保存，关闭后当前录入将丢失，确认关闭吗',
      '关闭凭证',
      expect.objectContaining({
        confirmButtonText: '确认',
        cancelButtonText: '取消'
      })
    )
    expect(sessionStorage.getItem('finance-new-voucher-draft:COMPANY_A')).toBeNull()

    wrapper.unmount()
    expect(sessionStorage.getItem('finance-new-voucher-draft:COMPANY_A')).toBeNull()
    expect(mocks.financeWorkspace.unregisterCloseGuard).toHaveBeenCalledWith('/finance/general-ledger/new-voucher')
  })

  it('keeps the current draft and opens detail directly when selecting a saved voucher', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: { entries: Array<{ cdigest: string }> }
    }

    vm.form.entries[0].cdigest = '未保存摘要'
    await flushPromises()
    await wrapper.get('[data-testid="voucher-no-trigger"]').trigger('click')
    await flushPromises()

    await wrapper.findAll('[data-testid="voucher-no-option"]')[0]?.trigger('click')
    await flushPromises()

    expect(mocks.elMessageBox.confirm).not.toHaveBeenCalledWith(
      '当前凭证未保存，切换后当前录入将丢失，确认切换吗',
      '切换凭证',
      expect.anything()
    )
    expect(mocks.router.push).toHaveBeenCalledWith({
      name: 'finance-query-voucher-detail',
      params: { voucherNo: 'COMPANY_A~2026~6~记~12' }
    })
  })

  it('shows the voucher dropdown in detail mode and saves before switching when the voucher is being edited', async () => {
    mocks.elMessageBox.confirm.mockResolvedValueOnce(undefined)
    mocks.financeApi.updateVoucher.mockResolvedValue({
      data: {
        voucherNo: 'COMPANY_A~2026~4~记~12',
        companyId: 'COMPANY_A',
        iyear: 2026,
        iyperiod: 202604,
        iperiod: 4,
        csign: '记',
        inoId: 12,
        entryCount: 2,
        totalDebit: '100.00',
        totalCredit: '100.00',
        status: 'UNPOSTED',
        checkedAt: null,
        postedAt: null
      }
    })
    const wrapper = await mountView({ pageMode: 'detail', voucherNo: 'COMPANY_A~2026~4~记~12' })
    const vm = wrapper.vm as unknown as {
      form: { entries: Array<{ cdigest: string }> }
    }

    await wrapper.get('[data-testid="voucher-no-trigger"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-testid="voucher-no-dropdown"]').exists()).toBe(true)

    await wrapper.findAll('button').find((button) => button.text() === '修改')?.trigger('click')
    await flushPromises()
    vm.form.entries[0].cdigest = '修改后摘要'
    await flushPromises()

    await wrapper.findAll('[data-testid="voucher-no-option"]')[0]?.trigger('click')
    await flushPromises()

    expect(mocks.elMessageBox.confirm).toHaveBeenCalledWith(
      '是否保存当前修改',
      '切换凭证',
      expect.objectContaining({
        confirmButtonText: '保存后切换',
        cancelButtonText: '直接切换'
      })
    )
    expect(mocks.financeApi.updateVoucher).toHaveBeenCalledTimes(1)
    expect(mocks.router.push).toHaveBeenCalledWith({
      name: 'finance-query-voucher-detail',
      params: { voucherNo: 'COMPANY_A~2026~6~记~12' }
    })
  })

  it('falls back to the meta bill date when no finance period context is available', async () => {
    financePeriodStore.currentYear = 0
    financePeriodStore.currentPeriod = 0
    financePeriodStore.currentYearPeriod = 0
    financePeriodStore.hasPeriodContext = false

    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: { dbillDate?: string }
    }

    expect(mocks.financeApi.getVoucherMeta).toHaveBeenCalledWith({ companyId: 'COMPANY_A' })
    expect(vm.form.dbillDate).toBe('2026-04-05')
  })

  it('fills a restored draft without bill date from the current period month end', async () => {
    sessionStorage.setItem('finance-new-voucher-draft:COMPANY_A', JSON.stringify({
      companyId: 'COMPANY_A',
      csign: '记',
      idoc: 0,
      cbill: '草稿制单员',
      entries: []
    }))

    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: { dbillDate?: string }
    }

    expect(vm.form.dbillDate).toBe('2026-06-30')
    expect(mocks.elMessage.success).toHaveBeenCalledWith('已恢复暂存草稿')
  })

  it('still treats a restored draft as unsaved content when closing the tab', async () => {
    mocks.elMessageBox.confirm.mockResolvedValueOnce(undefined)
    sessionStorage.setItem('finance-new-voucher-draft:COMPANY_A', JSON.stringify({
      companyId: 'COMPANY_A',
      csign: '记',
      dbillDate: '2026-06-30',
      entries: [
        { cdigest: '恢复的草稿', ccode: '560101', md: '100.00' }
      ]
    }))

    await mountView({ pageMode: 'create' })

    const guard = mocks.financeWorkspace.registerCloseGuard.mock.calls.at(-1)?.[1] as (() => Promise<boolean>) | undefined
    expect(await guard?.()).toBe(true)
    expect(mocks.elMessageBox.confirm).toHaveBeenCalledWith(
      '当前凭证未保存，关闭后当前录入将丢失，确认关闭吗',
      '关闭凭证',
      expect.objectContaining({
        confirmButtonText: '确认',
        cancelButtonText: '取消'
      })
    )
  })

  it('persists unsaved create-mode input and restores it after leaving and returning', async () => {
    vi.useFakeTimers()
    try {
      const wrapper = await mountView({ pageMode: 'create' })
      const vm = wrapper.vm as unknown as {
        form: {
          entries: Array<{
            cdigest: string
            ccode: string
            md?: string
          }>
        }
      }

      vm.form.entries[0].cdigest = '切页前草稿'
      vm.form.entries[0].ccode = '560101'
      vm.form.entries[0].md = '100.00'
      await flushPromises()
      await vi.advanceTimersByTimeAsync(220)

      const rawDraft = sessionStorage.getItem('finance-new-voucher-draft:COMPANY_A')
      expect(rawDraft).toContain('切页前草稿')

      while (mountedWrappers.length) {
        mountedWrappers.pop()?.unmount()
      }

      const restoredWrapper = await mountView({ pageMode: 'create' })
      const restoredVm = restoredWrapper.vm as unknown as {
        form: {
          entries: Array<{
            cdigest: string
            ccode: string
            md?: string
          }>
        }
      }

      expect(restoredVm.form.entries[0]).toMatchObject({
        cdigest: '切页前草稿',
        ccode: '560101',
        md: '100.00'
      })
      expect(mocks.elMessage.success).toHaveBeenCalledWith('已恢复暂存草稿')
    } finally {
      vi.useRealTimers()
    }
  })

  it('renders a compact document card without redundant section headings and shows company name only', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const companyBox = wrapper.get('.voucher-company-box')
    const infoGridChildren = wrapper.get('.voucher-info-grid').element.children

    expect(wrapper.find('.voucher-info-side').exists()).toBe(false)
    expect(wrapper.findAll('.status-chip')).toHaveLength(0)
    expect(companyBox.text()).toBe(financeCompanyStore.currentCompanyName)
    expect(companyBox.text()).not.toBe(financeCompanyStore.currentCompanyLabel)
    expect(wrapper.find('.voucher-info-company').exists()).toBe(true)
    expect(wrapper.find('.voucher-info-code').exists()).toBe(true)
    expect(wrapper.find('.voucher-info-date').exists()).toBe(true)
    expect(wrapper.find('.voucher-info-period').exists()).toBe(true)
    expect(wrapper.find('.voucher-info-maker').exists()).toBe(true)
    expect(wrapper.find('.voucher-info-docs').exists()).toBe(true)
    expect(wrapper.find('.voucher-info-field-note').exists()).toBe(true)
    expect(wrapper.find('.voucher-info-spacer').exists()).toBe(true)
    expect(infoGridChildren).toHaveLength(8)
    expect(infoGridChildren[1]?.className).toContain('voucher-info-code')
    expect(wrapper.find('.voucher-info-code .voucher-number-group').exists()).toBe(true)
    expect(wrapper.findAll('.voucher-field-label').length).toBeGreaterThan(0)
    expect(wrapper.find('.voucher-ledger-header').exists()).toBe(false)
    expect(wrapper.find('.voucher-section-head').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('凭证明细')
    expect(wrapper.text()).not.toContain('借方合计')
    expect(wrapper.text()).not.toContain('贷方合计')
    expect(wrapper.text()).not.toContain('当前行')
  })

  it('renders archive options as code on the left and name on the right', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const meta = buildMeta()

    const optionTexts = wrapper.findAll('option').map((option) => option.text())
    expect(optionTexts).toContain(meta.accountOptions[0].label)
    expect(optionTexts).toContain(meta.customerOptions[0].label)
    expect(optionTexts).toContain(meta.supplierOptions[0].label)
    expect(optionTexts).toContain(meta.projectClassOptions[0].label)
    expect(optionTexts).toContain(meta.projectOptions[0].label)
  })

  it('filters project options by the selected project class', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: { entries: Array<{ ccode?: string; citemClass?: string }> }
      getFilteredProjectOptions: () => Array<{ value: string }>
    }

    vm.form.entries[0].ccode = '1001'
    vm.form.entries[0].citemClass = '01'
    await flushPromises()
    await nextTick()

    expect(vm.getFilteredProjectOptions().map((item) => item.value)).toEqual(['000001'])
  })


  it('renders the department selector as a tree and keeps code/name filtering', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const treeSelect = wrapper.getComponent(TreeSelectStub)
    const vm = wrapper.vm as unknown as {
      selectedRow: { cdeptId?: string }
      departmentTreeOptions: Array<{ value: string; children: Array<{ value: string }> }>
    }

    expect(vm.departmentTreeOptions.map((item) => item.value)).toEqual(['10'])
    expect(vm.departmentTreeOptions[0]?.children.map((item) => item.value)).toEqual(['11'])

    const filterNodeMethod = treeSelect.props('filterNodeMethod') as ((query: string, data: { code?: string; name?: string; label?: string }) => boolean) | undefined
    expect(filterNodeMethod?.('D002', { code: 'D002', name: 'Expense Admin', label: 'D002  Expense Admin' })).toBe(true)
    expect(filterNodeMethod?.('Expense', { code: 'D002', name: 'Expense Admin', label: 'D002  Expense Admin' })).toBe(true)
    expect(filterNodeMethod?.('missing', { code: 'D002', name: 'Expense Admin', label: 'D002  Expense Admin' })).toBe(false)

    await treeSelect.setValue('11')
    expect(vm.selectedRow.cdeptId).toBe('11')
  })

  it('links assist control availability to the selected account subject and clears disabled values', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      selectedRow: {
        ccode?: string
        cdeptId?: string
        cpersonId?: string
        ccusId?: string
        csupId?: string
        citemClass?: string
        citemId?: string
      }
      assistDisabledState: {
        department: boolean
        employee: boolean
        customer: boolean
        supplier: boolean
        projectClass: boolean
        project: boolean
      }
      currentAssistCapability: {
        lockedProjectClassCode?: string
      }
    }

    expect(vm.assistDisabledState.department).toBe(true)
    expect(vm.assistDisabledState.project).toBe(true)

    vm.selectedRow.ccode = '1001'
    await flushPromises()
    await nextTick()

    expect(vm.assistDisabledState.department).toBe(false)
    expect(vm.assistDisabledState.employee).toBe(false)
    expect(vm.assistDisabledState.customer).toBe(false)
    expect(vm.assistDisabledState.supplier).toBe(false)
    expect(vm.assistDisabledState.projectClass).toBe(true)
    expect(vm.assistDisabledState.project).toBe(false)
    expect(vm.currentAssistCapability.lockedProjectClassCode).toBe('01')
    expect(vm.selectedRow.citemClass).toBe('01')

    vm.selectedRow.cdeptId = '11'
    vm.selectedRow.cpersonId = '2'
    vm.selectedRow.ccusId = 'C00001'
    vm.selectedRow.csupId = 'V00001'
    vm.selectedRow.citemId = '000001'
    vm.selectedRow.ccode = '6601'
    await flushPromises()
    await nextTick()

    expect(vm.assistDisabledState.department).toBe(true)
    expect(vm.assistDisabledState.employee).toBe(true)
    expect(vm.assistDisabledState.customer).toBe(true)
    expect(vm.assistDisabledState.supplier).toBe(true)
    expect(vm.assistDisabledState.project).toBe(true)
    expect(vm.selectedRow.cdeptId).toBe('')
    expect(vm.selectedRow.cpersonId).toBe('')
    expect(vm.selectedRow.ccusId).toBe('')
    expect(vm.selectedRow.csupId).toBe('')
    expect(vm.selectedRow.citemClass).toBe('')
    expect(vm.selectedRow.citemId).toBe('')
  })

  it('shows account code and snapshot name in detail mode even when the option is missing from meta', async () => {
    const meta = buildMeta()
    meta.accountOptions = [{ value: '1001', code: '1001', name: '库存现金', label: '1001  库存现金' }]
    mocks.financeApi.getVoucherMeta.mockResolvedValue({ data: meta })

    const wrapper = await mountView({ pageMode: 'detail', voucherNo: 'COMPANY_A~2026~4~记~12' })
    const vm = wrapper.vm as unknown as {
      form: { iperiod?: number }
    }

    expect(mocks.financeApi.getVoucherDetail).toHaveBeenCalledWith('COMPANY_A', 'COMPANY_A~2026~4~记~12')
    expect(wrapper.text()).toContain('100201  银行存款')
    expect(vm.form.iperiod).toBe(4)
    expect(wrapper.find('.voucher-ledger-header').exists()).toBe(false)
    expect(wrapper.find('.voucher-section-head').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('凭证明细')
    expect(wrapper.text()).not.toContain('当前行')
  })

  it('renders review mode toolbar with review actions and keeps the form readonly', async () => {
    const detail = buildDetail()
    detail.status = 'UNPOSTED'
    detail.statusLabel = '未记账'
    mocks.financeApi.getVoucherDetail.mockResolvedValue({ data: detail })

    const wrapper = await mountView({ pageMode: 'review', voucherNo: 'COMPANY_A~2026~4~记~12' })

    expect(wrapper.text()).toContain('审核凭证')
    expect(wrapper.text()).toContain('审核')
    expect(wrapper.text()).toContain('导出')
    expect(wrapper.text()).toContain('查找')
    expect(wrapper.text()).toContain('反审核')
    expect(wrapper.text()).toContain('标记错误')
    expect(wrapper.text()).not.toContain('保存')
    expect(wrapper.text()).not.toContain('修改')
    expect(wrapper.text()).toContain('审核：未审核')
    expect(wrapper.find('.voucher-ledger-header').exists()).toBe(false)
    expect(wrapper.find('.voucher-section-head').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('凭证明细')
    expect(wrapper.text()).not.toContain('当前行')
  })

  it('reviews the current voucher and jumps to the next reviewable voucher', async () => {
    const wrapper = await mountView({ pageMode: 'review', voucherNo: 'COMPANY_A~2026~4~记~12' })

    await wrapper.findAll('button').find((button) => button.text() === '审核')?.trigger('click')
    await flushPromises()

    expect(mocks.financeApi.reviewVoucher).toHaveBeenCalledWith('COMPANY_A', 'COMPANY_A~2026~4~记~12')
    expect(mocks.router.replace).toHaveBeenCalledWith({
      name: 'finance-review-voucher-detail',
      params: { voucherNo: 'COMPANY_A~2026~4~记~13' }
    })
  })

  it('shows the last-voucher message when review mode has no next voucher', async () => {
    mocks.financeApi.reviewVoucher.mockResolvedValueOnce({
      data: {
        action: 'REVIEW',
        voucherNo: 'COMPANY_A~2026~4~记~12',
        status: 'REVIEWED',
        statusLabel: '已审核',
        checkerName: '审核人甲',
        nextVoucherNo: '',
        lastVoucherOfMonth: true
      }
    })

    const detail = buildDetail()
    detail.status = 'REVIEWED'
    detail.statusLabel = '已审核'
    detail.checkerName = '审核人甲'
    mocks.financeApi.getVoucherDetail.mockResolvedValue({ data: detail })

    const wrapper = await mountView({ pageMode: 'review', voucherNo: 'COMPANY_A~2026~4~记~12' })

    await wrapper.findAll('button').find((button) => button.text() === '审核')?.trigger('click')
    await flushPromises()

    expect(mocks.elMessage.warning).toHaveBeenCalledWith('当前是最后一张')
  })

  it('switches the review error button to clear-error when the voucher is already marked error', async () => {
    const detail = buildDetail()
    detail.status = 'ERROR'
    detail.statusLabel = '已标记错误'
    detail.checkerName = '审核人甲'
    mocks.financeApi.getVoucherDetail.mockResolvedValue({ data: detail })

    const wrapper = await mountView({ pageMode: 'review', voucherNo: 'COMPANY_A~2026~4~记~12' })

    expect(wrapper.text()).toContain('取消错误')

    await wrapper.findAll('button').find((button) => button.text() === '取消错误')?.trigger('click')
    await flushPromises()

    expect(mocks.financeApi.clearVoucherError).toHaveBeenCalledWith('COMPANY_A', 'COMPANY_A~2026~4~记~12')
  })

  it('exports the current voucher and finds a matching row in review mode', async () => {
    const promptSpy = vi.spyOn(window, 'prompt').mockReturnValueOnce('银行')
    const wrapper = await mountView({ pageMode: 'review', voucherNo: 'COMPANY_A~2026~4~记~12' })
    const vm = wrapper.vm as unknown as { selectedRow: { cdigest?: string } }

    await wrapper.findAll('button').find((button) => button.text() === '导出')?.trigger('click')
    await flushPromises()
    expect(mocks.financeApi.exportVouchers).toHaveBeenCalledWith({ companyId: 'COMPANY_A', voucherNo: 'COMPANY_A~2026~4~记~12' })

    await wrapper.findAll('button').find((button) => button.text() === '查找')?.trigger('click')
    await flushPromises()

    expect(vm.selectedRow.cdigest).toBe('摘要 B')
    expect(mocks.elMessage.success).toHaveBeenCalledWith('已定位到第 2 行')
    promptSpy.mockRestore()
  })

  it('shows readable notices when account set or archive data is missing', async () => {
    financeCompanyStore.currentCompanyHasActiveAccountSet = false
    let wrapper = await mountView({ pageMode: 'create' })
    expect(wrapper.text()).toContain('当前公司未创建账套，请切换公司或先建账。')
    wrapper.unmount()

    financeCompanyStore.currentCompanyHasActiveAccountSet = true
    const meta = buildMeta()
    meta.accountOptions = []
    meta.customerOptions = []
    meta.supplierOptions = []
    meta.projectClassOptions = []
    meta.projectOptions = []
    mocks.financeApi.getVoucherMeta.mockResolvedValue({ data: meta })

    wrapper = await mountView({ pageMode: 'create' })
    expect(wrapper.text()).toContain('当前公司账套已启用，但暂无会计科目数据，请检查账套初始化结果。')
    expect(wrapper.text()).toContain('当前公司暂无客户档案数据。')
    expect(wrapper.text()).toContain('当前公司暂无供应商档案数据。')
    expect(wrapper.text()).toContain('当前公司暂无项目档案数据。')
  })

  it('reloads voucher meta when the finance company context changes', async () => {
    const wrapper = await mountView({ pageMode: 'create' })

    financeCompanyStore.currentCompanyId = 'COMPANY_B'
    financeCompanyStore.currentCompanyName = '深圳测试公司'
    financeCompanyStore.currentCompanyLabel = '002  深圳测试公司'
    financePeriodStore.currentYear = 2026
    financePeriodStore.currentPeriod = 7
    financePeriodStore.currentYearPeriod = 202607
    await flushPromises()

    expect(mocks.financeApi.getVoucherMeta).toHaveBeenCalledWith({ companyId: 'COMPANY_A', billDate: '2026-06-30' })
    expect(mocks.financeApi.getVoucherMeta).toHaveBeenCalledWith({ companyId: 'COMPANY_B', billDate: '2026-07-31' })

    wrapper.unmount()
  })

  it('saves vouchers through the current finance company context', async () => {
    mocks.financeApi.createVoucher.mockResolvedValue({
      data: {
        voucherNo: 'COMPANY_A~2026~4~记~12',
        companyId: 'COMPANY_A',
        iyear: 2026,
        iyperiod: 202604,
        iperiod: 4,
        csign: '记',
        inoId: 12,
        entryCount: 2,
        totalDebit: '100.00',
        totalCredit: '100.00',
        status: 'UNPOSTED',
        checkedAt: null,
        postedAt: null
      }
    })

    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: {
        entries: Array<{
          cdigest: string
          ccode: string
          md?: string
          mc?: string
        }>
      }
    }

    vm.form.entries[0].cdigest = '摘要 A'
    vm.form.entries[0].ccode = '560101'
    vm.form.entries[0].md = '100.00'
    vm.form.entries[1].cdigest = '摘要 B'
    vm.form.entries[1].ccode = '560101'
    vm.form.entries[1].mc = '100.00'
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text() === '保存')?.trigger('click')
    await flushPromises()

    expect(mocks.financeApi.createVoucher).toHaveBeenCalled()
    expect(mocks.elMessage.success).toHaveBeenCalled()
  })

  it('refreshes saved voucher suggestions after create-mode save succeeds', async () => {
    mocks.financeApi.listVouchers
      .mockResolvedValueOnce({
        data: {
          total: 1,
          items: [
            {
              voucherNo: 'COMPANY_A~2026~6~记~18',
              displayVoucherNo: '记-0018',
              companyId: 'COMPANY_A',
              iyear: 2026,
              iyperiod: 202606,
              iperiod: 6,
              csign: '记',
              voucherTypeLabel: '记账凭证',
              dbillDate: '2026-06-20',
              summary: '旧凭证',
              cbill: '财务制单员',
              idoc: 1,
              status: 'UNPOSTED',
              statusLabel: '未记账',
              editable: true,
              entryCount: 2,
              totalDebit: '100.00',
              totalCredit: '100.00',
              inoId: 18
            }
          ]
        }
      })
      .mockResolvedValueOnce({
        data: {
          total: 1,
          items: [
            {
              voucherNo: 'COMPANY_A~2026~6~记~18',
              displayVoucherNo: '记-0018',
              companyId: 'COMPANY_A',
              iyear: 2026,
              iyperiod: 202606,
              iperiod: 6,
              csign: '记',
              voucherTypeLabel: '记账凭证',
              dbillDate: '2026-06-20',
              summary: '旧凭证',
              cbill: '财务制单员',
              idoc: 1,
              status: 'UNPOSTED',
              statusLabel: '未记账',
              editable: true,
              entryCount: 2,
              totalDebit: '100.00',
              totalCredit: '100.00',
              inoId: 18
            }
          ]
        }
      })
      .mockResolvedValueOnce({
        data: {
          total: 2,
          items: [
            {
              voucherNo: 'COMPANY_A~2026~6~记~12',
              displayVoucherNo: '记-0012',
              companyId: 'COMPANY_A',
              iyear: 2026,
              iyperiod: 202606,
              iperiod: 6,
              csign: '记',
              voucherTypeLabel: '记账凭证',
              dbillDate: '2026-06-30',
              summary: '新保存摘要',
              cbill: '财务制单员',
              idoc: 1,
              status: 'UNPOSTED',
              statusLabel: '未记账',
              editable: true,
              entryCount: 2,
              totalDebit: '100.00',
              totalCredit: '100.00',
              inoId: 12
            },
            {
              voucherNo: 'COMPANY_A~2026~6~记~18',
              displayVoucherNo: '记-0018',
              companyId: 'COMPANY_A',
              iyear: 2026,
              iyperiod: 202606,
              iperiod: 6,
              csign: '记',
              voucherTypeLabel: '记账凭证',
              dbillDate: '2026-06-20',
              summary: '旧凭证',
              cbill: '财务制单员',
              idoc: 1,
              status: 'UNPOSTED',
              statusLabel: '未记账',
              editable: true,
              entryCount: 2,
              totalDebit: '100.00',
              totalCredit: '100.00',
              inoId: 18
            }
          ]
        }
      })
    mocks.financeApi.createVoucher.mockResolvedValue({
      data: {
        voucherNo: 'COMPANY_A~2026~6~记~12',
        companyId: 'COMPANY_A',
        iyear: 2026,
        iyperiod: 202606,
        iperiod: 6,
        csign: '记',
        inoId: 12,
        entryCount: 2,
        totalDebit: '100.00',
        totalCredit: '100.00',
        status: 'UNPOSTED',
        checkedAt: null,
        postedAt: null
      }
    })

    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: {
        entries: Array<{
          cdigest: string
          ccode: string
          md?: string
          mc?: string
        }>
      }
    }

    vm.form.entries[0].cdigest = '摘要 A'
    vm.form.entries[0].ccode = '560101'
    vm.form.entries[0].md = '100.00'
    vm.form.entries[1].cdigest = '摘要 B'
    vm.form.entries[1].ccode = '560101'
    vm.form.entries[1].mc = '100.00'
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text() === '保存')?.trigger('click')
    await flushPromises()

    expect(mocks.financeApi.listVouchers).toHaveBeenCalledTimes(3)
    expect(mocks.financeApi.listVouchers).toHaveBeenNthCalledWith(3, {
      companyId: 'COMPANY_A',
      billMonth: '2026-06',
      page: 1,
      pageSize: 500
    })

    await wrapper.get('[data-testid="voucher-no-trigger"]').trigger('click')
    await flushPromises()

    const optionTexts = wrapper.findAll('[data-testid="voucher-no-option"]').map((option) => option.text())
    expect(optionTexts.some((text) => text.includes('记-0012') && text.includes('新保存摘要'))).toBe(true)
  })

  it('blocks save when a voucher digest exceeds the tightened length limit', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: {
        entries: Array<{
          cdigest: string
          ccode: string
          md?: string
          mc?: string
        }>
      }
    }

    vm.form.entries[0].cdigest = 'A'.repeat(256)
    vm.form.entries[0].ccode = '560101'
    vm.form.entries[0].md = '100.00'
    vm.form.entries[1].cdigest = '摘要 B'
    vm.form.entries[1].ccode = '560101'
    vm.form.entries[1].mc = '100.00'
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text() === '保存')?.trigger('click')
    await flushPromises()

    expect(mocks.financeApi.createVoucher).not.toHaveBeenCalled()
    expect(mocks.elMessage.warning).toHaveBeenCalledWith('第 1 行摘要最多 255 个字符')
  })

  it('blocks save when currency name exceeds the tightened length limit', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: {
        entries: Array<{
          cdigest: string
          ccode: string
          cexchName?: string
          md?: string
          mc?: string
        }>
      }
      validateVoucher: (showToast?: boolean) => boolean
    }

    vm.form.entries[0].cdigest = '\u6458\u8981 A'
    vm.form.entries[0].ccode = '560101'
    vm.form.entries[0].cexchName = 'C'.repeat(33)
    vm.form.entries[0].md = '100.00'
    vm.form.entries[1].cdigest = '\u6458\u8981 B'
    vm.form.entries[1].ccode = '560101'
    vm.form.entries[1].mc = '100.00'
    await flushPromises()

    expect(vm.validateVoucher(true)).toBe(false)

    expect(mocks.financeApi.createVoucher).not.toHaveBeenCalled()
    expect(mocks.elMessage.warning).toHaveBeenCalledWith('\u7b2c 1 \u884c\u5e01\u79cd\u540d\u79f0\u6700\u591a 32 \u4e2a\u5b57\u7b26')
  })

  it('blocks save when an archive selection is stale or project ownership mismatches', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: {
        entries: Array<{
          cdigest: string
          ccode: string
          ccusId?: string
          cashFlowItemId?: number
          citemClass?: string
          citemId?: string
          md?: string
          mc?: string
        }>
      }
    }

    vm.form.entries[0].cdigest = '摘要 A'
    vm.form.entries[0].ccode = '1001'
    vm.form.entries[0].cashFlowItemId = 101
    vm.form.entries[0].ccusId = 'C99999'
    vm.form.entries[0].md = '100.00'
    vm.form.entries[1].cdigest = '摘要 B'
    vm.form.entries[1].ccode = '1001'
    vm.form.entries[1].cashFlowItemId = 101
    vm.form.entries[1].citemClass = '01'
    vm.form.entries[1].citemId = '000002'
    vm.form.entries[1].mc = '100.00'
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text() === '保存')?.trigger('click')
    await flushPromises()

    expect(mocks.financeApi.createVoucher).not.toHaveBeenCalled()
    expect(mocks.elMessage.warning).toHaveBeenCalledWith('第 1 行客户不存在或当前不可用')
  })

  it('auto clears the selected project when it no longer belongs to the selected project class', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: {
        entries: Array<{
          cdigest: string
          ccode: string
          citemClass?: string
          citemId?: string
          md?: string
          mc?: string
        }>
      }
    }

    vm.form.entries[0].cdigest = '摘要 A'
    vm.form.entries[0].ccode = '1001'
    vm.form.entries[0].citemClass = '01'
    vm.form.entries[0].citemId = '000002'
    vm.form.entries[0].md = '100.00'
    vm.form.entries[1].cdigest = '摘要 B'
    vm.form.entries[1].ccode = '1001'
    vm.form.entries[1].mc = '100.00'
    await flushPromises()
    await nextTick()

    expect(vm.form.entries[0].citemId).toBe('')
  })

  it('warns and restores the previous leaf subject when leaving a non-leaf subject field', async () => {
    mocks.elMessageBox.alert.mockResolvedValue(undefined)
    const wrapper = await mountView({ pageMode: 'create' })
    const subjectSelect = wrapper.findAll('select[data-subject-row-id]')[0]
    const vm = wrapper.vm as unknown as {
      form: {
        entries: Array<{
          ccode: string
          ccodeName?: string
          cdeptId?: string
          md?: string
        }>
      }
    }

    await subjectSelect?.trigger('focus')
    await subjectSelect?.setValue('1001')
    vm.form.entries[0].cdeptId = '11'
    await flushPromises()

    await subjectSelect?.setValue('6601')
    await flushPromises()

    await wrapper.findAll('input').find((input) => input.attributes('placeholder') === '0.00')?.trigger('focus')
    await flushPromises()
    await nextTick()

    expect(mocks.elMessageBox.alert).toHaveBeenCalledTimes(1)
    expect(vm.form.entries[0].ccode).toBe('1001')
    expect(vm.form.entries[0].cdeptId).toBe('')
  })

  it('blocks save when the current row uses a non-leaf subject', async () => {
    mocks.elMessageBox.alert.mockResolvedValue(undefined)
    const wrapper = await mountView({ pageMode: 'create' })
    const subjectSelect = wrapper.findAll('select[data-subject-row-id]')[0]
    const vm = wrapper.vm as unknown as {
      form: {
        entries: Array<{
          cdigest: string
          ccode: string
          md?: string
          mc?: string
        }>
      }
    }

    vm.form.entries[0].cdigest = '摘要 A'
    vm.form.entries[0].md = '100.00'
    vm.form.entries[1].cdigest = '摘要 B'
    vm.form.entries[1].ccode = '1001'
    vm.form.entries[1].mc = '100.00'
    await subjectSelect?.trigger('focus')
    await subjectSelect?.setValue('6601')
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text() === '保存')?.trigger('click')
    await flushPromises()

    expect(mocks.elMessageBox.alert).toHaveBeenCalledTimes(1)
    expect(mocks.financeApi.createVoucher).not.toHaveBeenCalled()
  })

  it('opens the cash-flow dialog when a cash subject leaves the amount field without a selection', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: {
        entries: Array<{
          cdigest: string
          ccode: string
          md?: string
          cashFlowItemId?: number
          cashFlowSubjectCode?: string
          cashFlowAmount?: string
        }>
      }
      cashFlowDialogVisible: boolean
    }

    vm.form.entries[0].cdigest = '摘要 A'
    vm.form.entries[0].ccode = '1001'
    vm.form.entries[0].md = '100.00'
    await flushPromises()

    await wrapper.findAll('input').find((input) => input.attributes('placeholder') === '0.00')?.trigger('blur')
    await flushPromises()

    expect(vm.cashFlowDialogVisible).toBe(true)
    expect(vm.form.entries[0].cashFlowItemId).toBeUndefined()
    expect(vm.form.entries[0].cashFlowSubjectCode).toBe('1001')
    expect(vm.form.entries[0].cashFlowAmount).toBe('100.00')
  })

  it('clears stale cash-flow selection when the cash amount changes', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: {
        entries: Array<{
          cdigest: string
          ccode: string
          md?: string
          cashFlowItemId?: number
          cashFlowItemName?: string
          cashFlowSubjectCode?: string
          cashFlowAmount?: string
        }>
      }
    }

    vm.form.entries[0].cdigest = '摘要 A'
    vm.form.entries[0].ccode = '1001'
    vm.form.entries[0].cashFlowItemId = 101
    vm.form.entries[0].cashFlowItemName = '销售商品、提供劳务收到的现金'
    vm.form.entries[0].cashFlowSubjectCode = '1001'
    vm.form.entries[0].cashFlowAmount = '100.00'
    await flushPromises()

    const debitInput = wrapper.findAll('input[data-voucher-field="md"]')[0]
    await debitInput?.setValue('120.00')
    await flushPromises()

    expect(vm.form.entries[0].cashFlowItemId).toBeUndefined()
    expect(vm.form.entries[0].cashFlowItemName).toBe('')
    expect(vm.form.entries[0].cashFlowAmount).toBe('120.00')
  })

  it('blocks save when a cash subject is missing cash-flow selection', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: {
        entries: Array<{
          cdigest: string
          ccode: string
          md?: string
          mc?: string
        }>
      }
    }

    vm.form.entries[0].cdigest = '摘要 A'
    vm.form.entries[0].ccode = '1001'
    vm.form.entries[0].md = '100.00'
    vm.form.entries[1].cdigest = '摘要 B'
    vm.form.entries[1].ccode = '560101'
    vm.form.entries[1].mc = '100.00'
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text() === '保存')?.trigger('click')
    await flushPromises()

    expect(mocks.financeApi.createVoucher).not.toHaveBeenCalled()
    expect((wrapper.vm as unknown as { cashFlowDialogVisible: boolean }).cashFlowDialogVisible).toBe(true)
  })

  it('blocks save when the entered cash-flow subject or amount is inconsistent with the voucher row', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: {
        entries: Array<{
          cdigest: string
          ccode: string
          md?: string
          mc?: string
          cashFlowItemId?: number
          cashFlowSubjectCode?: string
          cashFlowAmount?: string
        }>
      }
    }

    vm.form.entries[0].cdigest = '摘要 A'
    vm.form.entries[0].ccode = '1001'
    vm.form.entries[0].md = '100.00'
    vm.form.entries[0].cashFlowItemId = 101
    vm.form.entries[0].cashFlowSubjectCode = '100201'
    vm.form.entries[0].cashFlowAmount = '120.00'
    vm.form.entries[1].cdigest = '摘要 B'
    vm.form.entries[1].ccode = '560101'
    vm.form.entries[1].mc = '100.00'
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text() === '保存')?.trigger('click')
    await flushPromises()

    expect(mocks.financeApi.createVoucher).not.toHaveBeenCalled()
    expect(mocks.elMessage.warning).toHaveBeenCalledWith('第 1 行现金流量科目必须与凭证分录科目一致')
  })

  it('renders the cash-flow item select with code and name instead of bare id', async () => {
    const meta = buildMeta()
    meta.cashFlowOptions = []
    mocks.financeApi.getVoucherMeta.mockResolvedValue({ data: meta })

    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: {
        entries: Array<{
          cdigest: string
          ccode: string
          md?: string
          mc?: string
          cashFlowItemId?: number
          cashFlowItemName?: string
        }>
      }
    }

    vm.form.entries[0].cdigest = '摘要 A'
    vm.form.entries[0].ccode = '1001'
    vm.form.entries[0].md = '100.00'
    vm.form.entries[0].cashFlowItemId = 101
    vm.form.entries[0].cashFlowItemName = '销售商品、提供劳务收到的现金'
    vm.form.entries[1].cdigest = '摘要 B'
    vm.form.entries[1].ccode = '560101'
    vm.form.entries[1].mc = '100.00'
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text() === '现金流量')?.trigger('click')
    await flushPromises()

    const cashFlowSelect = wrapper
      .findAll('select')
      .find((select) => select.findAll('option').some((option) => option.text().includes('销售商品、提供劳务收到的现金')))

    expect(cashFlowSelect).toBeTruthy()
    expect(cashFlowSelect?.findAll('option').map((option) => option.text())).toContain('销售商品、提供劳务收到的现金')
    expect((cashFlowSelect?.element as HTMLSelectElement).value).toBe('101')
  })

  it('shows shortcut titles and triggers save from F6', async () => {
    mocks.financeApi.createVoucher.mockResolvedValue({
      data: {
        voucherNo: 'COMPANY_A~2026~4~记~12',
        companyId: 'COMPANY_A',
        iyear: 2026,
        iyperiod: 202604,
        iperiod: 4,
        csign: '记',
        inoId: 12,
        entryCount: 2,
        totalDebit: '100.00',
        totalCredit: '100.00',
        status: 'UNPOSTED',
        checkedAt: null,
        postedAt: null
      }
    })
    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: {
        entries: Array<{
          cdigest: string
          ccode: string
          md?: string
          mc?: string
        }>
      }
    }

    vm.form.entries[0].cdigest = '摘要 A'
    vm.form.entries[0].ccode = '560101'
    vm.form.entries[0].md = '100.00'
    vm.form.entries[1].cdigest = '摘要 B'
    vm.form.entries[1].ccode = '560101'
    vm.form.entries[1].mc = '100.00'
    await flushPromises()

    expect(wrapper.findAll('button').find((button) => button.text() === '新增')?.attributes('title')).toBe('新增（F5）')
    expect(wrapper.findAll('button').find((button) => button.text() === '保存')?.attributes('title')).toBe('保存（F6）')

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'F6' }))
    await flushPromises()

    expect(mocks.financeApi.createVoucher).toHaveBeenCalledTimes(1)
  })

  it('copies the current voucher into a new unsaved draft with Ctrl+F', async () => {
    const wrapper = await mountView({ pageMode: 'detail', voucherNo: 'COMPANY_A~2026~4~记~12' })

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'f', ctrlKey: true }))
    await flushPromises()

    expect(mocks.financeWorkspace.requestCreateVoucherTakeover).toHaveBeenCalledTimes(1)
    expect(mocks.financeWorkspace.invalidateCache).toHaveBeenCalledWith('/finance/general-ledger/new-voucher')
    expect(mocks.router.push).toHaveBeenCalledWith({ name: 'finance-new-voucher' })
    const rawDraft = sessionStorage.getItem('finance-new-voucher-draft:COMPANY_A')
    expect(rawDraft).toBeTruthy()
    const draft = JSON.parse(String(rawDraft)) as {
      inoId?: number
      entries: Array<{ cdigest: string; ccode: string; md?: string; mc?: string }>
    }
    expect(draft.inoId).toBeUndefined()
    expect(draft.entries).toHaveLength(2)
    expect(draft.entries[0]).toMatchObject({ cdigest: '摘要 A', ccode: '1001', md: '100.00' })
    expect(draft.entries[1]).toMatchObject({ cdigest: '摘要 B', ccode: '100201', mc: '100.00' })

    mocks.elMessage.success.mockClear()
    routeState.name = 'finance-new-voucher'
    routeState.path = '/finance/general-ledger/new-voucher'
    routeState.fullPath = '/finance/general-ledger/new-voucher'
    await mountView({ pageMode: 'create' }, { autoEnterCreateEditing: false })
    expect(mocks.elMessage.success).not.toHaveBeenCalledWith('已恢复暂存草稿')
  })

  it('starts a new voucher from detail view without showing the create-page reset prompt', async () => {
    const wrapper = await mountView({ pageMode: 'detail', voucherNo: 'COMPANY_A~2026~4~记~12' })

    await wrapper.findAll('button').find((button) => button.text() === '新增')?.trigger('click')
    await flushPromises()

    expect(mocks.financeWorkspace.requestCreateVoucherTakeover).toHaveBeenCalledTimes(1)
    expect(mocks.elMessageBox.confirm).not.toHaveBeenCalledWith(
      '将清空当前录入内容并开始新的凭证，是否继续？',
      '新增凭证',
      expect.anything()
    )
    expect(mocks.router.push).toHaveBeenCalledWith({ name: 'finance-new-voucher' })
  })

  it('shows a visible void marker and restores a voided voucher back to normal state', async () => {
    const voidedDetail = {
      ...buildDetail('COMPANY_A~2026~4~记~12'),
      status: 'VOIDED',
      statusLabel: '已作废',
      editable: false,
      voidedAt: '2026-06-20 10:00:00',
      voidedByName: '财务制单员'
    }
    const restoredDetail = {
      ...buildDetail('COMPANY_A~2026~4~记~12'),
      status: 'UNPOSTED',
      statusLabel: '未记账',
      editable: true,
      voidedAt: '',
      voidedByName: ''
    }
    mocks.financeApi.getVoucherDetail.mockResolvedValueOnce({ data: voidedDetail })
    mocks.financeApi.getVoucherDetail.mockResolvedValueOnce({ data: restoredDetail })
    mocks.financeApi.restoreVoucher.mockResolvedValueOnce({
      data: {
        action: 'RESTORE',
        voucherNo: 'COMPANY_A~2026~4~记~12',
        status: 'UNPOSTED',
        statusLabel: '未记账'
      }
    })

    const wrapper = await mountView({ pageMode: 'detail', voucherNo: 'COMPANY_A~2026~4~记~12' })

    expect(wrapper.find('.voucher-void-flag').text()).toBe('作废')
    expect(wrapper.findAll('button').some((button) => button.text() === '恢复')).toBe(true)

    await wrapper.findAll('button').find((button) => button.text() === '恢复')?.trigger('click')
    await flushPromises()

    expect(mocks.financeApi.restoreVoucher).toHaveBeenCalledWith('COMPANY_A', 'COMPANY_A~2026~4~记~12')
    expect(wrapper.find('.voucher-void-flag').exists()).toBe(false)
    expect(wrapper.findAll('button').some((button) => button.text() === '作废')).toBe(true)
  })

  it('reloads fresh monthly meta before starting a new voucher after a transfer voucher is the current last voucher', async () => {
    const monthlyMeta = buildMeta()
    monthlyMeta.defaultBillDate = '2026-06-30'
    monthlyMeta.defaultPeriod = 6
    monthlyMeta.defaultYearPeriod = 202606
    monthlyMeta.defaultVoucherType = '记'
    monthlyMeta.suggestedVoucherNo = 26

    const lockedTransferMeta = {
      ...monthlyMeta,
      defaultVoucherType: '转',
      suggestedVoucherNo: 25
    }

    mocks.financeApi.getVoucherMeta.mockImplementation(async (payload?: { csign?: string }) => ({
      data: payload?.csign === '转' ? lockedTransferMeta : monthlyMeta
    }))
    mocks.financeApi.listVouchers.mockResolvedValue({
      data: {
        total: 2,
        items: [
          {
            voucherNo: 'COMPANY_A~2026~6~记~24',
            displayVoucherNo: '记-0024',
            companyId: 'COMPANY_A',
            iyear: 2026,
            iyperiod: 202606,
            iperiod: 6,
            csign: '记',
            voucherTypeLabel: '记账凭证',
            dbillDate: '2026-06-18',
            summary: '普通凭证',
            cbill: '财务制单员',
            idoc: 1,
            status: 'UNPOSTED',
            statusLabel: '未记账',
            editable: true,
            entryCount: 2,
            totalDebit: '100.00',
            totalCredit: '100.00',
            inoId: 24
          },
          {
            voucherNo: 'COMPANY_A~2026~6~转~25',
            displayVoucherNo: '转-0025',
            companyId: 'COMPANY_A',
            iyear: 2026,
            iyperiod: 202606,
            iperiod: 6,
            csign: '转',
            voucherTypeLabel: '转账凭证',
            dbillDate: '2026-06-30',
            summary: '期末结转',
            cbill: '财务制单员',
            idoc: 1,
            status: 'UNPOSTED',
            statusLabel: '未记账',
            editable: true,
            entryCount: 2,
            totalDebit: '100.00',
            totalCredit: '100.00',
            inoId: 25
          }
        ]
      }
    })
    mocks.financeApi.getVoucherDetail.mockResolvedValue({
      data: {
        ...buildDetail('COMPANY_A~2026~6~转~25'),
        voucherNo: 'COMPANY_A~2026~6~转~25',
        displayVoucherNo: '转-0025',
        iyperiod: 202606,
        iperiod: 6,
        csign: '转',
        inoId: 25,
        dbillDate: '2026-06-30'
      }
    })

    const wrapper = await mountView({ pageMode: 'create' }, { autoEnterCreateEditing: false })
    const vm = wrapper.vm as unknown as {
      form: { csign: string; inoId?: number }
    }

    expect(vm.form.csign).toBe('转')
    expect(vm.form.inoId).toBe(25)

    await wrapper.findAll('button').find((button) => button.text() === '新增')?.trigger('click')
    await flushPromises()

    expect(mocks.financeApi.getVoucherMeta).toHaveBeenCalledWith({ companyId: 'COMPANY_A', billDate: '2026-06-30' })
    expect(mocks.financeApi.getVoucherMeta).toHaveBeenCalledWith({
      companyId: 'COMPANY_A',
      billDate: '2026-06-30',
      csign: '记'
    })
    expect(vm.form.csign).toBe('记')
    expect(vm.form.inoId).toBe(26)
  })

  it('prompts to save changes when closing a modified detail voucher', async () => {
    mocks.elMessageBox.confirm.mockResolvedValueOnce(undefined)
    mocks.financeApi.updateVoucher.mockResolvedValue({
      data: {
        voucherNo: 'COMPANY_A~2026~4~记~12',
        companyId: 'COMPANY_A',
        iyear: 2026,
        iyperiod: 202604,
        iperiod: 4,
        csign: '记',
        inoId: 12,
        entryCount: 2,
        totalDebit: '100.00',
        totalCredit: '100.00',
        status: 'UNPOSTED',
        checkedAt: null,
        postedAt: null
      }
    })
    const wrapper = await mountView({ pageMode: 'detail', voucherNo: 'COMPANY_A~2026~4~记~12' })
    const vm = wrapper.vm as unknown as {
      form: { entries: Array<{ cdigest: string }> }
    }

    await wrapper.findAll('button').find((button) => button.text() === '修改')?.trigger('click')
    await flushPromises()
    vm.form.entries[0].cdigest = '修改后摘要'
    await flushPromises()

    const guard = mocks.financeWorkspace.registerCloseGuard.mock.calls.at(-1)?.[1] as (() => Promise<boolean>) | undefined
    expect(await guard?.()).toBe(true)
    expect(mocks.elMessageBox.confirm).toHaveBeenCalledWith(
      '当前凭证已修改，是否保存修改？',
      '关闭凭证',
      expect.objectContaining({
        confirmButtonText: '保存并关闭',
        cancelButtonText: '直接关闭'
      })
    )
    expect(mocks.financeApi.updateVoucher).toHaveBeenCalledTimes(1)
  })

  it('supports calculator keyboard entry, backspace, enter, and escape while the dialog is open', async () => {
    const wrapper = await mountView({ pageMode: 'create' })

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'F9' }))
    await flushPromises()

    const calculatorInput = wrapper.findAll('input').find((input) => input.attributes('placeholder') === '请输入算式，例如 100+20/2')
    expect(calculatorInput?.exists()).toBe(true)

    for (const key of ['1', '0', '0', '+', '2', '0', '/', '2']) {
      window.dispatchEvent(new KeyboardEvent('keydown', { key }))
    }
    await flushPromises()
    expect((calculatorInput?.element as HTMLInputElement).value).toBe('100+20/2')

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Backspace' }))
    await flushPromises()
    expect((calculatorInput?.element as HTMLInputElement).value).toBe('100+20/')

    window.dispatchEvent(new KeyboardEvent('keydown', { key: '2' }))
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }))
    await flushPromises()

    expect((calculatorInput?.element as HTMLInputElement).value).toBe('110')
    expect(wrapper.text()).toContain('结果：110')

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await flushPromises()
    expect(wrapper.text()).not.toContain('结果：110.00')
  })

  it('shows the carry-back shortcut hint and applies the calculator result with Space', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: {
        entries: Array<{ md?: string }>
      }
    }

    const firstDebitInput = wrapper.findAll('input[data-voucher-field="md"]')[0]
    await firstDebitInput?.trigger('focus')

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'F9' }))
    await flushPromises()

    const applyButton = wrapper.findAll('button').find((button) => button.text() === '带回金额框')
    expect(applyButton?.attributes('title')).toBe('带回金额框（空格）')

    for (const key of ['1', '2', '0']) {
      window.dispatchEvent(new KeyboardEvent('keydown', { key }))
    }
    await flushPromises()

    window.dispatchEvent(new KeyboardEvent('keydown', { key: ' ', code: 'Space' }))
    await flushPromises()

    expect(vm.form.entries[0]?.md).toBe('120')
    expect(wrapper.findAll('button').some((button) => button.text() === '带回金额框')).toBe(false)
    expect((document.activeElement as HTMLInputElement | null)?.getAttribute('data-voucher-field')).toBe('md')
  })

  it('adds a clear action in create mode and resets voucher content after confirmation', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: {
        ctext1: string
        entries: Array<{
          cdigest: string
          ccode: string
          ccodeName: string
          md?: string
          cdeptId?: string
          cpersonId?: string
          ccusId?: string
          csupId?: string
          citemClass?: string
          citemId?: string
          cashFlowItemId?: number
          cashFlowItemName?: string
          cashFlowSubjectCode?: string
          cashFlowSubjectName?: string
          cashFlowAmount?: string
        }>
      }
    }

    vm.form.ctext1 = '临时备注'
    Object.assign(vm.form.entries[0], {
      cdigest: '临时摘要',
      ccode: '1001',
      ccodeName: '库存现金',
      md: '100.00',
      cdeptId: 'DEPT-01',
      cpersonId: 'EMP-01',
      ccusId: 'CUS-01',
      csupId: 'SUP-01',
      citemClass: 'PCLS-01',
      citemId: 'PROJ-01',
      cashFlowItemId: 101,
      cashFlowItemName: '销售商品、提供劳务收到的现金',
      cashFlowSubjectCode: '1001',
      cashFlowSubjectName: '库存现金',
      cashFlowAmount: '100.00'
    })
    await flushPromises()

    const clearButton = wrapper.findAll('button').find((button) => button.text() === '清空')
    expect(clearButton?.exists()).toBe(true)

    await clearButton?.trigger('click')
    await flushPromises()

    expect(mocks.elMessageBox.confirm).toHaveBeenCalledWith(
      '是否清空凭证',
      '清空凭证',
      expect.objectContaining({
        confirmButtonText: '确认',
        cancelButtonText: '取消'
      })
    )
    expect(vm.form.ctext1).toBe('')
    expect(vm.form.entries[0]?.cdigest).toBe('')
    expect(vm.form.entries[0]?.ccode).toBe('')
    expect(vm.form.entries[0]?.md).toBe('')
    expect(vm.form.entries[0]?.cdeptId).toBe('')
    expect(vm.form.entries[0]?.cpersonId).toBe('')
    expect(vm.form.entries[0]?.ccusId).toBe('')
    expect(vm.form.entries[0]?.csupId).toBe('')
    expect(vm.form.entries[0]?.citemClass).toBe('')
    expect(vm.form.entries[0]?.citemId).toBe('')
    expect(vm.form.entries[0]?.cashFlowItemId).toBeUndefined()
    expect(vm.form.entries[0]?.cashFlowItemName).toBe('')
    expect(vm.form.entries[0]?.cashFlowSubjectCode).toBe('')
    expect(vm.form.entries[0]?.cashFlowSubjectName).toBe('')
    expect(vm.form.entries[0]?.cashFlowAmount).toBe('')
  })

  it('shows a delete confirmation for Ctrl+D and only deletes after confirmation', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: {
        entries: Array<{ localId: string }>
      }
    }
    const initialLength = vm.form.entries.length

    await wrapper.findAll('button').find((button) => button.text() === '插入行')?.trigger('click')
    await flushPromises()
    expect(vm.form.entries).toHaveLength(initialLength + 1)

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'd', ctrlKey: true }))
    await flushPromises()

    expect(wrapper.text()).toContain('确认删除当前分录吗？')

    await wrapper.findAll('button').find((button) => button.text() === '否')?.trigger('click')
    await flushPromises()
    expect(vm.form.entries).toHaveLength(initialLength + 1)

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'd', ctrlKey: true }))
    await flushPromises()
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }))
    await flushPromises()

    expect(vm.form.entries).toHaveLength(initialLength)
    expect(wrapper.text()).not.toContain('确认删除当前分录吗？')
  })

  it('auto balances signed amounts when pressing equals in amount fields', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: {
        entries: Array<{
          localId: string
          cdigest: string
          ccode: string
          md?: string
          mc?: string
        }>
      }
      totalDebit: string
      totalCredit: string
    }

    vm.form.entries[0].cdigest = '摘要 A'
    vm.form.entries[0].ccode = '560101'
    vm.form.entries[0].md = '100.00'
    vm.form.entries[1].cdigest = '摘要 B'
    vm.form.entries[1].ccode = '560101'
    await flushPromises()

    const creditInput = wrapper.findAll('input[data-voucher-field="mc"]')[1]
    await creditInput.trigger('keydown', { key: '=' })
    await flushPromises()
    expect(vm.form.entries[1].mc).toBe('100.00')
    expect((creditInput.element as HTMLInputElement).value).toBe('100.00')

    vm.form.entries[1].mc = ''
    await flushPromises()

    const debitInput = wrapper.findAll('input[data-voucher-field="md"]')[1]
    await debitInput.trigger('keydown', { key: '=' })
    await flushPromises()
    expect(vm.form.entries[1].md).toBe('-100.00')
    expect((debitInput.element as HTMLInputElement).value).toBe('-100.00')
  })

  it('toggles amount direction with space and only shifts focus when the current field is empty', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: {
        entries: Array<{
          cdigest: string
          ccode: string
          md?: string
          mc?: string
        }>
      }
      totalDebit: string
      totalCredit: string
    }

    vm.form.entries[0].cdigest = '摘要 A'
    vm.form.entries[0].ccode = '560101'
    vm.form.entries[0].md = '100.00'
    vm.form.entries[1].cdigest = '摘要 B'
    vm.form.entries[1].ccode = '560101'
    vm.form.entries[1].mc = '-100.00'
    await flushPromises()

    const firstDebitInput = wrapper.findAll('input[data-voucher-field="md"]')[0]
    await firstDebitInput.trigger('focus')
    await firstDebitInput.trigger('keydown', { key: ' ', code: 'Space' })
    await flushPromises()

    expect(vm.form.entries[0].md).toBe('')
    expect(vm.form.entries[0].mc).toBe('100.00')
    expect((firstDebitInput.element as HTMLInputElement).value).toBe('')
    expect((wrapper.findAll('input[data-voucher-field="mc"]')[0]?.element as HTMLInputElement).value).toBe('100.00')
    expect(vm.totalDebit).toBe('100.00')
    expect(vm.totalCredit).toBe('100.00')
    expect((document.activeElement as HTMLInputElement | null)?.getAttribute('data-voucher-field')).toBe('mc')

    const secondCreditInput = wrapper.findAll('input[data-voucher-field="mc"]')[1]
    await secondCreditInput.trigger('focus')
    await secondCreditInput.trigger('keydown', { key: ' ', code: 'Space' })
    await flushPromises()

    expect(vm.form.entries[1].md).toBe('-100.00')
    expect(vm.form.entries[1].mc).toBe('')
    expect((secondCreditInput.element as HTMLInputElement).value).toBe('')
    expect((wrapper.findAll('input[data-voucher-field="md"]')[1]?.element as HTMLInputElement).value).toBe('-100.00')
    expect(vm.totalDebit).toBe('0.00')
    expect(vm.totalCredit).toBe('200.00')
    expect((document.activeElement as HTMLInputElement | null)?.getAttribute('data-voucher-field')).toBe('md')

    vm.form.entries[0].md = ''
    vm.form.entries[0].mc = '100.00'
    await flushPromises()

    const firstDebitInputAfterClear = wrapper.findAll('input[data-voucher-field="md"]')[0]
    await firstDebitInputAfterClear.trigger('focus')
    await firstDebitInputAfterClear.trigger('keydown', { key: ' ', code: 'Space' })
    await flushPromises()

    expect(vm.form.entries[0].md).toBe('')
    expect(vm.form.entries[0].mc).toBe('100.00')
    expect((document.activeElement as HTMLInputElement | null)?.getAttribute('data-voucher-field')).toBe('mc')
  })
})




