import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
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

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage,
  ElMessageBox: mocks.elMessageBox
}))

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
  template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
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
  setup(_, { expose }) {
    expose({
      validateBeforeSubmit: mocks.runtimeEditor.validateBeforeSubmit
    })
    return () => null
  }
})

const globalStubs = {
  'el-card': SimpleContainer,
  'el-icon': SimpleContainer,
  'el-tag': TagStub,
  'el-button': ButtonStub,
  'el-input': InputStub,
  'el-empty': EmptyStub,
  'expense-runtime-form-editor': ExpenseRuntimeFormEditorStub
}

function buildTemplateSummary(
  templateCode = 'TPL-001',
  templateName = '差旅报销模板',
  templateType = 'report',
  templateTypeLabel = '报销单'
) {
  return {
    templateCode,
    templateName,
    templateType,
    templateTypeLabel,
    categoryCode: 'travel',
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
    departmentOptions: [],
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

function buildExpenseDetail(detailNo: string, actualPaymentAmount: string) {
  return {
    detailNo,
    detailType: 'COMMON',
    formData: {
      actualPaymentAmount
    }
  }
}

function writeDraft(
  draftKey: string,
  templateCode: string,
  options: {
    formValues?: Record<string, unknown>
    expenseDetails?: Array<Record<string, unknown>>
  } = {}
) {
  window.sessionStorage.setItem(
    `expense-create-draft:${draftKey}`,
    JSON.stringify({
      templateCode,
      formValues: options.formValues || {},
      expenseDetails: options.expenseDetails || []
    })
  )
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

  it('groups template cards by template type in the fixed business order and keeps grouping after search', async () => {
    mocks.expenseCreateApi.listTemplates.mockResolvedValue({
      data: [
        buildTemplateSummary('TPL-001', '差旅报销模板', 'report', '报销单'),
        buildTemplateSummary('TPL-002', '用章申请模板', 'application', '申请单'),
        buildTemplateSummary('TPL-003', '备用金借款模板', 'loan', '借款单'),
        buildTemplateSummary('TPL-004', '采购合同模板', 'contract', '合同单')
      ]
    })

    const wrapper = await mountView()
    const groupTitles = wrapper.findAll('[data-testid="expense-template-group-title"]').map((item) => item.text())

    expect(groupTitles).toEqual(['报销单', '申请单', '借款单', '合同单'])

    const groups = wrapper.findAll('[data-testid="expense-template-group"]')
    expect(groups).toHaveLength(4)
    expect(groups[0]!.text()).toContain('差旅报销模板')
    expect(groups[1]!.text()).toContain('用章申请模板')
    expect(groups[2]!.text()).toContain('备用金借款模板')
    expect(groups[3]!.text()).toContain('采购合同模板')
    expect(wrapper.findAll('[data-testid="expense-template-grid"]').every((item) => (
      item.classes().includes('expense-wb-template-grid')
    ))).toBe(true)
    expect(wrapper.findAll('.expense-wb-template-card')).toHaveLength(4)

    await wrapper.get('input').setValue('借款')
    await flushPromises()

    const filteredGroupTitles = wrapper.findAll('[data-testid="expense-template-group-title"]').map((item) => item.text())
    expect(filteredGroupTitles).toEqual(['借款单'])
    expect(wrapper.text()).toContain('备用金借款模板')
    expect(wrapper.text()).not.toContain('差旅报销模板')
    expect(wrapper.text()).not.toContain('用章申请模板')
    expect(wrapper.text()).not.toContain('采购合同模板')
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
})
