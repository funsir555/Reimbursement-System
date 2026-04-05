import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, inject, provide } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ExpenseApprovalView from '@/views/expense/ExpenseApprovalView.vue'

const mocks = vi.hoisted(() => ({
  router: {
    push: vi.fn()
  },
  expenseApprovalApi: {
    listPending: vi.fn(),
    approve: vi.fn(),
    reject: vi.fn()
  },
  elMessage: {
    error: vi.fn(),
    warning: vi.fn(),
    success: vi.fn()
  },
  elMessageBox: {
    prompt: vi.fn()
  }
}))

vi.mock('vue-router', () => ({
  useRouter: () => mocks.router
}))

vi.mock('@/api', () => ({
  expenseApprovalApi: mocks.expenseApprovalApi
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage,
  ElMessageBox: mocks.elMessageBox
}))

const SimpleContainer = defineComponent({
  template: '<div><slot name="reference" /><slot name="header" /><slot /><slot name="footer" /></div>'
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
  emits: ['update:modelValue'],
  template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
})

const SelectStub = defineComponent({
  props: {
    modelValue: {
      type: String,
      default: ''
    }
  },
  emits: ['update:modelValue'],
  template: '<div><slot /></div>'
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
      <template v-for="row in rows" :key="row.documentCode + prop">
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

async function mountView() {
  const wrapper = mount(ExpenseApprovalView, {
    global: {
      stubs: {
        'el-card': SimpleContainer,
        'el-button': ButtonStub,
        'el-input': InputStub,
        'el-select': SelectStub,
        'el-option': true,
        'el-date-picker': SimpleContainer,
        'el-table': TableStub,
        'el-table-column': TableColumnStub,
        'el-tag': SimpleContainer,
        'el-pagination': PaginationStub,
        'el-icon': SimpleContainer,
        'el-popover': SimpleContainer,
        'el-checkbox': SimpleContainer
      },
      directives: {
        loading: () => undefined
      }
    }
  })
  await flushPromises()
  return wrapper
}

describe('ExpenseApprovalView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    mocks.router.push.mockResolvedValue(undefined)
    mocks.expenseApprovalApi.listPending.mockResolvedValue({
      data: [
        {
          taskId: 1,
          documentCode: 'DOC-001',
          documentTitle: '差旅审批单',
          documentReason: '上海出差',
          submitterName: '张三',
          nodeName: '财务审批',
          amount: 1880.5,
          submittedAt: '2026-04-01 10:00:00',
          taskCreatedAt: '2026-04-01 10:30:00',
          documentStatusLabel: '审批中',
          paymentCompanyName: '华南公司'
        }
      ]
    })
    mocks.elMessageBox.prompt.mockResolvedValue({ value: '同意' })
    mocks.expenseApprovalApi.approve.mockResolvedValue({})
    mocks.expenseApprovalApi.reject.mockResolvedValue({})
  })

  it('renders advanced filter and visible column actions', async () => {
    const wrapper = await mountView()

    expect(mocks.expenseApprovalApi.listPending).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('高级筛选')
    expect(wrapper.text()).toContain('显示字段')
    expect(wrapper.text()).toContain('刷新待办')
    expect(wrapper.text()).toContain('返回我的报销')
    expect(wrapper.find('[data-testid="expense-advanced-panel"]').exists()).toBe(false)

    await wrapper.get('[data-testid="expense-advanced-filter-trigger"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="expense-toolbar-main"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="expense-advanced-panel"]').classes()).toContain('expense-wb-advanced-panel--dropdown')
    expect(wrapper.get('[data-testid="expense-advanced-grid"]').classes()).toContain('expense-wb-advanced-grid--four-column')
  })

  it('supports allowed advanced filters and keeps action flow', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      filters: { paymentCompanyName: string }
      filteredItems: Array<{ documentCode: string }>
      openDetail: (documentCode: string) => void
      handleAction: (taskId: number, action: 'approve' | 'reject') => Promise<void>
    }

    vm.filters.paymentCompanyName = '华南'
    await flushPromises()
    expect(vm.filteredItems.map((item) => item.documentCode)).toEqual(['DOC-001'])

    vm.openDetail('DOC-001')
    await vm.handleAction(1, 'approve')

    expect(mocks.router.push).toHaveBeenCalledWith('/expense/documents/DOC-001')
    expect(mocks.expenseApprovalApi.approve).toHaveBeenCalledWith(1, { comment: '同意' })
  })

  it('shares column widths across pages and persists page-specific column order', async () => {
    window.localStorage.setItem('expense:workbench:column-widths', JSON.stringify({ documentCode: 260 }))
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      handleVisibleColumnsChange: (value: string[]) => void
      handleColumnDragStart: (key: string) => void
      handleColumnDrop: (key: string) => void
      visibleColumnDefinitions: Array<{ key: string; width?: number }>
    }

    vm.handleVisibleColumnsChange(['documentCode', 'taskCreatedAt'])
    vm.handleColumnDragStart('taskCreatedAt')
    vm.handleColumnDrop('documentCode')
    await flushPromises()

    expect(vm.visibleColumnDefinitions.map((item) => item.key)).toEqual(['taskCreatedAt', 'documentCode'])
    expect(vm.visibleColumnDefinitions.find((item) => item.key === 'documentCode')?.width).toBe(260)
    expect(JSON.parse(window.localStorage.getItem('expense:approval:visible-columns') || '[]')).toEqual(['taskCreatedAt', 'documentCode'])
    expect(JSON.parse(window.localStorage.getItem('expense:approval:column-order') || '[]')[0]).toBe('taskCreatedAt')
  })
})
