import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ProcessManagementView from '@/views/process/ProcessManagementView.vue'

const mocks = vi.hoisted(() => ({
  route: {
    query: {},
    fullPath: '/expense/workbench/process-management'
  },
  router: {
    push: vi.fn(),
    replace: vi.fn()
  },
  processApi: {
    getOverview: vi.fn(),
    getTemplateTypes: vi.fn(),
    listFlows: vi.fn(),
    listFormDesigns: vi.fn(),
    listExpenseDetailDesigns: vi.fn(),
    copyTemplate: vi.fn(),
    deleteTemplate: vi.fn(),
    deleteFormDesign: vi.fn(),
    deleteFlow: vi.fn()
  },
  elMessage: {
    warning: vi.fn(),
    error: vi.fn(),
    success: vi.fn()
  },
  elMessageBox: {
    confirm: vi.fn()
  }
}))

vi.mock('vue-router', () => ({
  useRoute: () => mocks.route,
  useRouter: () => mocks.router
}))

vi.mock('@/api', () => ({
  processApi: mocks.processApi
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessage: mocks.elMessage,
    ElMessageBox: mocks.elMessageBox
  }
})

vi.mock('@/utils/permissions', () => ({
  readStoredUser: () => ({
    permissionCodes: [
      'expense:process_management:view',
      'expense:process_management:create',
      'expense:process_management:edit'
    ]
  }),
  hasPermission: (permissionCode: string, source?: { permissionCodes?: string[] } | string[] | null) => {
    const ownedCodes = Array.isArray(source)
      ? source
      : source?.permissionCodes || [
        'expense:process_management:view',
        'expense:process_management:create',
        'expense:process_management:edit'
      ]
    return ownedCodes.includes(permissionCode)
  }
}))

vi.mock('@element-plus/icons-vue', async () => {
  const actual = await vi.importActual<typeof import('@element-plus/icons-vue')>('@element-plus/icons-vue')
  return {
    ...actual,
    CircleCheckFilled: { template: '<span />' },
    CopyDocument: { template: '<span />' },
    Document: { template: '<span />' },
    Files: { template: '<span />' },
    Plus: { template: '<span />' },
    Search: { template: '<span />' },
    Tools: { template: '<span />' },
    TrendCharts: { template: '<span />' }
  }
})

const SimpleContainer = defineComponent({
  template: '<div><slot name="header" /><slot /><slot name="footer" /></div>'
})

const ButtonStub = defineComponent({
  emits: ['click'],
  template: '<button type="button" @click="$emit(\'click\', $event)"><slot /></button>'
})

