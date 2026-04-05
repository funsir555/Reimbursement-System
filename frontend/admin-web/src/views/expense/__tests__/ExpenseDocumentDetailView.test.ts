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

function buildDocumentDetail(totalAmount: string | number | null | undefined = 1880.5) {
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
    actionLogs: []
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

  it('removes the flow outline section and appends current pending approvers into the timeline', async () => {
    mocks.expenseApi.getDetail.mockResolvedValue({
      data: {
        ...buildDocumentDetail(),
        actionLogs: [
          {
            id: 10,
            documentCode: 'DOC-001',
            actionType: 'SUBMIT',
            actorUserId: 1,
            actorName: 'ZhangSan',
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
    expect(wrapper.text()).toContain('ZhangSan 提交单据')
    expect(wrapper.text()).toContain('Finance LiSi 待审批')
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

  it('keeps detail usable when navigation request fails', async () => {
    mocks.expenseApi.getNavigation.mockRejectedValue(new Error('navigation failed'))

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('差旅报销单')
    expect(wrapper.text()).toContain('审批轨迹')
    expect(mocks.elMessage.error).not.toHaveBeenCalled()
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

  it('renders the floating action bar right aligned with enlarged action buttons', async () => {
    mocks.resolveExpenseDetailActions.mockReturnValue([
      { key: 'recall', label: '召回', primary: true, type: 'primary' },
      { key: 'print', label: '打印' },
      { key: 'comment', label: '评论' }
    ])
    mocks.resolveDisabledExpenseDetailActionHint.mockReturnValue('异常单据不可操作')

    const wrapper = await mountView()
    const actionBar = wrapper.get('[data-testid="detail-floating-actions"]')

    expect(actionBar.classes()).toContain('detail-floating-actions')
    const buttons = wrapper.findAll('.detail-floating-button')
    expect(buttons).toHaveLength(3)
    expect(wrapper.text()).toContain('异常单据不可操作')
  })
})
