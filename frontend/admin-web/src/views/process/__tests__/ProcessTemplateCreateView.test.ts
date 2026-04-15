import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ProcessTemplateCreateView from '@/views/process/ProcessTemplateCreateView.vue'

const mocks = vi.hoisted(() => ({
  route: {
    params: { templateType: 'report' },
    query: {},
    fullPath: '/expense/workbench/process-management/create/report'
  },
  router: {
    push: vi.fn(),
    replace: vi.fn()
  },
  processApi: {
    getOverview: vi.fn(),
    getFormOptions: vi.fn(),
    getTemplateDetail: vi.fn(),
    listFlows: vi.fn(),
    listFormDesigns: vi.fn(),
    createTemplate: vi.fn(),
    updateTemplate: vi.fn()
  },
  elMessage: {
    warning: vi.fn(),
    error: vi.fn(),
    success: vi.fn(),
    info: vi.fn()
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
  ElMessage: mocks.elMessage
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
  Close: { template: '<span />' },
  Plus: { template: '<span />' }
}))

const SimpleContainer = defineComponent({
  template: '<div><slot name="header" /><slot /><slot name="footer" /></div>'
})

const ButtonStub = defineComponent({
  props: {
    loading: {
      type: Boolean,
      default: false
    }
  },
  emits: ['click'],
  template: '<button type="button" :data-loading="loading ? \'true\' : \'false\'" @click="$emit(\'click\', $event)"><slot /></button>'
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
      type: [String, Number],
      default: ''
    }
  },
  emits: ['update:modelValue'],
  template: '<select :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><slot /></select>'
})

const OptionStub = defineComponent({
  props: {
    value: {
      type: [String, Number],
      default: ''
    },
    label: {
      type: String,
      default: ''
    }
  },
  template: '<option :value="value">{{ label }}</option>'
})

