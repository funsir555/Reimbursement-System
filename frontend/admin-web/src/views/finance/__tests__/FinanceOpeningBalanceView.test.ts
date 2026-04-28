import { defineComponent, nextTick, reactive } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import FinanceOpeningBalanceView from '@/views/finance/FinanceOpeningBalanceView.vue'

const EXPANDED_STORAGE_KEY = 'finance-opening-balance-expanded-keys'

const financeCompanyStore = reactive({
  currentCompanyId: 'COMPANY_A',
  currentCompanyName: '测试公司A',
  registerSwitchGuard: vi.fn(),
  unregisterSwitchGuard: vi.fn(),
  applyCurrentCompany: vi.fn()
})

const financePeriodStore = reactive({
  currentYear: 2026,
  currentPeriod: 6,
  currentYearPeriod: 202606,
  hasPeriodContext: true,
  switchPeriod: vi.fn()
})

const mocks = vi.hoisted(() => ({
  openingBalanceApi: {
    getMeta: vi.fn(),
    listRows: vi.fn(),
    saveRows: vi.fn(),
    getAssistBalances: vi.fn(),
    saveAssistBalances: vi.fn(),
    commit: vi.fn(),
    openBook: vi.fn(),
    carryForward: vi.fn(),
    carryForwardPreview: vi.fn(),
    trialBalance: vi.fn(),
    reconcile: vi.fn()
  },
  message: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  },
  messageBox: {
    alert: vi.fn(() => Promise.resolve()),
    confirm: vi.fn(() => Promise.resolve())
  }
}))

vi.mock('@/api', () => ({
  openingBalanceApi: mocks.openingBalanceApi
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
    ElMessage: mocks.message,
    ElMessageBox: mocks.messageBox
  }
})

const InputStub = defineComponent({
  props: {
    modelValue: { type: [String, Number], default: '' },
    disabled: { type: Boolean, default: false }
  },
  emits: ['update:modelValue', 'blur', 'change'],
  template:
    '<input :value="modelValue" :disabled="disabled" @input="$emit(\'update:modelValue\', $event.target.value)" @blur="$emit(\'blur\')" @change="$emit(\'change\', $event.target.value)" />'
})

const SelectStub = defineComponent({
  props: {
    modelValue: { type: [String, Number], default: '' },
    disabled: { type: Boolean, default: false }
  },
  emits: ['update:modelValue', 'change'],
  template:
    '<select :value="modelValue" :disabled="disabled" @change="$emit(\'update:modelValue\', $event.target.value); $emit(\'change\', $event.target.value)"><slot /></select>'
})

const OptionStub = defineComponent({
  props: {
    label: { type: String, default: '' },
    value: { type: [String, Number], default: '' }
  },
  template: '<option :value="value">{{ label }}</option>'
})

const ButtonStub = defineComponent({
  props: {
    disabled: { type: Boolean, default: false }
  },
  emits: ['click'],
  template: '<button type="button" :disabled="disabled" @click="$emit(\'click\', $event)"><slot /></button>'
})

const TableStub = defineComponent({
  props: {
    data: { type: Array, default: () => [] }
  },
  template: '<div class="table-stub"><slot /><div v-for="row in data" :key="row.subjectCode">{{ row.subjectCode }}</div></div>'
})

const TableColumnStub = defineComponent({
  template: '<div><slot :row="{}" :$index="0" /></div>'
})

async function mountView() {
  const wrapper = mount(FinanceOpeningBalanceView, {
    global: {
      directives: {
        loading: () => undefined
      },
      stubs: {
        'el-button': ButtonStub,
        'el-input': InputStub,
        'el-select': SelectStub,
        'el-option': OptionStub,
        'el-table': TableStub,
        'el-table-column': TableColumnStub,
        'el-dialog': defineComponent({ template: '<div><slot /><slot name="footer" /></div>' })
      }
    }
  })
  await flushPromises()
  await nextTick()
  return wrapper
}

