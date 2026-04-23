import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ExpenseCreateView from '@/views/expense/ExpenseCreateView.vue'

const mocks = vi.hoisted(() => ({
  route: {
    name: 'expense-create',
    params: {},
    query: {},
    fullPath: '/expense/create'
  },
  router: {
    push: vi.fn(),
    replace: vi.fn(),
    back: vi.fn()
  },
  expenseApi: {
    getEditContext: vi.fn(),
    resubmit: vi.fn()
  },
  expenseApprovalApi: {
    getModifyContext: vi.fn(),
    modify: vi.fn()
  },
  expenseCreateApi: {
    listTemplates: vi.fn(),
    getTemplateDetail: vi.fn(),
    submit: vi.fn()
  },
  elMessage: {
    error: vi.fn(),
    warning: vi.fn(),
    success: vi.fn()
  },
  elMessageBox: {
    confirm: vi.fn()
  },
  runtimeEditor: {
    validateBeforeSubmit: vi.fn()
  }
}))

vi.mock('vue-router', () => ({
  useRoute: () => mocks.route,
  useRouter: () => mocks.router
}))

vi.mock('@/api', () => ({
  expenseApi: mocks.expenseApi,
  expenseApprovalApi: mocks.expenseApprovalApi,
  expenseCreateApi: mocks.expenseCreateApi
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
    permissionCodes: ['expense:create:view', 'expense:create:create', 'expense:create:submit', 'expense:list:view']
  }),
  hasPermission: (permissionCode: string, source?: string[] | { permissionCodes?: string[] } | null) => {
    const ownedCodes = Array.isArray(source) ? source : source?.permissionCodes || ['expense:create:view']
    return ownedCodes.includes(permissionCode)
  },
  resolveFirstAccessiblePath: () => '/expense/list'
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
  template: '<input v-bind="$attrs" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
})

const SelectStub = defineComponent({
  props: {
    modelValue: {
      type: [String, Number, Array],
      default: ''
    }
  },
  emits: ['update:modelValue'],
  setup(_props, { attrs, emit }) {
    const onChange = (event: Event) => {
      const target = event.target as HTMLSelectElement
      if (attrs.multiple !== undefined) {
        emit('update:modelValue', Array.from(target.selectedOptions).map((option) => option.value))
        return
      }
      emit('update:modelValue', target.value)
    }
    return { onChange }
  },
  template: '<select v-bind="$attrs" :value="modelValue" @change="onChange"><slot /></select>'
})

const OptionStub = defineComponent({
  props: {
    label: {
      type: String,
      default: ''
    },
    value: {
      type: [String, Number],
      default: ''
    }
  },
  template: '<option :value="value">{{ label }}</option>'
})

