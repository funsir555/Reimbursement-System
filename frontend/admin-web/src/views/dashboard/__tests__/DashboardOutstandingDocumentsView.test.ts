import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, inject, provide } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import DashboardOutstandingDocumentsView from '@/views/dashboard/DashboardOutstandingDocumentsView.vue'

const mocks = vi.hoisted(() => ({
  router: {
    push: vi.fn()
  },
  dashboardApi: {
    listOutstandingDocuments: vi.fn(),
    bindWriteoff: vi.fn()
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
  dashboardApi: mocks.dashboardApi,
  asyncTaskApi: mocks.asyncTaskApi
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage
}))

vi.mock('@/utils/downloadCenter', () => ({
  openDownloadCenter: mocks.downloadCenter.openDownloadCenter
}))

vi.mock('@/utils/money', async () => {
  const actual = await vi.importActual<typeof import('@/utils/money')>('@/utils/money')
  return actual
})

const SimpleContainer = defineComponent({
  template: '<div><slot name="reference" /><slot name="header" /><slot /><slot name="footer" /></div>'
})

const ButtonStub = defineComponent({
  emits: ['click'],
  template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
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
      <template v-for="row in rows" :key="String(row.documentCode || row.no || '') + prop">
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
  const wrapper = mount(DashboardOutstandingDocumentsView, {
    props: {
      kind: 'LOAN'
    },
    global: {
      stubs: {
        'el-card': SimpleContainer,
        'el-button': ButtonStub,
        'el-table': TableStub,
        'el-table-column': TableColumnStub,
        'el-pagination': PaginationStub,
        'el-tag': SimpleContainer,
        'el-icon': SimpleContainer,
        'dashboard-writeoff-picker-dialog': SimpleContainer
      },
      directives: {
        loading: () => undefined
      }
    }
  })
  await flushPromises()
  return wrapper
}

describe('DashboardOutstandingDocumentsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.router.push.mockResolvedValue(undefined)
    mocks.dashboardApi.listOutstandingDocuments.mockResolvedValue({
      data: [
        {
          documentCode: 'DOC-001',
          no: 'DOC-001',
          documentTitle: 'Travel',
          templateName: 'Template A',
          documentStatusLabel: 'APPROVING',
          submittedAt: '2026-04-06 09:00',
          amount: '1200.00',
          outstandingAmount: '800.00'
        }
      ]
    })
    mocks.dashboardApi.bindWriteoff.mockResolvedValue({})
    mocks.asyncTaskApi.exportExpenseScene.mockResolvedValue({ code: 200 })
  })

  it('renders the compact layout hooks for outstanding documents', async () => {
    const wrapper = await mountView()

    expect(mocks.dashboardApi.listOutstandingDocuments).toHaveBeenCalledWith('LOAN')
    expect(wrapper.get('[data-testid="outstanding-page"]').classes()).toContain('expense-wb-page--dense-list')
    expect(wrapper.get('[data-testid="outstanding-stat-grid"]').classes()).toContain('expense-wb-stat-grid--list-dense')
    expect(wrapper.get('[data-testid="outstanding-toolbar-main"]').classes()).toContain('expense-wb-toolbar__row--dense')
    expect(wrapper.get('[data-testid="outstanding-toolbar-heading"]').classes()).toContain('expense-wb-toolbar__heading--inline')
    expect(wrapper.get('[data-testid="outstanding-table-shell"]').classes()).toContain('expense-wb-table-shell--compact')
  })

  it('keeps dashboard and detail navigation working', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      goDashboard: () => void
      openDetail: (row: { documentCode: string; no: string }) => void
    }

    vm.goDashboard()
    vm.openDetail({ documentCode: 'DOC-001', no: 'DOC-001' })

    expect(mocks.router.push).toHaveBeenCalledWith('/dashboard')
    expect(mocks.router.push).toHaveBeenCalledWith('/expense/documents/DOC-001')
  })

  it('exports current outstanding documents with page kind', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      handleExport: () => Promise<void>
    }

    await vm.handleExport()

    expect(mocks.asyncTaskApi.exportExpenseScene).toHaveBeenCalledWith({
      scene: 'OUTSTANDING',
      documentCodes: ['DOC-001'],
      kind: 'LOAN'
    })
    expect(mocks.downloadCenter.openDownloadCenter).toHaveBeenCalledTimes(1)
    expect(mocks.elMessage.success).toHaveBeenCalledWith('导出任务已提交，请到下载中心查看进度')
  })
})
