import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ExpenseTypeManagementPanel from '@/components/process/ExpenseTypeManagementPanel.vue'

const mocks = vi.hoisted(() => ({
  processApi: {
    listExpenseTypesTree: vi.fn(),
    getExpenseTypeMeta: vi.fn(),
    getExpenseTypeDetail: vi.fn(),
    createExpenseType: vi.fn(),
    updateExpenseType: vi.fn(),
    deleteExpenseType: vi.fn()
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

vi.mock('@/api', () => ({
  processApi: mocks.processApi
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage,
  ElMessageBox: mocks.elMessageBox
}))

vi.mock('@element-plus/icons-vue', () => ({
  Check: { template: '<span />' },
  CircleCheckFilled: { template: '<span />' },
  Delete: { template: '<span />' },
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

const SwitchStub = defineComponent({
  props: {
    modelValue: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue'],
  template: '<input type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)" />'
})

const TreeStub = defineComponent({
  setup(_, { expose }) {
    expose({
      setCurrentKey: () => undefined
    })
    return {}
  },
  template: '<div><slot name="default" :data="{ expenseName: \'差旅\', expenseCode: \'660001\', status: 1 }" /></div>'
})

const globalStubs = {
  'el-card': SimpleContainer,
  'el-button': ButtonStub,
  'el-input': InputStub,
  'el-tag': SimpleContainer,
  'el-icon': SimpleContainer,
  'el-tree': TreeStub,
  'el-empty': SimpleContainer,
  'el-form': SimpleContainer,
  'el-form-item': SimpleContainer,
  'el-select': SimpleContainer,
  'el-option': SimpleContainer,
  'el-switch': SwitchStub,
  'el-skeleton': SimpleContainer
}

async function mountView() {
  mocks.processApi.listExpenseTypesTree.mockResolvedValue({
    data: [
      {
        id: 1,
        expenseName: '差旅费',
        expenseCode: '660001',
        status: 1,
        children: []
      }
    ]
  })
  mocks.processApi.getExpenseTypeMeta.mockResolvedValue({
    data: {
      departmentOptions: [],
      userOptions: [],
      invoiceFreeOptions: [],
      taxDeductionOptions: [],
      taxSeparationOptions: []
    }
  })
  mocks.processApi.getExpenseTypeDetail.mockResolvedValue({
    data: {
      id: 1,
      expenseName: '差旅费',
      expenseCode: '660001',
      expenseDescription: '差旅费用类型',
      scopeDeptIds: [],
      scopeUserIds: [],
      invoiceFreeMode: 'REQUIRED',
      taxDeductionMode: 'NONE',
      taxSeparationMode: 'INCLUDED',
      status: 1
    }
  })

  const wrapper = mount(ExpenseTypeManagementPanel, {
    global: {
      stubs: globalStubs,
      directives: {
        loading: () => undefined
      }
    }
  })
  await flushPromises()
  return wrapper
}

describe('ExpenseTypeManagementPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('removes the hero area and keeps compact summary cards with the create action', async () => {
    const wrapper = await mountView()

    expect(wrapper.text()).not.toContain('Expense Type Studio')
    expect(wrapper.text()).not.toContain('左侧按树形维护费用类型，右侧集中配置基础信息、适用范围和发票税务规则，让类型维护更像一个完整的配置工作台。')
    expect(wrapper.text()).not.toContain('费用类型树中当前维护的全部节点数量')

    const summaryGrid = wrapper.get('[data-testid="expense-type-summary-grid"]')
    expect(summaryGrid.classes()).toContain('expense-wb-stat-grid--compact')
    expect(summaryGrid.text()).toContain('全部类型')
    expect(summaryGrid.text()).toContain('1')
    expect(wrapper.findAll('button').some((item) => item.text().includes('新增费用类型'))).toBe(true)
  })
})