const TagStub = defineComponent({
  template: '<span><slot /></span>'
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

const ExpenseRuntimeFormEditorStub = defineComponent({
  props: {
    modelValue: {
      type: Object,
      default: () => ({})
    },
    currentUserCompanyId: {
      type: String,
      default: ''
    },
    approvalEditMode: {
      type: Boolean,
      default: false
    },
    allowEditFormModule: {
      type: Boolean,
      default: false
    },
    allowEditPayAccount: {
      type: Boolean,
      default: false
    }
  },
  setup(props, { expose }) {
    expose({
      validateBeforeSubmit: mocks.runtimeEditor.validateBeforeSubmit
    })
    return () => h('div', {
      'data-testid': 'expense-runtime-form-editor',
      'data-model-value': JSON.stringify(props.modelValue || {}),
      'data-current-user-company-id': props.currentUserCompanyId || '',
      'data-approval-edit-mode': String(props.approvalEditMode),
      'data-allow-edit-form-module': String(props.allowEditFormModule),
      'data-allow-edit-pay-account': String(props.allowEditPayAccount)
    })
  }
})

const globalStubs = {
  'el-card': SimpleContainer,
  'el-icon': SimpleContainer,
  'el-tag': TagStub,
  'el-button': ButtonStub,
  'el-input': InputStub,
  'el-select': SelectStub,
  'el-option': OptionStub,
  'el-empty': EmptyStub,
  'expense-runtime-form-editor': ExpenseRuntimeFormEditorStub
}

function buildTemplateSummary(
  templateCode = 'TPL-001',
  templateName = '差旅报销模板',
  templateType = 'report',
  templateTypeLabel = '报销单',
  categoryCode = 'employee-expense',
  categoryName = '员工费用类'
) {
  return {
    templateCode,
    templateName,
    templateType,
    templateTypeLabel,
    categoryCode,
    categoryName,
    formDesignCode: 'FD-001'
  }
}

function buildTemplateDetail(
  templateCode = 'TPL-001',
  templateName = '差旅报销模板',
  templateType = 'report',
  templateTypeLabel = '报销单',
  options: {
    blocks?: Array<Record<string, unknown>>
    expenseDetailDesignCode?: string
  } = {}
) {
  return {
    templateCode,
    templateName,
    templateType,
    templateTypeLabel,
    categoryCode: 'travel',
    templateDescription: 'template description',
    formDesignCode: 'FD-001',
    approvalFlowCode: 'FLOW-001',
    flowName: '标准审批流程',
    formName: templateType === 'contract' ? '合同主表单' : '差旅报销单',
    schema: {
      layoutMode: 'TWO_COLUMN',
      blocks: options.blocks || []
    },
    flowSnapshot: {
      nodes: [],
      routes: []
    },
    sharedArchives: [],
    expenseDetailDesignCode: options.expenseDetailDesignCode ?? (templateType === 'report' ? 'EDD-001' : ''),
    expenseDetailDesignName: templateType === 'report' ? '费用明细' : '',
    expenseDetailType: 'COMMON',
    expenseDetailTypeLabel: '普通报销',
    expenseDetailModeDefault: '',
    expenseDetailSchema: {
      layoutMode: 'TWO_COLUMN',
      blocks: []
    },
    expenseDetailSharedArchives: [],
    companyOptions: [],
    departmentOptions: [],
    userOptions: [],
    currentUserCompanyId: '',
    currentUserCompanyName: '',
    currentUserDeptId: 1,
    currentUserDeptName: '财务部'
  }
}

function buildAmountBlock(fieldKey: string) {
  return {
    blockId: `block-${fieldKey}`,
    fieldKey,
    kind: 'CONTROL',
    props: {
      controlType: 'AMOUNT'
    }
  }
}

function buildPaymentCompanyBlock(overrides: Record<string, unknown> = {}) {
  return {
    blockId: 'block-payment-company',
    fieldKey: 'paymentCompany',
    kind: 'BUSINESS_COMPONENT',
    label: '付款公司',
    props: {
      componentCode: 'payment-company',
      defaultCompanyMode: 'NONE',
      defaultCompanyId: '',
      ...overrides
    }
  }
}

function buildUndertakeDepartmentBlock(overrides: Record<string, unknown> = {}) {
  return {
    blockId: 'block-undertake-department',
    fieldKey: 'undertakeDepartment',
    kind: 'BUSINESS_COMPONENT',
    label: '承担部门',
    props: {
      componentCode: 'undertake-department',
      defaultDeptMode: 'NONE',
      defaultDeptId: '',
      ...overrides
    }
  }
}

function buildExpenseDetail(detailNo: string, formData: string | Record<string, unknown>) {
  return {
    detailNo,
    detailType: 'COMMON',
    formData: typeof formData === 'string'
      ? { actualPaymentAmount: formData }
      : formData
  }
}

function writeDraft(
  draftKey: string,
  templateCode: string,
  options: {
    formValues?: Record<string, unknown>
    expenseDetails?: Array<Record<string, unknown>>
    manualApproverSelections?: Record<string, string[]>
  } = {}
) {
  window.sessionStorage.setItem(
    `expense-create-draft:${draftKey}`,
    JSON.stringify({
      templateCode,
      formValues: options.formValues || {},
      expenseDetails: options.expenseDetails || [],
      manualApproverSelections: options.manualApproverSelections || {}
    })
  )
}

function runtimeFormValue(wrapper: ReturnType<typeof mount>) {
  return JSON.parse(wrapper.get('[data-testid="expense-runtime-form-editor"]').attributes('data-model-value') || '{}')
}

async function mountView() {
  const wrapper = mount(ExpenseCreateView, {
    global: {
      stubs: globalStubs,
      directives: {
        loading: () => undefined
      }
    }
  })
  await flushPromises()
  return wrapper
}

describe('ExpenseCreateView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.sessionStorage.clear()
    mocks.route.name = 'expense-create'
    mocks.route.params = {}
    mocks.route.query = {}
    mocks.route.fullPath = '/expense/create'
    mocks.router.push.mockResolvedValue(undefined)
    mocks.router.replace.mockResolvedValue(undefined)
    mocks.router.back.mockResolvedValue(undefined)
    mocks.expenseCreateApi.listTemplates.mockResolvedValue({ data: [buildTemplateSummary()] })
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({ data: null })
    mocks.expenseCreateApi.submit.mockResolvedValue({ data: { documentCode: 'DOC-001' } })
    mocks.expenseApi.getEditContext.mockResolvedValue({ data: null })
    mocks.expenseApi.resubmit.mockResolvedValue({ data: { documentCode: 'DOC-002' } })
    mocks.expenseApprovalApi.getModifyContext.mockResolvedValue({ data: null })
    mocks.expenseApprovalApi.modify.mockResolvedValue({ data: { documentCode: 'DOC-003' } })
    mocks.runtimeEditor.validateBeforeSubmit.mockReturnValue(true)
    vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(() => ({
      x: 80,
      y: 0,
      top: 0,
      left: 80,
      bottom: 200,
      right: 1280,
      width: 1200,
      height: 200,
      toJSON: () => ({})
    } as DOMRect))
  })

  it('loads template list once on first enter and does not fetch template detail without route context', async () => {
    await mountView()

    expect(mocks.expenseCreateApi.listTemplates).toHaveBeenCalledTimes(1)
    expect(mocks.expenseCreateApi.getTemplateDetail).not.toHaveBeenCalled()
    expect(mocks.expenseApi.getEditContext).not.toHaveBeenCalled()
    expect(mocks.expenseApprovalApi.getModifyContext).not.toHaveBeenCalled()
  })

  it('does not render a standalone invoice OCR workbench after template detail is loaded', async () => {
    mocks.route.query = { templateCode: 'TPL-001', draftKey: 'draft-001' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-001&draftKey=draft-001'
    writeDraft('draft-001', 'TPL-001')
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({ data: buildTemplateDetail() })

    const wrapper = await mountView()

    expect(wrapper.find('[data-testid="expense-invoice-workbench-stub"]').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('发票附件 OCR')
    expect(wrapper.find('[data-testid="expense-runtime-form-editor"]').exists()).toBe(true)
  })

  it('does not fetch template detail when templateCode exists but draftKey is missing', async () => {
    mocks.route.query = { templateCode: 'TPL-001' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-001'

    await mountView()

    expect(mocks.expenseCreateApi.listTemplates).toHaveBeenCalledTimes(1)
    expect(mocks.expenseCreateApi.getTemplateDetail).not.toHaveBeenCalled()
  })

  it('shows a recoverable error state and retries template loading', async () => {
    mocks.expenseCreateApi.listTemplates
      .mockRejectedValueOnce(new Error('加载单据模板超时，请检查后端服务或稍后重试'))
      .mockResolvedValueOnce({ data: [buildTemplateSummary('TPL-002', '备用合同模板', 'contract', '合同单')] })

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('加载单据模板超时，请检查后端服务或稍后重试')

    const retryButton = wrapper.findAll('button').find((item) => item.text().includes('重新加载'))
    expect(retryButton).toBeTruthy()

    await retryButton!.trigger('click')
    await flushPromises()

    expect(mocks.expenseCreateApi.listTemplates).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('备用合同模板')
  })

  it('groups template cards by category in the fixed business order and keeps grouping after search', async () => {
    mocks.expenseCreateApi.listTemplates.mockResolvedValue({
      data: [
        buildTemplateSummary('TPL-001', '对公付款报销模板', 'report', '报销单', 'enterprise-payment', '企业往来类'),
        buildTemplateSummary('TPL-002', '对公付款申请模板', 'application', '申请单', 'enterprise-payment', '企业往来类'),
        buildTemplateSummary('TPL-003', '员工借款模板', 'loan', '借款单', 'employee-expense', '员工费用类'),
        buildTemplateSummary('TPL-004', '专项事项申请模板', 'contract', '合同单', 'business-application', '事项申请类')
      ]
    })

    const wrapper = await mountView()
    const groupTitles = wrapper.findAll('[data-testid="expense-template-group-title"]').map((item) => item.text())

    expect(groupTitles).toEqual(['企业往来类', '员工费用类', '事项申请类'])

    const groups = wrapper.findAll('[data-testid="expense-template-group"]')
    expect(groups).toHaveLength(3)
    expect(groups[0]!.text()).toContain('对公付款报销模板')
    expect(groups[0]!.text()).toContain('对公付款申请模板')
    expect(groups[0]!.text()).toContain('报销单')
    expect(groups[0]!.text()).toContain('申请单')
    expect(groups[1]!.text()).toContain('员工借款模板')
    expect(groups[2]!.text()).toContain('专项事项申请模板')
    expect(wrapper.findAll('[data-testid="expense-template-grid"]').every((item) => (
      item.classes().includes('expense-wb-template-grid')
    ))).toBe(true)
    expect(wrapper.findAll('.expense-wb-template-card')).toHaveLength(4)

    await wrapper.get('input').setValue('对公')
    await flushPromises()

    const filteredGroupTitles = wrapper.findAll('[data-testid="expense-template-group-title"]').map((item) => item.text())
    expect(filteredGroupTitles).toEqual(['企业往来类'])
    expect(wrapper.text()).toContain('对公付款报销模板')
    expect(wrapper.text()).toContain('对公付款申请模板')
    expect(wrapper.text()).not.toContain('员工借款模板')
    expect(wrapper.text()).not.toContain('专项事项申请模板')
  })

  it('shows only the bottom floating action bar in create mode with route context', async () => {
    mocks.route.query = { templateCode: 'TPL-001', draftKey: 'draft-001' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-001&draftKey=draft-001'
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
      data: buildTemplateDetail()
    })

    const wrapper = await mountView()
    const floatingBar = wrapper.get('[data-testid="expense-create-floating-bar"]')
    const submitButtons = wrapper.findAll('button').filter((item) => item.text().includes('提交审批单'))

    expect(mocks.expenseCreateApi.getTemplateDetail).toHaveBeenCalledWith('TPL-001')
    expect(floatingBar.classes()).toContain('expense-create-floating-bar')
    expect(floatingBar.attributes('style')).toContain('width: 1200px')
    expect(wrapper.find('.expense-create-floating-bar__inner').exists()).toBe(true)
    expect(wrapper.get('[data-testid="expense-create-floating-amount"]').text()).toContain('金额：¥ 0.00')
    expect(wrapper.get('[data-testid="expense-create-back-to-chooser"]').text()).toContain('返回上一层')
    expect(floatingBar.text()).not.toContain('返回我的报销')
    expect(floatingBar.text()).toContain('保存草稿')
    expect(floatingBar.text()).toContain('提交审批单')
    expect(wrapper.text()).not.toContain('template description')
    expect(submitButtons).toHaveLength(1)
    expect(submitButtons[0]!.classes()).toContain('expense-create-floating-bar__button')
    expect(submitButtons[0]!.classes()).toContain('expense-create-floating-bar__button--primary')
  })

  it('returns to the template chooser from the new top back action in create mode', async () => {
    mocks.route.query = { templateCode: 'TPL-001', draftKey: 'draft-001' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-001&draftKey=draft-001'
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
      data: buildTemplateDetail()
    })
    writeDraft('draft-001', 'TPL-001', {
      formValues: {
        amountField: '12.34'
      }
    })

    const wrapper = await mountView()

    await wrapper.get('[data-testid="expense-create-back-to-chooser"]').trigger('click')
    await flushPromises()

    expect(mocks.router.replace).toHaveBeenCalledWith({
      name: 'expense-create',
      query: {}
    })
    expect(window.sessionStorage.getItem('expense-create-draft:draft-001')).toBeNull()
  })

  it('manually saves draft from the floating action bar without triggering submit', async () => {
    mocks.route.query = { templateCode: 'TPL-001', draftKey: 'draft-001' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-001&draftKey=draft-001'
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
      data: buildTemplateDetail()
    })

    const wrapper = await mountView()
    mocks.elMessage.success.mockClear()

    const saveDraftButton = wrapper.findAll('button').find((item) => item.text().includes('保存草稿'))
    expect(saveDraftButton).toBeTruthy()

    await saveDraftButton!.trigger('click')
    await flushPromises()

    expect(mocks.expenseCreateApi.submit).not.toHaveBeenCalled()
    expect(mocks.elMessage.success).toHaveBeenCalledWith('草稿已保存')

    const storedDraft = JSON.parse(window.sessionStorage.getItem('expense-create-draft:draft-001') || '{}')
    expect(storedDraft.templateCode).toBe('TPL-001')
    expect(storedDraft.templateDetail?.templateName).toBe('差旅报销模板')
  })

  it('ignores pending draft persistence after unmount', async () => {
    vi.useFakeTimers()
    try {
      mocks.route.query = { templateCode: 'TPL-001', draftKey: 'draft-async-guard' }
      mocks.route.fullPath = '/expense/create?templateCode=TPL-001&draftKey=draft-async-guard'
      mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
        data: buildTemplateDetail()
      })
      writeDraft('draft-async-guard', 'TPL-001', {
        formValues: {
          amountField: '88.88'
        }
      })

      const storageSetItemSpy = vi.spyOn(Storage.prototype, 'setItem')
      const wrapper = await mountView()

      expect(vi.getTimerCount()).toBeGreaterThan(0)
      storageSetItemSpy.mockClear()

      wrapper.unmount()
      expect(() => vi.runOnlyPendingTimers()).not.toThrow()
      expect(storageSetItemSpy).not.toHaveBeenCalled()
    } finally {
      vi.useRealTimers()
    }
  })

  it('submits a contract template without expense details', async () => {
    mocks.route.query = { templateCode: 'TPL-002', draftKey: 'draft-002' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-002&draftKey=draft-002'
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
      data: buildTemplateDetail('TPL-002', '采购合同模板', 'contract', '合同单')
    })

    const wrapper = await mountView()
    const submitButton = wrapper.findAll('button').find((item) => item.text().includes('提交审批单'))

    expect(wrapper.text()).toContain('合同单')
    expect(submitButton).toBeTruthy()

    await submitButton!.trigger('click')
    await flushPromises()

    expect(mocks.expenseCreateApi.submit).toHaveBeenCalledWith({
      templateCode: 'TPL-002',
      formData: {
        __totalAmount: '0.00'
      },
      expenseDetails: []
    })
    expect(mocks.elMessage.success).toHaveBeenCalledWith('审批单已提交')
    expect(mocks.router.push).toHaveBeenCalledWith('/expense/documents/DOC-001')
  })

  it('shows floating total amount from expense details when the template has expense details', async () => {
    mocks.route.query = { templateCode: 'TPL-001', draftKey: 'draft-report-amount' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-001&draftKey=draft-report-amount'
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
      data: buildTemplateDetail()
    })
    writeDraft('draft-report-amount', 'TPL-001', {
      formValues: {
        amountField: '99.99'
      },
      expenseDetails: [buildExpenseDetail('DETAIL-001', '0.10'), buildExpenseDetail('DETAIL-002', '0.20')]
    })

    const wrapper = await mountView()

    expect(wrapper.get('[data-testid="expense-create-floating-amount"]').text()).toContain('金额：¥ 0.30')
    expect(wrapper.text()).toContain('金额汇总')
    expect(wrapper.text()).toContain('¥ 0.30')
  })

  it('renders compact expense detail cards without obsolete detail metadata text', async () => {
    mocks.route.query = { templateCode: 'TPL-001', draftKey: 'draft-report-details' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-001&draftKey=draft-report-details'
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
      data: buildTemplateDetail()
    })
    writeDraft('draft-report-details', 'TPL-001', {
      expenseDetails: [
        {
          detailNo: 'DETAIL-001',
          detailTitle: '差旅行程',
          detailType: 'COMMON',
          sortOrder: 1,
          formData: {
            actualPaymentAmount: '12.30'
          }
        }
      ]
    })

    const wrapper = await mountView()
    const detailCard = wrapper.get('.expense-wb-detail-card')

    expect(detailCard.find('.expense-wb-detail-card__body').exists()).toBe(true)
    expect(detailCard.text()).toContain('差旅行程')
    expect(detailCard.text()).toContain('金额：¥ 12.30')
    expect(detailCard.text()).toContain('编辑')
    expect(detailCard.text()).toContain('删除')
    expect(wrapper.text()).not.toContain('明细编号')
    expect(wrapper.text()).not.toContain('排序')
  })

  it('falls back to summing main form amount controls when the template has no expense details', async () => {
    mocks.route.query = { templateCode: 'TPL-003', draftKey: 'draft-contract-amount' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-003&draftKey=draft-contract-amount'
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
      data: buildTemplateDetail('TPL-003', '合同模板', 'contract', '合同单', {
        blocks: [buildAmountBlock('amountOne'), buildAmountBlock('amountTwo')]
      })
    })
    writeDraft('draft-contract-amount', 'TPL-003', {
      formValues: {
        amountOne: '0.10',
        amountTwo: '0.20'
      }
    })

    const wrapper = await mountView()

    expect(wrapper.get('[data-testid="expense-create-floating-amount"]').text()).toContain('金额：¥ 0.30')
  })

  it('writes __totalAmount as an exact money string when creating', async () => {
    mocks.route.query = { templateCode: 'TPL-004', draftKey: 'draft-submit-amount' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-004&draftKey=draft-submit-amount'
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
      data: buildTemplateDetail('TPL-004', '合同模板', 'contract', '合同单', {
        blocks: [buildAmountBlock('amountOne'), buildAmountBlock('amountTwo')]
      })
    })
    writeDraft('draft-submit-amount', 'TPL-004', {
      formValues: {
        amountOne: '100.10',
        amountTwo: '0.20'
      }
    })

    const wrapper = await mountView()
    const submitButton = wrapper.findAll('button').find((item) => item.text().includes('提交审批单'))

    expect(submitButton).toBeTruthy()

    await submitButton!.trigger('click')
    await flushPromises()

    expect(mocks.expenseCreateApi.submit).toHaveBeenCalledWith({
      templateCode: 'TPL-004',
      formData: {
        amountOne: '100.10',
        amountTwo: '0.20',
        __totalAmount: '100.30'
      },
      expenseDetails: []
    })
  })

  it('falls back from actual payment amount to detail amount when submitting expense details', async () => {
    mocks.route.query = { templateCode: 'TPL-004A', draftKey: 'draft-detail-amount-fallback' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-004A&draftKey=draft-detail-amount-fallback'
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
      data: {
        ...buildTemplateDetail('TPL-004A', '对公模板', 'report', '报销单'),
        expenseDetailType: 'ENTERPRISE_TRANSACTION',
        expenseDetailModeDefault: 'PREPAY_UNBILLED'
      }
    })
    writeDraft('draft-detail-amount-fallback', 'TPL-004A', {
      expenseDetails: [
        {
          detailNo: 'D-001',
          detailType: 'ENTERPRISE_TRANSACTION',
          businessSceneMode: 'PREPAY_UNBILLED',
          formData: {
            amount: '88.50',
            actualPaymentAmount: ''
          }
        }
      ]
    })

    const wrapper = await mountView()
    const submitButton = wrapper.findAll('button').find((item) => item.text().includes('提交审批单'))

    await submitButton!.trigger('click')
    await flushPromises()

    expect(mocks.expenseCreateApi.submit).toHaveBeenCalledWith(expect.objectContaining({
      formData: expect.objectContaining({
        __totalAmount: '88.50'
      })
    }))
  })

  it('blocks enterprise expense-detail submit until each detail explicitly selects a business scenario', async () => {
    mocks.route.query = { templateCode: 'TPL-004B', draftKey: 'draft-missing-business-scenario' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-004B&draftKey=draft-missing-business-scenario'
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
      data: {
        ...buildTemplateDetail('TPL-004B', '对公模板', 'report', '报销单'),
        expenseDetailType: 'ENTERPRISE_TRANSACTION',
        expenseDetailTypeLabel: '企业往来',
        expenseDetailModeDefault: '',
        expenseDetailSchema: {
          layoutMode: 'TWO_COLUMN',
          blocks: [
            {
              blockId: 'business-scenario',
              fieldKey: 'businessScenario',
              kind: 'CONTROL',
              label: '业务场景',
              props: {
                controlType: 'SELECT',
                systemFieldCode: 'BUSINESS_SCENARIO',
                enabledSceneModes: ['INVOICE_FULL_PAYMENT', 'PREPAY_UNBILLED'],
                options: [
                  { label: '全额付款', value: 'INVOICE_FULL_PAYMENT' },
                  { label: '预付未到票', value: 'PREPAY_UNBILLED' }
                ]
              }
            }
          ]
        }
      }
    })
    writeDraft('draft-missing-business-scenario', 'TPL-004B', {
      expenseDetails: [
        {
          detailNo: 'D-001',
          detailTitle: '费用明细 1',
          detailType: 'ENTERPRISE_TRANSACTION',
          formData: {
            businessScenario: '',
            actualPaymentAmount: '12.00'
          }
        }
      ]
    })

    const wrapper = await mountView()
    const submitButton = wrapper.findAll('button').find((item) => item.text().includes('提交审批单'))

    await submitButton!.trigger('click')
    await flushPromises()

    expect(mocks.expenseCreateApi.submit).not.toHaveBeenCalled()
    expect(mocks.elMessage.warning).toHaveBeenCalledWith('请先为“费用明细 1”选择业务场景')
  })

  it('preserves related and writeoff form values when submitting', async () => {
    mocks.route.query = { templateCode: 'TPL-014', draftKey: 'draft-related-writeoff-submit' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-014&draftKey=draft-related-writeoff-submit'
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
      data: buildTemplateDetail('TPL-014', '业务关系模板', 'contract', '合同单', {
        blocks: [
          {
            blockId: 'related-1',
            fieldKey: 'relatedDocs',
            label: '关联单据',
            kind: 'BUSINESS_COMPONENT',
            props: { componentCode: 'related-document' }
          },
          {
            blockId: 'writeoff-1',
            fieldKey: 'writeoffDocs',
            label: '核销单据',
            kind: 'BUSINESS_COMPONENT',
            props: { componentCode: 'writeoff-document' }
          }
        ]
      })
    })
    writeDraft('draft-related-writeoff-submit', 'TPL-014', {
      formValues: {
        relatedDocs: [
          {
            documentCode: 'DOC-REL-001',
            documentTitle: '项目申请单',
            templateType: 'application',
            templateTypeLabel: '申请单',
            statusLabel: '已完成'
          }
        ],
        writeoffDocs: [
          {
            documentCode: 'DOC-WO-001',
            documentTitle: '借款单',
            templateType: 'loan',
            templateTypeLabel: '借款单',
            writeOffSourceKind: 'LOAN',
            writeOffAmount: '120.00',
            remainingAmount: '380.00'
          }
        ]
      }
    })

    const wrapper = await mountView()
    const submitButton = wrapper.findAll('button').find((item) => item.text().includes('提交审批单'))

    expect(submitButton).toBeTruthy()

    await submitButton!.trigger('click')
    await flushPromises()

    expect(mocks.expenseCreateApi.submit).toHaveBeenCalledTimes(1)
    const payload = mocks.expenseCreateApi.submit.mock.calls[0]?.[0] as {
      templateCode: string
      formData: Record<string, unknown>
      expenseDetails: unknown[]
    }
    expect(payload.templateCode).toBe('TPL-014')
    expect(payload.expenseDetails).toEqual([])
    expect(payload.formData.__totalAmount).toBe('0.00')
    expect(payload.formData.relatedDocs).toEqual([
      expect.objectContaining({
        documentCode: 'DOC-REL-001',
        documentTitle: '项目申请单'
      })
    ])
    expect(payload.formData.writeoffDocs).toEqual([
      expect.objectContaining({
        documentCode: 'DOC-WO-001',
        writeOffAmount: '120.00',
        writeOffSourceKind: 'LOAN'
      })
    ])
  })

  it('writes __totalAmount as an exact money string when resubmitting', async () => {
    mocks.route.name = 'expense-document-resubmit'
    mocks.route.params = { documentCode: 'DOC-100' }
    mocks.route.fullPath = '/expense/documents/DOC-100/resubmit'
    mocks.expenseApi.getEditContext.mockResolvedValue({
      data: {
        documentCode: 'DOC-100',
        templateCode: 'TPL-005',
        templateName: '合同模板',
        templateType: 'contract',
        templateTypeLabel: '合同单',
        categoryCode: 'contract',
        templateDescription: 'template description',
        formDesignCode: 'FD-001',
        approvalFlowCode: 'FLOW-001',
        flowName: '标准审批流程',
        formName: '合同主表单',
        schema: {
          layoutMode: 'TWO_COLUMN',
          blocks: [buildAmountBlock('amountOne'), buildAmountBlock('amountTwo')]
        },
        sharedArchives: [],
        expenseDetailDesignCode: '',
        expenseDetailDesignName: '',
        expenseDetailType: '',
        expenseDetailTypeLabel: '',
        expenseDetailModeDefault: '',
        expenseDetailSchema: {
          layoutMode: 'TWO_COLUMN',
          blocks: []
        },
        expenseDetailSharedArchives: [],
        companyOptions: [],
        departmentOptions: [],
        currentUserDeptId: 1,
        currentUserDeptName: '财务部',
        formData: {
          amountOne: '80.10',
          amountTwo: '20.20'
        },
        expenseDetails: []
      }
    })

    const wrapper = await mountView()
    const submitButton = wrapper.findAll('button').find((item) => item.text().includes('重新提交审批单'))

    expect(submitButton).toBeTruthy()

    await submitButton!.trigger('click')
    await flushPromises()

    expect(mocks.expenseApi.resubmit).toHaveBeenCalledWith('DOC-100', {
      formData: {
        amountOne: '80.10',
        amountTwo: '20.20',
        __totalAmount: '100.30'
      },
      expenseDetails: []
    })
  })

  it('shows a top back button, hides hero stat cards, and prefers returnTo on the shared resubmit page', async () => {
    mocks.route.name = 'expense-document-resubmit'
    mocks.route.params = { documentCode: 'DOC-102' }
    mocks.route.query = { returnTo: '/expense/list?tab=rejected' }
    mocks.route.fullPath = '/expense/documents/DOC-102/resubmit'
    mocks.expenseApi.getEditContext.mockResolvedValue({
      data: {
        documentCode: 'DOC-102',
        ...buildTemplateDetail('TPL-006A', '办公费用模板', 'report', '报销单'),
        formData: {},
        expenseDetails: []
      }
    })

    const wrapper = await mountView()

    expect(wrapper.get('[data-testid="expense-resubmit-hero-back"]').text()).toContain('返回')
    expect(wrapper.findAll('.expense-wb-stat-card')).toHaveLength(0)

    await wrapper.get('[data-testid="expense-resubmit-hero-back"]').trigger('click')
    await flushPromises()

    expect(mocks.router.push).toHaveBeenCalledWith('/expense/list?tab=rejected')
    expect(mocks.router.back).not.toHaveBeenCalled()
  })

  it('falls back to router.back before the detail page when resubmit has no returnTo', async () => {
    mocks.route.name = 'expense-document-resubmit'
    mocks.route.params = { documentCode: 'DOC-103' }
    mocks.route.fullPath = '/expense/documents/DOC-103/resubmit'
    window.history.pushState({}, '', '/expense/list')
    mocks.expenseApi.getEditContext.mockResolvedValue({
      data: {
        documentCode: 'DOC-103',
        ...buildTemplateDetail('TPL-006B', '市场费用模板', 'report', '报销单'),
        formData: {},
        expenseDetails: []
      }
    })

    const wrapper = await mountView()

    await wrapper.get('[data-testid="expense-resubmit-hero-back"]').trigger('click')
    await flushPromises()

    expect(mocks.router.back).toHaveBeenCalledTimes(1)
  })

  it('prefers the local resubmit draft over edit context for form values and expense details', async () => {
    mocks.route.name = 'expense-document-resubmit'
    mocks.route.params = { documentCode: 'DOC-200' }
    mocks.route.fullPath = '/expense/documents/DOC-200/resubmit'
    mocks.expenseApi.getEditContext.mockResolvedValue({
      data: {
        documentCode: 'DOC-200',
        ...buildTemplateDetail('TPL-020', '差旅重提模板', 'report', '报销单', {
          blocks: [buildAmountBlock('amountOne')]
        }),
        formData: {
          amountOne: '1.00'
        },
        expenseDetails: [buildExpenseDetail('DETAIL-OLD', '5.00')]
      }
    })
    writeDraft('resubmit-DOC-200', 'TPL-020', {
      formValues: {
        amountOne: '9.90'
      },
      expenseDetails: [buildExpenseDetail('DETAIL-001', '12.34'), buildExpenseDetail('DETAIL-002', '7.66')]
    })

    const wrapper = await mountView()
    const submitButton = wrapper.findAll('button').find((item) => item.text().includes('重新提交审批单'))

    expect(runtimeFormValue(wrapper).amountOne).toBe('9.90')
    expect(wrapper.get('[data-testid="expense-create-floating-amount"]').text()).toContain('金额：¥ 20.00')

    await submitButton!.trigger('click')
    await flushPromises()

    expect(mocks.expenseApi.resubmit).toHaveBeenCalledWith('DOC-200', {
      formData: {
        amountOne: '9.90',
        __totalAmount: '20.00'
      },
      expenseDetails: [
        expect.objectContaining({
          detailNo: 'DETAIL-001',
          formData: expect.objectContaining({ actualPaymentAmount: '12.34' })
        }),
        expect.objectContaining({
          detailNo: 'DETAIL-002',
          formData: expect.objectContaining({ actualPaymentAmount: '7.66' })
        })
      ]
    })
  })

  it('keeps restored counterparty and payee account selections when reopening a saved draft', async () => {
    mocks.route.query = { templateCode: 'TPL-021', draftKey: 'draft-payee-restore' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-021&draftKey=draft-payee-restore'
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
      data: buildTemplateDetail('TPL-021', '对公收款模板', 'report', '报销单')
    })
    writeDraft('draft-payee-restore', 'TPL-021', {
      formValues: {
        paymentCompany: 'COMPANY-001',
        counterparty: 'VEN-001',
        payeeAccount: {
          value: 'VENDOR_ACCOUNT:1',
          label: '供应商默认账户',
          ownerName: '广州供应商',
          accountNoMasked: '****0001'
        }
      }
    })

    const wrapper = await mountView()
    const modelValue = runtimeFormValue(wrapper)

    expect(modelValue.paymentCompany).toBe('COMPANY-001')
    expect(modelValue.counterparty).toBe('VEN-001')
    expect(modelValue.payeeAccount).toEqual(expect.objectContaining({
      value: 'VENDOR_ACCOUNT:1',
      label: '供应商默认账户'
    }))
  })

  it('uses draft-edit wording when a draft enters through the shared resubmit route', async () => {
    mocks.route.name = 'expense-document-resubmit'
    mocks.route.params = { documentCode: 'DOC-101' }
    mocks.route.query = { entry: 'draft' }
    mocks.route.fullPath = '/expense/documents/DOC-101/resubmit?entry=draft'
    mocks.expenseApi.getEditContext.mockResolvedValue({
      data: {
        documentCode: 'DOC-101',
        ...buildTemplateDetail('TPL-006', '办公费用模板', 'report', '报销单'),
        formData: {},
        expenseDetails: []
      }
    })

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('编辑草稿单据')
    expect(wrapper.text()).toContain('草稿编辑')
    expect(wrapper.text()).toContain('提交审批单')
    expect(wrapper.text()).not.toContain('召回后重新提交审批单')
    expect(wrapper.find('[data-testid="expense-resubmit-hero-back"]').exists()).toBe(true)
    expect(wrapper.findAll('.expense-wb-stat-card')).toHaveLength(0)
  })

  it('blocks submit when relation fieldKey exceeds 64 characters', async () => {
    mocks.route.query = { templateCode: 'TPL-001', draftKey: 'draft-001' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-001&draftKey=draft-001'
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
      data: buildTemplateDetail('TPL-001', '差旅报销模板', 'report', '报销单', {
        blocks: [
          {
            blockId: 'relation-1',
            fieldKey: 'r'.repeat(65),
            label: '关联单据',
            kind: 'BUSINESS_COMPONENT',
            props: {
              componentCode: 'related-document'
            }
          }
        ]
      })
    })

    const wrapper = await mountView()
    const submitButton = wrapper.findAll('button').find((item) => item.text().includes('提交审批单'))

    expect(submitButton).toBeTruthy()

    await submitButton!.trigger('click')
    await flushPromises()

    expect(mocks.expenseCreateApi.submit).not.toHaveBeenCalled()
    expect(mocks.elMessage.warning).toHaveBeenCalledWith(`字段标识 ${'r'.repeat(65)}最多 64 个字符`)
  })

  it('defaults payment company to the fixed configured company when the option is available', async () => {
    mocks.route.query = { templateCode: 'TPL-006', draftKey: 'draft-payment-company-fixed' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-006&draftKey=draft-payment-company-fixed'
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
      data: {
        ...buildTemplateDetail('TPL-006', '付款公司模板', 'contract', '合同单', {
          blocks: [buildPaymentCompanyBlock({ defaultCompanyMode: 'FIXED_COMPANY', defaultCompanyId: 'COMPANY_A' })]
        }),
        companyOptions: [{ value: 'COMPANY_A', label: '广州公司' }]
      }
    })

    const wrapper = await mountView()

    expect(runtimeFormValue(wrapper).paymentCompany).toBe('COMPANY_A')
  })

  it('defaults payment company to the submitter company when configured and available', async () => {
    mocks.route.query = { templateCode: 'TPL-007', draftKey: 'draft-payment-company-submitter' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-007&draftKey=draft-payment-company-submitter'
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
      data: {
        ...buildTemplateDetail('TPL-007', '付款公司模板', 'contract', '合同单', {
          blocks: [buildPaymentCompanyBlock({ defaultCompanyMode: 'SUBMITTER_COMPANY' })]
        }),
        companyOptions: [{ value: 'COMPANY_B', label: '上海分公司' }],
        currentUserCompanyId: 'COMPANY_B',
        currentUserCompanyName: '上海分公司'
      }
    })

    const wrapper = await mountView()

    expect(runtimeFormValue(wrapper).paymentCompany).toBe('COMPANY_B')
  })

  it('passes current user company id to the runtime form editor', async () => {
    mocks.route.query = { templateCode: 'TPL-007A', draftKey: 'draft-runtime-company-context' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-007A&draftKey=draft-runtime-company-context'
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
      data: {
        ...buildTemplateDetail('TPL-007A', '付款公司模板', 'contract', '合同单'),
        currentUserCompanyId: 'COMPANY_CTX',
        currentUserCompanyName: '上下文公司'
      }
    })

    const wrapper = await mountView()

    expect(wrapper.get('[data-testid="expense-runtime-form-editor"]').attributes('data-current-user-company-id')).toBe('COMPANY_CTX')
  })

  it('does not render pre-submit manual approver selection even when the flow contains manual select nodes', async () => {
    mocks.route.query = { templateCode: 'TPL-007B', draftKey: 'draft-manual-approver' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-007B&draftKey=draft-manual-approver'
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
      data: {
        ...buildTemplateDetail('TPL-007B', '手选审批模板', 'contract', '合同单'),
        flowSnapshot: {
          nodes: [
            {
              nodeKey: 'manual-finance',
              nodeName: '财务复核',
              nodeType: 'APPROVAL',
              displayOrder: 1,
              config: {
                approverType: 'MANUAL_SELECT'
              }
            }
          ],
          routes: []
        },
        userOptions: [
          { value: 2, label: '李四' },
          { value: 3, label: '王五' }
        ]
      }
    })

    const wrapper = await mountView()

    expect(wrapper.text()).not.toContain('手动选择审批人')
    expect(wrapper.find('[data-testid="expense-manual-approver-card"]').exists()).toBe(false)
    const submitButton = wrapper.findAll('button').find((item) => item.text().includes('提交审批单'))
    await submitButton!.trigger('click')
    await flushPromises()

    expect(mocks.expenseCreateApi.submit).toHaveBeenCalledWith({
      templateCode: 'TPL-007B',
      formData: {
        __totalAmount: '0.00'
      },
      expenseDetails: []
    })
  })

  it('passes modify permission flags to the runtime editor and locks expense detail editing in approval modify mode', async () => {
    mocks.route.name = 'expense-approval-task-modify'
    mocks.route.params = { taskId: '55' }
    mocks.route.fullPath = '/expense/approval/tasks/55/modify'
    mocks.expenseApprovalApi.getModifyContext.mockResolvedValue({
      data: {
        ...buildTemplateDetail('TPL-007C', '审批改单模板', 'report', '报销单'),
        editMode: 'MODIFY',
        documentCode: 'DOC-003',
        taskId: 55,
        formData: {},
        expenseDetails: [
          {
            detailNo: 'DETAIL-001',
            detailTitle: '住宿费',
            detailType: 'COMMON',
            sortOrder: 1,
            formData: {
              actualPaymentAmount: '12.30'
            }
          }
        ],
        allowEditFormModule: true,
        allowEditPayAccount: false
      }
    })

    const wrapper = await mountView()
    const editor = wrapper.get('[data-testid="expense-runtime-form-editor"]')

    expect(editor.attributes('data-approval-edit-mode')).toBe('true')
    expect(editor.attributes('data-allow-edit-form-module')).toBe('true')
    expect(editor.attributes('data-allow-edit-pay-account')).toBe('false')
    expect(wrapper.findAll('button').find((item) => item.text() === '新增费用明细')?.attributes('disabled')).toBeDefined()
    expect(wrapper.findAll('button').find((item) => item.text() === '编辑')?.attributes('disabled')).toBeDefined()
    expect(wrapper.findAll('button').find((item) => item.text() === '删除')?.attributes('disabled')).toBeDefined()
  })

  it('keeps the restored draft payment company instead of overriding it with a default', async () => {
    mocks.route.query = { templateCode: 'TPL-008', draftKey: 'draft-payment-company-existing' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-008&draftKey=draft-payment-company-existing'
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
      data: {
        ...buildTemplateDetail('TPL-008', '付款公司模板', 'contract', '合同单', {
          blocks: [buildPaymentCompanyBlock({ defaultCompanyMode: 'FIXED_COMPANY', defaultCompanyId: 'COMPANY_A' })]
        }),
        companyOptions: [
          { value: 'COMPANY_A', label: '广州公司' },
          { value: 'COMPANY_B', label: '上海分公司' }
        ]
      }
    })
    writeDraft('draft-payment-company-existing', 'TPL-008', {
      formValues: {
        paymentCompany: 'COMPANY_B'
      }
    })

    const wrapper = await mountView()

    expect(runtimeFormValue(wrapper).paymentCompany).toBe('COMPANY_B')
  })

  it('leaves payment company empty when the configured default is not in company options', async () => {
    mocks.route.query = { templateCode: 'TPL-009', draftKey: 'draft-payment-company-invalid' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-009&draftKey=draft-payment-company-invalid'
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
      data: {
        ...buildTemplateDetail('TPL-009', '付款公司模板', 'contract', '合同单', {
          blocks: [buildPaymentCompanyBlock({ defaultCompanyMode: 'FIXED_COMPANY', defaultCompanyId: 'COMPANY_X' })]
        }),
        companyOptions: [{ value: 'COMPANY_A', label: '广州公司' }]
      }
    })

    const wrapper = await mountView()

    expect(runtimeFormValue(wrapper).paymentCompany).toBe('')
  })

  it('defaults undertake department to the submitter department when configured and available', async () => {
    mocks.route.query = { templateCode: 'TPL-010', draftKey: 'draft-undertake-department-submitter' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-010&draftKey=draft-undertake-department-submitter'
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
      data: {
        ...buildTemplateDetail('TPL-010', '承担部门模板', 'report', '报销单', {
          blocks: [buildUndertakeDepartmentBlock({ defaultDeptMode: 'SUBMITTER_DEPARTMENT' })]
        }),
        departmentOptions: [{ value: 'DEPT_FIN', label: '财务部' }],
        currentUserDeptId: 'DEPT_FIN',
        currentUserDeptName: '财务部'
      }
    })

    const wrapper = await mountView()

    expect(runtimeFormValue(wrapper).undertakeDepartment).toBe('DEPT_FIN')
  })

  it('defaults undertake department to the fixed configured department when the option is available', async () => {
    mocks.route.query = { templateCode: 'TPL-011', draftKey: 'draft-undertake-department-fixed' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-011&draftKey=draft-undertake-department-fixed'
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
      data: {
        ...buildTemplateDetail('TPL-011', '承担部门模板', 'report', '报销单', {
          blocks: [buildUndertakeDepartmentBlock({ defaultDeptMode: 'FIXED_DEPARTMENT', defaultDeptId: 'DEPT_HR' })]
        }),
        departmentOptions: [{ value: 'DEPT_HR', label: '人力资源部' }]
      }
    })

    const wrapper = await mountView()

    expect(runtimeFormValue(wrapper).undertakeDepartment).toBe('DEPT_HR')
  })

  it('leaves undertake department empty when the configured default is not in department options', async () => {
    mocks.route.query = { templateCode: 'TPL-012', draftKey: 'draft-undertake-department-invalid' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-012&draftKey=draft-undertake-department-invalid'
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
      data: {
        ...buildTemplateDetail('TPL-012', '承担部门模板', 'report', '报销单', {
          blocks: [buildUndertakeDepartmentBlock({ defaultDeptMode: 'FIXED_DEPARTMENT', defaultDeptId: 'DEPT_IT' })]
        }),
        departmentOptions: [{ value: 'DEPT_FIN', label: '财务部' }]
      }
    })

    const wrapper = await mountView()

    expect(runtimeFormValue(wrapper).undertakeDepartment).toBe('')
  })

  it('keeps the restored draft undertake department instead of overriding it with a default', async () => {
    mocks.route.query = { templateCode: 'TPL-013', draftKey: 'draft-undertake-department-existing' }
    mocks.route.fullPath = '/expense/create?templateCode=TPL-013&draftKey=draft-undertake-department-existing'
    mocks.expenseCreateApi.getTemplateDetail.mockResolvedValue({
      data: {
        ...buildTemplateDetail('TPL-013', '承担部门模板', 'report', '报销单', {
          blocks: [buildUndertakeDepartmentBlock({ defaultDeptMode: 'SUBMITTER_DEPARTMENT' })]
        }),
        departmentOptions: [
          { value: 'DEPT_FIN', label: '财务部' },
          { value: 'DEPT_HR', label: '人力资源部' }
        ],
        currentUserDeptId: 'DEPT_FIN',
        currentUserDeptName: '财务部'
      }
    })
    writeDraft('draft-undertake-department-existing', 'TPL-013', {
      formValues: {
        undertakeDepartment: 'DEPT_HR'
      }
    })

    const wrapper = await mountView()

    expect(runtimeFormValue(wrapper).undertakeDepartment).toBe('DEPT_HR')
  })
})
