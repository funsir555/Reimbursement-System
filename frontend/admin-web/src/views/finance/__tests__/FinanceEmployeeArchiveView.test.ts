import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, inject, provide, reactive } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import FinanceEmployeeArchiveView from '@/views/finance/FinanceEmployeeArchiveView.vue'

const mocks = vi.hoisted(() => ({
  financeArchiveApi: {
    getEmployeeMeta: vi.fn(),
    queryEmployees: vi.fn()
  },
  financeCompany: {
    currentCompanyId: 'COMPANY_A',
    currentCompanyName: '广州测试公司'
  },
  elMessage: {
    error: vi.fn()
  }
}))

const financeCompanyStore = reactive(mocks.financeCompany)

vi.mock('@/api', () => ({
  financeArchiveApi: mocks.financeArchiveApi
}))

vi.mock('@/stores/financeCompany', () => ({
  useFinanceCompanyStore: () => financeCompanyStore
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage
}))

const SimpleStub = defineComponent({
  template: '<div><slot /><slot name="append" /><slot name="footer" /><slot name="title" /></div>'
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
      <template v-for="row in rows" :key="String((row.userId || '') + prop)">
        <slot :row="row">
          <span>{{ prop ? row[prop] : '' }}</span>
        </slot>
      </template>
    </div>
  `
})

const PaginationStub = defineComponent({
  props: {
    currentPage: {
      type: Number,
      default: 1
    },
    pageSize: {
      type: Number,
      default: 10
    },
    pageSizes: {
      type: Array,
      default: () => []
    }
  },
  emits: ['update:currentPage', 'update:pageSize'],
  template: `
    <div data-testid="pagination">
      <span data-testid="pagination-current">{{ currentPage }}</span>
      <span data-testid="pagination-size">{{ pageSize }}</span>
      <button data-testid="pagination-next" type="button" @click="$emit('update:currentPage', currentPage + 1)">next</button>
      <button
        v-for="size in pageSizes"
        :key="size"
        type="button"
        :data-testid="\`pagination-size-\${size}\`"
        @click="$emit('update:pageSize', size)"
      >
        {{ size }}
      </button>
    </div>
  `
})

async function mountView() {
  const wrapper = mount(FinanceEmployeeArchiveView, {
    global: {
      stubs: {
        'el-card': SimpleStub,
        'el-button': SimpleStub,
        'el-input': SimpleStub,
        'el-select': SimpleStub,
        'el-option': true,
        'el-table': TableStub,
        'el-table-column': TableColumnStub,
        'el-tag': SimpleStub,
        'el-drawer': SimpleStub,
        'el-pagination': PaginationStub
      },
      directives: {
        loading: () => undefined
      }
    }
  })
  await flushPromises()
  return wrapper
}

describe('FinanceEmployeeArchiveView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    financeCompanyStore.currentCompanyId = 'COMPANY_A'
    financeCompanyStore.currentCompanyName = '广州测试公司'
    mocks.financeArchiveApi.getEmployeeMeta.mockResolvedValue({
      data: {
        departments: [
          {
            id: 10,
            deptName: '财务中心',
            companyId: 'COMPANY_A',
            children: [
              {
                id: 11,
                deptName: '费用管理部',
                companyId: 'COMPANY_A',
                children: []
              }
            ]
          }
        ]
      }
    })
    mocks.financeArchiveApi.queryEmployees.mockResolvedValue({
      data: Array.from({ length: 12 }, (_, index) => ({
        userId: index + 1,
        name: index === 0 ? '张三' : `员工${index + 1}`,
        username: `user${index + 1}`,
        phone: `138000000${String(index).padStart(2, '0')}`,
        email: `user${index + 1}@test.com`,
        deptName: '费用管理部',
        companyName: '广州测试公司',
        position: '专员',
        laborRelationBelong: '广州',
        sourceType: index % 2 === 0 ? 'MANUAL' : 'WECOM',
        status: 1,
        roleCodes: ['finance-user'],
        syncManaged: true,
        lastSyncAt: '2026-04-15T10:20:30'
      }))
    })
  })

  it('shows the first 10 employees by default', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      employees: Array<{ name: string }>
      paginatedEmployees: Array<{ name: string }>
    }

    expect(mocks.financeArchiveApi.getEmployeeMeta).toHaveBeenCalledTimes(1)
    expect(mocks.financeArchiveApi.queryEmployees).toHaveBeenCalledWith({
      keyword: undefined,
      companyId: 'COMPANY_A',
      deptId: undefined,
      status: undefined
    })
    expect(vm.employees).toHaveLength(12)
    expect(vm.paginatedEmployees).toHaveLength(10)
    expect(wrapper.get('[data-testid="pagination-current"]').text()).toBe('1')
  })

  it('changes the visible employee count when the page size changes', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      paginatedEmployees: Array<{ name: string }>
    }

    await wrapper.get('[data-testid="pagination-size-20"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="pagination-size"]').text()).toBe('20')
    expect(vm.paginatedEmployees).toHaveLength(12)
  })

  it('returns to page 1 after filters reset', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      paginatedEmployees: Array<{ username: string }>
      resetFilters: () => void
    }

    await wrapper.get('[data-testid="pagination-next"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="pagination-current"]').text()).toBe('2')

    vm.resetFilters()
    await flushPromises()

    expect(wrapper.get('[data-testid="pagination-current"]').text()).toBe('1')
    expect(vm.paginatedEmployees).toHaveLength(10)
  })

  it('opens the detail drawer with a paginated employee row', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      paginatedEmployees: Array<Record<string, unknown>>
      openDetail: (row: Record<string, unknown>) => void
      currentEmployee?: Record<string, unknown>
    }

    await wrapper.get('[data-testid="pagination-next"]').trigger('click')
    await flushPromises()

    vm.openDetail(vm.paginatedEmployees[0]!)
    await flushPromises()

    expect(vm.currentEmployee?.username).toBe('user11')
    expect(wrapper.text()).toContain('user11')
  })
})
