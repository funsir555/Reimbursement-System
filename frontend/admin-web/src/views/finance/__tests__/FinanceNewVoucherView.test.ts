import { defineComponent, nextTick, reactive } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import FinanceNewVoucherView from '@/views/finance/FinanceNewVoucherView.vue'

const routeState = reactive({
  name: 'finance-new-voucher',
  params: {} as Record<string, unknown>
})

const mocks = vi.hoisted(() => ({
  financeApi: {
    getVoucherMeta: vi.fn(),
    getVoucherDetail: vi.fn(),
    createVoucher: vi.fn(),
    updateVoucher: vi.fn()
  },
  financeCompany: {
    currentCompanyId: 'COMPANY_A',
    currentCompanyName: '广州远智教育科技有限公司',
    registerSwitchGuard: vi.fn(),
    unregisterSwitchGuard: vi.fn()
  },
  router: {
    push: vi.fn(),
    replace: vi.fn()
  },
  elMessage: {
    success: vi.fn(),
    warning: vi.fn(),
    error: vi.fn()
  },
  elMessageBox: {
    confirm: vi.fn()
  }
}))

const financeCompanyStore = reactive(mocks.financeCompany)
const mountedWrappers: Array<{ unmount: () => void }> = []

vi.mock('@/api', () => ({
  financeApi: mocks.financeApi
}))

vi.mock('@/stores/financeCompany', () => ({
  useFinanceCompanyStore: () => financeCompanyStore
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

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage,
  ElMessageBox: mocks.elMessageBox
}))

vi.mock('@element-plus/icons-vue', () => ({
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
}))

const InputStub = defineComponent({
  props: {
    modelValue: { type: [String, Number], default: '' },
    placeholder: { type: String, default: '' },
    readonly: { type: Boolean, default: false },
    disabled: { type: Boolean, default: false }
  },
  emits: ['update:modelValue'],
  template: '<input :value="modelValue" :placeholder="placeholder" :readonly="readonly" :disabled="disabled" @input="$emit(\'update:modelValue\', $event.target.value)" />'
})

const SelectStub = defineComponent({
  props: {
    modelValue: { type: [String, Number], default: '' },
    disabled: { type: Boolean, default: false }
  },
  emits: ['update:modelValue', 'visible-change'],
  template: '<select :value="modelValue" :disabled="disabled" @change="$emit(\'update:modelValue\', $event.target.value)"><slot /></select>'
})

const OptionStub = defineComponent({
  props: { label: { type: String, default: '' }, value: { type: [String, Number], default: '' } },
  template: '<option :value="value">{{ label }}</option>'
})

const ButtonStub = defineComponent({
  emits: ['click'],
  template: '<button type="button" @click="$emit(\'click\', $event)"><slot /></button>'
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
  emits: ['update:modelValue', 'focus', 'keydown'],
  template: '<input :value="modelValue" :placeholder="placeholder" :readonly="readonly" :disabled="disabled" @input="$emit(\'update:modelValue\', $event.target.value)" @focus="$emit(\'focus\')" @keydown="$emit(\'keydown\', $event)" />'
})

const mountOptions = {
  global: {
    stubs: {
      'el-button': ButtonStub,
      'el-input': InputStub,
      'el-select': SelectStub,
      'el-option': OptionStub,
      'el-input-number': NumberStub,
      'el-date-picker': InputStub,
      'el-dialog': defineComponent({ template: '<div><slot /><slot name="footer" /></div>' }),
      'el-icon': true,
      'money-input': MoneyInputStub
    }
  }
}

function buildMeta() {
  return {
    companyOptions: [{ value: 'COMPANY_A', label: '广州远智教育科技有限公司' }],
    departmentOptions: [],
    employeeOptions: [],
    voucherTypeOptions: [{ value: '记', label: '记账凭证' }],
    currencyOptions: [],
    accountOptions: [{ value: '1001', label: '1001 库存现金' }],
    customerOptions: [],
    supplierOptions: [],
    projectClassOptions: [],
    projectOptions: [],
    defaultCompanyId: 'COMPANY_A',
    defaultBillDate: '2026-04-05',
    defaultPeriod: 4,
    defaultVoucherType: '记',
    suggestedVoucherNo: 12,
    defaultMaker: '财务制单员',
    defaultAttachedDocCount: 0,
    defaultCurrency: 'CNY'
  }
}

