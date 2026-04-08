import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ProcessManagementView from '@/views/process/ProcessManagementView.vue'

const mocks = vi.hoisted(() => ({
  route: {
    query: {}
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
    deleteTemplate: vi.fn()
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

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage,
  ElMessageBox: mocks.elMessageBox
}))

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

vi.mock('@element-plus/icons-vue', () => ({
  CircleCheckFilled: { template: '<span />' },
  CopyDocument: { template: '<span />' },
  Document: { template: '<span />' },
  Files: { template: '<span />' },
  Plus: { template: '<span />' },
  Search: { template: '<span />' },
  Tools: { template: '<span />' },
  TrendCharts: { template: '<span />' }
}))

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

const globalStubs = {
  'process-workbench-sidebar': true,
  'template-type-dialog': true,
  'custom-archive-management-panel': true,
  'expense-detail-design-management-panel': true,
  'expense-type-management-panel': true,
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
    navItems: [],
    summary: {
      totalTemplates: templates.length,
      enabledTemplates: templates.length,
      draftTemplates: 0,
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
    updatedAt: '2026-04-02 10:00',
    owner: '流程管理员',
    color: '#2563eb',
    ...overrides
  }
}

describe('ProcessManagementView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.route.query = {}
    mocks.router.push.mockResolvedValue(undefined)
    mocks.router.replace.mockResolvedValue(undefined)
  })

  it('keeps the compact template grid and removes highlight pills', async () => {
    const wrapper = await mountView([buildTemplate()])

    expect(wrapper.text()).not.toContain('Flow Studio')
    expect(wrapper.text()).not.toContain('移动端提单')
    expect(wrapper.text()).not.toContain('暂无亮点')

    const summaryGrid = wrapper.get('[data-testid="process-summary-grid"]')
    expect(summaryGrid.classes()).toContain('xl:grid-cols-4')
    expect(summaryGrid.text()).toContain('1')

    const templateGrid = wrapper.get('[data-testid="process-template-grid"]')
    expect(templateGrid.classes()).toContain('xl:grid-cols-4')
    expect(templateGrid.text()).toContain('差旅报销单')
    expect(templateGrid.text()).toContain('差旅费用报销')
  })

  it('renders bound flow, form, and expense detail form metadata', async () => {
    const wrapper = await mountView([buildTemplate()])

    expect(wrapper.text()).toContain('绑定流程')
    expect(wrapper.text()).toContain('差旅审批流程')
    expect(wrapper.text()).toContain('绑定表单')
    expect(wrapper.text()).toContain('差旅报销表单')
    expect(wrapper.text()).toContain('绑定明细表单')
    expect(wrapper.text()).toContain('差旅费用明细表单')
  })

  it('opens bound flow, form, and expense detail editors from clickable names', async () => {
    const wrapper = await mountView(
      [buildTemplate()],
      {
        flows: [{ id: 88, flowCode: 'FLOW-001', flowName: '差旅审批流程' }],
        forms: [{ id: 66, formCode: 'FD-001', formName: '差旅报销表单' }],
        expenseDetails: [{ id: 55, detailCode: 'EDD-001', detailName: '差旅费用明细表单' }]
      }
    )

    const buttons = wrapper.findAll('button')
    const flowButton = buttons.find((item) => item.text() === '差旅审批流程')
    const formButton = buttons.find((item) => item.text() === '差旅报销表单')
    const expenseDetailButton = buttons.find((item) => item.text() === '差旅费用明细表单')

    expect(flowButton).toBeTruthy()
    expect(formButton).toBeTruthy()
    expect(expenseDetailButton).toBeTruthy()

    await flowButton!.trigger('click')
    await formButton!.trigger('click')
    await expenseDetailButton!.trigger('click')

    expect(mocks.router.push).toHaveBeenNthCalledWith(1, {
      name: 'expense-workbench-process-flow-edit',
      params: { id: 88 }
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

  it('keeps missing bound resources non-clickable and falls back to static text', async () => {
    const wrapper = await mountView(
      [
        buildTemplate({
          flowCode: 'FLOW-404',
          flowName: '缺失流程',
          formCode: 'FD-404',
          formName: '缺失表单',
          expenseDetailDesignCode: 'EDD-404',
          expenseDetailDesignName: '缺失明细表单'
        }),
        buildTemplate({
          id: 2,
          templateCode: 'FX202604020002',
          name: '日常报销单',
          expenseDetailDesignCode: undefined,
          expenseDetailDesignName: undefined
        })
      ],
      {
        flows: [{ id: 88, flowCode: 'FLOW-001', flowName: '差旅审批流程' }],
        forms: [{ id: 66, formCode: 'FD-001', formName: '差旅报销表单' }],
        expenseDetails: [{ id: 55, detailCode: 'EDD-001', detailName: '差旅费用明细表单' }]
      }
    )

    const clickableTexts = wrapper.findAll('button').map((item) => item.text())

    expect(wrapper.text()).toContain('缺失流程')
    expect(wrapper.text()).toContain('缺失表单')
    expect(wrapper.text()).toContain('缺失明细表单')
    expect(wrapper.text()).toContain('未绑定明细表单')
    expect(clickableTexts).not.toContain('缺失流程')
    expect(clickableTexts).not.toContain('缺失表单')
    expect(clickableTexts).not.toContain('缺失明细表单')
    expect(mocks.router.push).not.toHaveBeenCalled()
  })
})
