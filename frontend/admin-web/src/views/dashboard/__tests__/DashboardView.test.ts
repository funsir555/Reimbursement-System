import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, inject, provide } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import DashboardView from '@/views/DashboardView.vue'

const mocks = vi.hoisted(() => ({
  router: {
    push: vi.fn()
  },
  dashboardApi: {
    getOverview: vi.fn()
  },
  elMessage: {
    error: vi.fn(),
    info: vi.fn(),
    warning: vi.fn()
  }
}))

vi.mock('vue-router', () => ({
  useRouter: () => mocks.router
}))

vi.mock('@/api', () => ({
  dashboardApi: mocks.dashboardApi
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage
}))

vi.mock('@/utils/permissions', () => ({
  hasAnyPermission: () => true
}))

const SimpleContainer = defineComponent({
  template: '<div><slot name="header" /><slot /><slot name="footer" /></div>'
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

async function mountView() {
  const wrapper = mount(DashboardView, {
    global: {
      stubs: {
        'el-card': SimpleContainer,
        'el-button': ButtonStub,
        'el-table': TableStub,
        'el-table-column': TableColumnStub,
        'el-tag': SimpleContainer,
        'el-icon': SimpleContainer
      },
      directives: {
        loading: () => undefined
      }
    }
  })
  await flushPromises()
  return wrapper
}

describe('DashboardView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    mocks.router.push.mockResolvedValue(undefined)
    mocks.dashboardApi.getOverview.mockResolvedValue({
      data: {
        user: {
          userId: 1,
          username: 'zhangsan',
          name: '张三',
          permissionCodes: ['dashboard:view', 'expense:approval:view', 'expense:list:view']
        },
        pendingApprovalCount: 3,
        pendingApprovalDelta: 1,
        pendingRepaymentCount: 2,
        pendingPrepayWriteOffCount: 4,
        unusedApplicationCount: 0,
        unpaidContractCount: 0,
        monthlyExpenseAmount: '0.00',
        monthlyExpenseCount: 0,
        invoiceCount: 0,
        monthlyInvoiceCount: 0,
        budgetRemaining: '0.00',
        budgetUsageRate: 0,
        recentExpenses: [
          {
            documentCode: 'DOC-001',
            no: 'DOC-001',
            documentTitle: '差旅报销单',
            templateName: '差旅报销模板',
            documentStatusLabel: '审批中',
            submittedAt: '2026-04-06 10:00',
            amount: '1200.50'
          }
        ]
      }
    })
  })

  it('renders all five hero stat labels and values', async () => {
    const wrapper = await mountView()

    expect(mocks.dashboardApi.getOverview).toHaveBeenCalledTimes(1)
    expect(wrapper.get('[data-testid="dashboard-stat-grid"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('待我审批')
    expect(wrapper.text()).toContain('待还款')
    expect(wrapper.text()).toContain('待核销')
    expect(wrapper.text()).toContain('未使用申请')
    expect(wrapper.text()).toContain('未付款合同单')
    expect(wrapper.text()).toContain('3')
    expect(wrapper.text()).toContain('2')
    expect(wrapper.text()).toContain('4')
  })

  it('keeps stat card actions working after the visibility fix', async () => {
    const wrapper = await mountView()

    await wrapper.get('[data-testid="dashboard-stat-card-approval"]').trigger('click')
    expect(mocks.router.push).toHaveBeenCalledWith('/expense/approval')

    await wrapper.get('[data-testid="dashboard-stat-card-application"]').trigger('click')
    expect(mocks.elMessage.info).toHaveBeenCalledWith('该模块暂未接入真实业务')
  })
})
