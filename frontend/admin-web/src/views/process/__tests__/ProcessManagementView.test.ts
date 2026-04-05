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
  template: `<button type="button" @click="$emit('click', $event)"><slot /></button>`
})

const InputStub = defineComponent({
  props: {
    modelValue: {
      type: String,
      default: ''
    }
  },
  emits: ['update:modelValue'],
  template: `<input :value="modelValue" @input="$emit('update:modelValue', $event.target.value)" />`
})

const SelectStub = defineComponent({
  props: {
    modelValue: {
      type: String,
      default: ''
    }
  },
  emits: ['update:modelValue'],
  template: `<select :value="modelValue" @change="$emit('update:modelValue', $event.target.value)"><slot /></select>`
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
        name: 'Category A',
        description: 'Category description',
        templateCount: templates.length,
        templates
      }
    ]
  }
}

async function mountView(templates: Array<Record<string, unknown>>, options?: {
  flows?: Array<Record<string, unknown>>
  forms?: Array<Record<string, unknown>>
}) {
  mocks.processApi.getOverview.mockResolvedValue({ data: buildOverview(templates) })
  mocks.processApi.getTemplateTypes.mockResolvedValue({ data: [] })
  mocks.processApi.listFlows.mockResolvedValue({ data: options?.flows || [] })
  mocks.processApi.listFormDesigns.mockResolvedValue({ data: options?.forms || [] })

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
    mocks.router.push.mockResolvedValue(undefined)
    mocks.router.replace.mockResolvedValue(undefined)
  })

  it('removes the hero banner and keeps compact summary and 4-column desktop template grid', async () => {
    const wrapper = await mountView([
      {
        id: 1,
        templateCode: 'FX202604020001',
        name: 'Template A',
        templateTypeCode: 'report',
        templateType: 'Report',
        businessDomain: 'Domain A',
        description: 'Template description',
        highlights: ['mobile'],
        flowCode: 'FLOW-001',
        flowName: 'Flow Alpha',
        formCode: 'FD-001',
        formName: 'Form Alpha',
        updatedAt: '2026-04-02 10:00',
        owner: 'Owner A',
        color: '#2563eb'
      }
    ])

    expect(wrapper.text()).not.toContain('Flow Studio')
    expect(wrapper.text()).not.toContain('单据与流程')
    expect(wrapper.text()).not.toContain('已纳入流程中心维护的全部单据模板')

    const summaryGrid = wrapper.get('[data-testid="process-summary-grid"]')
    expect(summaryGrid.classes()).toContain('xl:grid-cols-4')
    expect(summaryGrid.text()).toContain('1')

    const templateGrid = wrapper.get('[data-testid="process-template-grid"]')
    expect(templateGrid.classes()).toContain('xl:grid-cols-4')
    expect(templateGrid.text()).toContain('Template A')
    expect(templateGrid.text()).toContain('Template description')
  })

  it('renders bound form instead of template owner', async () => {
    const wrapper = await mountView([
      {
        id: 1,
        templateCode: 'FX202604020001',
        name: 'Template A',
        templateTypeCode: 'report',
        templateType: 'Report',
        businessDomain: 'Domain A',
        description: 'Template description',
        highlights: ['mobile'],
        flowCode: 'FLOW-001',
        flowName: 'Flow Alpha',
        formCode: 'FD-001',
        formName: 'Form Alpha',
        updatedAt: '2026-04-02 10:00',
        owner: 'Owner A',
        color: '#2563eb'
      }
    ])

    expect(wrapper.text()).toContain('绑定表单')
    expect(wrapper.text()).toContain('Form Alpha')
    expect(wrapper.text()).not.toContain('模板负责人')
  })

  it('opens flow and form editors from bound names', async () => {
    const wrapper = await mountView([
      {
        id: 1,
        templateCode: 'FX202604020001',
        name: 'Template A',
        templateTypeCode: 'report',
        templateType: 'Report',
        businessDomain: 'Domain A',
        description: 'Template description',
        highlights: ['mobile'],
        flowCode: 'FLOW-001',
        flowName: 'Flow Alpha',
        formCode: 'FD-001',
        formName: 'Form Alpha',
        updatedAt: '2026-04-02 10:00',
        owner: 'Owner A',
        color: '#2563eb'
      }
    ], {
      flows: [{ id: 88, flowCode: 'FLOW-001', flowName: 'Flow Alpha' }],
      forms: [{ id: 66, formCode: 'FD-001', formName: 'Form Alpha' }]
    })

    const buttons = wrapper.findAll('button')
    const flowButton = buttons.find((item) => item.text() === 'Flow Alpha')
    const formButton = buttons.find((item) => item.text() === 'Form Alpha')

    expect(flowButton).toBeTruthy()
    expect(formButton).toBeTruthy()

    await flowButton!.trigger('click')
    await formButton!.trigger('click')

    expect(mocks.router.push).toHaveBeenNthCalledWith(1, {
      name: 'expense-workbench-process-flow-edit',
      params: { id: 88 }
    })
    expect(mocks.router.push).toHaveBeenNthCalledWith(2, {
      name: 'expense-workbench-process-form-edit',
      params: { id: 66 }
    })
  })

  it('keeps bound names non-clickable when editable mapping is missing', async () => {
    const wrapper = await mountView([
      {
        id: 1,
        templateCode: 'FX202604020001',
        name: 'Template A',
        templateTypeCode: 'report',
        templateType: 'Report',
        businessDomain: 'Domain A',
        description: 'Template description',
        highlights: ['mobile'],
        flowCode: 'FLOW-404',
        flowName: 'Missing Flow',
        formCode: 'FD-404',
        formName: 'Missing Form',
        updatedAt: '2026-04-02 10:00',
        owner: 'Owner A',
        color: '#2563eb'
      }
    ], {
      flows: [{ id: 88, flowCode: 'FLOW-001', flowName: 'Flow Alpha' }],
      forms: [{ id: 66, formCode: 'FD-001', formName: 'Form Alpha' }]
    })

    const clickableTexts = wrapper.findAll('button').map((item) => item.text())

    expect(wrapper.text()).toContain('Missing Flow')
    expect(wrapper.text()).toContain('Missing Form')
    expect(clickableTexts).not.toContain('Missing Flow')
    expect(clickableTexts).not.toContain('Missing Form')
    expect(mocks.router.push).not.toHaveBeenCalled()
  })
})
