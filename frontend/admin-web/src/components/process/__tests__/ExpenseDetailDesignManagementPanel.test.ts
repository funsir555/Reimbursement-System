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
        detailName: '交通明细',
        detailCode: 'ED-001',
        detailDescription: '交通费用明细',
        detailType: 'NORMAL_REIMBURSEMENT',
        detailTypeLabel: '普通报销',
        updatedAt: '2026-04-05 10:00'
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

  it('removes the hero area, keeps compact summary cards, and moves create into the toolbar', async () => {
    const wrapper = await mountView()

    expect(wrapper.text()).not.toContain('Detail Form Studio')
    expect(wrapper.text()).not.toContain('为报销模板维护独立的费用明细子表单。普通报销与企业往来共用同一套设计能力，但可以分别配置不同结构。')
    expect(wrapper.text()).not.toContain('当前可用于模板绑定的费用明细设计总数')

    const summaryGrid = wrapper.get('[data-testid="expense-detail-summary-grid"]')
    expect(summaryGrid.classes()).toContain('expense-wb-stat-grid--compact')
    expect(summaryGrid.text()).toContain('全部设计')
    expect(summaryGrid.text()).toContain('1')

    const createButton = wrapper.get('[data-testid="expense-detail-toolbar-create"]')
    expect(createButton.text()).toContain('新建费用明细表单')

    await createButton.trigger('click')

    expect(mocks.router.push).toHaveBeenCalledWith({
      name: 'expense-workbench-process-expense-detail-create'
    })
  })
})
