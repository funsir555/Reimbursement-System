import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, inject, provide } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ExpenseDocumentsView from '@/views/expense/ExpenseDocumentsView.vue'

const mocks = vi.hoisted(() => ({
  router: {
    push: vi.fn()
  },
  expenseApi: {
    queryDocuments: vi.fn()
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
  emits: ['row-dblclick'],
  setup(props) {
    provide('tableRows', props.data)
    return {}
  },
  template: `
    <div>
      <button
        v-for="row in data"
        :key="row.documentCode"
        class="row-dblclick-trigger"
        :data-document-code="row.documentCode"
        @dblclick="$emit('row-dblclick', row)"
      >
        {{ row.documentCode }}
      </button>
      <slot />
    </div>
  `
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
  const wrapper = mount(ExpenseDocumentsView, {
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

describe('ExpenseDocumentsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    mocks.router.push.mockResolvedValue(undefined)
    mocks.expenseApi.queryDocuments.mockResolvedValue({
      data: [
        {
          documentCode: 'DOC-001',
          no: 'DOC-001',
          documentTitle: '差旅审批单',
          reason: '上海出差',
          submitterName: '张三',
          templateName: '差旅报销模板',
          currentNodeName: '财务审批',
          amount: 1880.5,
          date: '2026-04-01',
          status: '审批中',
          documentStatusLabel: '审批中',
          submittedAt: '2026-04-01 10:00',
          payeeName: '李四'
        },
        {
          documentCode: 'DOC-002',
          no: 'DOC-002',
          documentTitle: '借款申请单',
          reason: '项目借款',
          submitterName: '李四',
          templateName: '借款模板',
          currentNodeName: '',
          amount: 5000,
          date: '2026-04-02',
          status: '草稿',
          documentStatusLabel: '草稿',
          submittedAt: '2026-04-02 09:00',
          payeeName: '王五'
        }
      ]
    })
    mocks.asyncTaskApi.exportExpenseScene.mockResolvedValue({ code: 200 })
  })

  it('loads query documents data and keeps only non-draft rows', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as { filteredExpenseList: Array<{ no: string; documentStatusLabel: string }> }

    expect(mocks.expenseApi.queryDocuments).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('高级筛选')
    expect(wrapper.text()).toContain('显示字段')
    expect(wrapper.text()).toContain('下载')
    expect(wrapper.text()).not.toContain('刷新列表')
    expect(wrapper.text()).not.toContain('返回待我审批')
    expect(wrapper.text()).toContain('总数 1')
    expect(wrapper.text()).toContain('已过滤 1')
    expect(wrapper.text()).not.toContain('单据查询列表')
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
    expect(vm.filteredExpenseList.map((item) => item.no)).toEqual(['DOC-001'])
    expect(vm.filteredExpenseList.some((item) => item.documentStatusLabel === '草稿')).toBe(false)
  })

  it('supports allowed advanced filters, drag sorting, and restoring default columns', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      filters: { payeeName: string }
      filteredExpenseList: Array<{ documentCode: string }>
      handleVisibleColumnsChange: (value: string[]) => void
      handleColumnDragStart: (key: string) => void
      handleColumnDrop: (key: string) => void
      restoreDefaultColumns: () => void
      visibleColumnDefinitions: Array<{ key: string }>
    }

    vm.filters.payeeName = '李四'
    await flushPromises()
    expect(vm.filteredExpenseList.map((item) => item.documentCode)).toEqual(['DOC-001'])

    vm.handleVisibleColumnsChange(['documentCode', 'submitterName'])
    vm.handleColumnDragStart('submitterName')
    vm.handleColumnDrop('documentCode')
    await flushPromises()
    expect(vm.visibleColumnDefinitions.map((item) => item.key)).toEqual(['submitterName', 'documentCode'])
    expect(JSON.parse(window.localStorage.getItem('expense:documents:visible-columns') || '[]')).toEqual(['submitterName', 'documentCode'])
    expect(JSON.parse(window.localStorage.getItem('expense:documents:column-order') || '[]')[0]).toBe('submitterName')

    vm.restoreDefaultColumns()
    await flushPromises()
    expect(vm.visibleColumnDefinitions.map((item) => item.key)).toEqual([
      'documentCode',
      'documentTitle',
      'templateName',
      'submitterName',
      'currentNodeName',
      'documentStatusLabel',
      'amount',
      'submittedAt'
    ])
  })

  it('filters query rows by clicked stat cards while keeping draft rows excluded', async () => {
    mocks.expenseApi.queryDocuments.mockResolvedValue({
      data: [
        {
          documentCode: 'DOC-001',
          no: 'DOC-001',
          documentTitle: '差旅审批单',
          reason: '上海出差',
          submitterName: '张三',
          templateName: '差旅报销模板',
          currentNodeName: '财务审批',
          amount: 1880.5,
          date: '2026-04-01',
          status: '审批中',
          documentStatusLabel: '审批中',
          submittedAt: '2026-04-01 10:00',
          payeeName: '李四'
        },
        {
          documentCode: 'DOC-003',
          no: 'DOC-003',
          documentTitle: '差旅完成单',
          reason: '深圳出差',
          submitterName: '王五',
          templateName: '差旅报销模板',
          currentNodeName: '',
          amount: 980,
          date: '2026-04-03',
          status: '待支付',
          documentStatusLabel: '待支付',
          submittedAt: '2026-04-03 10:00',
          payeeName: '赵六'
        },
        {
          documentCode: 'DOC-004',
          no: 'DOC-004',
          documentTitle: '异常单据',
          reason: '系统异常',
          submitterName: '钱七',
          templateName: '通用模板',
          currentNodeName: '系统节点',
          amount: 200,
          date: '2026-04-04',
          status: '流程异常',
          documentStatusLabel: '流程异常',
          submittedAt: '2026-04-04 11:00',
          payeeName: '孙八'
        },
        {
          documentCode: 'DOC-002',
          no: 'DOC-002',
          documentTitle: '借款申请单',
          reason: '项目借款',
          submitterName: '李四',
          templateName: '借款模板',
          currentNodeName: '',
          amount: 5000,
          date: '2026-04-02',
          status: '草稿',
          documentStatusLabel: '草稿',
          submittedAt: '2026-04-02 09:00',
          payeeName: '王五'
        }
      ]
    })

    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      filters: { documentStatusLabel: string }
      filteredExpenseList: Array<{ documentCode: string; documentStatusLabel: string }>
      currentPage: number
    }

    await wrapper.get('[data-testid="expense-documents-stat-pending"]').trigger('click')
    await flushPromises()
    expect(vm.filters.documentStatusLabel).toBe('审批中')
    expect(vm.filteredExpenseList.map((item) => item.documentCode)).toEqual(['DOC-001'])
    expect(wrapper.get('[data-testid="expense-documents-stat-pending"]').classes()).toContain('expense-wb-stat-card--filterable')
    expect(wrapper.get('[data-testid="expense-documents-stat-pending"]').classes()).toContain('expense-wb-stat-card--active')

    await wrapper.get('[data-testid="expense-documents-stat-pending-payment"]').trigger('click')
    await flushPromises()
    expect(vm.filters.documentStatusLabel).toBe('待支付')
    expect(vm.filteredExpenseList.map((item) => item.documentCode)).toEqual(['DOC-003'])

    await wrapper.get('[data-testid="expense-documents-stat-exception"]').trigger('click')
    await flushPromises()
    expect(vm.filters.documentStatusLabel).toBe('流程异常')
    expect(vm.filteredExpenseList.map((item) => item.documentCode)).toEqual(['DOC-004'])
    expect(vm.filteredExpenseList.some((item) => item.documentStatusLabel === '草稿')).toBe(false)

    vm.currentPage = 3
    await wrapper.get('[data-testid="expense-documents-stat-all"]').trigger('click')
    await flushPromises()
    expect(vm.filters.documentStatusLabel).toBe('')
    expect(vm.currentPage).toBe(1)
    expect(vm.filteredExpenseList.map((item) => item.documentCode)).toEqual(['DOC-001', 'DOC-003', 'DOC-004'])
  })

  it('opens the existing document detail page when clicking a document number', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      openDetail: (row: { documentCode: string; no: string }) => void
    }

    vm.openDetail({ documentCode: 'DOC-001', no: 'DOC-001' })

    expect(mocks.router.push).toHaveBeenCalledWith('/expense/documents/DOC-001')
  })

  it('opens the existing document detail page on row double click', async () => {
    const wrapper = await mountView()

    await wrapper.get('.row-dblclick-trigger[data-document-code="DOC-001"]').trigger('dblclick')

    expect(mocks.router.push).toHaveBeenCalledWith('/expense/documents/DOC-001')
  })

  it('submits export task with current filtered document codes', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      filters: { payeeName: string }
      handleExport: () => Promise<void>
    }

    vm.filters.payeeName = '李四'
    await flushPromises()
    await vm.handleExport()

    expect(mocks.asyncTaskApi.exportExpenseScene).toHaveBeenCalledWith({
      scene: 'DOCUMENT_QUERY',
      documentCodes: ['DOC-001']
    })
    expect(mocks.downloadCenter.openDownloadCenter).toHaveBeenCalledTimes(1)
    expect(mocks.elMessage.success).toHaveBeenCalledWith('导出任务已提交，请到下载中心查看进度')
  })
})
