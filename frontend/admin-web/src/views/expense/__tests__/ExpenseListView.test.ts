import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, inject, provide } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ExpenseListView from '@/views/expense/ExpenseListView.vue'

const mocks = vi.hoisted(() => ({
  router: {
    push: vi.fn()
  },
  expenseApi: {
    list: vi.fn()
  },
  asyncTaskApi: {
    exportExpenseScene: vi.fn()
  },
  elMessage: {
    error: vi.fn(),
    warning: vi.fn(),
    success: vi.fn()
  },
  downloadCenter: {
    openDownloadCenter: vi.fn()
  }
}))

vi.mock('vue-router', () => ({
  useRouter: () => mocks.router
}))

vi.mock('@/api', () => ({
  expenseApi: mocks.expenseApi,
  asyncTaskApi: mocks.asyncTaskApi
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage
}))

vi.mock('@/utils/downloadCenter', () => ({
  openDownloadCenter: mocks.downloadCenter.openDownloadCenter
}))

vi.mock('@/utils/permissions', () => ({
  EXPENSE_CREATE_ENTRY_PERMISSION_CODES: ['expense:create'],
  hasAnyPermission: () => true,
  hasPermission: () => true,
  readStoredUser: () => ({ permissionCodes: ['expense:create', 'expense:list:edit', 'expense:list:delete', 'expense:list:submit'] })
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
  const wrapper = mount(ExpenseListView, {
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

describe('ExpenseListView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    mocks.router.push.mockResolvedValue(undefined)
    mocks.expenseApi.list.mockResolvedValue({
      data: [
        {
          documentCode: 'DOC-001',
          no: 'DOC-001',
          type: '差旅费',
          reason: '上海出差',
          documentTitle: '差旅审批单',
          templateName: '差旅报销模板',
          currentNodeName: '财务审批',
          amount: 1880.5,
          date: '2026-04-01',
          status: '审批中',
          documentStatusLabel: '审批中',
          submittedAt: '2026-04-01 10:00',
          paymentCompanyName: '华南公司',
          payeeName: '李四',
          counterpartyName: '广州供应商',
          submitterDeptName: '财务部',
          undertakeDepartmentNames: ['市场部'],
          tagNames: ['重点']
        },
        {
          documentCode: 'DOC-002',
          no: 'DOC-002',
          type: '办公费',
          reason: '办公采购',
          documentTitle: '办公采购单',
          templateName: '办公报销模板',
          currentNodeName: '直属主管',
          amount: 320,
          date: '2026-04-02',
          status: '草稿',
          documentStatusLabel: '草稿',
          submittedAt: '2026-04-02 09:00',
          paymentCompanyName: '华北公司',
          payeeName: '王五',
          counterpartyName: '北京供应商',
          submitterDeptName: '行政部',
          undertakeDepartmentNames: ['行政部'],
          tagNames: ['日常']
        }
      ]
    })
    mocks.asyncTaskApi.exportExpenseScene.mockResolvedValue({ code: 200 })
  })

  it('renders advanced filter and visible column actions', async () => {
    const wrapper = await mountView()

    expect(mocks.expenseApi.list).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('高级筛选')
    expect(wrapper.text()).toContain('显示字段')
    expect(wrapper.text()).toContain('新建报销')
    expect(wrapper.text()).toContain('下载')
    expect(wrapper.text()).not.toContain('刷新列表')
    expect(wrapper.text()).toContain('总数 2')
    expect(wrapper.text()).toContain('已过滤 2')
    expect(wrapper.text()).not.toContain('报销单据列表')
    expect(wrapper.text()).not.toContain('搜索单号或事由')
    expect(wrapper.find('[data-testid="expense-advanced-panel"]').exists()).toBe(false)
    expect(wrapper.classes()).toContain('expense-wb-page--dense-list')
    expect(wrapper.find('.expense-wb-stat-grid--dense').exists()).toBe(true)
    expect(wrapper.find('.expense-wb-stat-grid--list-dense').exists()).toBe(true)
    expect(wrapper.findAll('.expense-wb-stat-card--dense').length).toBeGreaterThan(0)
    expect(wrapper.find('.expense-wb-toolbar--dense').exists()).toBe(true)
    expect(wrapper.find('.expense-wb-table-shell--compact').exists()).toBe(true)

    await wrapper.get('[data-testid="expense-advanced-filter-trigger"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="expense-toolbar-main"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="expense-toolbar-main"]').classes()).toContain('expense-wb-toolbar__row--dense')
    expect(wrapper.get('[data-testid="expense-toolbar-heading"]').classes()).toContain('expense-wb-toolbar__heading--inline')
    expect(wrapper.get('[data-testid="expense-advanced-panel"]').classes()).toContain('expense-wb-advanced-panel--dropdown')
    expect(wrapper.get('[data-testid="expense-advanced-grid"]').classes()).toContain('expense-wb-advanced-grid--four-column')
  })

  it('filters only by the allowed advanced filter fields', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      filters: {
        paymentCompanyName: string
      }
      filteredExpenseList: Array<{ documentCode: string }>
    }

    vm.filters.paymentCompanyName = '华南'
    await flushPromises()

    expect(vm.filteredExpenseList.map((item) => item.documentCode)).toEqual(['DOC-001'])
    expect(wrapper.text()).not.toContain('事由筛选')
    expect(wrapper.text()).not.toContain('金额筛选')
  })

  it('persists visible columns, column order, and shared column widths', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      handleVisibleColumnsChange: (value: string[]) => void
      handleColumnDragStart: (key: string) => void
      handleColumnDrop: (key: string) => void
      handleHeaderDragEnd: (newWidth: number, oldWidth: number, column: { columnKey?: string; property?: string }) => void
      visibleColumnDefinitions: Array<{ key: string; width?: number }>
    }

    vm.handleVisibleColumnsChange(['documentCode', 'amount'])
    vm.handleColumnDragStart('amount')
    vm.handleColumnDrop('documentCode')
    vm.handleHeaderDragEnd(260, 180, { columnKey: 'documentCode' })
    await flushPromises()

    expect(vm.visibleColumnDefinitions.map((item) => item.key)).toEqual(['amount', 'documentCode'])
    expect(vm.visibleColumnDefinitions.find((item) => item.key === 'documentCode')?.width).toBe(260)
    expect(JSON.parse(window.localStorage.getItem('expense:list:visible-columns') || '[]')).toEqual(['amount', 'documentCode'])
    expect(JSON.parse(window.localStorage.getItem('expense:list:column-order') || '[]')[0]).toBe('amount')
    expect(JSON.parse(window.localStorage.getItem('expense:workbench:column-widths') || '{}').documentCode).toBe(260)

    wrapper.unmount()

    const nextWrapper = await mountView()
    const nextVm = nextWrapper.vm as unknown as {
      visibleColumnDefinitions: Array<{ key: string; width?: number }>
    }
    expect(nextVm.visibleColumnDefinitions.map((item) => item.key)).toEqual(['amount', 'documentCode'])
    expect(nextVm.visibleColumnDefinitions.find((item) => item.key === 'documentCode')?.width).toBe(260)
  })

  it('keeps existing create and detail navigation', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      goCreateExpense: () => void
      openDetail: (row: { documentCode: string; no: string }) => void
    }

    vm.goCreateExpense()
    vm.openDetail({ documentCode: 'DOC-001', no: 'DOC-001' })

    expect(mocks.router.push).toHaveBeenCalledWith('/expense/create')
    expect(mocks.router.push).toHaveBeenCalledWith('/expense/documents/DOC-001')
  })

  it('submits export task with filtered document codes', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      filters: { paymentCompanyName: string }
      handleExport: () => Promise<void>
    }

    vm.filters.paymentCompanyName = '华南'
    await flushPromises()
    await vm.handleExport()

    expect(mocks.asyncTaskApi.exportExpenseScene).toHaveBeenCalledWith({
      scene: 'MY_EXPENSES',
      documentCodes: ['DOC-001']
    })
    expect(mocks.downloadCenter.openDownloadCenter).toHaveBeenCalledTimes(1)
    expect(mocks.elMessage.success).toHaveBeenCalledWith('导出任务已提交，请到下载中心查看进度')
  })
})
