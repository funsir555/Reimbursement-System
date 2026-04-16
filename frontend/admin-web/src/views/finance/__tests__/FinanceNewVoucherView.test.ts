import { computed, defineComponent, nextTick, reactive } from 'vue'
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
    currentCompanyLabel: '001  广州远智教育科技有限公司',
    currentCompanyHasActiveAccountSet: true,
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

const TreeSelectStub = defineComponent({
  props: {
    modelValue: { type: [String, Number], default: '' },
    disabled: { type: Boolean, default: false },
    data: { type: Array, default: () => [] },
    filterNodeMethod: { type: Function, default: undefined }
  },
  emits: ['update:modelValue'],
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
  template: '<select data-testid="department-tree-select" :value="modelValue" :disabled="disabled" @change="$emit(\'update:modelValue\', $event.target.value)"><option v-for="item in flattened" :key="item.value" :value="item.value">{{ item.label }}</option></select>'
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
      'el-tree-select': TreeSelectStub,
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
    companyOptions: [{ value: 'COMPANY_A', code: '001', name: '广州远智教育科技有限公司', label: '001  广州远智教育科技有限公司' }],
    departmentOptions: [
      { value: '10', code: 'D001', name: 'Finance Center', label: 'D001  Finance Center' },
      { value: '11', code: 'D002', name: 'Expense Admin', label: 'D002  Expense Admin', parentValue: '10' }
    ],
    employeeOptions: [{ value: '2', code: '2', name: '员工甲', label: '2  员工甲' }],
    voucherTypeOptions: [{ value: '记', label: '记账凭证' }],
    currencyOptions: [{ value: 'CNY', label: '人民币' }],
    accountOptions: [{ value: '1001', code: '1001', name: '库存现金', label: '1001  库存现金' }],
    customerOptions: [{ value: 'C00001', code: 'C00001', name: '华南客户', label: 'C00001  华南客户' }],
    supplierOptions: [{ value: 'V00001', code: 'V00001', name: '核心供应商', label: 'V00001  核心供应商' }],
    projectClassOptions: [{ value: '01', code: '01', name: '市场项目', label: '01  市场项目' }],
    projectOptions: [
      { value: '000001', code: '000001', name: '华南推广项目', label: '000001  华南推广项目', parentValue: '01' },
      { value: '000002', code: '000002', name: '华北推广项目', label: '000002  华北推广项目', parentValue: '02' }
    ],
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
      { inid: 1, cdigest: '摘要 A', ccode: '1001', ccodeName: '库存现金', md: '100.00', mc: '' },
      { inid: 2, cdigest: '摘要 B', ccode: '1002', ccodeName: '银行存款', md: '', mc: '100.00' }
    ]
  }
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
    financeCompanyStore.currentCompanyLabel = '001  广州远智教育科技有限公司'
    financeCompanyStore.currentCompanyHasActiveAccountSet = true
    mocks.financeApi.getVoucherMeta.mockResolvedValue({ data: buildMeta() })
    mocks.financeApi.getVoucherDetail.mockResolvedValue({ data: buildDetail() })
  })

  it('loads voucher meta with the finance company context', async () => {
    const wrapper = await mountView({ pageMode: 'create' })

    expect(mocks.financeApi.getVoucherMeta).toHaveBeenCalledWith({ companyId: 'COMPANY_A' })
    expect(wrapper.text()).toContain('凭证编号')
    expect(wrapper.text()).toContain('备注')
  })

  it('renders archive options as code on the left and name on the right', async () => {
    const wrapper = await mountView({ pageMode: 'create' })

    const optionTexts = wrapper.findAll('option').map((option) => option.text())
    expect(optionTexts).toContain('1001  库存现金')
    expect(optionTexts).toContain('C00001  华南客户')
    expect(optionTexts).toContain('V00001  核心供应商')
    expect(optionTexts).toContain('01  市场项目')
    expect(optionTexts).toContain('000001  华南推广项目')
  })

  it('filters project options by the selected project class', async () => {
    const wrapper = await mountView({ pageMode: 'create' })
    const vm = wrapper.vm as unknown as {
      form: { entries: Array<{ citemClass?: string }> }
      getFilteredProjectOptions: () => Array<{ value: string }>
    }

    vm.form.entries[0].citemClass = '01'
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

  it('shows account code and snapshot name in detail mode even when the option is missing from meta', async () => {
    const meta = buildMeta()
    meta.accountOptions = [{ value: '1001', code: '1001', name: '库存现金', label: '1001  库存现金' }]
    mocks.financeApi.getVoucherMeta.mockResolvedValue({ data: meta })

    const wrapper = await mountView({ pageMode: 'detail', voucherNo: 'COMPANY_A~4~记~12' })

    expect(mocks.financeApi.getVoucherDetail).toHaveBeenCalledWith('COMPANY_A', 'COMPANY_A~4~记~12')
    expect(wrapper.text()).toContain('1002  银行存款')
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
    await flushPromises()

    expect(mocks.financeApi.getVoucherMeta).toHaveBeenCalledWith({ companyId: 'COMPANY_A' })
    expect(mocks.financeApi.getVoucherMeta).toHaveBeenCalledWith({ companyId: 'COMPANY_B' })

    wrapper.unmount()
  })

  it('saves vouchers through the current finance company context', async () => {
    mocks.financeApi.createVoucher.mockResolvedValue({
      data: {
        voucherNo: 'COMPANY_A~4~记~12',
        companyId: 'COMPANY_A',
        iperiod: 4,
        csign: '记',
        inoId: 12,
        entryCount: 2,
        totalDebit: '100.00',
        totalCredit: '100.00',
        status: 'UNPOSTED'
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
    vm.form.entries[0].ccode = '1001'
    vm.form.entries[0].md = '100.00'
    vm.form.entries[1].cdigest = '摘要 B'
    vm.form.entries[1].ccode = '1001'
    vm.form.entries[1].mc = '100.00'
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text() === '保存')?.trigger('click')
    await flushPromises()

    expect(mocks.financeApi.createVoucher).toHaveBeenCalled()
    expect(mocks.elMessage.success).toHaveBeenCalled()
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
    vm.form.entries[0].ccode = '1001'
    vm.form.entries[0].md = '100.00'
    vm.form.entries[1].cdigest = '摘要 B'
    vm.form.entries[1].ccode = '1001'
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
    vm.form.entries[0].ccode = '1001'
    vm.form.entries[0].cexchName = 'C'.repeat(33)
    vm.form.entries[0].md = '100.00'
    vm.form.entries[1].cdigest = '\u6458\u8981 B'
    vm.form.entries[1].ccode = '1001'
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
          citemClass?: string
          citemId?: string
          md?: string
          mc?: string
        }>
      }
    }

    vm.form.entries[0].cdigest = '摘要 A'
    vm.form.entries[0].ccode = '1001'
    vm.form.entries[0].ccusId = 'C99999'
    vm.form.entries[0].md = '100.00'
    vm.form.entries[1].cdigest = '摘要 B'
    vm.form.entries[1].ccode = '1001'
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

    vm.form.entries[0].cdigest = '??? A'
    vm.form.entries[0].ccode = '1001'
    vm.form.entries[0].citemClass = '01'
    vm.form.entries[0].citemId = '000002'
    vm.form.entries[0].md = '100.00'
    vm.form.entries[1].cdigest = '??? B'
    vm.form.entries[1].ccode = '1001'
    vm.form.entries[1].mc = '100.00'
    await flushPromises()
    await nextTick()

    expect(vm.form.entries[0].citemId).toBe('')
  })
})
