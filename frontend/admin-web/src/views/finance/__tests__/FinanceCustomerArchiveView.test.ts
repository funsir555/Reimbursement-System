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
    },
    pageSizes: {
      type: Array,
      default: () => []
    }
  },
  emits: ['update:currentPage', 'update:pageSize'],
  template: `
    <div data-testid="pagination">
      <span data-testid="pagination-total">{{ total }}</span>
      <span data-testid="pagination-current">{{ currentPage }}</span>
      <span data-testid="pagination-size">{{ pageSize }}</span>
      <button data-testid="pagination-next" type="button" @click="$emit('update:currentPage', currentPage + 1)">next</button>
      <button
        v-for="size in pageSizes"
        :key="size"
        type="button"
        :data-testid="\`pagination-size-\${size}\`"
        @click="$emit('update:pageSize', size)"
      >
        {{ size }}
      </button>
    </div>
  `
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
        'money-input': MoneyInputStub,
        'el-pagination': PaginationStub
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
        'money-input': MoneyInputStub,
        'el-pagination': PaginationStub
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
      data: Array.from({ length: 12 }, (_, index) => ({
        cCusCode: `CUS${String(index + 1).padStart(3, '0')}`,
        cCusName: index === 0 ? '广州客户' : `Customer ${index + 1}`,
        cCusAbbName: index === 0 ? '广州简称' : `C${index + 1}`,
        cCusPerson: '张三',
        cCusHand: '13800000000',
        cCusBank: '建设银行',
        cCusAccount: '6222000012345678',
        iARMoney: '1200.00',
        companyId: 'COMPANY_A',
        active: true,
        updatedAt: '2026-04-05 10:00:00'
      }))
    })
    mocks.financeArchiveApi.getCustomerDetail.mockResolvedValue({
      data: {
        cCusCode: 'CUS001',
        cCusName: '广州客户',
        cCusAbbName: '广州简称',
        cCusPerson: '张三',
        cCusHand: '13800000000',
        companyId: 'COMPANY_A',
        active: true
      }
    })
    mocks.financeArchiveApi.createCustomer.mockResolvedValue({ data: { cCusCode: 'CUS001' } })
    mocks.financeArchiveApi.updateCustomer.mockResolvedValue({ data: { cCusCode: 'CUS001' } })
    mocks.elMessageBox.confirm.mockResolvedValue(undefined)
  })

  it('loads customer list rows with code, name and abbreviation', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      customers: Array<{ cCusCode: string; cCusName: string; cCusAbbName: string }>
      paginatedCustomers: Array<{ cCusCode: string }>
    }

    expect(mocks.financeArchiveApi.listCustomers).toHaveBeenCalledWith({
      companyId: 'COMPANY_A',
      keyword: '',
      includeDisabled: false
    })
    expect(vm.customers).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          cCusCode: 'CUS001',
          cCusName: '广州客户',
          cCusAbbName: '广州简称'
        })
      ])
    )
    expect(vm.customers[0]?.cCusCode).toBe('CUS001')
    expect(vm.customers[0]?.cCusName).toBe('广州客户')
    expect(vm.customers[0]?.cCusAbbName).toBe('广州简称')
    expect(vm.paginatedCustomers).toHaveLength(10)
    expect(wrapper.get('[data-testid="pagination-total"]').text()).toBe('12')
  })

  it('paginates customer rows locally and resets to the first page after reset', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      paginatedCustomers: Array<{ cCusCode: string }>
      resetFilters: () => void
    }

    await wrapper.get('[data-testid="pagination-next"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="pagination-current"]').text()).toBe('2')
    expect(vm.paginatedCustomers).toHaveLength(2)
    expect(vm.paginatedCustomers[0]?.cCusCode).toBe('CUS011')

    vm.resetFilters()
    await flushPromises()

    expect(wrapper.get('[data-testid="pagination-current"]').text()).toBe('1')
    expect(vm.paginatedCustomers).toHaveLength(10)
  })

  it('updates the visible row count when the page size changes', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      paginatedCustomers: Array<{ cCusCode: string }>
    }

    await wrapper.get('[data-testid="pagination-size-20"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="pagination-size"]').text()).toBe('20')
    expect(wrapper.get('[data-testid="pagination-current"]').text()).toBe('1')
    expect(vm.paginatedCustomers).toHaveLength(12)
  })

  it('opens customer detail with the list code and hydrates the edit form', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      openEditDialog: (customerCode: string) => Promise<void>
      customerForm: Record<string, string | number | undefined>
    }

    await vm.openEditDialog('CUS001')

    expect(mocks.financeArchiveApi.getCustomerDetail).toHaveBeenCalledWith('COMPANY_A', 'CUS001')
    expect(vm.customerForm.cCusCode).toBe('CUS001')
    expect(vm.customerForm.cCusName).toBe('广州客户')
    expect(vm.customerForm.cCusAbbName).toBe('广州简称')
  })

  it('updates customer records with the same camel-case payload contract', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      openEditDialog: (customerCode: string) => Promise<void>
      customerForm: Record<string, string | number | undefined>
      saveCustomer: () => Promise<void>
    }

    await vm.openEditDialog('CUS001')
    vm.customerForm.cCusName = '深圳客户'
    vm.customerForm.cCusAbbName = '深圳简称'
    await vm.saveCustomer()

    expect(mocks.financeArchiveApi.updateCustomer).toHaveBeenCalledWith(
      'COMPANY_A',
      'CUS001',
      expect.objectContaining({
        companyId: 'COMPANY_A',
        cCusName: '深圳客户',
        cCusAbbName: '深圳简称'
      })
    )
    expect(mocks.elMessage.success).toHaveBeenCalledWith('客户档案已更新')
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

  it('blocks customer save when a tightened field exceeds the limit', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      openCreateDialog: () => void
      customerForm: Record<string, string | number | undefined>
      saveCustomer: () => Promise<void>
    }

    vm.openCreateDialog()
    vm.customerForm.cCusName = '深圳客户'
    vm.customerForm.cCusCode = 'C'.repeat(65)
    await vm.saveCustomer()

    expect(mocks.financeArchiveApi.createCustomer).not.toHaveBeenCalled()
    expect(mocks.elMessage.warning).toHaveBeenCalledWith('客户编码最多 64 个字符')
  })

  it('blocks customer save when a bank field exceeds the tightened length limit', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      openCreateDialog: () => void
      customerForm: Record<string, string | number | undefined>
      saveCustomer: () => Promise<void>
    }

    vm.openCreateDialog()
    vm.customerForm.cCusName = '深圳客户'
    vm.customerForm.cCusBank = 'B'.repeat(129)
    await vm.saveCustomer()

    expect(mocks.financeArchiveApi.createCustomer).not.toHaveBeenCalled()
    expect(mocks.elMessage.warning).toHaveBeenCalledWith('开户银行最多 128 个字符')
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