const SwitchStub = defineComponent({
  props: {
    modelValue: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue'],
  template: '<input type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)" />'
})

const InputNumberStub = defineComponent({
  props: {
    modelValue: {
      type: Number,
      default: undefined
    }
  },
  emits: ['update:modelValue'],
  template: '<input type="number" :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))" />'
})

const CheckboxGroupStub = defineComponent({
  props: {
    modelValue: {
      type: Array,
      default: () => []
    }
  },
  emits: ['update:modelValue'],
  template: '<div><slot /></div>'
})

const CheckboxStub = defineComponent({
  template: '<label><slot /></label>'
})

const TreeStub = defineComponent({
  setup(_, { slots, expose }) {
    expose({
      filter: vi.fn(),
      setCheckedKeys: vi.fn()
    })
    return () => h('div', slots.default ? slots.default({ data: { expenseName: 'stub' } }) : [])
  }
})

const globalStubs = {
  'process-workbench-sidebar': true,
  'el-form': SimpleContainer,
  'el-card': SimpleContainer,
  'el-form-item': SimpleContainer,
  'el-tag': SimpleContainer,
  'el-dialog': SimpleContainer,
  'el-empty': SimpleContainer,
  'el-button': ButtonStub,
  'el-input': InputStub,
  'el-select': SelectStub,
  'el-option': OptionStub,
  'el-switch': SwitchStub,
  'el-input-number': InputNumberStub,
  'el-checkbox-group': CheckboxGroupStub,
  'el-checkbox': CheckboxStub,
  'el-tree': TreeStub,
  'el-icon': SimpleContainer
}

function buildFormOptions(templateType = 'report', templateTypeLabel = '报销单') {
  return {
    templateType,
    templateTypeLabel,
    categoryOptions: [{ label: '员工报销', value: 'employee-expense' }],
    numberingRulePreview: 'FX202603310001',
    formDesignOptions: [{ label: '测试表单', value: 'FD202603290001' }],
    expenseDetailDesignOptions: templateType === 'report'
      ? [
          {
            id: 2,
            detailCode: 'EDD202603310002',
            detailName: '对公付款',
            detailType: 'ENTERPRISE_TRANSACTION',
            detailTypeLabel: '企业往来',
            detailDescription: '',
            updatedAt: '2026-03-31 12:00'
          }
        ]
      : [],
    expenseDetailModeOptions: templateType === 'report'
      ? [
          { label: '预付未到票', value: 'PREPAY_UNBILLED' },
          { label: '到票全部支付', value: 'INVOICE_FULL_PAYMENT' }
        ]
      : [],
    printModes: [{ label: '默认打印模板', value: 'default-print' }],
    approvalFlows: [{ label: '对公付款审批', value: 'FLOW-001' }],
    paymentModes: [{ label: '不生成付款单', value: 'none' }],
    allocationForms: [{ label: '默认分摊表', value: 'allocation-default' }],
    expenseTypes: [],
    departmentOptions: [],
    aiAuditModes: [{ label: '关闭 AI 审核', value: 'disabled' }],
    tagOptions: [],
    installmentOptions: []
  }
}

function seedDraft(partial: Record<string, unknown>, templateType = 'report') {
  window.sessionStorage.setItem(`process-template-create-draft:${templateType}`, JSON.stringify({
    templateType,
    ...partial
  }))
}

async function mountView() {
  const wrapper = mount(ProcessTemplateCreateView, {
    global: {
      stubs: globalStubs
    }
  })
  await flushPromises()
  return wrapper
}

function findSaveButton(wrapper: Awaited<ReturnType<typeof mountView>>) {
  const button = wrapper.findAll('button').find((item) => item.text().includes('保存模板'))
  expect(button).toBeTruthy()
  return button!
}

describe('ProcessTemplateCreateView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.sessionStorage.clear()
    mocks.route.params = { templateType: 'report' }
    mocks.route.query = {}
    mocks.route.fullPath = '/expense/workbench/process-management/create/report'
    mocks.router.push.mockResolvedValue(undefined)
    mocks.router.replace.mockResolvedValue(undefined)
    mocks.processApi.getOverview.mockResolvedValue({ data: { navItems: [] } })
    mocks.processApi.getFormOptions.mockResolvedValue({ data: buildFormOptions() })
    mocks.processApi.getTemplateDetail.mockResolvedValue({ data: null })
    mocks.processApi.listFlows.mockResolvedValue({ data: [] })
    mocks.processApi.listFormDesigns.mockResolvedValue({ data: [] })
    mocks.processApi.createTemplate.mockResolvedValue({
      data: {
        id: 1,
        templateCode: 'FX202603310001',
        templateName: '对公费用报销',
        status: 'ENABLED'
      }
    })
    mocks.processApi.updateTemplate.mockResolvedValue({
      data: {
        id: 1,
        templateCode: 'FX202603310001',
        templateName: '对公费用报销',
        status: 'ENABLED'
      }
    })
    HTMLElement.prototype.scrollIntoView = vi.fn()
  })

  it('shows blockers and skips save request when required fields are missing', async () => {
    const wrapper = await mountView()

    await findSaveButton(wrapper).trigger('click')
    await flushPromises()

    expect(mocks.processApi.createTemplate).not.toHaveBeenCalled()
    expect(mocks.elMessage.warning).toHaveBeenCalledWith('请先填写单据名称。')
    expect(wrapper.text()).toContain('当前还不能保存模板')
    expect(wrapper.text()).toContain('请先填写单据名称。')
    expect(wrapper.text()).toContain('报销模板必须绑定费用明细表单。')
    expect(findSaveButton(wrapper).attributes('data-loading')).toBe('false')
  })

  it('submits template save payload when blockers are cleared', async () => {
    seedDraft({
      category: 'employee-expense',
      templateName: '对公费用报销',
      formDesign: 'FD202603290001',
      approvalFlow: 'FLOW-001',
      expenseDetailDesign: 'EDD202603310002',
      expenseDetailModeDefault: 'PREPAY_UNBILLED'
    })

    const wrapper = await mountView()

    await findSaveButton(wrapper).trigger('click')
    await flushPromises()

    expect(mocks.processApi.createTemplate).toHaveBeenCalledTimes(1)
    expect(mocks.processApi.createTemplate).toHaveBeenCalledWith(expect.objectContaining({
      templateType: 'report',
      templateName: '对公费用报销',
      formDesign: 'FD202603290001',
      approvalFlow: 'FLOW-001',
      expenseDetailDesign: 'EDD202603310002',
      expenseDetailModeDefault: 'PREPAY_UNBILLED'
    }))
    expect(mocks.elMessage.success).toHaveBeenCalledWith('模板已保存，可在新建报销中直接使用：FX202603310001')
    expect(mocks.router.push).toHaveBeenCalledWith('/expense/workbench/process-management')
  })

  it('does not require expense detail design when creating a contract template', async () => {
    mocks.route.params = { templateType: 'contract' }
    mocks.route.fullPath = '/expense/workbench/process-management/create/contract'
    mocks.processApi.getFormOptions.mockResolvedValue({ data: buildFormOptions('contract', '合同单') })
    seedDraft({
      category: 'business-application',
      templateName: '采购合同模板',
      formDesign: 'FD202603290001',
      approvalFlow: 'FLOW-001'
    }, 'contract')

    const wrapper = await mountView()

    await findSaveButton(wrapper).trigger('click')
    await flushPromises()

    expect(wrapper.text()).not.toContain('报销模板必须绑定费用明细表单。')
    expect(mocks.processApi.createTemplate).toHaveBeenCalledWith(expect.objectContaining({
      templateType: 'contract',
      templateName: '采购合同模板',
      formDesign: 'FD202603290001',
      approvalFlow: 'FLOW-001',
      expenseDetailDesign: ''
    }))
  })

  it('restores save button state when save request fails', async () => {
    seedDraft({
      category: 'employee-expense',
      templateName: '对公费用报销',
      formDesign: 'FD202603290001',
      approvalFlow: 'FLOW-001',
      expenseDetailDesign: 'EDD202603310002',
      expenseDetailModeDefault: 'PREPAY_UNBILLED'
    })
    mocks.processApi.createTemplate.mockRejectedValue(new Error('保存模板超时，请检查后端服务或稍后重试'))

    const wrapper = await mountView()

    await findSaveButton(wrapper).trigger('click')
    await flushPromises()

    expect(mocks.elMessage.error).toHaveBeenCalledWith('保存模板超时，请检查后端服务或稍后重试')
    expect(findSaveButton(wrapper).attributes('data-loading')).toBe('false')
  })

  it('blocks save when template name exceeds 64 characters', async () => {
    seedDraft({
      category: 'employee-expense',
      templateName: 'A'.repeat(65),
      formDesign: 'FD202603290001',
      approvalFlow: 'FLOW-001',
      expenseDetailDesign: 'EDD202603310002',
      expenseDetailModeDefault: 'PREPAY_UNBILLED'
    })

    const wrapper = await mountView()

    await findSaveButton(wrapper).trigger('click')
    await flushPromises()

    expect(mocks.processApi.createTemplate).not.toHaveBeenCalled()
    expect(mocks.elMessage.warning).toHaveBeenCalledWith('模板名称最多 64 个字符')
  })
})
