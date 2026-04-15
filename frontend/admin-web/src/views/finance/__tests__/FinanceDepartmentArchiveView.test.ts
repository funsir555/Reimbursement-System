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
        'el-drawer': SimpleStub
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
      data: [
        {
          id: 11,
          deptCode: 'D002',
          deptName: '费用管理部',
          parentId: 10,
          parentDeptName: '财务中心',
          companyId: 'COMPANY_A',
          companyName: '广州测试公司',
          leaderUserId: 101,
          leaderName: '李经理',
          status: 1,
          syncSource: 'WECOM',
          syncManaged: true,
          syncEnabled: true,
          syncStatus: 'SUCCESS',
          syncRemark: '同步正常',
          sortOrder: 2,
          lastSyncAt: '2026-04-15T10:20:30',
          statDepartmentBelong: '财务',
          statRegionBelong: '华南',
          statAreaBelong: '广州'
        }
      ]
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

    wrapper.unmount()
  })

  it('opens the detail drawer in read-only mode', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      openDetail: (row: Record<string, unknown>) => void
      currentDepartment?: Record<string, unknown>
    }

    vm.openDetail({
      deptCode: 'D002',
      deptName: '费用管理部',
      parentDeptName: '财务中心',
      companyName: '广州测试公司',
      leaderName: '李经理',
      syncSource: 'WECOM',
      syncManaged: true,
      syncEnabled: true,
      syncStatus: 'SUCCESS',
      syncRemark: '同步正常',
      status: 1,
      sortOrder: 2
    })
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
})
