import { flushPromises, mount } from '@vue/test-utils'
import { computed, defineComponent, h, inject, provide } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ExpenseVoucherGenerationView from '@/views/expense/ExpenseVoucherGenerationView.vue'

const mocks = vi.hoisted(() => ({
  expenseVoucherGenerationApi: {
    getMeta: vi.fn(),
    getTemplatePolicies: vi.fn(),
    getSubjectMappings: vi.fn(),
    getPushDocuments: vi.fn(),
    getGeneratedVouchers: vi.fn(),
    getGeneratedVoucherDetail: vi.fn(),
    createTemplatePolicy: vi.fn(),
    updateTemplatePolicy: vi.fn(),
    createSubjectMapping: vi.fn(),
    updateSubjectMapping: vi.fn(),
    pushDocuments: vi.fn()
  },
  elMessage: {
    success: vi.fn(),
    warning: vi.fn()
  },
  elMessageBox: {
    confirm: vi.fn()
  }
}))

vi.mock('@/api', () => ({
  expenseVoucherGenerationApi: mocks.expenseVoucherGenerationApi
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage,
  ElMessageBox: mocks.elMessageBox
}))

vi.mock('@/utils/permissions', () => ({
  hasPermission: (permissionCode: string, source?: string[] | { permissionCodes?: string[] } | null) => {
    const codes = Array.isArray(source)
      ? source
      : Array.isArray(source?.permissionCodes)
        ? source.permissionCodes
        : []
    return codes.includes(permissionCode)
  },
  readStoredUser: () => ({
    permissionCodes: [
      'expense:voucher_generation:view',
      'expense:voucher_generation:mapping:view',
      'expense:voucher_generation:mapping:edit',
      'expense:voucher_generation:push:view',
      'expense:voucher_generation:push:execute',
      'expense:voucher_generation:query:view'
    ]
  })
}))

const tabsKey = Symbol('tabs')

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
  template: '<button type="button" :disabled="disabled" @click="$emit(\'click\', $event)"><slot /></button>'
})

const DialogLikeStub = defineComponent({
  props: {
    modelValue: {
      type: Boolean,
      default: false
    }
  },
  template: '<div v-if="modelValue"><slot /><slot name="footer" /></div>'
})

const TableStub = defineComponent({
  props: {
    data: {
      type: Array,
      default: () => []
    }
  },
  setup(props) {
    provide('tableRows', computed(() => props.data))
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
    const rows = inject<any>('tableRows')
    return { rows }
  },
  template: `
    <div>
      <template v-for="(row, index) in rows?.value || []" :key="String(index) + prop">
        <slot :row="row">
          <span>{{ prop ? row[prop] : '' }}</span>
        </slot>
      </template>
    </div>
  `
})

const PaginationStub = defineComponent({
  template: '<div />'
})

const TabsStub = defineComponent({
  props: {
    modelValue: {
      type: String,
      default: ''
    }
  },
  emits: ['update:modelValue', 'tab-change'],
  setup(props, { attrs, emit, slots }) {
    provide(tabsKey, {
      activeTab: computed(() => props.modelValue),
      setActive: (name: string) => {
        emit('update:modelValue', name)
        emit('tab-change', name)
      }
    })
    return () => h('div', attrs, slots.default ? slots.default() : [])
  }
})

const TabPaneStub = defineComponent({
  props: {
    label: {
      type: String,
      default: ''
    },
    name: {
      type: String,
      default: ''
    }
  },
  setup() {
    const tabs = inject<{ activeTab: { value: string }; setActive: (name: string) => void }>(tabsKey)
    return { tabs }
  },
  template: `
    <section>
      <button type="button" :data-testid="'voucher-tab-' + name" @click="tabs?.setActive(name)">{{ label }}</button>
      <div v-if="tabs?.activeTab.value === name">
        <slot />
      </div>
    </section>
  `
})

async function mountView() {
  const wrapper = mount(ExpenseVoucherGenerationView, {
    global: {
      stubs: {
        'el-tabs': TabsStub,
        'el-tab-pane': TabPaneStub,
        'el-card': SimpleContainer,
        'el-button': ButtonStub,
        'el-table': TableStub,
        'el-table-column': TableColumnStub,
        'el-tag': SimpleContainer,
        'el-pagination': PaginationStub,
        'el-dialog': DialogLikeStub,
        'el-drawer': DialogLikeStub,
        'el-select': SimpleContainer,
        'el-option': true,
        'el-input': SimpleContainer,
        'el-switch': SimpleContainer,
        'el-descriptions': SimpleContainer,
        'el-descriptions-item': SimpleContainer
      },
      directives: {
        loading: () => undefined
      }
    }
  })
  await flushPromises()
  return wrapper
}

