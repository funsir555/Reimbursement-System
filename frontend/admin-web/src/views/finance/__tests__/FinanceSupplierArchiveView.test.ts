import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, inject, provide, reactive } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import FinanceSupplierArchiveView from '@/views/finance/FinanceSupplierArchiveView.vue'

const mocks = vi.hoisted(() => ({
  financeArchiveApi: {
    listSuppliers: vi.fn(),
    getSupplierDetail: vi.fn(),
    createSupplier: vi.fn(),
    updateSupplier: vi.fn(),
    disableSupplier: vi.fn()
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
      'finance:archives:suppliers:view',
      'finance:archives:suppliers:create',
      'finance:archives:suppliers:edit',
      'finance:archives:suppliers:delete'
    ]
  })
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
      <template v-for="row in rows" :key="String((row.cVenCode || '') + prop)">
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
  const wrapper = mount(FinanceSupplierArchiveView, {
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
        'el-pagination': PaginationStub,
        SupplierPaymentInfoFields: true
      },
      directives: {
        loading: () => undefined
      }
    }
  })
  await flushPromises()
  return wrapper
}

describe('FinanceSupplierArchiveView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    financeCompanyStore.currentCompanyId = 'COMPANY_A'
    financeCompanyStore.currentCompanyName = '广州远智教育科技有限公司'
    mocks.financeArchiveApi.listSuppliers.mockResolvedValue({
      data: Array.from({ length: 12 }, (_, index) => ({
        cVenCode: `VEN${String(index + 1).padStart(3, '0')}`,
        cVenName: index === 0 ? '核心供应商' : `Vendor ${index + 1}`,
        cVenAbbName: index === 0 ? '核心简称' : `V${index + 1}`,
        cVenPerson: '李四',
        cVenPhone: '020-88888888',
        active: true,
        companyId: 'COMPANY_A'
      }))
    })
    mocks.financeArchiveApi.getSupplierDetail.mockResolvedValue({
      data: {
        cVenCode: 'VEN001',
        cVenName: '核心供应商',
        cVenAbbName: '核心简称',
        receiptAccountName: '核心收款名',
        cVenBank: '招商银行',
        cVenAccount: '6222000012345678',
        companyId: 'COMPANY_A',
        active: true
      }
    })
    mocks.financeArchiveApi.createSupplier.mockResolvedValue({ data: { cVenCode: 'VEN001' } })
    mocks.financeArchiveApi.updateSupplier.mockResolvedValue({ data: { cVenCode: 'VEN001' } })
    mocks.elMessageBox.confirm.mockResolvedValue(undefined)
  })

  it('loads supplier list rows with code, name and abbreviation', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      vendors: Array<{ cVenCode: string; cVenName: string; cVenAbbName: string }>
      paginatedVendors: Array<{ cVenCode: string }>
    }

    expect(mocks.financeArchiveApi.listSuppliers).toHaveBeenCalledWith({
      companyId: 'COMPANY_A',
      keyword: '',
      includeDisabled: false
    })
    expect(vm.vendors).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          cVenCode: 'VEN001',
          cVenName: '核心供应商',
          cVenAbbName: '核心简称'
        })
      ])
    )
    expect(vm.vendors[0]?.cVenCode).toBe('VEN001')
    expect(vm.vendors[0]?.cVenName).toBe('核心供应商')
    expect(vm.vendors[0]?.cVenAbbName).toBe('核心简称')
    expect(vm.paginatedVendors).toHaveLength(10)
  })

  it('paginates supplier rows locally and keeps actions on the visible page', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      paginatedVendors: Array<{ cVenCode: string }>
      disableSupplier: (vendorCode: string) => Promise<void>
    }

    await wrapper.get('[data-testid="pagination-next"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="pagination-current"]').text()).toBe('2')
    expect(vm.paginatedVendors).toHaveLength(2)
    expect(vm.paginatedVendors[0]?.cVenCode).toBe('VEN011')

    await vm.disableSupplier(vm.paginatedVendors[0]!.cVenCode)

    expect(mocks.financeArchiveApi.disableSupplier).toHaveBeenCalledWith('COMPANY_A', 'VEN011')
  })

  it('updates the visible supplier count when the page size changes', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      paginatedVendors: Array<{ cVenCode: string }>
    }

    await wrapper.get('[data-testid="pagination-size-20"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="pagination-size"]').text()).toBe('20')
    expect(vm.paginatedVendors).toHaveLength(12)
  })

  it('keeps vendor section order as basic, bank, contact, finance', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      vendorSections: Array<{ key: string }>
    }

    expect(vm.vendorSections.map((item) => item.key)).toEqual(['basic', 'bank', 'contact', 'finance'])
  })

  it('expands only basic and bank sections by default for create dialog', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      openCreateDialog: () => void
      activeSections: string[]
    }

    vm.openCreateDialog()

    expect(vm.activeSections).toEqual(['basic', 'bank'])
    expect(vm.activeSections).not.toContain('contact')
    expect(vm.activeSections).not.toContain('finance')
  })

  it('opens supplier detail with the list code and hydrates the edit form', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      openEditDialog: (vendorCode: string) => Promise<void>
      vendorForm: Record<string, string | number | undefined>
      activeSections: string[]
    }

    await vm.openEditDialog('VEN001')

    expect(mocks.financeArchiveApi.getSupplierDetail).toHaveBeenCalledWith('COMPANY_A', 'VEN001')
    expect(vm.vendorForm.cVenCode).toBe('VEN001')
    expect(vm.vendorForm.cVenName).toBe('核心供应商')
    expect(vm.vendorForm.cVenAbbName).toBe('核心简称')
    expect(vm.vendorForm.receiptAccountName).toBe('核心收款名')
    expect(vm.activeSections).toEqual(['basic', 'bank'])
    expect(vm.activeSections).not.toContain('contact')
    expect(vm.activeSections).not.toContain('finance')
  })

  it('updates supplier records with the same camel-case payload contract', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      openEditDialog: (vendorCode: string) => Promise<void>
      vendorForm: Record<string, string | number | undefined>
      saveSupplier: () => Promise<void>
    }

    await vm.openEditDialog('VEN001')
    vm.vendorForm.cVenName = '深圳供应商'
    vm.vendorForm.cVenAbbName = '深圳简称'
    vm.vendorForm.receiptAccountName = '深圳收款名'
    await vm.saveSupplier()

    expect(mocks.financeArchiveApi.updateSupplier).toHaveBeenCalledWith(
      'COMPANY_A',
      'VEN001',
      expect.objectContaining({
        companyId: 'COMPANY_A',
        cVenName: '深圳供应商',
        cVenAbbName: '深圳简称',
        receiptAccountName: '深圳收款名'
      })
    )
    expect(mocks.elMessage.success).toHaveBeenCalledWith('供应商档案已更新')
  })

  it('creates supplier records with the current company context', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      openCreateDialog: () => void
      vendorForm: Record<string, string | number | undefined>
      saveSupplier: () => Promise<void>
    }

    vm.openCreateDialog()
    vm.vendorForm.cVenName = '深圳供应商'
    vm.vendorForm.cVenCode = 'VEN001'
    await vm.saveSupplier()

    expect(mocks.financeArchiveApi.createSupplier).toHaveBeenCalledWith(
      'COMPANY_A',
      expect.objectContaining({
        companyId: 'COMPANY_A',
        cVenName: '深圳供应商',
        cVenCode: 'VEN001'
      })
    )
    expect(mocks.elMessage.success).toHaveBeenCalledWith('供应商档案已创建')
  })

  it('blocks supplier save when a bank account field exceeds the tightened length limit', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      openCreateDialog: () => void
      vendorForm: Record<string, string | number | undefined>
      saveSupplier: () => Promise<void>
    }

    vm.openCreateDialog()
    vm.vendorForm.cVenName = '深圳供应商'
    vm.vendorForm.receiptAccountName = 'A'.repeat(129)
    await vm.saveSupplier()

    expect(mocks.financeArchiveApi.createSupplier).not.toHaveBeenCalled()
    expect(mocks.elMessage.warning).toHaveBeenCalledWith('账户名最多 128 个字符')
  })

  it('blocks supplier save when a tightened field exceeds the limit', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      openCreateDialog: () => void
      vendorForm: Record<string, string | number | undefined>
      saveSupplier: () => Promise<void>
    }

    vm.openCreateDialog()
    vm.vendorForm.cVenName = '深圳供应商'
    vm.vendorForm.cVenCode = 'V'.repeat(65)
    await vm.saveSupplier()

    expect(mocks.financeArchiveApi.createSupplier).not.toHaveBeenCalled()
    expect(mocks.elMessage.warning).toHaveBeenCalledWith('供应商编码最多 64 个字符')
  })
})
