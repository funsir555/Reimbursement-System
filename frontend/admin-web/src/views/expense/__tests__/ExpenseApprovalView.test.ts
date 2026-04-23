import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, inject, provide } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ExpenseApprovalView from '@/views/expense/ExpenseApprovalView.vue'

const mocks = vi.hoisted(() => ({
  route: {
    fullPath: '/expense/approval'
  },
  router: {
    push: vi.fn()
  },
  expenseApi: {
    getDetail: vi.fn()
  },
  expenseApprovalApi: {
    listPending: vi.fn(),
    approve: vi.fn(),
    reject: vi.fn()
  },
  asyncTaskApi: {
    exportExpenseScene: vi.fn()
  },
  elMessage: {
    error: vi.fn(),
    warning: vi.fn(),
    success: vi.fn()
  },
  elMessageBox: {
    prompt: vi.fn()
  },
  downloadCenter: {
    openDownloadCenter: vi.fn()
  }
}))

vi.mock('vue-router', () => ({
  useRoute: () => mocks.route,
  useRouter: () => mocks.router
}))

vi.mock('@/api', () => ({
  expenseApi: mocks.expenseApi,
  expenseApprovalApi: mocks.expenseApprovalApi,
  asyncTaskApi: mocks.asyncTaskApi
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessage: mocks.elMessage,
    ElMessageBox: mocks.elMessageBox
  }
})

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
  template: '<input v-bind="$attrs" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
})

const SelectStub = defineComponent({
  props: {
    modelValue: {
      type: String,
      default: ''
    }
  },
  emits: ['update:modelValue'],
  template: '<select v-bind="$attrs" :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><slot /></select>'
})

const OptionStub = defineComponent({
  props: {
    label: {
      type: String,
      default: ''
    },
    value: {
      type: String,
      default: ''
    }
  },
  template: '<option :value="value">{{ label }}</option>'
})

const DialogStub = defineComponent({
  props: {
    modelValue: {
      type: Boolean,
      default: false
    }
  },
  template: '<div v-if="modelValue"><slot /><slot name="footer" /></div>'
})

