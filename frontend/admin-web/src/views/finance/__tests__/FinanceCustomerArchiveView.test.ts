import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, inject, provide, reactive } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import FinanceCustomerArchiveView from '@/views/finance/FinanceCustomerArchiveView.vue'

const mocks = vi.hoisted(() => ({
  financeArchiveApi: {
    listCustomers: vi.fn(),
    getCustomerDetail: vi.fn(),
    createCustomer: vi.fn(),
    updateCustomer: vi.fn(),
    disableCustomer: vi.fn()
  },
  financeCompany: {
    currentCompanyId: 'COMPANY_A',
    currentCompanyName: '广州远智教育科技有限公司',
    registerSwitchGuard: vi.fn(),
    unregisterSwitchGuard: vi.fn()
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

vi.mock('@/api', () => ({
  financeArchiveApi: mocks.financeArchiveApi
}))

vi.mock('@/stores/financeCompany', () => ({
  useFinanceCompanyStore: () => financeCompanyStore
}))

vi.mock('@/utils/permissions', () => ({
  hasPermission: () => true,
  readStoredUser: () => ({
    permissionCodes: [
      'finance:archives:customers:view',
      'finance:archives:customers:create',
      'finance:archives:customers:edit',
      'finance:archives:customers:delete'
    ]
  })
}))

vi.mock('@/utils/money', () => ({
  formatMoney: (value: string | number) => String(value || '0.00')
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage,
  ElMessageBox: mocks.elMessageBox
}))

const SimpleContainer = defineComponent({
  template: '<div><slot name="reference" /><slot /><slot name="footer" /></div>'
})

const ButtonStub = defineComponent({
  emits: ['click'],
  template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
})

const InputStub = defineComponent({
  props: {
    modelValue: {
      type: [String, Number],
      default: ''
    }
  },
  emits: ['update:modelValue', 'keyup.enter'],
  template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
})

const SwitchStub = defineComponent({
  props: {
    modelValue: {
      type: [Boolean, Number],
      default: false
    }
  },
  emits: ['update:modelValue', 'change'],
  template: '<input type="checkbox" :checked="Boolean(modelValue)" @change="$emit(\'update:modelValue\', $event.target.checked); $emit(\'change\', $event.target.checked)" />'
})

const NumberStub = defineComponent({
  props: {
    modelValue: {
      type: [String, Number],
      default: ''
    }
  },
  emits: ['update:modelValue'],
  template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value || 0))" />'
})

const TableStub = defineComponent({
  props: {
    data: {
      type: Array,
      default: () => []
    }
  },
  setup(props) {
    provide('tableRows', props.data)
    return {}
  },
  template: '<div><slot /></div>'
})

const TableColumnStub = defineComponent({
  props: {
    prop: {
      type: String,
      default: ''
    }
  },
  setup() {
    const rows = inject<any[]>('tableRows', [])
    return { rows }
  },
  template: `
    <div>
      <template v-for="row in rows" :key="String((row.cCusCode || '') + prop)">
        <slot :row="row">
          <span>{{ prop ? row[prop] : '' }}</span>
        </slot>
      </template>
    </div>
  `
})

const MoneyInputStub = defineComponent({
  props: {
    modelValue: {
      type: String,
      default: ''
    }
  },
  emits: ['update:modelValue'],
  template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
})

async function mountView() {
  const wrapper = mount(FinanceCustomerArchiveView, {
    global: {
      stubs: {
        'el-card': SimpleContainer,
        'el-button': ButtonStub,
        'el-input': InputStub,
        'el-switch': SwitchStub,
        'el-table': TableStub,
        'el-table-column': TableColumnStub,
        'el-tag': SimpleContainer,
        'el-dialog': SimpleContainer,
        'el-form': SimpleContainer,
        'el-form-item': SimpleContainer,
        'el-collapse': SimpleContainer,
        'el-collapse-item': SimpleContainer,
        'el-input-number': NumberStub,
        'el-date-picker': InputStub,
        'el-icon': true,
        'money-input': MoneyInputStub
      },
      directives: {
        loading: () => undefined
      }
    }
  })
  await flushPromises()
  return wrapper
}

async function mountKeepAliveHost(showCustomer = true) {
  const DummyView = defineComponent({ template: '<div data-testid="dummy-view">dummy</div>' })
  const KeepAliveHost = defineComponent({
    props: {
      showCustomer: {
        type: Boolean,
        default: true
      }
    },
    setup() {
      return { FinanceCustomerArchiveView, DummyView }
    },
    template: `
      <keep-alive>
        <component :is="showCustomer ? FinanceCustomerArchiveView : DummyView" />
      </keep-alive>
    `
  })

  const wrapper = mount(KeepAliveHost, {
    props: { showCustomer },
    global: {
      stubs: {
        'el-card': SimpleContainer,
        'el-button': ButtonStub,
        'el-input': InputStub,
        'el-switch': SwitchStub,
        'el-table': TableStub,
        'el-table-column': TableColumnStub,
        'el-tag': SimpleContainer,
        'el-dialog': SimpleContainer,
        'el-form': SimpleContainer,
        'el-form-item': SimpleContainer,
        'el-collapse': SimpleContainer,
        'el-collapse-item': SimpleContainer,
        'el-input-number': NumberStub,
        'el-date-picker': InputStub,
        'el-icon': true,
        'money-input': MoneyInputStub
      },
      directives: {
        loading: () => undefined
      }
    }
  })

  await flushPromises()
  return wrapper
}

describe('FinanceCustomerArchiveView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    financeCompanyStore.currentCompanyId = 'COMPANY_A'
    financeCompanyStore.currentCompanyName = '广州远智教育科技有限公司'
    mocks.financeArchiveApi.listCustomers.mockResolvedValue({
      data: [
        {
          cCusCode: 'CUS001',
          cCusName: '广州客户',
          cCusAbbName: '广州客',
          cCusPerson: '张三',
          cCusHand: '13800000000',
          cCusBank: '建设银行',
          cCusAccount: '6222000012345678',
          iARMoney: '1200.00',
          companyId: 'COMPANY_A',
          active: true,
          updatedAt: '2026-04-05 10:00:00'
        }
      ]
    })
    mocks.financeArchiveApi.createCustomer.mockResolvedValue({ data: { cCusCode: 'CUS001' } })
    mocks.elMessageBox.confirm.mockResolvedValue(undefined)
  })

  it('loads customers with the current finance company', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as { customers: Array<{ cCusName: string }> }

    expect(mocks.financeArchiveApi.listCustomers).toHaveBeenCalledWith({
      companyId: 'COMPANY_A',
      keyword: '',
      includeDisabled: false
    })
    expect(wrapper.text()).toContain('客户档案')
    expect(vm.customers.map((item) => item.cCusName)).toEqual(['广州客户'])
  })

  it('creates customer records with the current company context', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      openCreateDialog: () => void
      customerForm: Record<string, string | number | undefined>
      saveCustomer: () => Promise<void>
    }

    vm.openCreateDialog()
    vm.customerForm.cCusName = '深圳客户'
    vm.customerForm.iARMoney = '100.00'
    await vm.saveCustomer()

    expect(mocks.financeArchiveApi.createCustomer).toHaveBeenCalledWith(
      'COMPANY_A',
      expect.objectContaining({
        companyId: 'COMPANY_A',
        cCusName: '深圳客户',
        iARMoney: '100.00'
      })
    )
    expect(mocks.elMessage.success).toHaveBeenCalledWith('客户档案已创建')
  })

  it('registers a company switch guard while editing', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as { openCreateDialog: () => void }

    expect(mocks.financeCompany.registerSwitchGuard).toHaveBeenCalledTimes(1)
    const [, guard] = mocks.financeCompany.registerSwitchGuard.mock.calls[0]

    vm.openCreateDialog()
    mocks.elMessageBox.confirm.mockRejectedValueOnce(new Error('cancel'))
    await expect(guard()).resolves.toBe(false)

    mocks.elMessageBox.confirm.mockResolvedValueOnce(undefined)
    await expect(guard()).resolves.toBe(true)

    wrapper.unmount()
    expect(mocks.financeCompany.unregisterSwitchGuard).toHaveBeenCalledWith('finance-customer-archive')
  })

  it('registers the company switch guard only while the page is active', async () => {
    const wrapper = await mountKeepAliveHost(true)

    expect(mocks.financeCompany.registerSwitchGuard).toHaveBeenCalledTimes(1)

    await wrapper.setProps({ showCustomer: false })
    await flushPromises()
    expect(mocks.financeCompany.unregisterSwitchGuard).toHaveBeenCalledWith('finance-customer-archive')

    await wrapper.setProps({ showCustomer: true })
    await flushPromises()
    expect(mocks.financeCompany.registerSwitchGuard).toHaveBeenCalledTimes(2)
  })
})
