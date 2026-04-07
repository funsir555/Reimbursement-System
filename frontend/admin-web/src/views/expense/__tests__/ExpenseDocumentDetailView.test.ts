import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, reactive } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ExpenseDocumentDetailView from '@/views/expense/ExpenseDocumentDetailView.vue'

const mocks = vi.hoisted(() => ({
  route: {
    params: { documentCode: 'DOC-001' }
  },
  router: {
    push: vi.fn(),
    back: vi.fn()
  },
  expenseApi: {
    getDetail: vi.fn(),
    getExpenseDetail: vi.fn(),
    getNavigation: vi.fn(),
    recall: vi.fn(),
    comment: vi.fn(),
    remind: vi.fn()
  },
  expenseApprovalApi: {
    approve: vi.fn(),
    reject: vi.fn(),
    modify: vi.fn(),
    addSign: vi.fn(),
    transfer: vi.fn(),
    listActionUsers: vi.fn()
  },
  elMessage: {
    warning: vi.fn(),
    error: vi.fn(),
    success: vi.fn(),
    info: vi.fn()
  },
  elMessageBox: {
    confirm: vi.fn(),
    prompt: vi.fn()
  },
  syncReadonlyPayeeLookups: vi.fn(),
  resolveExpenseDetailActions: vi.fn(),
  resolveDisabledExpenseDetailActionHint: vi.fn()
}))

mocks.route = reactive({
  params: { documentCode: 'DOC-001' }
})

vi.mock('vue-router', () => ({
  useRoute: () => mocks.route,
  useRouter: () => mocks.router
}))

vi.mock('@/api', () => ({
  expenseApi: mocks.expenseApi,
  expenseApprovalApi: mocks.expenseApprovalApi
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage,
  ElMessageBox: mocks.elMessageBox
}))

vi.mock('@/views/expense/useReadonlyPayeeLookups', () => ({
  useReadonlyPayeeLookups: () => ({
    payeeOptionMap: {},
    payeeAccountOptionMap: {},
    syncReadonlyPayeeLookups: mocks.syncReadonlyPayeeLookups
  })
}))

vi.mock('@/views/expense/expenseDetailActionMatrix', () => ({
  resolveExpenseDetailActions: mocks.resolveExpenseDetailActions,
  resolveDisabledExpenseDetailActionHint: mocks.resolveDisabledExpenseDetailActionHint
}))