const FormItemStub = defineComponent({
  props: {
    label: {
      type: String,
      default: ''
    }
  },
  template: '<label><span>{{ label }}</span><slot /></label>'
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
  const wrapper = mount(ExpenseApprovalView, {
    global: {
      stubs: {
        'el-card': SimpleContainer,
        'el-button': ButtonStub,
        'el-input': InputStub,
        'el-select': SelectStub,
        'el-option': OptionStub,
        'el-date-picker': SimpleContainer,
        'el-table': TableStub,
        'el-table-column': TableColumnStub,
        'el-tag': SimpleContainer,
        'el-pagination': PaginationStub,
        'el-icon': SimpleContainer,
        'el-popover': SimpleContainer,
        'el-checkbox': SimpleContainer,
        'el-dialog': DialogStub,
        'el-form-item': FormItemStub
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
    mocks.route.fullPath = '/expense/approval'
    mocks.router.push.mockResolvedValue(undefined)
    mocks.expenseApprovalApi.listPending.mockResolvedValue({
      data: [
        {
          taskId: 1,
          documentCode: 'DOC-001',
          documentTitle: '差旅审批单',
          documentReason: '上海出差',
          submitterName: '张三',
          nodeKey: 'finance',
          nodeName: '财务审批',
          status: 'PENDING',
          amount: 1880.5,
          submittedAt: '2026-04-01 10:00:00',
          taskCreatedAt: '2026-04-01 10:30:00',
          documentStatusLabel: '审批中',
          paymentCompanyName: '华南公司'
        }
      ]
    })
    mocks.expenseApi.getDetail.mockResolvedValue({
      data: {
        flowSnapshot: {
          nodes: []
        }
      }
    })
    mocks.elMessageBox.prompt.mockResolvedValue({ value: '同意' })
    mocks.expenseApprovalApi.approve.mockResolvedValue({})
    mocks.expenseApprovalApi.reject.mockResolvedValue({})
    mocks.asyncTaskApi.exportExpenseScene.mockResolvedValue({ code: 200 })
  })

  it('renders advanced filter and visible column actions', async () => {
    const wrapper = await mountView()

    expect(mocks.expenseApprovalApi.listPending).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('高级筛选')
    expect(wrapper.text()).toContain('显示字段')
    expect(wrapper.text()).toContain('下载')
    expect(wrapper.text()).not.toContain('刷新待办')
    expect(wrapper.text()).not.toContain('返回我的报销')
    expect(wrapper.text()).toContain('待审 1')
    expect(wrapper.text()).toContain('金额合计 ¥ 1,880.50')
    expect(wrapper.text()).not.toContain('审批任务列表')
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

    expect(mocks.router.push).toHaveBeenCalledWith({
      path: '/expense/documents/DOC-001',
      query: { returnTo: '/expense/approval' }
    })
    expect(mocks.expenseApprovalApi.approve).toHaveBeenCalledWith(1, { comment: '同意' })
  })

  it('opens detail on row double click while approval actions do not navigate to detail', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      handleAction: (taskId: number, action: 'approve' | 'reject') => Promise<void>
    }

    await wrapper.get('.row-dblclick-trigger[data-document-code="DOC-001"]').trigger('dblclick')
    expect(mocks.router.push).toHaveBeenCalledWith({
      path: '/expense/documents/DOC-001',
      query: { returnTo: '/expense/approval' }
    })

    mocks.router.push.mockClear()
    await vm.handleAction(1, 'approve')
    expect(mocks.router.push).not.toHaveBeenCalled()
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

  it('submits export task with filtered task ids', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      filters: { paymentCompanyName: string }
      handleExport: () => Promise<void>
    }

    vm.filters.paymentCompanyName = '华南'
    await flushPromises()
    await vm.handleExport()

    expect(mocks.asyncTaskApi.exportExpenseScene).toHaveBeenCalledWith({
      scene: 'PENDING_APPROVAL',
      taskIds: [1]
    })
    expect(mocks.downloadCenter.openDownloadCenter).toHaveBeenCalledTimes(1)
    expect(mocks.elMessage.success).toHaveBeenCalledWith('导出任务已提交，请到下载中心查看进度')
  })

  it('opens the reject dialog with target node options and submits targetNodeKey when configured', async () => {
    mocks.expenseApi.getDetail.mockResolvedValue({
      data: {
        flowSnapshot: {
          nodes: [
            {
              nodeKey: 'finance',
              nodeName: '财务审批',
              nodeType: 'APPROVAL',
              config: {
                specialSettings: ['REJECT_TO_ANY_NODE']
              }
            },
            {
              nodeKey: 'leader',
              nodeName: '部门负责人审批',
              nodeType: 'APPROVAL',
              config: {}
            },
            {
              nodeKey: 'payment',
              nodeName: '付款处理',
              nodeType: 'PAYMENT',
              config: {}
            }
          ]
        },
        approvalNodeStatuses: [
          {
            nodeKey: 'leader',
            nodeName: '部门负责人审批',
            nodeType: 'APPROVAL',
            status: 'APPROVED',
            assigneeNames: ['李四']
          },
          {
            nodeKey: 'finance',
            nodeName: '财务审批',
            nodeType: 'APPROVAL',
            status: 'PENDING'
          },
          {
            nodeKey: 'payment',
            nodeName: '付款处理',
            nodeType: 'PAYMENT',
            status: 'NOT_REACHED'
          }
        ]
      }
    })

    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      handleAction: (taskId: number, action: 'approve' | 'reject') => Promise<void>
    }

    await vm.handleAction(1, 'reject')
    await flushPromises()

    expect(wrapper.text()).toContain('驳回到提单人')
    expect(wrapper.text()).toContain('部门负责人审批（李四）')
    expect(wrapper.text()).toContain('驳回到节点')

    await wrapper.get('input[placeholder="请输入驳回原因"]').setValue('退回补充材料')
    const selects = wrapper.findAll('select')
    await selects[selects.length - 1]!.setValue('leader')
    const rejectButtons = wrapper.findAll('button').filter((item) => item.text() === '驳回')
    await rejectButtons[rejectButtons.length - 1]!.trigger('click')
    await flushPromises()

    expect(mocks.expenseApprovalApi.reject).toHaveBeenCalledWith(1, {
      comment: '退回补充材料',
      targetNodeKey: 'leader'
    })
    expect(mocks.elMessage.success).toHaveBeenCalledWith('审批已驳回')
  })
})