function buildDetail() {
  return {
    voucherNo: 'COMPANY_A~4~记~12',
    displayVoucherNo: '记-0012',
    companyId: 'COMPANY_A',
    iperiod: 4,
    csign: '记',
    voucherTypeLabel: '记账凭证',
    inoId: 12,
    dbillDate: '2026-04-05',
    idoc: 1,
    cbill: '财务制单员',
    ctext1: '',
    ctext2: '已有凭证',
    status: 'UNPOSTED',
    statusLabel: '未记账',
    editable: true,
    totalDebit: '100.00',
    totalCredit: '100.00',
    entries: [
      { inid: 1, cdigest: '摘要 A', ccode: '1001', md: '100.00', mc: '' },
      { inid: 2, cdigest: '摘要 B', ccode: '1001', md: '', mc: '100.00' }
    ]
  }
}

function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

async function mountView(props: { pageMode?: 'create' | 'detail'; voucherNo?: string } = {}) {
  const wrapper = mount(FinanceNewVoucherView, {
    ...mountOptions,
    props
  })
  mountedWrappers.push(wrapper)

  await flushPromises()
  await nextTick()
  return wrapper
}

async function mountKeepAliveHost(props: { showVoucher: boolean; pageMode?: 'create' | 'detail'; voucherNo?: string }) {
  const DummyView = defineComponent({ template: '<div data-testid="dummy-view">dummy</div>' })
  const KeepAliveHost = defineComponent({
    components: { FinanceNewVoucherView, DummyView },
    props: {
      showVoucher: { type: Boolean, default: true },
      pageMode: { type: String, default: 'create' },
      voucherNo: { type: String, default: '' }
    },
    setup() {
      return { FinanceNewVoucherView, DummyView }
    },
    template: `
      <keep-alive>
        <component
          :is="showVoucher ? FinanceNewVoucherView : DummyView"
          :page-mode="pageMode"
          :voucher-no="voucherNo"
        />
      </keep-alive>
    `
  })

  const wrapper = mount(KeepAliveHost, {
    ...mountOptions,
    props
  })
  mountedWrappers.push(wrapper)

  await flushPromises()
  await nextTick()
  return wrapper
}