function buildMeta(opened = true) {
  return {
    companyOptions: [{ value: 'COMPANY_A', label: '测试公司A' }],
    departmentOptions: [{ value: '10', label: '财务部' }],
    employeeOptions: [{ value: '2', label: '员工甲' }],
    customerOptions: [],
    supplierOptions: [],
    projectClassOptions: [{ value: '97', label: '项目分类1' }],
    projectOptions: [{ value: '2002', label: '项目1', parentValue: '97' }],
    defaultCompanyId: 'COMPANY_A',
    defaultYear: 2026,
    defaultPeriod: 6,
    defaultYearPeriod: 202606,
    status: opened ? 'OPENED' : 'NOT_OPENED',
    statusLabel: opened ? '已开账' : '未开账',
    opened
  }
}

function buildRows() {
  return [
    {
      subjectCode: '5601',
      subjectName: '管理费用',
      subjectLevel: 1,
      sortOrder: 5601,
      leafFlag: 0,
      hasChildren: true,
      editable: false,
      assistRequired: false,
      balanceDirectionLabel: '借',
      cexchName: '人民币',
      mb: '180.00',
      children: [
        {
          subjectCode: '560101',
          parentSubjectCode: '5601',
          subjectName: '广告宣传费',
          subjectLevel: 2,
          sortOrder: 560101,
          leafFlag: 1,
          hasChildren: false,
          editable: true,
          assistRequired: true,
          bdept: 1,
          bitem: 1,
          cassItem: '97',
          balanceDirectionLabel: '借',
          cexchName: '人民币',
          mb: '100.00',
          children: []
        }
      ]
    },
    {
      subjectCode: '100201',
      subjectName: '银行存款',
      subjectLevel: 1,
      sortOrder: 100201,
      leafFlag: 1,
      hasChildren: false,
      editable: true,
      assistRequired: false,
      balanceDirectionLabel: '借',
      cexchName: '人民币',
      mb: '80.00',
      children: []
    }
  ]
}

