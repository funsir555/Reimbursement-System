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
    updateVoucher: vi.fn(),
    reviewVoucher: vi.fn(),
    unreviewVoucher: vi.fn(),
    markVoucherError: vi.fn(),
    clearVoucherError: vi.fn(),
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
    alert: vi.fn(),
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
  emits: ['focus', 'update:modelValue'],
  template: '<input :value="modelValue" :placeholder="placeholder" :readonly="readonly" :disabled="disabled" @focus="$emit(\'focus\')" @input="$emit(\'update:modelValue\', $event.target.value)" />'
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
    accountOptions: [
      {
        value: '1001',
        code: '1001',
        name: '库存现金',
        label: '1001  库存现金',
        leafFlag: 1,
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
        leafFlag: 0,
        bperson: 0,
        bcus: 0,
        bsup: 0,
        bdept: 0,
        bitem: 0
      }
    ],
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
    voucherNo: 'COMPANY_A~4~璁皛12',
    displayVoucherNo: '记0012',
    companyId: 'COMPANY_A',
    iperiod: 4,
    csign: '记',
    voucherTypeLabel: '记账凭证',
    inoId: 12,
    dbillDate: '2026-04-05',
    idoc: 1,
    cbill: '财务制单员',
    checkerName: '',
    ctext1: '',
    ctext2: '已有凭证',
    status: 'UNPOSTED',
    statusLabel: '未记账',
    editable: true,
    totalDebit: '100.00',
    totalCredit: '100.00',
    entries: [
      { inid: 1, cdigest: '摘要 A', ccode: '1001', ccodeName: '库存现金', md: '100.00', mc: '' },
      { inid: 2, cdigest: '摘要 B', ccode: '100201', ccodeName: '银行存款', md: '', mc: '100.00' }
    ]
  }
}

async function mountView(props: { pageMode?: 'create' | 'detail' | 'review'; voucherNo?: string } = {}) {
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
    mocks.financeApi.reviewVoucher.mockResolvedValue({
      data: {
        action: 'REVIEW',
        voucherNo: 'COMPANY_A~4~记0012',
        status: 'REVIEWED',
        statusLabel: '已审核',
        checkerName: '审核人甲',
        nextVoucherNo: 'COMPANY_A~4~璁皛13',
        lastVoucherOfMonth: false
      }
    })
    mocks.financeApi.unreviewVoucher.mockResolvedValue({
      data: {
        action: 'UNREVIEW',
        voucherNo: 'COMPANY_A~4~璁皛12',
        status: 'UNPOSTED',
        statusLabel: '未记账'
      }
    })
    mocks.financeApi.markVoucherError.mockResolvedValue({
      data: {
        action: 'MARK_ERROR',
        voucherNo: 'COMPANY_A~4~璁皛12',
        status: 'ERROR',
        statusLabel: '已标记错误'
      }
    })
    mocks.financeApi.clearVoucherError.mockResolvedValue({
      data: {
        action: 'CLEAR_ERROR',
        voucherNo: 'COMPANY_A~4~璁皛12',
        status: 'UNPOSTED',
        statusLabel: '未记账'
      }
    })
    mocks.financeApi.exportVouchers.mockResolvedValue(undefined)
  })

  it('loads voucher meta with the finance company context', async () => {
    const wrapper = await mountView({ pageMode: 'create' })

    expect(mocks.financeApi.getVoucherMeta).toHaveBeenCalledWith({ companyId: 'COMPANY_A' })
    expect(wrapper.text()).toContain('凭证编号')
    expect(wrapper.text()).toContain('备注')
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

    const wrapper = await mountView({ pageMode: 'detail', voucherNo: 'COMPANY_A~4~璁皛12' })

    expect(mocks.financeApi.getVoucherDetail).toHaveBeenCalledWith('COMPANY_A', 'COMPANY_A~4~璁皛12')
    expect(wrapper.text()).toContain('100201  银行存款')
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

    const wrapper = await mountView({ pageMode: 'review', voucherNo: 'COMPANY_A~4~璁皛12' })

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
    const wrapper = await mountView({ pageMode: 'review', voucherNo: 'COMPANY_A~4~璁皛12' })

    await wrapper.findAll('button').find((button) => button.text() === '审核')?.trigger('click')
    await flushPromises()

    expect(mocks.financeApi.reviewVoucher).toHaveBeenCalledWith('COMPANY_A', 'COMPANY_A~4~璁皛12')
    expect(mocks.router.replace).toHaveBeenCalledWith({
      name: 'finance-review-voucher-detail',
      params: { voucherNo: 'COMPANY_A~4~璁皛13' }
    })
  })

  it('shows the last-voucher message when review mode has no next voucher', async () => {
    mocks.financeApi.reviewVoucher.mockResolvedValueOnce({
      data: {
        action: 'REVIEW',
        voucherNo: 'COMPANY_A~4~璁皛12',
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

    const wrapper = await mountView({ pageMode: 'review', voucherNo: 'COMPANY_A~4~璁皛12' })

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

    const wrapper = await mountView({ pageMode: 'review', voucherNo: 'COMPANY_A~4~璁皛12' })

    expect(wrapper.text()).toContain('取消错误')

    await wrapper.findAll('button').find((button) => button.text() === '取消错误')?.trigger('click')
    await flushPromises()

    expect(mocks.financeApi.clearVoucherError).toHaveBeenCalledWith('COMPANY_A', 'COMPANY_A~4~璁皛12')
  })

  it('exports the current voucher and finds a matching row in review mode', async () => {
    const promptSpy = vi.spyOn(window, 'prompt').mockReturnValueOnce('银行')
    const wrapper = await mountView({ pageMode: 'review', voucherNo: 'COMPANY_A~4~璁皛12' })
    const vm = wrapper.vm as unknown as { selectedRow: { cdigest?: string } }

    await wrapper.findAll('button').find((button) => button.text() === '导出')?.trigger('click')
    await flushPromises()
    expect(mocks.financeApi.exportVouchers).toHaveBeenCalledWith({ companyId: 'COMPANY_A', voucherNo: 'COMPANY_A~4~璁皛12' })

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
    await flushPromises()

    expect(mocks.financeApi.getVoucherMeta).toHaveBeenCalledWith({ companyId: 'COMPANY_A' })
    expect(mocks.financeApi.getVoucherMeta).toHaveBeenCalledWith({ companyId: 'COMPANY_B' })

    wrapper.unmount()
  })

  it('saves vouchers through the current finance company context', async () => {
    mocks.financeApi.createVoucher.mockResolvedValue({
      data: {
        voucherNo: 'COMPANY_A~4~璁皛12',
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
})

