import { defineComponent, nextTick, reactive } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import FinanceOpeningBalanceView from '@/views/finance/FinanceOpeningBalanceView.vue'

const financeCompanyStore = reactive({
  currentCompanyId: 'COMPANY_A',
  currentCompanyName: '测试公司A'
})

const mocks = vi.hoisted(() => ({
  openingBalanceApi: {
    getMeta: vi.fn(),
    listRows: vi.fn(),
    saveRows: vi.fn(),
    getAssistBalances: vi.fn(),
    saveAssistBalances: vi.fn(),
    openBook: vi.fn(),
    carryForward: vi.fn(),
    trialBalance: vi.fn(),
    reconcile: vi.fn()
  },
  message: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  },
  messageBox: {
    alert: vi.fn(() => Promise.resolve())
  }
}))

vi.mock('@/api', () => ({
  openingBalanceApi: mocks.openingBalanceApi
}))

vi.mock('@/stores/financeCompany', () => ({
  useFinanceCompanyStore: () => financeCompanyStore
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
  template: '<input :value="modelValue" :disabled="disabled" @input="$emit(\'update:modelValue\', $event.target.value)" @blur="$emit(\'blur\')" @change="$emit(\'change\', $event.target.value)" />'
})

const InputNumberStub = defineComponent({
  props: {
    modelValue: { type: Number, default: 0 }
  },
  emits: ['update:modelValue', 'change'],
  template: '<input type="number" :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))" @change="$emit(\'change\', Number($event.target.value))" />'
})

const SelectStub = defineComponent({
  props: {
    modelValue: { type: [String, Number], default: '' },
    disabled: { type: Boolean, default: false }
  },
  emits: ['update:modelValue', 'change'],
  template: '<select :value="modelValue" :disabled="disabled" @change="$emit(\'update:modelValue\', $event.target.value); $emit(\'change\', $event.target.value)"><slot /></select>'
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
  template: `
    <div>
      <div v-for="row in data" :key="row.subjectCode" class="table-row">
        <slot name="default" :row="row" />
      </div>
    </div>
  `
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
        'el-input-number': InputNumberStub,
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
    projectClassOptions: [{ value: '97', label: '项目大类1' }],
    projectOptions: [{ value: '2002', label: '项目1', parentValue: '97' }],
    defaultCompanyId: 'COMPANY_A',
    defaultYear: 2026,
    defaultPeriod: 4,
    defaultYearPeriod: 202604,
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
      leafFlag: 0,
      editable: false,
      assistRequired: false,
      balanceDirectionLabel: '借',
      cexchName: '人民币',
      mb: '0.00'
    },
    {
      subjectCode: '560101',
      subjectName: '广告宣传费',
      subjectLevel: 2,
      leafFlag: 1,
      editable: true,
      assistRequired: true,
      bdept: 1,
      bitem: 1,
      cassItem: '97',
      balanceDirectionLabel: '借',
      cexchName: '人民币',
      mb: '100.00'
    },
    {
      subjectCode: '100201',
      subjectName: '银行存款',
      subjectLevel: 1,
      leafFlag: 1,
      editable: true,
      assistRequired: false,
      balanceDirectionLabel: '借',
      cexchName: '人民币',
      mb: '80.00'
    }
  ]
}

describe('FinanceOpeningBalanceView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.openingBalanceApi.getMeta.mockResolvedValue({ data: buildMeta() })
    mocks.openingBalanceApi.listRows.mockResolvedValue({ data: buildRows() })
    mocks.openingBalanceApi.getAssistBalances.mockResolvedValue({
      data: [{ cdeptId: '10', citemClass: '97', citemId: '2002', mb: '100.00' }]
    })
    mocks.openingBalanceApi.saveAssistBalances.mockResolvedValue({ data: [] })
    mocks.openingBalanceApi.saveRows.mockResolvedValue({ data: buildRows() })
    mocks.openingBalanceApi.openBook.mockResolvedValue({ data: { message: '开账任务已提交' } })
    mocks.openingBalanceApi.carryForward.mockResolvedValue({ data: { message: '结转任务已提交' } })
    mocks.openingBalanceApi.trialBalance.mockResolvedValue({
      data: { balanced: true, totalDebit: '180.00', totalCredit: '180.00', difference: '0.00', abnormalSubjects: [] }
    })
    mocks.openingBalanceApi.reconcile.mockResolvedValue({
      data: { matched: true, differenceSubjects: [], missingAssistSubjects: [], illegalAssistMessages: [] }
    })
  })

  it('renders the four toolbar buttons and current company summary', async () => {
    const wrapper = await mountView()

    expect(wrapper.text()).toContain('期初余额')
    expect(wrapper.text()).toContain('开账')
    expect(wrapper.text()).toContain('结转')
    expect(wrapper.text()).toContain('试算')
    expect(wrapper.text()).toContain('对账')
    expect(wrapper.text()).toContain('测试公司A')
    expect(mocks.openingBalanceApi.listRows).toHaveBeenCalledWith({
      companyId: 'COMPANY_A',
      iyear: 2026,
      iperiod: 4
    })
  })

  it('marks assist leaf subjects as assist-required rows', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      rows: Array<{ subjectCode: string; assistRequired: boolean }>
    }

    expect(vm.rows.find((item) => item.subjectCode === '560101')?.assistRequired).toBe(true)
  })

  it('opens the assist dialog and saves assist lines back through the api', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      rows: Array<{ subjectCode: string }>
      openAssistDialog: (row: { subjectCode: string }) => Promise<void>
      saveAssistDialog: () => Promise<void>
    }

    await vm.openAssistDialog(buildRows()[1] as { subjectCode: string })
    await vm.saveAssistDialog()

    expect(mocks.openingBalanceApi.getAssistBalances).toHaveBeenCalledWith('560101', {
      companyId: 'COMPANY_A',
      iyear: 2026,
      iperiod: 4
    })
    expect(mocks.openingBalanceApi.saveAssistBalances).toHaveBeenCalled()
  })
})
