import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ProcessFlowDesignerView from '@/views/process/ProcessFlowDesignerView.vue'

const mocks = vi.hoisted(() => ({
  route: {
    name: 'expense-workbench-process-flow-edit',
    params: {
      id: '1'
    },
    query: {}
  },
  router: {
    push: vi.fn()
  },
  processApi: {
    listFlows: vi.fn(),
    getFlowMeta: vi.fn(),
    getFlowDetail: vi.fn(),
    createFlow: vi.fn(),
    updateFlow: vi.fn(),
    publishFlow: vi.fn(),
    updateFlowStatus: vi.fn(),
    createFlowScene: vi.fn()
  },
  elMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
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
  ArrowLeft: { template: '<span />' },
  Check: { template: '<span />' },
  Delete: { template: '<span />' },
  RefreshRight: { template: '<span />' }
}))

const SimpleContainer = defineComponent({
  template: '<div><slot name="header" /><slot /><slot name="footer" /></div>'
})

const ButtonStub = defineComponent({
  props: {
    loading: {
      type: Boolean,
      default: false
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },
  emits: ['click'],
  template: '<button type="button" :disabled="disabled" @click="$emit(\'click\', $event)"><slot /></button>'
})

const InputStub = defineComponent({
  props: {
    modelValue: {
      type: [String, Number],
      default: ''
    }
  },
  emits: ['update:modelValue'],
  template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
})

const SwitchStub = defineComponent({
  props: {
    modelValue: {
      type: Boolean,
      default: false
    }
  },
  emits: ['change', 'update:modelValue'],
  template: '<button type="button" data-testid="attach-below-switch" @click="$emit(\'update:modelValue\', !modelValue); $emit(\'change\', !modelValue)"><slot /></button>'
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

const TagStub = defineComponent({
  template: '<span data-testid="tag"><slot /></span>'
})

const EmptyStub = defineComponent({
  props: {
    description: {
      type: String,
      default: ''
    }
  },
  template: '<div>{{ description }}</div>'
})

const ProcessFlowCanvasRendererStub = defineComponent({
  emits: ['select-node', 'select-route', 'insert-node', 'drag-node-start', 'drag-node-end', 'drag-node-over', 'drop-node'],
  template: `
    <div>
      <button type="button" data-testid="select-branch" @click="$emit('select-node', 'branch-1')">
        select branch
      </button>
      <button type="button" data-testid="select-approval-root" @click="$emit('select-node', 'approval-root')">
        select approval root
      </button>
      <button type="button" data-testid="select-route" @click="$emit('select-route', 'route-1')">
        select route
      </button>
      <button type="button" data-testid="select-route-2" @click="$emit('select-route', 'route-2')">
        select route 2
      </button>
      <button
        type="button"
        data-testid="move-lane-node-to-root"
        @click="$emit('drag-node-start', 'approval-lane')"
      >
        start drag lane node
      </button>
      <button
        type="button"
        data-testid="drop-lane-node-to-root"
        @click="$emit('drop-node', { containerKey: null, index: 0, blockKey: 'insert-root-0' })"
      >
        drop lane node to root
      </button>
    </div>
  `
})

function buildFlowMeta() {
  return {
    nodeTypeOptions: [],
    sceneOptions: [{ id: 1, sceneName: 'Default Scene', sceneDescription: '', status: 1 }],
    approvalApproverTypeOptions: [],
    approvalManagerRuleModeOptions: [],
    approvalManagerDeptSourceOptions: [],
    approvalManagerLevelOptions: [],
    approvalManagerLookupLevelOptions: [],
    approvalManualCandidateScopeOptions: [],
    ccReceiverTypeOptions: [],
    paymentExecutorTypeOptions: [],
    missingHandlerOptions: [],
    approvalModeOptions: [],
    defaultApprovalOpinions: [],
    approvalSpecialOptions: [],
    ccTimingOptions: [],
    ccSpecialOptions: [],
    paymentActionOptions: [],
    paymentSpecialOptions: [],
    branchOperatorOptions: [],
    branchConditionFields: [],
    companyOptions: [],
    departmentOptions: [],
    userOptions: [],
    expenseTypeOptions: [],
    archiveOptions: []
  }
}

function buildFlowDetail(flowName = 'Travel Approval Flow', routeAttachFlags: [boolean, boolean] = [false, false]) {
  return {
    id: 1,
    flowCode: 'FLOW-001',
    flowName,
    flowDescription: 'flow description',
    status: 'DRAFT',
    statusLabel: '草稿',
    nodes: [
      {
        nodeKey: 'approval-root',
        nodeName: '审批节点 1',
        nodeType: 'APPROVAL',
        sceneId: 1,
        displayOrder: 1,
        config: {}
      },
      {
        nodeKey: 'branch-1',
        nodeName: '流程分支 1',
        nodeType: 'BRANCH',
        sceneId: 1,
        displayOrder: 2,
        config: {}
      },
      {
        nodeKey: 'approval-lane',
        nodeName: '泳道审批节点',
        nodeType: 'APPROVAL',
        sceneId: 1,
        parentNodeKey: 'route-1',
        displayOrder: 1,
        config: {}
      }
    ],
    routes: [
      {
        routeKey: 'route-1',
        sourceNodeKey: 'branch-1',
        routeName: '分支 A',
        priority: 1,
        attachBelowNodes: routeAttachFlags[0],
        conditionGroups: []
      },
      {
        routeKey: 'route-2',
        sourceNodeKey: 'branch-1',
        routeName: '分支 B',
        priority: 2,
        attachBelowNodes: routeAttachFlags[1],
        conditionGroups: []
      }
    ]
  }
}

function buildSavedFlowDetail(payload: { flowName: string; flowDescription?: string; nodes: any[]; routes: any[] }) {
  return {
    id: 1,
    flowCode: 'FLOW-001',
    flowName: payload.flowName,
    flowDescription: payload.flowDescription || 'flow description',
    status: 'DRAFT',
    statusLabel: 'Draft',
    nodes: payload.nodes,
    routes: payload.routes
  }
}

async function mountView(
  flowName = 'Travel Approval Flow',
  detail = buildFlowDetail(flowName)
) {
  mocks.processApi.listFlows.mockResolvedValue({
    data: [
      {
        id: 1,
        flowCode: 'FLOW-001',
        flowName: flowName || '未命名流程',
        status: 'DRAFT',
        statusLabel: '草稿',
        updatedAt: '2026-04-03 10:00'
      }
    ]
  })
  mocks.processApi.getFlowMeta.mockResolvedValue({ data: buildFlowMeta() })
  mocks.processApi.getFlowDetail.mockResolvedValue({ data: detail })
  mocks.processApi.updateFlow.mockImplementation(async (_id: number, payload: any) => ({ data: buildSavedFlowDetail(payload) }))

  const wrapper = mount(ProcessFlowDesignerView, {
    global: {
      stubs: {
        ProcessFlowCanvasRenderer: ProcessFlowCanvasRendererStub,
        'el-card': SimpleContainer,
        'el-button': ButtonStub,
        'el-input': InputStub,
        'el-switch': SwitchStub,
        'el-form-item': FormItemStub,
        'el-tag': TagStub,
        'el-icon': SimpleContainer,
        'el-empty': EmptyStub,
        'el-dialog': SimpleContainer,
        'el-select': SimpleContainer,
        'el-option': true,
        'el-checkbox-group': SimpleContainer,
        'el-checkbox': SimpleContainer,
        'el-radio-group': SimpleContainer,
        'el-radio-button': SimpleContainer,
        'el-input-number': SimpleContainer
      },
      directives: {
        loading: () => undefined
      }
    }
  })

  await flushPromises()
  mountedWrappers.push(wrapper)
  return wrapper
}

const mountedWrappers: Array<{ unmount: () => void }> = []

describe('ProcessFlowDesignerView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.route.name = 'expense-workbench-process-flow-edit'
    mocks.route.params = { id: '1' }
    mocks.route.query = {}
    mocks.router.push.mockResolvedValue(undefined)
  })

  afterEach(() => {
    while (mountedWrappers.length) {
      mountedWrappers.pop()?.unmount()
    }
  })

  it('keeps return and refresh actions on the left, removes local create button, and shows the unified floating footer', async () => {
    const wrapper = await mountView()
    const buttons = wrapper.findAll('button')

    expect(buttons[0]?.text()).toContain('返回上一级')
    expect(buttons[1]?.text()).toContain('刷新')
    expect(buttons.some((item) => item.text().includes('新建流程'))).toBe(false)
    expect(wrapper.get('[data-testid="flow-canvas-scroll"]').classes()).toContain('flow-canvas-scroll')
    expect(wrapper.get('[data-testid="designer-side-panel"]').classes()).toContain('designer-side-scroll')

    const floatingBar = wrapper.get('[data-testid="process-flow-designer-floating-bar"]')
    expect(floatingBar.classes()).toContain('process-flow-designer-floating-bar')
    expect(wrapper.get('[data-testid="process-flow-designer-floating-bar-inner"]').classes()).toContain(
      'process-flow-designer-floating-bar__inner'
    )
    expect(floatingBar.text()).toContain('保存草稿')
    expect(floatingBar.text()).toContain('发布流程')
    expect(floatingBar.text()).toContain('停用流程')

    const footerButtons = buttons.filter((item) => (
      item.text().includes('保存草稿') || item.text().includes('发布流程') || item.text().includes('停用流程')
    ))
    expect(footerButtons).toHaveLength(3)
    expect(footerButtons.every((item) => item.classes().includes('process-flow-designer-floating-bar__button'))).toBe(true)
    expect(footerButtons.some((item) => item.classes().includes('process-flow-designer-floating-bar__button--success'))).toBe(true)
  })

  it('shows current flow name in branch summary tag after selecting a branch node', async () => {
    const wrapper = await mountView('Travel Approval Flow')

    await wrapper.get('[data-testid="select-branch"]').trigger('click')
    await flushPromises()

    const tagTexts = wrapper.findAll('[data-testid="tag"]').map((item) => item.text())
    expect(tagTexts).toContain('Travel Approval Flow')
  })

  it('falls back to unnamed flow label when current flow name is empty', async () => {
    const wrapper = await mountView('')

    await wrapper.get('[data-testid="select-branch"]').trigger('click')
    await flushPromises()

    const tagTexts = wrapper.findAll('[data-testid="tag"]').map((item) => item.text())
    expect(tagTexts).toContain('未命名流程')
  })

  it('deletes the selected node after pressing Delete and confirming once', async () => {
    mocks.elMessageBox.confirm.mockResolvedValue(undefined)

    const wrapper = await mountView('Travel Approval Flow')
    await wrapper.get('[data-testid="select-approval-root"]').trigger('click')
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Delete', bubbles: true }))
    await flushPromises()

    expect(mocks.elMessageBox.confirm).toHaveBeenCalledTimes(1)
    expect(mocks.elMessage.success).toHaveBeenCalledWith('节点已删除')
  })

  it('does not trigger Delete shortcut when focus is inside an input', async () => {
    mocks.elMessageBox.confirm.mockResolvedValue(undefined)

    const wrapper = await mountView('Travel Approval Flow')
    await wrapper.get('[data-testid="select-approval-root"]').trigger('click')

    const input = wrapper.find('input')
    input.element.dispatchEvent(new KeyboardEvent('keydown', { key: 'Delete', bubbles: true }))
    await flushPromises()

    expect(mocks.elMessageBox.confirm).not.toHaveBeenCalled()
  })

  it('does not trigger Delete shortcut when only a route is selected', async () => {
    mocks.elMessageBox.confirm.mockResolvedValue(undefined)

    const wrapper = await mountView('Travel Approval Flow')
    await wrapper.get('[data-testid="select-route"]').trigger('click')
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Delete', bubbles: true }))
    await flushPromises()

    expect(mocks.elMessageBox.confirm).not.toHaveBeenCalled()
  })

  it('confirms before moving a node to a new insert position', async () => {
    mocks.elMessageBox.confirm.mockResolvedValue(undefined)

    const wrapper = await mountView('Travel Approval Flow')
    await wrapper.get('[data-testid="move-lane-node-to-root"]').trigger('click')
    await wrapper.get('[data-testid="drop-lane-node-to-root"]').trigger('click')
    await flushPromises()

    expect(mocks.elMessageBox.confirm).toHaveBeenCalledWith(
      '确定将当前节点移动到这个位置吗？',
      '调整节点位置',
      expect.objectContaining({
        type: 'warning',
        confirmButtonText: '确认修改',
        cancelButtonText: '取消'
      })
    )
    expect(mocks.elMessage.success).toHaveBeenCalledWith('节点位置已调整')
  })

  it('moves the attached lane to the left and shows the attach badge after enabling attach below nodes', async () => {
    const wrapper = await mountView('Travel Approval Flow')

    await wrapper.get('[data-testid="select-route-2"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="attach-below-switch"]').trigger('click')
    await flushPromises()

    const routePills = wrapper.findAll('.route-pill').map((item) => item.text())
    expect(routePills[0]).toContain('分支 B')
    expect(routePills[1]).toContain('分支 A')
    expect(wrapper.text()).toContain('附带下方节点')
  })

  it('persists attachBelowNodes=false after turning off an attached route and saving draft', async () => {
    const wrapper = await mountView('Travel Approval Flow', buildFlowDetail('Travel Approval Flow', [true, false]))

    await wrapper.get('[data-testid="select-route"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="attach-below-switch"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="process-flow-designer-floating-bar"]').findAll('button')[0].trigger('click')
    await flushPromises()

    expect(mocks.processApi.updateFlow).toHaveBeenCalledTimes(1)
    const payload = mocks.processApi.updateFlow.mock.calls[0][1]
    expect(payload.routes.find((item: any) => item.routeKey === 'route-1')?.attachBelowNodes).toBe(false)
    expect(payload.routes.find((item: any) => item.routeKey === 'route-2')?.attachBelowNodes).toBe(false)

    expect(wrapper.getComponent(SwitchStub).props('modelValue')).toBe(false)
  })

  it('persists attachBelowNodes=true on the newly enabled route and keeps it first after saving draft', async () => {
    const wrapper = await mountView('Travel Approval Flow', buildFlowDetail('Travel Approval Flow', [true, false]))

    await wrapper.get('[data-testid="select-route-2"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="attach-below-switch"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="process-flow-designer-floating-bar"]').findAll('button')[0].trigger('click')
    await flushPromises()

    expect(mocks.processApi.updateFlow).toHaveBeenCalledTimes(1)
    const payload = mocks.processApi.updateFlow.mock.calls[0][1]
    expect(payload.routes.find((item: any) => item.routeKey === 'route-2')).toMatchObject({
      priority: 1,
      attachBelowNodes: true
    })
    expect(payload.routes.find((item: any) => item.routeKey === 'route-1')).toMatchObject({
      priority: 2,
      attachBelowNodes: false
    })

    expect(wrapper.getComponent(SwitchStub).props('modelValue')).toBe(true)
    const routePills = wrapper.findAll('.route-pill').map((item) => item.text())
    expect(routePills[0]).toContain('B')
    expect(routePills[1]).toContain('A')
  })

})