describe('FinanceNewVoucherView', () => {
  afterEach(() => {
    while (mountedWrappers.length) {
      mountedWrappers.pop()?.unmount()
    }
  })

  beforeEach(() => {
    vi.clearAllMocks()
    sessionStorage.clear()
    routeState.name = 'finance-new-voucher'
    routeState.params = {}
    financeCompanyStore.currentCompanyId = 'COMPANY_A'
    financeCompanyStore.currentCompanyName = '广州远智教育科技有限公司'
    mocks.financeApi.getVoucherMeta.mockResolvedValue({ data: buildMeta() })
    mocks.financeApi.getVoucherDetail.mockResolvedValue({ data: buildDetail() })
  })

  it('loads voucher meta with the global finance company', async () => {
    const wrapper = await mountView({ pageMode: 'create' })

    expect(mocks.financeApi.getVoucherMeta).toHaveBeenCalledWith({ companyId: 'COMPANY_A' })
    expect(wrapper.text()).toContain('凭证编号')
    expect(wrapper.text()).toContain('备注')
    expect(wrapper.find('input[placeholder="请输入凭证号"]').exists()).toBe(true)
    expect(wrapper.text()).not.toContain('附加说明')
  })

  it('restores the draft for the current company only', async () => {
    sessionStorage.setItem('finance-new-voucher-draft:COMPANY_A', JSON.stringify({
      companyId: 'COMPANY_A',
      iperiod: 4,
      csign: '记',
      dbillDate: '2026-04-05',
      idoc: 2,
      cbill: '张三',
      ctext1: '公司 A 草稿',
      ctext2: '',
      entries: [
        { inid: 1, cdigest: '摘要 A', ccode: '1001', md: '100.00', mc: '' },
        { inid: 2, cdigest: '摘要 B', ccode: '1001', md: '', mc: '100.00' }
      ]
    }))

    const wrapper = await mountView({ pageMode: 'create' })

    const noteInput = wrapper.find('input[placeholder="请输入备注"]')
    expect((noteInput.element as HTMLInputElement).value).toBe('公司 A 草稿')
    expect(mocks.elMessage.success).toHaveBeenCalledWith('已恢复暂存草稿')
  })

  it('loads voucher detail in readonly mode via props', async () => {
    const wrapper = await mountView({ pageMode: 'detail', voucherNo: 'COMPANY_A~4~记~12' })

    expect(mocks.financeApi.getVoucherDetail).toHaveBeenCalledWith('COMPANY_A', 'COMPANY_A~4~记~12')
    expect(wrapper.text()).toContain('凭证详情')
    expect(wrapper.text()).toContain('修改')
    expect(wrapper.text()).not.toContain('保存')
  })

  it('does not reload a cached voucher page after it is deactivated', async () => {
    const wrapper = await mountKeepAliveHost({ showVoucher: true, pageMode: 'create' })
    const initialMetaCallCount = mocks.financeApi.getVoucherMeta.mock.calls.length

    expect(initialMetaCallCount).toBeGreaterThan(0)
    expect(mocks.financeCompany.registerSwitchGuard).toHaveBeenCalledTimes(1)

    await wrapper.setProps({ showVoucher: false })
    await flushPromises()

    routeState.name = 'dashboard'
    routeState.params = {}
    financeCompanyStore.currentCompanyId = 'COMPANY_B'
    await flushPromises()

    expect(mocks.financeApi.getVoucherMeta).toHaveBeenCalledTimes(initialMetaCallCount)
    expect(mocks.financeCompany.unregisterSwitchGuard).toHaveBeenCalledWith('finance-new-voucher')
  })

  it('suppresses stale load errors after the voucher page is deactivated', async () => {
    const pendingMeta = deferred<{ data: ReturnType<typeof buildMeta> }>()
    mocks.financeApi.getVoucherMeta.mockReturnValueOnce(pendingMeta.promise)

    const wrapper = await mountKeepAliveHost({ showVoucher: true, pageMode: 'create' })

    expect(mocks.financeApi.getVoucherMeta).toHaveBeenCalledTimes(1)

    await wrapper.setProps({ showVoucher: false })
    await flushPromises()

    pendingMeta.reject(new Error('stale failure'))
    await flushPromises()

    expect(mocks.elMessage.error).not.toHaveBeenCalled()
  })


  it('filters project options by selected project class and clears mismatched project', async () => {
    mocks.financeApi.getVoucherMeta.mockResolvedValue({
      data: {
        ...buildMeta(),
        projectClassOptions: [
          { value: 'CLASS001', label: '???' },
          { value: 'CLASS002', label: '???' }
        ],
        projectOptions: [
          { value: 'PROJ001', label: '???', parentValue: 'CLASS001' },
          { value: 'PROJ002', label: '???', parentValue: 'CLASS002' }
        ]
      }
    })

    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: { entries: Array<{ citemClass?: string; citemId?: string }> }
      selectedRow: { citemClass?: string; citemId?: string }
      getFilteredProjectOptions: () => Array<{ value: string }>
    }

    vm.form.entries[0].citemClass = 'CLASS001'
    vm.form.entries[0].citemId = 'PROJ001'
    await nextTick()
    expect(vm.getFilteredProjectOptions().map((item) => item.value)).toEqual(['PROJ001'])

    vm.form.entries[0].citemClass = 'CLASS002'
    await nextTick()
    expect(vm.selectedRow.citemId).toBe('')
    expect(vm.getFilteredProjectOptions().map((item) => item.value)).toEqual(['PROJ002'])
  })
})
