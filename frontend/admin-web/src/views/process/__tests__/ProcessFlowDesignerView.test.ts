import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ProcessConditionGroupEditor from '@/components/process/ProcessConditionGroupEditor.vue'
import ProcessFlowDesignerView from '@/views/process/ProcessFlowDesignerView.vue'

const mocks = vi.hoisted(() => ({
  route: {
    name: 'expense-workbench-process-flow-edit',
    params: {
      id: '1'
    },
    query: {},
    fullPath: '/expense/workbench/process-flow/1'
  },
  router: {
    push: vi.fn(),
    back: vi.fn()
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
    ArrowLeft: { template: '<span />' },
    Check: { template: '<span />' },
    Delete: { template: '<span />' },
    RefreshRight: { template: '<span />' }
  }
})

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
  template: '<button type="button" :data-testid="$attrs[\'data-testid\'] || \'switch\'" @click="$emit(\'update:modelValue\', !modelValue); $emit(\'change\', !modelValue)"><slot /></button>'
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
  emits: ['select-node', 'select-route', 'add-route-lane', 'insert-node', 'drag-node-start', 'drag-node-end', 'drag-node-over', 'drop-node'],
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
      <button type="button" data-testid="add-route-lane" @click="$emit('add-route-lane', 'branch-1')">
        add route lane
      </button>
      <button
        type="button"
        data-testid="move-lane-node-to-root"
        @click="$emit('drag-node-start', { nodeKey: 'approval-lane', mode: 'move' })"
      >
        start drag lane node
      </button>
      <button
        type="button"
        data-testid="copy-lane-node-to-root"
        @click="$emit('drag-node-start', { nodeKey: 'approval-lane', mode: 'copy' })"
      >
        copy drag lane node
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
    branchOperatorOptions: [
      { value: 'EQ', label: '等于' },
      { value: 'NE', label: '不等于' },
      { value: 'IN', label: '属于' },
      { value: 'NOT_IN', label: '不属于' }
    ],
    branchConditionFields: [
      { key: 'paymentCompanyId', label: '公司抬头', valueType: 'company', operatorKeys: ['IN', 'NOT_IN'] },
      { key: 'undertakeDeptIdWithChildren', label: '承担部门(含下级部门)', valueType: 'department', operatorKeys: ['IN', 'NOT_IN'] },
      { key: 'undertakeDeptIdExact', label: '承担部门(不含下级部门)', valueType: 'department', operatorKeys: ['IN', 'NOT_IN'] }
    ],
    companyOptions: [{ label: '广州远智教育科技有限公司', value: 'COMPANY_A' }],
    departmentOptions: [{ label: '广州团队', value: '15' }],
    userOptions: [],
    userGroupOptions: [],
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
        defaultRoute: false,
        attachBelowNodes: routeAttachFlags[0],
        conditionGroups: []
      },
      {
        routeKey: 'route-2',
        sourceNodeKey: 'branch-1',
        routeName: '分支 B',
        priority: 2,
        defaultRoute: false,
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
  detail = buildFlowDetail(flowName),
  options?: { detailError?: Error; meta?: Record<string, unknown> }
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
  mocks.processApi.getFlowMeta.mockResolvedValue({ data: { ...buildFlowMeta(), ...(options?.meta || {}) } })
  if (options?.detailError) {
    mocks.processApi.getFlowDetail.mockRejectedValue(options.detailError)
  } else {
    mocks.processApi.getFlowDetail.mockResolvedValue({ data: detail })
  }
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
    mocks.route.fullPath = '/expense/workbench/process-flow/1'
    mocks.router.push.mockResolvedValue(undefined)
    mocks.router.back.mockResolvedValue(undefined)
  })

  afterEach(() => {
    while (mountedWrappers.length) {
      mountedWrappers.pop()?.unmount()
    }
  })

  it('keeps return and refresh actions on the left, removes local create button, and shows the canvas toolbar actions', async () => {
    const wrapper = await mountView()
    const buttons = wrapper.findAll('button')

    expect(buttons[0]?.text()).toContain('返回上一级')
    expect(buttons[1]?.text()).toContain('刷新')
    expect(buttons.some((item) => item.text().includes('新建流程'))).toBe(false)
    expect(wrapper.get('[data-testid="flow-canvas-shell"]').classes()).toContain('flow-canvas-shell')
    expect(wrapper.get('[data-testid="flow-canvas-scroll"]').classes()).toContain('flow-canvas-scroll')
    expect(wrapper.get('[data-testid="flow-canvas-surface"]').classes()).toContain('flow-canvas-surface')
    expect(wrapper.get('[data-testid="flow-track"]').classes()).toContain('flow-track')
    expect(wrapper.get('[data-testid="designer-side-panel"]').classes()).toContain('designer-side-scroll')
    expect(wrapper.get('[data-testid="designer-config-drawer"]').classes()).not.toContain('is-open')
    expect(wrapper.find('[data-testid="process-flow-designer-floating-bar"]').exists()).toBe(false)

    const toolbar = wrapper.get('[data-testid="flow-canvas-toolbar"]')
    expect(toolbar.text()).toContain('保存草稿')
    expect(toolbar.text()).toContain('发布流程')
    expect(toolbar.text()).toContain('停用流程')

    const toolbarButtons = buttons.filter((item) => (
      item.text().includes('保存草稿') || item.text().includes('发布流程') || item.text().includes('停用流程')
    ))
    expect(toolbarButtons).toHaveLength(3)
  })

  it('returns to the page that opened the designer when returnTo is present', async () => {
    mocks.route.query = { returnTo: '/expense/workbench/process-management?section=approval-flow' }
    const wrapper = await mountView()
    mocks.router.push.mockClear()
    mocks.router.back.mockClear()

    await wrapper.findAll('button')[0]!.trigger('click')

    expect(mocks.router.push).toHaveBeenCalledWith('/expense/workbench/process-management?section=approval-flow')
    expect(mocks.router.back).not.toHaveBeenCalled()
  })

  it('falls back to browser history when returnTo is missing', async () => {
    const historyLengthDescriptor = Object.getOwnPropertyDescriptor(window.history, 'length')
    Object.defineProperty(window.history, 'length', {
      configurable: true,
      value: 2
    })

    try {
      const wrapper = await mountView()
      mocks.router.push.mockClear()
      mocks.router.back.mockClear()
      await wrapper.findAll('button')[0]!.trigger('click')

      expect(mocks.router.back).toHaveBeenCalledTimes(1)
      expect(mocks.router.push).not.toHaveBeenCalled()
    } finally {
      if (historyLengthDescriptor) {
        Object.defineProperty(window.history, 'length', historyLengthDescriptor)
      }
    }
  })

  it('opens the drawer and first route panel after selecting a branch node', async () => {
    const wrapper = await mountView('Travel Approval Flow')

    await wrapper.get('[data-testid="select-branch"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="designer-config-drawer"]').classes()).toContain('is-open')
    expect(wrapper.find('[data-testid="attach-below-switch"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="designer-side-panel"] input[value="分支 A"]').exists()).toBe(true)
    expect(wrapper.find('.route-pill-grid').exists()).toBe(false)
  })

  it('still opens the first route panel when current flow name is empty', async () => {
    const wrapper = await mountView('')

    await wrapper.get('[data-testid="select-branch"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="designer-config-drawer"]').classes()).toContain('is-open')
    expect(wrapper.find('[data-testid="attach-below-switch"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="designer-side-panel"] input[value="分支 A"]').exists()).toBe(true)
  })

  it('collapses the drawer and clears selection after clicking blank canvas', async () => {
    const wrapper = await mountView('Travel Approval Flow')

    await wrapper.get('[data-testid="select-branch"]').trigger('click')
    await flushPromises()

    const scroll = wrapper.get('[data-testid="flow-canvas-scroll"]')
    const pointerDownEvent = new Event('pointerdown', { bubbles: true })
    Object.defineProperties(pointerDownEvent, {
      button: { value: 0 },
      clientX: { value: 120 },
      clientY: { value: 120 },
      pointerId: { value: 1 }
    })
    scroll.element.dispatchEvent(pointerDownEvent)

    const pointerUpEvent = new Event('pointerup', { bubbles: true })
    Object.defineProperties(pointerUpEvent, {
      button: { value: 0 },
      clientX: { value: 120 },
      clientY: { value: 120 },
      pointerId: { value: 1 }
    })
    scroll.element.dispatchEvent(pointerUpEvent)
    await flushPromises()

    expect(wrapper.get('[data-testid="designer-config-drawer"]').classes()).not.toContain('is-open')
    expect(wrapper.find('[data-testid="attach-below-switch"]').exists()).toBe(false)
  })

  it('collapses and reopens the drawer manually without losing the current selection', async () => {
    const wrapper = await mountView('Travel Approval Flow')

    await wrapper.get('[data-testid="select-route-2"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="designer-config-drawer"]').classes()).toContain('is-open')
    await wrapper.get('[data-testid="designer-config-drawer-toggle"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="designer-config-drawer"]').classes()).not.toContain('is-open')
    expect(wrapper.find('[data-testid="designer-side-panel"] input[value="分支 B"]').exists()).toBe(true)

    await wrapper.get('[data-testid="designer-config-drawer-handle"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="designer-config-drawer"]').classes()).toContain('is-open')
    expect(wrapper.find('[data-testid="designer-side-panel"] input[value="分支 B"]').exists()).toBe(true)
  })

  it('moves compare value to its own row and removes the conditionGroups helper copy', async () => {
    const detail = buildFlowDetail('Travel Approval Flow')
    detail.routes[0].conditionGroups = [
      {
        groupNo: 1,
        conditions: [
          {
            fieldKey: 'amount',
            operator: 'EQ',
            compareValue: '100'
          }
        ]
      }
    ]

    const wrapper = await mountView('Travel Approval Flow', detail)
    await wrapper.get('[data-testid="select-route"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).not.toContain('分支条件直接保存在当前 route 的')
    expect(wrapper.find('.process-flow-condition-primary-grid').exists()).toBe(true)
    expect(wrapper.find('[data-testid="process-flow-condition-value-row"]').exists()).toBe(true)
    expect(wrapper.html().indexOf('process-flow-condition-primary-grid')).toBeLessThan(
      wrapper.html().indexOf('process-flow-condition-value-row')
    )
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

  it('deletes the selected route after pressing Delete and confirming once', async () => {
    mocks.elMessageBox.confirm.mockResolvedValue(undefined)

    const detail = buildFlowDetail('Travel Approval Flow')
    detail.routes.push({
      routeKey: 'route-3',
      sourceNodeKey: 'branch-1',
      routeName: '分支 C',
      priority: 3,
      defaultRoute: false,
      attachBelowNodes: false,
      conditionGroups: []
    })

    const wrapper = await mountView('Travel Approval Flow', detail)
    await wrapper.get('[data-testid="select-route"]').trigger('click')
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Delete', bubbles: true }))
    await flushPromises()

    expect(mocks.elMessageBox.confirm).toHaveBeenCalledTimes(1)
    expect(mocks.elMessage.success).toHaveBeenCalledWith('分支已删除')
  })

  it('keeps route delete actions inside the route panel and selects the newly added route', async () => {
    const wrapper = await mountView('Travel Approval Flow')

    await wrapper.get('[data-testid="select-route"]').trigger('click')
    await flushPromises()

    const routeDeleteButtons = wrapper.findAll('button').filter((item) => item.text().includes('删除当前分支'))
    expect(routeDeleteButtons).toHaveLength(1)
    expect(wrapper.find('[data-testid="remove-branch-block-button"]').exists()).toBe(true)

    await wrapper.get('[data-testid="add-route-lane"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="designer-side-panel"] input[value*="条件分支"]').exists()).toBe(true)
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

  it('copies an approval node through the existing drop slot and preserves the original node on save', async () => {
    const wrapper = await mountView('Travel Approval Flow')

    await wrapper.get('[data-testid="copy-lane-node-to-root"]').trigger('click')
    await wrapper.get('[data-testid="drop-lane-node-to-root"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="flow-canvas-toolbar"]').findAll('button')[0].trigger('click')
    await flushPromises()

    expect(mocks.elMessageBox.confirm).not.toHaveBeenCalled()
    expect(mocks.elMessage.success).toHaveBeenCalledWith('节点副本已添加')
    expect(mocks.processApi.updateFlow).toHaveBeenCalledTimes(1)

    const payload = mocks.processApi.updateFlow.mock.calls[0][1]
    const originalNode = payload.nodes.find((item: any) => item.nodeKey === 'approval-lane')
    const copiedNode = payload.nodes.find((item: any) => (
      item.nodeName === '泳道审批节点' &&
      item.nodeKey !== 'approval-lane'
    ))

    expect(originalNode).toBeTruthy()
    expect(copiedNode).toBeTruthy()
    expect(copiedNode.nodeType).toBe('APPROVAL')
    expect(copiedNode.parentNodeKey).toBe('')
    expect(copiedNode.config).toEqual(originalNode.config)
  })

  it('passes the expanded branch condition fields into the shared condition editor', async () => {
    const detail = buildFlowDetail('Travel Approval Flow')
    detail.routes[0].conditionGroups = [
      {
        groupNo: 1,
        conditions: [
          {
            fieldKey: 'paymentCompanyId',
            operator: 'IN',
            compareValue: ['COMPANY_A']
          }
        ]
      }
    ]

    const wrapper = await mountView('Travel Approval Flow', detail)
    await wrapper.get('[data-testid="select-route"]').trigger('click')
    await flushPromises()

    const editor = wrapper.getComponent(ProcessConditionGroupEditor)
    expect(editor.props('fields')).toEqual(expect.arrayContaining([
      expect.objectContaining({ key: 'paymentCompanyId', label: '公司抬头', valueType: 'company' }),
      expect.objectContaining({ key: 'undertakeDeptIdWithChildren', label: '承担部门(含下级部门)', valueType: 'department' }),
      expect.objectContaining({ key: 'undertakeDeptIdExact', label: '承担部门(不含下级部门)', valueType: 'department' })
    ]))
    expect(editor.props('optionSources')).toMatchObject({
      company: [{ label: '广州远智教育科技有限公司', value: 'COMPANY_A' }],
      department: [{ label: '广州团队', value: '15' }]
    })
  })

  it('shows the else branch helper and hides condition editing when the selected route is a default route', async () => {
    const detail = buildFlowDetail('Travel Approval Flow')
    detail.routes[0].defaultRoute = true
    detail.routes[0].conditionGroups = [
      {
        groupNo: 1,
        conditions: [
          {
            fieldKey: 'paymentCompanyId',
            operator: 'IN',
            compareValue: ['COMPANY_A']
          }
        ]
      }
    ]

    const wrapper = await mountView('Travel Approval Flow', detail)
    await wrapper.get('[data-testid="select-route"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="default-route-switch"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('不满足所有条件时进入该分支')
    expect(wrapper.text()).toContain('当前分支已作为 else 分支')
    expect(wrapper.findComponent(ProcessConditionGroupEditor).exists()).toBe(false)
  })

  it('saves the selected route as the only default branch after enabling else mode', async () => {
    const detail = buildFlowDetail('Travel Approval Flow')
    detail.routes[0].conditionGroups = [
      {
        groupNo: 1,
        conditions: [
          {
            fieldKey: 'paymentCompanyId',
            operator: 'IN',
            compareValue: ['COMPANY_A']
          }
        ]
      }
    ]

    const wrapper = await mountView('Travel Approval Flow', detail)
    await wrapper.get('[data-testid="select-route"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="default-route-switch"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="flow-canvas-toolbar"]').findAll('button')[0].trigger('click')
    await flushPromises()

    expect(mocks.processApi.updateFlow).toHaveBeenCalledTimes(1)
    const payload = mocks.processApi.updateFlow.mock.calls[0][1]
    expect(payload.routes.find((item: any) => item.routeKey === 'route-1')).toMatchObject({
      defaultRoute: true,
      conditionGroups: []
    })
    expect(payload.routes.find((item: any) => item.routeKey === 'route-2')?.defaultRoute).toBe(false)
  })

  it('moves the attached lane to the left and shows the attach badge after enabling attach below nodes', async () => {
    const wrapper = await mountView('Travel Approval Flow')

    await wrapper.get('[data-testid="select-route-2"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="attach-below-switch"]').trigger('click')
    await flushPromises()

    await wrapper.get('[data-testid="select-branch"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="designer-side-panel"] input[value="分支 B"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('附带下方节点')
    expect(wrapper.text()).not.toContain('开启后，当前泳道会自动排到最左侧，并承接当前分支块后方的公共尾部节点。')
    expect(wrapper.text()).not.toContain('已开启')
    expect(wrapper.text()).not.toContain('未开启')
  })

  it('persists attachBelowNodes=false after turning off an attached route and saving draft', async () => {
    const wrapper = await mountView('Travel Approval Flow', buildFlowDetail('Travel Approval Flow', [true, false]))

    await wrapper.get('[data-testid="select-route"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="attach-below-switch"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="flow-canvas-toolbar"]').findAll('button')[0].trigger('click')
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
    await wrapper.get('[data-testid="flow-canvas-toolbar"]').findAll('button')[0].trigger('click')
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
    await wrapper.get('[data-testid="select-branch"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-testid="designer-side-panel"] input[value="分支 B"]').exists()).toBe(true)
  })

  it('keeps flow name, flow description, and flow code in the shared top row and still saves flowDescription', async () => {
    const wrapper = await mountView('Travel Approval Flow')

    const metaGrid = wrapper.get('[data-testid="flow-meta-grid"]')
    expect(metaGrid.classes()).toContain('flow-meta-grid')
    expect(metaGrid.text()).toContain('流程名称')
    expect(metaGrid.text()).toContain('流程说明')
    expect(metaGrid.text()).toContain('流程编码')

    const descriptionInput = wrapper.findAll('input').find((item) => (item.element as HTMLInputElement).value === 'flow description')
    expect(descriptionInput).toBeTruthy()
    await descriptionInput!.setValue('updated flow description')
    await wrapper.get('[data-testid="flow-canvas-toolbar"]').findAll('button')[0].trigger('click')
    await flushPromises()

    expect(mocks.processApi.updateFlow).toHaveBeenCalledTimes(1)
    expect(mocks.processApi.updateFlow.mock.calls[0][1].flowDescription).toBe('updated flow description')
  })

  it('forces manager approval nodes with managerLevel > 1 to save as AND_SIGN and shows the countersign hint', async () => {
    const detail = buildFlowDetail('Travel Approval Flow')
    detail.nodes[0]!.config = {
      approverType: 'MANAGER',
      missingHandler: 'AUTO_SKIP',
      approvalMode: 'OR_SIGN',
      opinionDefaults: [],
      specialSettings: [],
      managerConfig: {
        ruleMode: 'FORM_DEPT_MANAGER',
        deptSource: 'UNDERTAKE_DEPT',
        managerLevel: 2,
        orgTreeLookupEnabled: true,
        orgTreeLookupLevel: 1
      },
      designatedMemberConfig: { userIds: [] },
      manualSelectConfig: { candidateScope: 'ALL_ACTIVE_USERS' }
    }

    const wrapper = await mountView('Travel Approval Flow', detail)

    await wrapper.get('[data-testid="select-approval-root"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('第 1..N 级主管共同审批')

    await wrapper.get('[data-testid="flow-canvas-toolbar"]').findAll('button')[0]!.trigger('click')
    await flushPromises()

    expect(mocks.processApi.updateFlow).toHaveBeenCalledTimes(1)
    const payload = mocks.processApi.updateFlow.mock.calls[0][1]
    expect(payload.nodes.find((item: any) => item.nodeKey === 'approval-root')?.config.approvalMode).toBe('AND_SIGN')
  })

  it('keeps designated user-group approval in AND_SIGN and shows the user-group countersign hint', async () => {
    const detail = buildFlowDetail('Travel Approval Flow')
    detail.nodes[0]!.config = {
      approverType: 'DESIGNATED_USER_GROUP',
      missingHandler: 'AUTO_SKIP',
      approvalMode: 'OR_SIGN',
      opinionDefaults: [],
      specialSettings: [],
      managerConfig: {},
      designatedMemberConfig: { userIds: [] },
      designatedUserGroupConfig: { groupId: 2001 },
      manualSelectConfig: { candidateScope: 'ALL_ACTIVE_USERS' }
    }

    const wrapper = await mountView('Travel Approval Flow', detail, {
      meta: {
        userGroupOptions: [
          { label: '行政中心 / 差旅分配组', value: '2001' }
        ]
      }
    })

    await wrapper.get('[data-testid="select-approval-root"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('指定用户组模式固定按会签处理')

    await wrapper.get('[data-testid="flow-canvas-toolbar"]').findAll('button')[0]!.trigger('click')
    await flushPromises()

    expect(mocks.processApi.updateFlow).toHaveBeenCalledTimes(1)
    const payload = mocks.processApi.updateFlow.mock.calls[0][1]
    expect(payload.nodes.find((item: any) => item.nodeKey === 'approval-root')?.config).toMatchObject({
      approverType: 'DESIGNATED_USER_GROUP',
      approvalMode: 'AND_SIGN',
      designatedUserGroupConfig: { groupId: 2001 }
    })
  })


  it('prefills copied approval flows in create mode and still saves through createFlow', async () => {
    mocks.route.name = 'expense-workbench-process-flow-create'
    mocks.route.params = {}
    mocks.route.query = { copyFromId: '1' }
    mocks.processApi.createFlow.mockImplementation(async (payload: any) => ({ data: buildSavedFlowDetail(payload) }))

    const wrapper = await mountView('Travel Approval Flow')

    expect(mocks.processApi.getFlowDetail).toHaveBeenCalledWith(1)
    expect(
      wrapper.findAll('input').some((item) => (item.element as HTMLInputElement).value === 'Travel Approval Flow-\u526f\u672c')
    ).toBe(true)

    await wrapper.findAll('button').find((item) => item.text().includes('\u4fdd\u5b58\u8349\u7a3f'))!.trigger('click')
    await flushPromises()

    expect(mocks.processApi.createFlow).toHaveBeenCalledWith(expect.objectContaining({
      flowName: 'Travel Approval Flow-\u526f\u672c',
      flowDescription: 'flow description',
      nodes: expect.any(Array),
      routes: expect.any(Array)
    }))
    expect(mocks.processApi.updateFlow).not.toHaveBeenCalled()
  })

  it('falls back to a blank approval flow create page when copy source loading fails', async () => {
    mocks.route.name = 'expense-workbench-process-flow-create'
    mocks.route.params = {}
    mocks.route.query = { copyFromId: '1' }

    const wrapper = await mountView('Travel Approval Flow', buildFlowDetail('Travel Approval Flow'), {
      detailError: new Error('\u6e90\u6d41\u7a0b\u4e0d\u5b58\u5728')
    })

    expect(mocks.elMessage.error).toHaveBeenCalledWith('\u6e90\u6d41\u7a0b\u4e0d\u5b58\u5728')
    expect(wrapper.find('input').element.value).toBe('')
  })

})
