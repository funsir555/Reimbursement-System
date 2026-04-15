import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, inject, provide } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import FinanceProjectArchiveView from '@/views/finance/FinanceProjectArchiveView.vue'

const mocks = vi.hoisted(() => ({
  financeArchiveApi: {
    getProjectArchiveMeta: vi.fn(),
    listProjectClasses: vi.fn(),
    createProjectClass: vi.fn(),
    updateProjectClass: vi.fn(),
    updateProjectClassStatus: vi.fn(),
    listProjects: vi.fn(),
    getProjectDetail: vi.fn(),
    createProject: vi.fn(),
    updateProject: vi.fn(),
    updateProjectStatus: vi.fn(),
    updateProjectClose: vi.fn()
  },
  financeCompany: {
    currentCompanyId: 'COMPANY_A',
    currentCompanyName: '公司A',
    registerSwitchGuard: vi.fn(),
    unregisterSwitchGuard: vi.fn()
  },
  elMessage: {
    success: vi.fn(),
    warning: vi.fn(),
    error: vi.fn()
  },
  elMessageBox: {
    confirm: vi.fn()
  }
}))

vi.mock('@/api', () => ({
  financeArchiveApi: mocks.financeArchiveApi
}))

vi.mock('@/stores/financeCompany', () => ({
  useFinanceCompanyStore: () => mocks.financeCompany
}))

vi.mock('@/utils/permissions', () => ({
  hasPermission: () => true,
  readStoredUser: () => ({
    permissionCodes: [
      'finance:archives:projects:view',
      'finance:archives:projects:create',
      'finance:archives:projects:edit',
      'finance:archives:projects:disable',
      'finance:archives:projects:close'
    ]
  })
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage,
  ElMessageBox: mocks.elMessageBox
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
      <template v-for="row in rows" :key="String((row.citemcode || row.project_class_code || '') + prop)">
        <slot :row="row">
          <span>{{ prop ? row[prop] : '' }}</span>
        </slot>
      </template>
    </div>
  `
})

async function mountView() {
  const wrapper = mount(FinanceProjectArchiveView, {
    global: {
      stubs: {
        'el-card': SimpleStub,
        'el-tabs': SimpleStub,
        'el-tab-pane': SimpleStub,
        'el-button': SimpleStub,
        'el-input': SimpleStub,
        'el-select': SimpleStub,
        'el-option': true,
        'el-table': TableStub,
        'el-table-column': TableColumnStub,
        'el-tag': SimpleStub,
        'el-dialog': SimpleStub,
        'el-form': SimpleStub,
        'el-form-item': SimpleStub,
        'el-date-picker': SimpleStub,
        'el-input-number': SimpleStub
      },
      directives: {
        loading: () => undefined
      }
    }
  })
  await flushPromises()
  return wrapper
}

describe('FinanceProjectArchiveView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.elMessageBox.confirm.mockResolvedValue(true)
    mocks.financeArchiveApi.getProjectArchiveMeta.mockResolvedValue({
      data: {
        statusOptions: [
          { value: '1', label: '启用' },
          { value: '0', label: '停用' }
        ],
        closeStatusOptions: [
          { value: '0', label: '未封存' },
          { value: '1', label: '已封存' }
        ],
        projectClassOptions: [{ value: '97', label: '97 / Market Projects' }]
      }
    })
    mocks.financeArchiveApi.listProjectClasses.mockResolvedValue({
      data: [{ project_class_code: '97', project_class_name: 'Market Projects', status: 1, has_projects: false }]
    })
    mocks.financeArchiveApi.listProjects.mockResolvedValue({
      data: [{ citemcode: '2002', citemname: 'Project One', citemccode: '97', status: 1, bclose: 0 }]
    })
    mocks.financeArchiveApi.createProject.mockResolvedValue({ data: {} })
    mocks.financeArchiveApi.updateProjectStatus.mockResolvedValue({ data: true })
    mocks.financeArchiveApi.updateProjectClose.mockResolvedValue({ data: true })
  })

  it('loads meta, project classes and projects on mount', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      meta: { projectClassOptions: Array<{ value: string }> }
      projectClasses: Array<{ project_class_code: string }>
      projects: Array<{ citemcode: string }>
    }

    expect(mocks.financeArchiveApi.getProjectArchiveMeta).toHaveBeenCalledWith('COMPANY_A')
    expect(mocks.financeArchiveApi.listProjectClasses).toHaveBeenCalledWith({
      companyId: 'COMPANY_A',
      keyword: undefined,
      status: undefined
    })
    expect(mocks.financeArchiveApi.listProjects).toHaveBeenCalledWith({
      companyId: 'COMPANY_A',
      keyword: undefined,
      projectClassCode: undefined,
      status: undefined,
      bclose: undefined
    })
    expect(vm.meta.projectClassOptions[0]?.value).toBe('97')
    expect(vm.projectClasses[0]?.project_class_code).toBe('97')
    expect(vm.projects[0]?.citemcode).toBe('2002')

    wrapper.unmount()
  })

  it('builds project payload from dialog form', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      openProjectDialog: (mode: 'create') => void
      projectForm: Record<string, unknown>
      buildProjectPayload: () => Record<string, unknown>
    }

    vm.openProjectDialog('create')
    vm.projectForm.citemcode = '2002'
    vm.projectForm.citemname = 'Project Two'
    vm.projectForm.citemccode = '97'
    vm.projectForm.iotherused = 2

    expect(vm.buildProjectPayload()).toMatchObject({
      citemcode: '2002',
      citemname: 'Project Two',
      citemccode: '97',
      iotherused: 2
    })

    wrapper.unmount()
  })


  it('accepts variable-length numeric project codes when saving', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      openProjectDialog: (mode: 'create') => void
      projectForm: Record<string, unknown>
      saveProject: () => Promise<void>
    }

    vm.openProjectDialog('create')
    vm.projectForm.citemcode = '2002'
    vm.projectForm.citemname = 'Project Two'
    vm.projectForm.citemccode = '7'
    vm.projectForm.iotherused = 0

    await vm.saveProject()
    await flushPromises()

    expect(mocks.financeArchiveApi.createProject).toHaveBeenCalledWith('COMPANY_A', {
      citemcode: '2002',
      citemname: 'Project Two',
      citemccode: '7',
      iotherused: 0,
      d_end_date: undefined
    })

    wrapper.unmount()
  })

  it('submits project status and close actions through dedicated endpoints', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      toggleProjectStatus: (row: Record<string, unknown>) => Promise<void>
      toggleProjectClose: (row: Record<string, unknown>) => Promise<void>
    }

    await vm.toggleProjectStatus({ citemcode: '2002', citemname: 'Project One', status: 1, bclose: 0 } as Record<string, unknown>)
    await vm.toggleProjectClose({ citemcode: '2002', citemname: 'Project One', status: 1, bclose: 0 } as Record<string, unknown>)
    await flushPromises()

    expect(mocks.financeArchiveApi.updateProjectStatus).toHaveBeenCalledWith('COMPANY_A', '2002', 0)
    expect(mocks.financeArchiveApi.updateProjectClose).toHaveBeenCalledWith('COMPANY_A', '2002', 1)

    wrapper.unmount()
  })
})