const InputStub = defineComponent({
  props: {
    modelValue: {
      type: String,
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
  template: '<select :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><slot /></select>'
})

const OptionStub = defineComponent({
  props: {
    value: {
      type: String,
      default: ''
    },
    label: {
      type: String,
      default: ''
    }
  },
  template: '<option :value="value">{{ label }}</option>'
})

const SidebarStub = defineComponent({
  props: {
    items: {
      type: Array,
      default: () => []
    },
    activeKey: {
      type: String,
      default: ''
    }
  },
  emits: ['select'],
  template: `
    <div data-testid="process-sidebar">
      <button
        v-for="item in items"
        :key="item.key"
        type="button"
        data-testid="process-sidebar-item"
        :data-key="item.key"
        @click="$emit('select', item.key)"
      >
        {{ item.label }}
      </button>
    </div>
  `
})

const ExpenseDetailPanelStub = defineComponent({
  template: '<div data-testid="expense-detail-panel">expense-detail-panel</div>'
})

const CustomArchivePanelStub = defineComponent({
  template: '<div data-testid="custom-archive-panel">custom-archive-panel</div>'
})

const ExpenseTypePanelStub = defineComponent({
  template: '<div data-testid="expense-type-panel">expense-type-panel</div>'
})

const UserGroupPanelStub = defineComponent({
  template: '<div data-testid="user-group-panel">user-group-panel</div>'
})

const globalStubs = {
  'process-workbench-sidebar': SidebarStub,
  'template-type-dialog': true,
  'custom-archive-management-panel': CustomArchivePanelStub,
  'expense-detail-design-management-panel': ExpenseDetailPanelStub,
  'expense-type-management-panel': ExpenseTypePanelStub,
  'user-group-management-panel': UserGroupPanelStub,
  'el-card': SimpleContainer,
  'el-button': ButtonStub,
  'el-input': InputStub,
  'el-select': SelectStub,
  'el-option': OptionStub,
  'el-tag': SimpleContainer,
  'el-icon': SimpleContainer
}

function buildOverview(templates: Array<Record<string, unknown>>) {
  return {
    navItems: [
      { key: 'document-flow', label: '单据与流程' },
      { key: 'form-design', label: '费用表单' },
      { key: 'approval-flow', label: '审批流程' },
      { key: 'expense-detail-form', label: '费用明细表单' },
      { key: 'custom-archive', label: '自定义档案' },
      { key: 'expense-type', label: '费用类型' }
    ],
    summary: {
      totalTemplates: templates.length,
      enabledTemplates: templates.filter((item) => item.status === 'ENABLED').length,
      draftTemplates: templates.filter((item) => item.status === 'DRAFT').length,
      aiAuditTemplates: 0
    },
    categories: [
      {
        code: 'employee-expense',
        name: '员工报销',
        description: '差旅与报销模板',
        templateCount: templates.length,
        templates
      }
    ]
  }
}

function buildTemplate(overrides: Record<string, unknown> = {}) {
  return {
    id: 1,
    templateCode: 'FX202604020001',
    name: '差旅报销单',
    templateTypeCode: 'report',
    templateType: '报销单',
    businessDomain: '员工报销',
    description: '差旅费用报销',
    highlights: ['移动端提单', '暂无亮点', '暂无亮点'],
    flowCode: 'FLOW-001',
    flowName: '差旅审批流程',
    formCode: 'FD-001',
    formName: '差旅报销表单',
    expenseDetailDesignCode: 'EDD-001',
    expenseDetailDesignName: '差旅费用明细表单',
    status: 'ENABLED',
    statusLabel: '已启用',
    updatedAt: '2026-04-02 10:00',
    owner: '流程管理员',
    color: '#2563eb',
    ...overrides
  }
}

function buildFormSummary(overrides: Record<string, unknown> = {}) {
  return {
    id: 66,
    formCode: 'FD-001',
    formName: '差旅报销表单',
    templateType: 'report',
    templateTypeLabel: '报销单',
    formDescription: '用于差旅报销的业务表单',
    updatedAt: '2026-04-10 08:30',
    ...overrides
  }
}

function buildFlowSummary(overrides: Record<string, unknown> = {}) {
  return {
    id: 88,
    flowCode: 'FLOW-001',
    flowName: '差旅审批流程',
    flowDescription: '覆盖普通报销场景的审批流',
    status: 'ENABLED',
    statusLabel: '已启用',
    currentVersionNo: 3,
    updatedAt: '2026-04-09 09:15',
    ...overrides
  }
}

async function mountView(
  templates: Array<Record<string, unknown>>,
  options?: {
    flows?: Array<Record<string, unknown>>
    forms?: Array<Record<string, unknown>>
    expenseDetails?: Array<Record<string, unknown>>
  }
) {
  mocks.processApi.getOverview.mockResolvedValue({ data: buildOverview(templates) })
  mocks.processApi.getTemplateTypes.mockResolvedValue({ data: [] })
  mocks.processApi.listFlows.mockResolvedValue({ data: options?.flows || [] })
  mocks.processApi.listFormDesigns.mockResolvedValue({ data: options?.forms || [] })
  mocks.processApi.listExpenseDetailDesigns.mockResolvedValue({ data: options?.expenseDetails || [] })

  const wrapper = mount(ProcessManagementView, {
    global: {
      stubs: globalStubs
    }
  })
  await flushPromises()
  return wrapper
}

describe('ProcessManagementView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.route.query = {}
    mocks.route.fullPath = '/expense/workbench/process-management'
    mocks.router.push.mockResolvedValue(undefined)
    mocks.router.replace.mockResolvedValue(undefined)
    mocks.elMessageBox.confirm.mockResolvedValue('confirm')
    mocks.processApi.copyTemplate.mockResolvedValue({
      data: { id: 101, templateCode: 'FX202604170099', templateName: '副本', status: 'DRAFT' }
    })
    mocks.processApi.deleteTemplate.mockResolvedValue({ data: true })
    mocks.processApi.deleteFormDesign.mockResolvedValue({ data: true })
    mocks.processApi.deleteFlow.mockResolvedValue({ data: true })
  })

  it('keeps the sidebar order and inserts user-group after custom-archive when overview has not returned it yet', async () => {
    const wrapper = await mountView([buildTemplate()])

    const keys = wrapper.findAll('[data-testid="process-sidebar-item"]').map((item) => item.attributes('data-key'))
    expect(keys).toEqual([
      'document-flow',
      'form-design',
      'approval-flow',
      'expense-detail-form',
      'custom-archive',
      'user-group',
      'expense-type'
    ])
  })

  it('renders the real user-group panel section', async () => {
    mocks.route.query = { section: 'user-group' }
    const wrapper = await mountView([buildTemplate()])

    expect(wrapper.get('[data-testid="user-group-panel"]').text()).toContain('user-group-panel')
  })

  it('keeps the document section metadata and opens bound editors from clickable names', async () => {
    const wrapper = await mountView(
      [buildTemplate()],
      {
        flows: [buildFlowSummary()],
        forms: [buildFormSummary()],
        expenseDetails: [{ id: 55, detailCode: 'EDD-001', detailName: '差旅费用明细表单' }]
      }
    )

    const templateGrid = wrapper.get('[data-testid="process-template-grid"]')
    expect(templateGrid.text()).toContain('差旅报销单')
    expect(templateGrid.text()).toContain('绑定流程')
    expect(templateGrid.text()).toContain('绑定表单')
    expect(templateGrid.text()).toContain('绑定明细表单')

    const buttons = wrapper.findAll('button')
    await buttons.find((item) => item.text() === '差旅审批流程')!.trigger('click')
    await buttons.find((item) => item.text() === '差旅报销表单')!.trigger('click')
    await buttons.find((item) => item.text() === '差旅费用明细表单')!.trigger('click')

    expect(mocks.router.push).toHaveBeenNthCalledWith(1, {
      name: 'expense-workbench-process-flow-edit',
      params: { id: 88 },
      query: { returnTo: '/expense/workbench/process-management' }
    })
    expect(mocks.router.push).toHaveBeenNthCalledWith(2, {
      name: 'expense-workbench-process-form-edit',
      params: { id: 66 }
    })
    expect(mocks.router.push).toHaveBeenNthCalledWith(3, {
      name: 'expense-workbench-process-expense-detail-edit',
      params: { id: 55 }
    })
  })

  it('renders the form-design section and opens create/edit routes', async () => {
    mocks.route.query = { section: 'form-design' }
    const wrapper = await mountView([buildTemplate()], { forms: [buildFormSummary()] })

    expect(wrapper.get('[data-testid="process-form-section"]').text()).toContain('增加费用表单')
    await wrapper.get('[data-testid="process-form-create"]').trigger('click')

    expect(mocks.router.push).toHaveBeenCalledWith({
      name: 'expense-workbench-process-form-create'
    })

    const modifyButton = wrapper.findAll('button').find((item) => item.text() === '修改')
    expect(modifyButton).toBeTruthy()
    await modifyButton!.trigger('click')

    expect(mocks.router.push).toHaveBeenLastCalledWith({
      name: 'expense-workbench-process-form-edit',
      params: { id: 66 }
    })
  })

  it('renders the approval-flow section and opens create/edit routes', async () => {
    mocks.route.query = { section: 'approval-flow' }
    mocks.route.fullPath = '/expense/workbench/process-management?section=approval-flow'
    const wrapper = await mountView([buildTemplate()], { flows: [buildFlowSummary()] })

    expect(wrapper.get('[data-testid="process-flow-section"]').text()).toContain('增加审批流程')
    await wrapper.get('[data-testid="process-flow-create"]').trigger('click')

    expect(mocks.router.push).toHaveBeenCalledWith({
      name: 'expense-workbench-process-flow-create',
      query: { returnTo: '/expense/workbench/process-management?section=approval-flow' }
    })

    const modifyButton = wrapper.findAll('button').find((item) => item.text() === '修改')
    expect(modifyButton).toBeTruthy()
    await modifyButton!.trigger('click')

    expect(mocks.router.push).toHaveBeenLastCalledWith({
      name: 'expense-workbench-process-flow-edit',
      params: { id: 88 },
      query: { returnTo: '/expense/workbench/process-management?section=approval-flow' }
    })
  })

  it('copies and deletes template/form/flow cards through the existing actions', async () => {
    const wrapper = await mountView([buildTemplate()], {
      flows: [buildFlowSummary()],
      forms: [buildFormSummary()]
    })

    await wrapper.findAll('button').find((item) => item.text() === '复制模板')!.trigger('click')
    await flushPromises()
    expect(mocks.processApi.copyTemplate).toHaveBeenCalledWith(1)

    await wrapper.findAll('button').find((item) => item.text() === '删除模板')!.trigger('click')
    await flushPromises()
    expect(mocks.processApi.deleteTemplate).toHaveBeenCalledWith(1)

    mocks.route.query = { section: 'form-design' }
    const formWrapper = await mountView([buildTemplate()], { forms: [buildFormSummary()] })
    await formWrapper.findAll('button').find((item) => item.text() === '删除')!.trigger('click')
    await flushPromises()
    expect(mocks.processApi.deleteFormDesign).toHaveBeenCalledWith(66)

    mocks.route.query = { section: 'approval-flow' }
    const flowWrapper = await mountView([buildTemplate()], { flows: [buildFlowSummary()] })
    await flowWrapper.findAll('button').find((item) => item.text() === '删除')!.trigger('click')
    await flushPromises()
    expect(mocks.processApi.deleteFlow).toHaveBeenCalledWith(88)
  })

  it('keeps expense-detail, custom-archive, and expense-type tabs mounted on their dedicated panels', async () => {
    mocks.route.query = { section: 'expense-detail-form' }
    const detailWrapper = await mountView([buildTemplate()])
    expect(detailWrapper.get('[data-testid="expense-detail-panel"]').text()).toContain('expense-detail-panel')

    mocks.route.query = { section: 'custom-archive' }
    const archiveWrapper = await mountView([buildTemplate()])
    expect(archiveWrapper.get('[data-testid="custom-archive-panel"]').text()).toContain('custom-archive-panel')

    mocks.route.query = { section: 'expense-type' }
    const expenseTypeWrapper = await mountView([buildTemplate()])
    expect(expenseTypeWrapper.get('[data-testid="expense-type-panel"]').text()).toContain('expense-type-panel')
  })

  it('switches sections through the sidebar without changing the route path', async () => {
    const wrapper = await mountView([buildTemplate()])

    const flowNav = wrapper.findAll('[data-testid="process-sidebar-item"]').find((item) => item.attributes('data-key') === 'approval-flow')
    expect(flowNav).toBeTruthy()
    await flowNav!.trigger('click')

    expect(mocks.router.replace).toHaveBeenCalledWith({
      path: '/expense/workbench/process-management',
      query: { section: 'approval-flow' }
    })
  })
})