describe('ExpenseVoucherGenerationView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.expenseVoucherGenerationApi.getMeta.mockResolvedValue({
      data: {
        companyOptions: [{ value: 'COMP-001', label: '华南公司' }],
        templateOptions: [{ value: 'TMP-001', label: '差旅报销模板' }],
        expenseTypeOptions: [{ value: 'TRAVEL', label: '差旅费' }],
        accountOptions: [{ value: '6601', label: '管理费用' }],
        voucherTypeOptions: [{ value: '记', label: '记' }],
        pushStatusOptions: [{ value: 'SUCCESS', label: '成功' }],
        defaultCompanyId: 'COMP-001',
        latestBatchNo: 'BATCH-001',
        pendingPushCount: 2,
        pushedVoucherCount: 4,
        pushFailureCount: 1,
        pendingPushAmount: '1200.00'
      }
    })
    mocks.expenseVoucherGenerationApi.getTemplatePolicies.mockResolvedValue({
      data: {
        total: 1,
        page: 1,
        pageSize: 5,
        items: [
          {
            id: 1,
            companyName: '华南公司',
            templateName: '差旅报销模板',
            creditAccountName: '其他应付款',
            voucherTypeLabel: '记',
            summaryRule: '报销单摘要',
            enabled: true,
            updatedAt: '2026-04-05 10:00:00'
          }
        ]
      }
    })
    mocks.expenseVoucherGenerationApi.getSubjectMappings.mockResolvedValue({
      data: {
        total: 1,
        page: 1,
        pageSize: 5,
        items: [
          {
            id: 1,
            companyName: '华南公司',
            templateName: '差旅报销模板',
            expenseTypeName: '差旅费',
            debitAccountName: '管理费用',
            enabled: true,
            updatedAt: '2026-04-05 10:00:00'
          }
        ]
      }
    })
    mocks.expenseVoucherGenerationApi.getPushDocuments.mockResolvedValue({
      data: {
        total: 1,
        page: 1,
        pageSize: 10,
        items: [
          {
            documentCode: 'DOC-001',
            templateName: '差旅报销模板',
            submitterName: '张三',
            companyName: '华南公司',
            totalAmount: '1200.00',
            expenseSummary: '交通费',
            finishedAt: '2026-04-05 11:00:00',
            pushStatus: 'PENDING',
            pushStatusLabel: '待推送',
            failureReason: '',
            canPush: true
          }
        ]
      }
    })
    mocks.expenseVoucherGenerationApi.getGeneratedVouchers.mockResolvedValue({
      data: {
        total: 1,
        page: 1,
        pageSize: 10,
        items: [
          {
            id: 1,
            documentCode: 'DOC-001',
            templateName: '差旅报销模板',
            companyName: '华南公司',
            totalAmount: '1200.00',
            voucherNo: 'V-20260405-001',
            pushStatus: 'SUCCESS',
            pushStatusLabel: '成功',
            pushedAt: '2026-04-05 12:00:00',
            failureReason: ''
          }
        ]
      }
    })
  })

  it('removes the hero, filters, and stats while keeping tabs as the top entry', async () => {
    const wrapper = await mountView()

    expect(wrapper.get('[data-testid="voucher-generation-tabs"]').exists()).toBe(true)
    expect(wrapper.text()).not.toContain('Expense Voucher Center')
    expect(wrapper.text()).not.toContain('刷新工作台')
    expect(wrapper.text()).not.toContain('重置条件')
    expect(wrapper.text()).not.toContain('待推送单据')
    expect(wrapper.text()).not.toContain('最近批次')
    expect(wrapper.text()).not.toContain('单据号 / 模板 / 申请人')
    expect(mocks.expenseVoucherGenerationApi.getMeta).toHaveBeenCalledTimes(1)
    expect(mocks.expenseVoucherGenerationApi.getTemplatePolicies).toHaveBeenCalledTimes(1)
    expect(mocks.expenseVoucherGenerationApi.getSubjectMappings).toHaveBeenCalledTimes(1)
    expect(mocks.expenseVoucherGenerationApi.getPushDocuments).not.toHaveBeenCalled()
    expect(mocks.expenseVoucherGenerationApi.getGeneratedVouchers).not.toHaveBeenCalled()
  })

  it('keeps tab-local actions available after switching tabs', async () => {
    const wrapper = await mountView()

    expect(wrapper.get('[data-testid="voucher-mapping-add-policy"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="voucher-mapping-add-subject"]').exists()).toBe(true)

    await wrapper.get('[data-testid="voucher-tab-push"]').trigger('click')
    await flushPromises()

    expect(mocks.expenseVoucherGenerationApi.getPushDocuments).toHaveBeenCalledTimes(1)
    expect(wrapper.get('[data-testid="voucher-push-refresh"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="voucher-push-batch"]').exists()).toBe(true)

    await wrapper.get('[data-testid="voucher-tab-query"]').trigger('click')
    await flushPromises()

    expect(mocks.expenseVoucherGenerationApi.getGeneratedVouchers).toHaveBeenCalledTimes(1)
    expect(wrapper.get('[data-testid="voucher-query-refresh"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('查看')
  })

  it('loads every tab with default pagination params instead of removed filters', async () => {
    const wrapper = await mountView()

    expect(mocks.expenseVoucherGenerationApi.getTemplatePolicies).toHaveBeenCalledWith({ page: 1, pageSize: 5 })
    expect(mocks.expenseVoucherGenerationApi.getSubjectMappings).toHaveBeenCalledWith({ page: 1, pageSize: 5 })

    await wrapper.get('[data-testid="voucher-tab-push"]').trigger('click')
    await flushPromises()
    expect(mocks.expenseVoucherGenerationApi.getPushDocuments).toHaveBeenCalledWith({ page: 1, pageSize: 10 })

    await wrapper.get('[data-testid="voucher-tab-query"]').trigger('click')
    await flushPromises()
    expect(mocks.expenseVoucherGenerationApi.getGeneratedVouchers).toHaveBeenCalledWith({ page: 1, pageSize: 10 })
  })
})
