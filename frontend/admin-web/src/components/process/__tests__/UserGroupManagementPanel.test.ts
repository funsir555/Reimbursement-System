import { flushPromises, mount } from '@vue/test-utils'
import { computed, defineComponent } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import UserGroupManagementPanel from '@/components/process/UserGroupManagementPanel.vue'

const mocks = vi.hoisted(() => ({
  processApi: {
    listUserGroupTree: vi.fn(),
    getUserGroupMeta: vi.fn(),
    getUserGroupDetail: vi.fn(),
    createUserGroup: vi.fn(),
    updateUserGroup: vi.fn(),
    deleteUserGroup: vi.fn()
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

vi.mock('@element-plus/icons-vue', async () => {
  const actual = await vi.importActual<typeof import('@element-plus/icons-vue')>('@element-plus/icons-vue')
  return {
    ...actual,
    Check: { template: '<span />' },
    CircleCheckFilled: { template: '<span />' },
    Delete: { template: '<span />' },
    Files: { template: '<span />' },
    Plus: { template: '<span />' },
    Search: { template: '<span />' }
  }
})

const SimpleContainer = defineComponent({
  template: '<div><slot name="header" /><slot /><slot name="footer" /></div>'
})

const ButtonStub = defineComponent({
  props: {
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

const SelectStub = defineComponent({
  props: {
    modelValue: {
      type: [String, Number, Array],
      default: ''
    }
  },
  emits: ['update:modelValue'],
  template: '<div><slot /></div>'
})

const OptionStub = defineComponent({
  template: '<div><slot /></div>'
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

const EmptyStub = defineComponent({
  props: {
    description: {
      type: String,
      default: ''
    }
  },
  template: '<div>{{ description }}<slot /></div>'
})

const DialogStub = defineComponent({
  props: {
    modelValue: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue'],
  template: '<div v-if="modelValue"><slot /><slot name="footer" /></div>'
})

const TreeStub = defineComponent({
  props: {
    data: {
      type: Array,
      default: () => []
    }
  },
  emits: ['node-click'],
  setup(props, { emit, expose }) {
    function flatten(nodes: any[]): any[] {
      return nodes.flatMap((item) => [item, ...flatten(item.children || [])])
    }
    const flatData = computed(() => flatten(props.data as any[]))
    expose({
      setCurrentKey: () => undefined
    })
    return {
      flatData,
      emitNode: (item: any) => emit('node-click', item)
    }
  },
  template: `
    <div>
      <button
        v-for="item in flatData"
        :key="item.id"
        type="button"
        :data-testid="'tree-node-' + item.id"
        @click="emitNode(item)"
      >
        <slot :data="item" />
      </button>
    </div>
  `
})

const ConditionEditorStub = defineComponent({
  props: {
    fields: {
      type: Array,
      default: () => []
    },
    optionSources: {
      type: Object,
      default: () => ({})
    },
    groups: {
      type: Array,
      default: () => []
    }
  },
  template: '<div data-testid="condition-editor">condition-editor</div>'
})

const treeData = [
  {
    id: 1,
    parentId: undefined,
    groupCode: '0001',
    groupName: '行政中心',
    codeLevel: 1,
    children: [
      {
        id: 2,
        parentId: 1,
        groupCode: '000101',
        groupName: '差旅分配组',
        codeLevel: 2,
        children: [
          {
            id: 3,
            parentId: 2,
            groupCode: '00010101',
            groupName: '华东差旅组',
            codeLevel: 3,
            children: []
          }
        ]
      }
    ]
  }
]

const details = {
  1: {
    id: 1,
    parentId: undefined,
    groupCode: '0001',
    groupName: '行政中心',
    codeLevel: 1,
    memberUserIds: [],
    scopeConditionGroups: []
  },
  2: {
    id: 2,
    parentId: 1,
    groupCode: '000101',
    groupName: '差旅分配组',
    codeLevel: 2,
    memberUserIds: [],
    scopeConditionGroups: []
  },
  3: {
    id: 3,
    parentId: 2,
    groupCode: '00010101',
    groupName: '华东差旅组',
    codeLevel: 3,
    memberUserIds: ['101', '102'],
    scopeConditionGroups: [
      {
        groupNo: 1,
        conditions: [
          {
            fieldKey: 'paymentCompanyId',
            operator: 'IN',
            compareValue: ['A_COMPANY']
          }
        ]
      }
    ]
  }
} as const

const meta = {
  scopeConditionFields: [
    {
      key: 'submitterDeptIdWithChildren',
      label: '提单人部门（含下级）',
      valueType: 'department',
      operatorKeys: ['IN', 'NOT_IN']
    },
    {
      key: 'submitterDeptIdExact',
      label: '提单人部门（不含下级）',
      valueType: 'department',
      operatorKeys: ['IN', 'NOT_IN']
    },
    {
      key: 'paymentCompanyId',
      label: '公司抬头',
      valueType: 'company',
      operatorKeys: ['IN', 'NOT_IN']
    },
    {
      key: 'actualPaymentAmount',
      label: '实际支付金额',
      valueType: 'number',
      operatorKeys: ['IN', 'NOT_IN']
    }
  ],
  scopeOperatorOptions: [
    { label: '属于', value: 'IN' },
    { label: '不属于', value: 'NOT_IN' }
  ],
  companyOptions: [
    { label: 'A公司', value: 'A_COMPANY' }
  ],
  departmentOptions: [
    { label: '行政中心', value: '1' }
  ],
  userOptions: [
    { label: '张三', value: '101' },
    { label: '李四', value: '102' }
  ]
}

async function mountPanel() {
  mocks.processApi.listUserGroupTree.mockResolvedValue({ data: treeData })
  mocks.processApi.getUserGroupMeta.mockResolvedValue({ data: meta })
  mocks.processApi.getUserGroupDetail.mockImplementation(async (id: number) => ({ data: details[id as 1 | 2 | 3] }))
  mocks.processApi.createUserGroup.mockImplementation(async (payload: any) => ({
    data: {
      id: 9,
      groupCode: '0002',
      codeLevel: 1,
      memberUserIds: [],
      scopeConditionGroups: [],
      ...payload
    }
  }))
  mocks.processApi.updateUserGroup.mockImplementation(async (_id: number, payload: any) => ({
    data: {
      ...payload,
      id: 3,
      groupCode: '00010101',
      codeLevel: 3
    }
  }))

  const wrapper = mount(UserGroupManagementPanel, {
    global: {
      stubs: {
        ProcessConditionGroupEditor: ConditionEditorStub,
        'el-card': SimpleContainer,
        'el-button': ButtonStub,
        'el-input': InputStub,
        'el-select': SelectStub,
        'el-option': OptionStub,
        'el-form': SimpleContainer,
        'el-form-item': FormItemStub,
        'el-empty': EmptyStub,
        'el-dialog': DialogStub,
        'el-tree': TreeStub,
        'el-tag': SimpleContainer,
        'el-icon': SimpleContainer,
        'el-skeleton': SimpleContainer
      },
      directives: {
        loading: () => undefined
      }
    }
  })

  await flushPromises()
  return wrapper
}

describe('UserGroupManagementPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.elMessageBox.confirm.mockResolvedValue('confirm')
  })

  it('loads tree/meta on mount and auto-opens the first group detail', async () => {
    const wrapper = await mountPanel()

    expect(mocks.processApi.listUserGroupTree).toHaveBeenCalledTimes(1)
    expect(mocks.processApi.getUserGroupMeta).toHaveBeenCalledTimes(1)
    expect(mocks.processApi.getUserGroupDetail).toHaveBeenCalledWith(1)
    expect(wrapper.text()).toContain('行政中心')
    const summaryGrid = wrapper.get('[data-testid="user-group-summary-grid"]')
    expect(summaryGrid.classes()).toContain('expense-wb-stat-grid--compact')
    expect(summaryGrid.text()).toContain('用户组总数')
    expect(wrapper.text()).not.toContain('按三级树维护审批分配用的自定义组织。')
    expect(wrapper.get('[data-testid="tree-node-1"]').text()).toBe('行政中心')
    expect(wrapper.get('[data-testid="tree-node-2"]').text()).toBe('差旅分配组')
    expect(wrapper.get('[data-testid="tree-node-3"]').text()).toBe('华东差旅组')
  })

  it('loads a level-3 group detail and saves it through updateUserGroup', async () => {
    const wrapper = await mountPanel()

    await wrapper.get('[data-testid="tree-node-3"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('华东差旅组')
    expect(wrapper.get('[data-testid="user-group-scope-trigger"]').text()).toContain('已配置 1 组条件 / 1 条逻辑')

    await wrapper.findAll('button').find((item) => item.text() === '保存')!.trigger('click')
    await flushPromises()

    expect(mocks.processApi.updateUserGroup).toHaveBeenCalledWith(3, {
      parentId: 2,
      groupName: '华东差旅组',
      memberUserIds: ['101', '102'],
      scopeConditionGroups: [
        {
          groupNo: 1,
          conditions: [
            {
              fieldKey: 'paymentCompanyId',
              operator: 'IN',
              compareValue: ['A_COMPANY']
            }
          ]
        }
      ]
    })
  })

  it('creates a new first-level group through createUserGroup', async () => {
    const wrapper = await mountPanel()

    await wrapper.findAll('button').find((item) => item.text() === '新增一级组')!.trigger('click')
    await flushPromises()

    const nameInput = wrapper.findAll('input')[1]!
    await nameInput.setValue('新的一级组')
    await wrapper.findAll('button').find((item) => item.text() === '保存')!.trigger('click')
    await flushPromises()

    expect(mocks.processApi.createUserGroup).toHaveBeenCalledWith({
      parentId: undefined,
      groupName: '新的一级组',
      memberUserIds: [],
      scopeConditionGroups: []
    })
  })

  it('passes the standardized submitter department scope fields into the shared editor', async () => {
    const wrapper = await mountPanel()

    await wrapper.get('[data-testid="tree-node-3"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="user-group-scope-trigger"]').trigger('click')
    await flushPromises()

    const editor = wrapper.getComponent(ConditionEditorStub)
    expect(editor.props('fields')).toEqual(expect.arrayContaining([
      expect.objectContaining({ key: 'submitterDeptIdWithChildren', label: '提单人部门（含下级）', valueType: 'department' }),
      expect.objectContaining({ key: 'submitterDeptIdExact', label: '提单人部门（不含下级）', valueType: 'department' })
    ]))
    expect(editor.props('optionSources')).toMatchObject({
      company: [{ label: 'A公司', value: 'A_COMPANY' }],
      department: [{ label: '行政中心', value: '1' }],
      user: [
        { label: '张三', value: '101' },
        { label: '李四', value: '102' }
      ]
    })
  })
})
