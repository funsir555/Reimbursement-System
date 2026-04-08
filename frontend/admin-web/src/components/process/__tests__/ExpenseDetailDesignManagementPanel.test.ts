import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ExpenseDetailDesignManagementPanel from '@/components/process/ExpenseDetailDesignManagementPanel.vue'

const mocks = vi.hoisted(() => ({
  router: {
    push: vi.fn()
  },
  processApi: {
    listExpenseDetailDesigns: vi.fn(),
    deleteExpenseDetailDesign: vi.fn()
  },
  elMessage: {
    warning: vi.fn(),
    error: vi.fn(),
    success: vi.fn()
  },
  elMessageBox: {
    confirm: vi.fn()
  }
}))

vi.mock('vue-router', () => ({
  useRouter: () => mocks.router
}))

vi.mock('@/api', () => ({
  processApi: mocks.processApi
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage,
  ElMessageBox: mocks.elMessageBox
}))

vi.mock('@element-plus/icons-vue', () => ({
  CircleCheckFilled: { template: '<span />' },
  Document: { template: '<span />' },
  Files: { template: '<span />' },
  Plus: { template: '<span />' },
  Search: { template: '<span />' }
}))

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

const InputStub = defineComponent({
  props: {
    modelValue: {
      type: [String, Number],
      default: ''
    }
  },
  emits: ['update:modelValue'],
  template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
})

const SegmentedStub = defineComponent({
  props: {
    modelValue: {
      type: String,
      default: ''
    },
    options: {
      type: Array,
      default: () => []
    }
  },
  emits: ['update:modelValue'],
  template: `
    <select :value="modelValue" @change="$emit('update:modelValue', $event.target.value)">
      <option v-for="item in options" :key="item.value" :value="item.value">{{ item.label }}</option>
    </select>
  `
})

const globalStubs = {
  'el-card': SimpleContainer,
  'el-button': ButtonStub,
  'el-input': InputStub,
  'el-segmented': SegmentedStub,
  'el-tag': SimpleContainer,
  'el-empty': SimpleContainer,
  'el-icon': SimpleContainer
}

async function mountView() {
  mocks.processApi.listExpenseDetailDesigns.mockResolvedValue({
    data: [
      {
        id: 1,
        detailName: '交通费明细',
        detailCode: 'ED-001',
        detailDescription: '用于差旅交通报销',
        detailType: 'NORMAL_REIMBURSEMENT',
        detailTypeLabel: '乱码占位',
        updatedAt: '2026-04-05 10:00'
      },
      {
        id: 2,
        detailName: '企业往来付款',
        detailCode: 'ED-002',
        detailDescription: '用于对公付款与预付场景',
        detailType: 'ENTERPRISE_TRANSACTION',
        detailTypeLabel: '乱码占位',
        updatedAt: '2026-04-05 11:00'
      }
    ]
  })

  const wrapper = mount(ExpenseDetailDesignManagementPanel, {
    global: {
      stubs: globalStubs
    }
  })
  await flushPromises()
  return wrapper
}

describe('ExpenseDetailDesignManagementPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.router.push.mockResolvedValue(undefined)
  })

  it('renders compact toolbar copy and prefers local detail type labels on cards', async () => {
    const wrapper = await mountView()

    expect(wrapper.text()).toContain('筛选与搜索')
    expect(wrapper.text()).toContain('新建费用明细表单')
    expect(wrapper.text()).toContain('全部设计')
    expect(wrapper.text()).toContain('普通报销')
    expect(wrapper.text()).toContain('企业往来')
    expect(wrapper.text()).not.toContain('乱码占位')

    const summaryGrid = wrapper.get('[data-testid="expense-detail-summary-grid"]')
    expect(summaryGrid.classes()).toContain('expense-wb-stat-grid--compact')
    expect(summaryGrid.text()).toContain('2')
  })

  it('keeps create action in the toolbar', async () => {
    const wrapper = await mountView()
    const createButton = wrapper.get('[data-testid="expense-detail-toolbar-create"]')

    await createButton.trigger('click')

    expect(mocks.router.push).toHaveBeenCalledWith({
      name: 'expense-workbench-process-expense-detail-create'
    })
  })

  it('navigates to create mode with copyFromId when copying a detail design', async () => {
    const wrapper = await mountView()
    const footer = wrapper.findAll('[data-testid="expense-detail-card-footer"]')[0]
    const copyButton = wrapper.findAll('[data-testid="expense-detail-copy-button"]')[0]

    expect(footer.classes()).toContain('expense-detail-design-card__footer')
    expect(copyButton.classes()).toContain('expense-detail-design-card__copy-button')

    await copyButton.trigger('click')

    expect(mocks.router.push).toHaveBeenCalledWith({
      name: 'expense-workbench-process-expense-detail-create',
      query: { copyFromId: '1' }
    })
  })
})