describe('FinanceOpeningBalanceView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    financeCompanyStore.currentCompanyId = 'COMPANY_A'
    financeCompanyStore.currentCompanyName = '测试公司A'
    financePeriodStore.currentYear = 2026
    financePeriodStore.currentPeriod = 6
    financePeriodStore.currentYearPeriod = 202606
    financePeriodStore.hasPeriodContext = true
    mocks.openingBalanceApi.getMeta.mockResolvedValue({ data: buildMeta() })
    mocks.openingBalanceApi.listRows.mockResolvedValue({ data: buildRows() })
    mocks.openingBalanceApi.getAssistBalances.mockResolvedValue({
      data: [{ cdeptId: '10', citemClass: '97', citemId: '2002', mb: '100.00' }]
    })
    mocks.openingBalanceApi.commit.mockResolvedValue({ data: buildRows() })
    mocks.openingBalanceApi.openBook.mockResolvedValue({ data: { message: '开账任务已提交' } })
    mocks.openingBalanceApi.carryForwardPreview.mockResolvedValue({
      data: {
        rows: buildRows(),
        assistLines: [{ subjectCode: '560101', lines: [{ cdeptId: '10', citemClass: '97', citemId: '2002', mb: '100.00' }] }]
      }
    })
    mocks.openingBalanceApi.trialBalance.mockResolvedValue({
      data: { balanced: true, totalDebit: '180.00', totalCredit: '180.00', difference: '0.00', abnormalSubjects: [] }
    })
    mocks.openingBalanceApi.reconcile.mockResolvedValue({
      data: { matched: true, differenceSubjects: [], missingAssistSubjects: [], illegalAssistMessages: [] }
    })
  })

  it('renders compact summary area and top save button', async () => {
    const wrapper = await mountView()

    expect(wrapper.find('.ob-summary-card').exists()).toBe(true)
    expect(wrapper.find('.ob-filter-card').exists()).toBe(false)
    expect(wrapper.text()).toContain('保存')
    expect(wrapper.text()).toContain('测试公司A')
    expect(mocks.openingBalanceApi.listRows).toHaveBeenCalledWith({
      companyId: 'COMPANY_A',
      iyear: 2026,
      iperiod: 6
    })
  })

  it('does not save main row immediately on edit and saves through commit instead', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      rows: Array<{ subjectCode: string; draftBalance: string; mb: string }>
      handleRowDraftChange: (row: { subjectCode: string; draftBalance: string; mb: string }, value: string) => void
      saveAll: () => Promise<void>
    }

    const bankRow = vm.rows.find((item) => item.subjectCode === '100201')
    expect(bankRow).toBeTruthy()

    vm.handleRowDraftChange(bankRow!, '99.00')
    expect(mocks.openingBalanceApi.saveRows).not.toHaveBeenCalled()

    await vm.saveAll()
    expect(mocks.openingBalanceApi.commit).toHaveBeenCalled()
  })

  it('keeps assist dialog changes in local draft and does not call direct assist save api', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      rows: Array<{ children?: Array<{ subjectCode: string; cassItem?: string; mb?: string; draftBalance?: string }> }>
      openAssistDialog: (row: { subjectCode: string; cassItem?: string; mb?: string; draftBalance?: string }) => Promise<void>
      saveAssistDialog: () => void
      assistLines: Array<{ mb: string }>
    }

    await vm.openAssistDialog(vm.rows[0].children?.[0] as { subjectCode: string; cassItem?: string; mb?: string; draftBalance?: string })
    vm.assistLines[0].mb = '120.00'
    vm.saveAssistDialog()

    expect(mocks.openingBalanceApi.getAssistBalances).toHaveBeenCalledWith('560101', {
      companyId: 'COMPANY_A',
      iyear: 2026,
      iperiod: 6
    })
    expect(mocks.openingBalanceApi.saveAssistBalances).not.toHaveBeenCalled()
  })

  it('uses carry-forward preview instead of direct carry-forward task', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      runCarryForwardPreview: () => Promise<void>
    }

    await vm.runCarryForwardPreview()

    expect(mocks.openingBalanceApi.carryForwardPreview).toHaveBeenCalledWith({
      companyId: 'COMPANY_A',
      iyear: 2026,
      iperiod: 6
    })
    expect(mocks.openingBalanceApi.carryForward).not.toHaveBeenCalled()
  })

  it('blocks trial and reconcile when there are unsaved drafts', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      rows: Array<{ subjectCode: string; draftBalance: string; mb: string }>
      handleRowDraftChange: (row: { subjectCode: string; draftBalance: string; mb: string }, value: string) => void
      runTrial: () => Promise<void>
      runReconcile: () => Promise<void>
    }

    const bankRow = vm.rows.find((item) => item.subjectCode === '100201')
    vm.handleRowDraftChange(bankRow!, '99.00')

    await vm.runTrial()
    await vm.runReconcile()

    expect(mocks.openingBalanceApi.trialBalance).not.toHaveBeenCalled()
    expect(mocks.openingBalanceApi.reconcile).not.toHaveBeenCalled()
    expect(mocks.message.warning).toHaveBeenCalled()
  })

  it('updates save status after commit success', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      rows: Array<{ subjectCode: string; draftBalance: string; mb: string }>
      handleRowDraftChange: (row: { subjectCode: string; draftBalance: string; mb: string }, value: string) => void
      saveAll: () => Promise<void>
      saveStatusText: string
      saveStatusHint: string
    }

    const bankRow = vm.rows.find((item) => item.subjectCode === '100201')
    vm.handleRowDraftChange(bankRow!, '99.00')
    expect(vm.saveStatusText).toBe('期初余额待保存')

    await vm.saveAll()

    expect(vm.saveStatusText).toBe('已保存')
    expect(vm.saveStatusHint).toContain('最近保存于')
  })

  it('restores and persists expanded row keys by company and period', async () => {
    localStorage.setItem(
      EXPANDED_STORAGE_KEY,
      JSON.stringify({
        'COMPANY_A:202606': ['5601']
      })
    )

    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      rows: Array<{ subjectCode: string }>
      expandedRowKeys: string[]
      handleExpandChange: (row: { subjectCode: string }, expandedRows: Array<{ subjectCode: string }>) => void
    }

    expect(vm.expandedRowKeys).toEqual(['5601'])

    vm.handleExpandChange(vm.rows[0] as { subjectCode: string }, [])

    const persisted = JSON.parse(localStorage.getItem(EXPANDED_STORAGE_KEY) || '{}')
    expect(persisted['COMPANY_A:202606']).toEqual([])
  })
})
