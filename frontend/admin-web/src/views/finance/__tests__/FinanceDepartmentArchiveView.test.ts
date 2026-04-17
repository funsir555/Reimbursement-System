import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, inject, provide, reactive } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { routeCatalog } from '@/router/route-catalog'
import FinanceDepartmentArchiveView from '@/views/finance/FinanceDepartmentArchiveView.vue'

const mocks = vi.hoisted(() => ({
  financeArchiveApi: {
    getDepartmentArchiveMeta: vi.fn(),
    queryDepartments: vi.fn()
  },
  financeCompany: {
    currentCompanyId: 'COMPANY_A',
    currentCompanyName: '广州测试公司',
    currentCompanyLabel: 'COMP202604050001 - 广州测试公司'
  },
  elMessage: {
    error: vi.fn()
  }
}))

const financeCompanyStore = reactive(mocks.financeCompany)

vi.mock('@/api', async () => {
  const actual = await vi.importActual<typeof import('@/api')>('@/api')
  return {
    ...actual,
    financeArchiveApi: mocks.financeArchiveApi
  }
})

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
      <template v-for="row in rows" :key="String((row.id || '') + prop)">
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
  const wrapper = mount(FinanceDepartmentArchiveView, {
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

describe('FinanceDepartmentArchiveView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    financeCompanyStore.currentCompanyId = 'COMPANY_A'
    financeCompanyStore.currentCompanyName = '广州测试公司'
    financeCompanyStore.currentCompanyLabel = 'COMP202604050001 - 广州测试公司'
    mocks.financeArchiveApi.getDepartmentArchiveMeta.mockResolvedValue({
      data: {
        departments: [
          {
            id: 10,
            deptCode: 'D001',
            deptName: '财务中心',
            companyId: 'COMPANY_A',
            syncSource: 'MANUAL',
            syncManaged: false,
            syncEnabled: true,
            status: 1,
            sortOrder: 1,
            children: [
              {
                id: 11,
                deptCode: 'D002',
                deptName: '费用管理部',
                parentId: 10,
                companyId: 'COMPANY_A',
                syncSource: 'WECOM',
                syncManaged: true,
                syncEnabled: true,
                status: 1,
                sortOrder: 2,
                children: []
              }
            ]
          }
        ],
        statusOptions: [
          { value: '1', label: '启用' },
          { value: '0', label: '停用' }
        ]
      }
    })
    mocks.financeArchiveApi.queryDepartments.mockResolvedValue({
      data: Array.from({ length: 12 }, (_, index) => ({
        id: 11 + index,
        deptCode: `D${String(index + 2).padStart(3, '0')}`,
        deptName: index === 0 ? '费用管理部' : `部门${index + 2}`,
        parentId: 10,
        parentDeptName: '财务中心',
        companyId: 'COMPANY_A',
        companyName: '广州测试公司',
        leaderUserId: 101 + index,
        leaderName: index === 0 ? '李经理' : `负责人${index + 2}`,
        status: 1,
        syncSource: 'WECOM',
        syncManaged: true,
        syncEnabled: true,
        syncStatus: 'SUCCESS',
        syncRemark: '同步正常',
        sortOrder: 2 + index,
        lastSyncAt: '2026-04-15T10:20:30',
        statDepartmentBelong: '财务',
        statRegionBelong: '华南',
        statAreaBelong: '广州'
      }))
    })
  })

  it('replaces the placeholder route with the real department archive view', () => {
    const financeRoot = routeCatalog.find((item) => item.path === '/')
    const departmentRoute = financeRoot?.children?.find((item) => item.path === 'finance/archives/departments')

    expect(String(departmentRoute?.component)).toContain('FinanceDepartmentArchiveView')
  })

  it('loads department meta and list on mount', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      departments: Array<{ deptName: string; leaderName?: string }>
      paginatedDepartments: Array<{ deptCode: string }>
    }

    expect(mocks.financeArchiveApi.getDepartmentArchiveMeta).toHaveBeenCalledTimes(1)
    expect(mocks.financeArchiveApi.queryDepartments).toHaveBeenCalledWith({
      keyword: undefined,
      parentId: undefined,
      status: undefined
    })
    expect(wrapper.text()).toContain('部门档案属于财务共享基础档案')
    expect(vm.departments[0]?.deptName).toBe('费用管理部')
    expect(vm.departments[0]?.leaderName).toBe('李经理')
    expect(vm.paginatedDepartments).toHaveLength(10)

    wrapper.unmount()
  })

  it('opens the detail drawer in read-only mode after pagination', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      openDetail: (row: Record<string, unknown>) => void
      paginatedDepartments: Array<Record<string, unknown>>
      currentDepartment?: Record<string, unknown>
    }

    await wrapper.get('[data-testid="pagination-next"]').trigger('click')
    await flushPromises()

    vm.openDetail(vm.paginatedDepartments[0]!)
    await flushPromises()

    expect(wrapper.text()).toContain('当前页面仅用于财务查看')
    expect(wrapper.text()).not.toContain('导入')
    expect(wrapper.text()).not.toContain('导出')

    wrapper.unmount()
  })

  it('updates the company badge without filtering out shared departments', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      departments: Array<{ deptName: string }>
    }

    financeCompanyStore.currentCompanyId = 'COMPANY_B'
    financeCompanyStore.currentCompanyName = '深圳测试公司'
    financeCompanyStore.currentCompanyLabel = 'COMP202604060001 - 深圳测试公司'
    await flushPromises()

    expect(wrapper.text()).toContain('COMP202604060001 - 深圳测试公司')
    expect(vm.departments[0]?.deptName).toBe('费用管理部')
    expect(mocks.financeArchiveApi.queryDepartments).toHaveBeenCalledTimes(1)

    wrapper.unmount()
  })

  it('changes the displayed department count when the page size changes', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      paginatedDepartments: Array<{ deptCode: string }>
    }

    await wrapper.get('[data-testid="pagination-size-20"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="pagination-size"]').text()).toBe('20')
    expect(vm.paginatedDepartments).toHaveLength(12)

    wrapper.unmount()
  })
})