vi.mock('@/utils/permissions', () => ({
  readStoredUser: () => ({
    userId: 1,
    permissionCodes: ['expense:list:view', 'expense:approval:view', 'expense:approval:approve', 'expense:approval:reject']
  }),
  hasPermission: () => true
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

const DialogStub = defineComponent({
  props: {
    modelValue: {
      type: Boolean,
      default: false
    }
  },
  template: '<div v-if="modelValue"><slot /><slot name="footer" /></div>'
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

const globalStubs = {
  'el-card': SimpleContainer,
  'el-tag': TagStub,
  'el-button': ButtonStub,
  'el-empty': EmptyStub,
  'el-timeline': SimpleContainer,
  'el-timeline-item': SimpleContainer,
  'el-dialog': DialogStub,
  'el-input': InputStub,
  'el-select': SimpleContainer,
  'el-option': true,
  'el-form-item': SimpleContainer,
  'el-icon': SimpleContainer,
  ExpenseFormReadonlyRenderer: {
    template: '<div data-testid="readonly-form" />'
  }
}

function buildDocumentDetail(
  totalAmount: string | number | null | undefined = 1880.5,
  overrides: Record<string, unknown> = {}
) {
  return {
    documentCode: 'DOC-001',
    documentTitle: '差旅报销单',
    status: 'PENDING_APPROVAL',
    statusLabel: '审批中',
    totalAmount,
    submitterUserId: 1,
    submitterName: 'ZhangSan',
    templateName: '差旅报销模板',
    templateType: 'report',
    currentNodeKey: 'finance',
    currentNodeName: '财务审批',
    currentTaskType: 'APPROVAL',
    submittedAt: '2026-04-01 10:00:00',
    templateSnapshot: {},
    formSchemaSnapshot: {
      layoutMode: 'TWO_COLUMN',
      blocks: []
    },
    formData: {},
    flowSnapshot: {
      nodes: [
        {
          nodeKey: 'finance',
          nodeName: '财务审批',
          nodeType: 'APPROVAL',
          displayOrder: 1,
          config: {}
        },
        {
          nodeKey: 'payment',
          nodeName: '支付处理',
          nodeType: 'PAYMENT',
          displayOrder: 2,
          config: {}
        }
      ],
      routes: []
    },
    departmentOptions: [],
    expenseDetails: [
      {
        detailNo: 'D001',
        detailType: 'COMMON',
        detailTypeLabel: '普通报销',
        detailTitle: '住宿费',
        sortOrder: 1,
        createdAt: '2026-04-01 10:10:00'
      },
      {
        detailNo: 'D002',
        detailType: 'COMMON',
        detailTypeLabel: '普通报销',
        detailTitle: '交通费',
        sortOrder: 2,
        createdAt: '2026-04-01 10:20:00'
      }
    ],
    currentTasks: [],
    actionLogs: [],
    ...overrides
  }
}

function buildExpenseDetail(detailNo: string, fileName: string, contentType: string) {
  return {
    documentCode: 'DOC-001',
    detailNo,
    detailType: 'COMMON',
    detailTypeLabel: '普通报销',
    detailTitle: detailNo === 'D001' ? '住宿费' : '交通费',
    sortOrder: detailNo === 'D001' ? 1 : 2,
    schemaSnapshot: {
      layoutMode: 'TWO_COLUMN',
      blocks: []
    },
    formData: {
      invoiceAttachments: [
        {
          attachmentId: `${detailNo}-ATT`,
          fileName,
          contentType,
          previewUrl: `/api/auth/expenses/attachments/${detailNo}-ATT/content`
        }
      ]
    }
  }
}

function createDeferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

async function mountView() {
  const wrapper = mount(ExpenseDocumentDetailView, {
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

describe('ExpenseDocumentDetailView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.route.params.documentCode = 'DOC-001'
    mocks.elMessageBox.confirm.mockReset()
    mocks.elMessageBox.prompt.mockReset()
    mocks.router.push.mockResolvedValue(undefined)
    mocks.router.back.mockResolvedValue(undefined)
    mocks.expenseApi.getDetail.mockResolvedValue({ data: buildDocumentDetail() })
    mocks.expenseApi.getNavigation.mockResolvedValue({ data: {} })
    mocks.resolveExpenseDetailActions.mockReturnValue([])
    mocks.resolveDisabledExpenseDetailActionHint.mockReturnValue('')
    mocks.expenseApi.getExpenseDetail.mockImplementation((documentCode: string, detailNo: string) => Promise.resolve({
      data: buildExpenseDetail(
        detailNo,
        detailNo === 'D001' ? 'hotel.pdf' : 'taxi.png',
        detailNo === 'D001' ? 'application/pdf' : 'image/png'
      )
    }))
  })

  it('toggles invoice workbench on repeat click and reuses cached detail data', async () => {
    const wrapper = await mountView()
    const detailCards = wrapper.findAll('[data-testid="expense-detail-card"]')

    expect(detailCards).toHaveLength(2)

    await detailCards[0]!.trigger('click')
    await flushPromises()

    expect(mocks.expenseApi.getExpenseDetail).toHaveBeenCalledTimes(1)
    expect(mocks.expenseApi.getExpenseDetail).toHaveBeenCalledWith('DOC-001', 'D001')
    expect(wrapper.get('[data-testid="expense-invoice-preview-file"]').text()).toContain('hotel.pdf')
    expect(wrapper.find('[data-testid="expense-invoice-preview-pdf"]').exists()).toBe(true)

    await detailCards[0]!.trigger('click')
    await flushPromises()

    expect(mocks.expenseApi.getExpenseDetail).toHaveBeenCalledTimes(1)
    expect(wrapper.find('[data-testid="expense-invoice-preview-file"]').exists()).toBe(false)

    await detailCards[0]!.trigger('click')
    await flushPromises()

    expect(mocks.expenseApi.getExpenseDetail).toHaveBeenCalledTimes(1)
    expect(wrapper.get('[data-testid="expense-invoice-preview-file"]').text()).toContain('hotel.pdf')

    await detailCards[1]!.trigger('click')
    await flushPromises()

    expect(mocks.expenseApi.getExpenseDetail).toHaveBeenCalledTimes(2)
    expect(wrapper.get('[data-testid="expense-invoice-preview-file"]').text()).toContain('taxi.png')
    expect(wrapper.find('[data-testid="expense-invoice-preview-image"]').exists()).toBe(true)
  })

  it('keeps the original detail navigation button', async () => {
    const wrapper = await mountView()
    const detailButton = wrapper.findAll('button').find((item) => item.text().includes('查看明细'))

    expect(detailButton).toBeTruthy()

    await detailButton!.trigger('click')
    await flushPromises()

    expect(mocks.router.push).toHaveBeenCalledWith({
      name: 'expense-document-expense-detail',
      params: {
        documentCode: 'DOC-001',
        detailNo: 'D001'
      }
    })
  })

  it('prefers detail submitterName over submit log actorName in the submit timeline item', async () => {
    mocks.expenseApi.getDetail.mockResolvedValue({
      data: {
        ...buildDocumentDetail(),
        submitterName: '李四',
        actionLogs: [
          {
            id: 10,
            documentCode: 'DOC-001',
            actionType: 'SUBMIT',
            actorUserId: 1,
            actorName: 'zhangsan',
            actionComment: '',
            payload: {},
            createdAt: '2026-04-01 10:00:00'
          }
        ],
        currentTasks: [
          {
            id: 1,
            documentCode: 'DOC-001',
            nodeKey: 'finance',
            nodeName: 'Finance',
            nodeType: 'APPROVAL',
            assigneeUserId: 2,
            assigneeName: 'LiSi',
            status: 'PENDING',
            taskBatchNo: 'B-1',
            createdAt: '2026-04-01 10:01:00'
          }
        ]
      }
    })

    const wrapper = await mountView()

    expect(wrapper.text()).not.toContain('流程概览')
    expect(wrapper.text()).toContain('李四 提交单据')
    expect(wrapper.text()).not.toContain('zhangsan 提交单据')
    expect(wrapper.text()).toContain('Finance LiSi 待审批')
  })

  it('falls back to detail submitterName when submit log actorName is missing', async () => {
    mocks.expenseApi.getDetail.mockResolvedValue({
      data: {
        ...buildDocumentDetail(),
        submitterName: 'Li Si',
        actionLogs: [
          {
            id: 10,
            documentCode: 'DOC-001',
            actionType: 'SUBMIT',
            actorUserId: 1,
            actorName: '',
            actionComment: '',
            payload: {},
            createdAt: '2026-04-01 10:00:00'
          }
        ],
        currentTasks: []
      }
    })

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('Li Si 提交单据')
  })

  it('falls back to generic submitter text when both submitterName and actorName are missing', async () => {
    mocks.expenseApi.getDetail.mockResolvedValue({
      data: {
        ...buildDocumentDetail(),
        submitterName: '',
        actionLogs: [
          {
            id: 10,
            documentCode: 'DOC-001',
            actionType: 'SUBMIT',
            actorUserId: 1,
            actorName: '',
            actionComment: '',
            payload: {},
            createdAt: '2026-04-01 10:00:00'
          }
        ],
        currentTasks: []
      }
    })

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('提单人 提交单据')
  })

  it('shows approval pending logs with approver names and falls back when approver is missing', async () => {
    mocks.expenseApi.getDetail.mockResolvedValue({
      data: {
        ...buildDocumentDetail(),
        currentTasks: [],
        actionLogs: [
          {
            id: 11,
            documentCode: 'DOC-001',
            nodeKey: 'finance',
            nodeName: 'Finance',
            actionType: 'APPROVAL_PENDING',
            actorUserId: undefined,
            actorName: '',
            actionComment: '',
            payload: {
              approverNames: ['LiSi', 'LiSi', 'WangWu']
            },
            createdAt: '2026-04-01 12:00:00'
          },
          {
            id: 12,
            documentCode: 'DOC-001',
            nodeKey: 'legal',
            nodeName: 'Legal',
            actionType: 'APPROVAL_PENDING',
            actorUserId: undefined,
            actorName: '',
            actionComment: '',
            payload: {},
            createdAt: '2026-04-01 12:10:00'
          }
        ]
      }
    })

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('Finance LiSi、WangWu 审批中')
    expect(wrapper.text()).toContain('Legal 未查询到审批人 审批中')
  })

  it('shows handled approver names from action logs in the timeline', async () => {
    mocks.expenseApi.getDetail.mockResolvedValue({
      data: {
        ...buildDocumentDetail(),
        currentTasks: [],
        actionLogs: [
          {
            id: 11,
            documentCode: 'DOC-001',
            nodeKey: 'finance',
            nodeName: 'Finance',
            actionType: 'APPROVE',
            actorUserId: 3,
            actorName: 'WangWu',
            actionComment: '',
            payload: {},
            createdAt: '2026-04-01 12:00:00'
          }
        ]
      }
    })

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('Finance WangWu 审批通过')
  })

  it('renders detail content even when navigation request is still pending', async () => {
    const navigationDeferred = createDeferred<{ data: Record<string, never> }>()
    mocks.expenseApi.getNavigation.mockReturnValue(navigationDeferred.promise)

    const wrapper = await mountView()

    expect(mocks.expenseApi.getDetail).toHaveBeenCalledWith('DOC-001')
    expect(mocks.expenseApi.getNavigation).toHaveBeenCalledWith('DOC-001')
    expect(wrapper.text()).toContain('差旅报销单')
    expect(wrapper.find('[data-testid="readonly-form"]').exists()).toBe(true)
  })

  it('uses one left-side scroll container for the readonly form and expense details', async () => {
    const wrapper = await mountView()
    const leftScroll = wrapper.get('[data-testid="detail-main-scroll"]')
    const detailCards = leftScroll.findAll('[data-testid="expense-detail-card"]')

    expect(leftScroll.classes()).toContain('detail-main-scroll')
    expect(leftScroll.find('[data-testid="readonly-form"]').exists()).toBe(true)
    expect(detailCards).toHaveLength(2)
    expect(wrapper.text()).toContain('审批流程')
    expect(wrapper.text()).toContain('审批轨迹')
  })

  it('renders a compact hero with only back, title, and amount', async () => {
    const wrapper = await mountView()
    const hero = wrapper.get('[data-testid="detail-hero"]')

    expect(hero.text()).toContain('返回')
    expect(hero.text()).toContain('差旅报销单')
    expect(hero.text()).toContain('金额')
    expect(hero.text()).toContain('¥ 1,880.50')
    expect(hero.text()).not.toContain('返回我的报销')
    expect(hero.text()).not.toContain('Expense Document')
    expect(hero.text()).not.toContain('查看提单快照')
    expect(hero.text()).not.toContain('单号')
    expect(hero.text()).not.toContain('提单人')
    expect(hero.text()).not.toContain('提交时间')
    expect(hero.text()).not.toContain('当前节点')
  })

  it('renders bank payment and bank receipt information when the detail includes bank data', async () => {
    mocks.expenseApi.getDetail.mockResolvedValue({
      data: buildDocumentDetail(1880.5, {
        bankPayment: {
          paymentStatusLabel: '已支付',
          companyBankAccountName: '华南公司基本户',
          receiptStatusLabel: '已获取回单',
          paidAt: '2026-04-06 09:30:00',
          bankFlowNo: 'BF-001',
          manualPaid: false
        },
        bankReceipts: [
          {
            attachmentId: 'ATT-001',
            fileName: 'DOC-001-银行回单.txt',
            contentType: 'text/plain',
            fileSize: 128,
            previewUrl: '/api/auth/expenses/attachments/ATT-001/content',
            receivedAt: '2026-04-06 10:00:00'
          }
        ]
      })
    })

    const wrapper = await mountView()
    const bankSection = wrapper.get('[data-testid="detail-bank-section"]')

    expect(bankSection.text()).toContain('银行付款 / 银行回单')
    expect(bankSection.text()).toContain('付款状态')
    expect(bankSection.text()).toContain('华南公司基本户')
    expect(bankSection.text()).toContain('已获取回单')
    expect(bankSection.text()).toContain('预览回单')
    expect(bankSection.text()).toContain('DOC-001-银行回单.txt')
  })

  it('keeps the compact hero back button wired to the original goBack fallback behavior', async () => {
    const wrapper = await mountView()

    await wrapper.get('[data-testid="detail-back-button"]').trigger('click')

    expect(mocks.router.back).not.toHaveBeenCalled()
    expect(mocks.router.push).toHaveBeenCalledWith('/expense/list')
  })

  it('keeps detail usable when navigation request fails', async () => {
    mocks.expenseApi.getNavigation.mockRejectedValue(new Error('navigation failed'))

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('差旅报销单')
    expect(wrapper.text()).toContain('审批轨迹')
    expect(mocks.elMessage.error).not.toHaveBeenCalled()
  })

  it('removes the duplicate pending approval block and keeps approval actions in the floating bar', async () => {
    mocks.expenseApi.getDetail.mockResolvedValue({
      data: {
        ...buildDocumentDetail(),
        currentTasks: [
          {
            id: 101,
            nodeKey: 'finance',
            nodeName: '财务审批',
            assigneeUserId: 1,
            assigneeName: '张三',
            createdAt: '2026-04-01 12:00:00'
          }
        ]
      }
    })
    mocks.resolveExpenseDetailActions.mockReturnValue([
      { key: 'approve', label: '通过', primary: true, type: 'primary' },
      { key: 'reject', label: '驳回', primary: true, type: 'danger' }
    ])

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('审批轨迹')
    expect(wrapper.text()).not.toContain('待我审批')
    expect(wrapper.text()).not.toContain('当前有 1 条待处理任务')

    const primaryGroup = wrapper.get('[data-testid="detail-floating-primary-actions"]')
    expect(primaryGroup.text()).toContain('通过')
    expect(primaryGroup.text()).toContain('驳回')
  })

  it('prefills the approval prompt with the clicked action label', async () => {
    const detailWithPendingTask = {
      ...buildDocumentDetail(),
      currentTasks: [
        {
          id: 101,
          documentCode: 'DOC-001',
          nodeKey: 'finance',
          nodeName: '财务审批',
          nodeType: 'APPROVAL',
          assigneeUserId: 1,
          assigneeName: '张三',
          status: 'PENDING',
          taskBatchNo: 'B-1',
          createdAt: '2026-04-01 12:00:00'
        }
      ]
    }
    mocks.expenseApi.getDetail.mockResolvedValue({
      data: detailWithPendingTask
    })
    mocks.resolveExpenseDetailActions.mockReturnValue([
      { key: 'approve', label: '通过', primary: true, type: 'primary' },
      { key: 'reject', label: '驳回', primary: true, type: 'danger' }
    ])
    mocks.elMessageBox.prompt
      .mockResolvedValueOnce({ value: '通过' })
      .mockResolvedValueOnce({ value: '驳回' })
    mocks.expenseApprovalApi.approve.mockResolvedValue({ data: detailWithPendingTask })
    mocks.expenseApprovalApi.reject.mockResolvedValue({ data: detailWithPendingTask })

    const wrapper = await mountView()
    const findActionButton = (label: string) => wrapper.findAll('.detail-floating-button').find((item) => item.text() === label)

    await findActionButton('通过')!.trigger('click')
    await flushPromises()

    expect(mocks.elMessageBox.prompt).toHaveBeenNthCalledWith(
      1,
      '可选填写审批意见',
      '通过审批',
      expect.objectContaining({
        inputType: 'textarea',
        inputValue: '通过',
        confirmButtonText: '通过'
      })
    )

    await findActionButton('驳回')!.trigger('click')
    await flushPromises()

    expect(mocks.elMessageBox.prompt).toHaveBeenNthCalledWith(
      2,
      '请填写驳回原因',
      '驳回审批',
      expect.objectContaining({
        inputType: 'textarea',
        inputValue: '驳回',
        confirmButtonText: '驳回'
      })
    )
  })

  it('submits the user-edited approval comment instead of forcing the default value', async () => {
    mocks.expenseApi.getDetail.mockResolvedValue({
      data: {
        ...buildDocumentDetail(),
        currentTasks: [
          {
            id: 101,
            documentCode: 'DOC-001',
            nodeKey: 'finance',
            nodeName: '财务审批',
            nodeType: 'APPROVAL',
            assigneeUserId: 1,
            assigneeName: '张三',
            status: 'PENDING',
            taskBatchNo: 'B-1',
            createdAt: '2026-04-01 12:00:00'
          }
        ]
      }
    })
    mocks.resolveExpenseDetailActions.mockReturnValue([
      { key: 'approve', label: '通过', primary: true, type: 'primary' }
    ])
    mocks.elMessageBox.prompt.mockResolvedValue({ value: '同意，请继续处理' })
    mocks.expenseApprovalApi.approve.mockResolvedValue({ data: buildDocumentDetail() })

    const wrapper = await mountView()

    await wrapper.get('.detail-floating-button').trigger('click')
    await flushPromises()

    expect(mocks.expenseApprovalApi.approve).toHaveBeenCalledWith(101, { comment: '同意，请继续处理' })
  })

  it('renders detail normally when totalAmount is a money string', async () => {
    mocks.expenseApi.getDetail.mockResolvedValue({
      data: buildDocumentDetail('1880.50')
    })

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('差旅报销单')
    expect(wrapper.text()).toContain('¥ 1,880.50')
    expect(mocks.elMessage.error).not.toHaveBeenCalled()
  })

  it('falls back to zero amount text when totalAmount is empty or invalid', async () => {
    mocks.expenseApi.getDetail.mockResolvedValueOnce({
      data: buildDocumentDetail('')
    }).mockResolvedValueOnce({
      data: buildDocumentDetail('invalid-money')
    })

    const firstWrapper = await mountView()
    expect(firstWrapper.text()).toContain('¥ 0.00')
    firstWrapper.unmount()

    const secondWrapper = await mountView()
    expect(secondWrapper.text()).toContain('¥ 0.00')
    expect(mocks.elMessage.error).not.toHaveBeenCalled()
  })

  it('exits loading and shows an error empty state when detail request fails', async () => {
    mocks.expenseApi.getDetail.mockRejectedValue(new Error('加载单据详情超时，请稍后重试'))

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('加载单据详情超时，请稍后重试')
    expect(wrapper.find('[data-testid="readonly-form"]').exists()).toBe(false)
    expect(mocks.elMessage.error).toHaveBeenCalledWith('加载单据详情超时，请稍后重试')
    expect(mocks.expenseApi.getNavigation).not.toHaveBeenCalled()
  })

  it('keeps main detail visible when expense detail request fails', async () => {
    mocks.expenseApi.getExpenseDetail.mockRejectedValue(new Error('加载费用明细超时，请稍后重试'))

    const wrapper = await mountView()
    const detailCards = wrapper.findAll('[data-testid="expense-detail-card"]')

    await detailCards[0]!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('差旅报销单')
    expect(wrapper.find('[data-testid="readonly-form"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('加载失败')
    expect(wrapper.text()).toContain('加载费用明细超时，请稍后重试')
  })

  it('ignores stale navigation responses after switching documents', async () => {
    const firstNavigation = createDeferred<{ data: { prevDocumentCode: string } }>()
    const secondNavigation = createDeferred<{ data: { prevDocumentCode: string } }>()
    mocks.expenseApi.getDetail.mockImplementation((documentCode: string) => Promise.resolve({
      data: {
        ...buildDocumentDetail(),
        documentCode,
        documentTitle: `单据-${documentCode}`
      }
    }))
    mocks.expenseApi.getNavigation.mockImplementation((documentCode: string) => (
      documentCode === 'DOC-001' ? firstNavigation.promise : secondNavigation.promise
    ))
    mocks.resolveExpenseDetailActions.mockImplementation((context: { prevDocumentCode?: string }) => (
      context.prevDocumentCode
        ? [{ key: `prev-${context.prevDocumentCode}`, label: `上一单 ${context.prevDocumentCode}` }]
        : []
    ))

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('单据-DOC-001')

    mocks.route.params.documentCode = 'DOC-002'
    await flushPromises()

    expect(wrapper.text()).toContain('单据-DOC-002')

    firstNavigation.resolve({ data: { prevDocumentCode: 'DOC-OLD' } })
    await flushPromises()
    expect(wrapper.text()).not.toContain('DOC-OLD')

    secondNavigation.resolve({ data: { prevDocumentCode: 'DOC-NEW' } })
    await flushPromises()
    expect(wrapper.text()).toContain('上一单 DOC-NEW')
  })

  it('refreshes navigation with the returned document code after timeline actions', async () => {
    mocks.resolveExpenseDetailActions.mockReturnValue([
      { key: 'comment', label: '评论' }
    ])
    mocks.expenseApi.comment.mockResolvedValue({
      data: {
        ...buildDocumentDetail(),
        documentCode: 'DOC-009',
        documentTitle: '更新后的单据'
      }
    })

    const wrapper = await mountView()
    mocks.expenseApi.getNavigation.mockClear()

    await wrapper.get('.detail-floating-button').trigger('click')
    await flushPromises()

    const commentInput = wrapper.findAll('input')[0]
    await commentInput!.setValue('需要尽快处理')
    await wrapper.findAll('button').find((item) => item.text().includes('发表评论'))!.trigger('click')
    await flushPromises()

    expect(mocks.expenseApi.comment).toHaveBeenCalled()
    expect(mocks.expenseApi.getNavigation).toHaveBeenCalledWith('DOC-009')
    expect(wrapper.text()).toContain('更新后的单据')
  })

  it('renders compact floating action buttons with all actions aligned to the right', async () => {
    mocks.resolveExpenseDetailActions.mockReturnValue([
      { key: 'approve', label: '通过', primary: true, type: 'primary' },
      { key: 'reject', label: '驳回', primary: true, type: 'danger' },
      { key: 'print', label: '打印' },
      { key: 'comment', label: '评论' }
    ])
    mocks.resolveDisabledExpenseDetailActionHint.mockReturnValue('异常单据不可操作')

    const wrapper = await mountView()
    const actionBar = wrapper.get('[data-testid="detail-floating-actions"]')
    const secondaryGroup = wrapper.get('[data-testid="detail-floating-secondary-actions"]')
    const primaryGroup = wrapper.get('[data-testid="detail-floating-primary-actions"]')

    expect(actionBar.classes()).toContain('detail-floating-actions')
    expect(secondaryGroup.classes()).toContain('detail-floating-actions__group--secondary')
    expect(primaryGroup.classes()).toContain('detail-floating-actions__group--primary')
    expect(actionBar.element.firstElementChild).toBe(secondaryGroup.element)
    expect(actionBar.element.lastElementChild).toBe(primaryGroup.element)

    const approveButton = primaryGroup.findAll('.detail-floating-button').find((item) => item.text() === '通过')
    expect(secondaryGroup.text()).toContain('打印')
    expect(secondaryGroup.text()).toContain('评论')
    expect(primaryGroup.text()).toContain('通过')
    expect(primaryGroup.text()).toContain('驳回')
    expect(approveButton?.classes()).toContain('detail-floating-button--approve')
    expect(approveButton?.classes()).toContain('detail-floating-button--colored')

    const buttons = wrapper.findAll('.detail-floating-button')
    expect(buttons).toHaveLength(4)
    expect(wrapper.text()).toContain('异常单据不可操作')
  })
})
