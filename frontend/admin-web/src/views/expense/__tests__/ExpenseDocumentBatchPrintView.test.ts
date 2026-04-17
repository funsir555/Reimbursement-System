import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, reactive } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ExpenseDocumentBatchPrintView from '@/views/expense/ExpenseDocumentBatchPrintView.vue'

const mocks = vi.hoisted(() => ({
  route: {
    query: {
      documentCodes: 'DOC-001,DOC-002'
    }
  },
  expenseApi: {
    getDetail: vi.fn(),
    getExpenseDetail: vi.fn()
  },
  elMessage: {
    error: vi.fn()
  },
  syncReadonlyPayeeLookupsBatch: vi.fn()
}))

mocks.route = reactive({
  query: {
    documentCodes: 'DOC-001,DOC-002'
  }
})

vi.mock('vue-router', () => ({
  useRoute: () => mocks.route
}))

vi.mock('@/api', () => ({
  expenseApi: mocks.expenseApi
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage
}))

vi.mock('@/views/expense/useReadonlyPayeeLookups', () => ({
  useReadonlyPayeeLookups: () => ({
    vendorOptionMap: {},
    payeeOptionMap: {},
    payeeAccountOptionMap: {},
    syncReadonlyPayeeLookupsBatch: mocks.syncReadonlyPayeeLookupsBatch
  })
}))

const SimpleContainer = defineComponent({
  template: '<div><slot /></div>'
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

function buildDetail(documentCode: string) {
  return {
    documentCode,
    documentTitle: `${documentCode} 标题`,
    status: 'PENDING_APPROVAL',
    statusLabel: '审批中',
    totalAmount: 100,
    templateSnapshot: {},
    formSchemaSnapshot: { layoutMode: 'TWO_COLUMN', blocks: [] },
    formData: {},
    flowSnapshot: {},
    companyOptions: [],
    departmentOptions: [],
    expenseDetails: [
      {
        detailNo: `${documentCode}-D001`,
        detailType: 'NORMAL_REIMBURSEMENT',
        detailTypeLabel: '普通报销'
      }
    ],
    currentTasks: [],
    actionLogs: []
  }
}

async function mountView() {
  const wrapper = mount(ExpenseDocumentBatchPrintView, {
    global: {
      stubs: {
        'el-empty': EmptyStub,
        'ExpenseDocumentPrintSheet': {
          props: ['detail'],
          template: '<div data-testid="expense-print-sheet">{{ detail.documentCode }}</div>'
        }
      },
      directives: {
        loading: () => undefined
      }
    }
  })
  await flushPromises()
  return wrapper
}

describe('ExpenseDocumentBatchPrintView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.route.query.documentCodes = 'DOC-001,DOC-002'
    mocks.expenseApi.getDetail.mockImplementation((documentCode: string) => Promise.resolve({
      data: buildDetail(documentCode)
    }))
    mocks.expenseApi.getExpenseDetail.mockImplementation((documentCode: string, detailNo: string) => Promise.resolve({
      data: {
        documentCode,
        detailNo,
        detailType: 'NORMAL_REIMBURSEMENT',
        detailTypeLabel: '普通报销',
        schemaSnapshot: { layoutMode: 'TWO_COLUMN', blocks: [] },
        formData: {}
      }
    }))
    mocks.syncReadonlyPayeeLookupsBatch.mockResolvedValue(undefined)
    vi.stubGlobal('print', vi.fn())
  })

  it('renders all requested documents and auto prints after loading', async () => {
    const wrapper = await mountView()

    expect(mocks.expenseApi.getDetail).toHaveBeenCalledTimes(2)
    expect(mocks.syncReadonlyPayeeLookupsBatch).toHaveBeenCalledTimes(1)
    expect(wrapper.findAll('[data-testid="expense-print-sheet"]')).toHaveLength(2)
    expect(wrapper.text()).toContain('DOC-001')
    expect(wrapper.text()).toContain('DOC-002')
    expect(window.print).toHaveBeenCalledTimes(1)
  })

  it('shows a stable empty state and skips print when document codes are missing', async () => {
    mocks.route.query.documentCodes = ''

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('缺少可打印的单据编号')
    expect(window.print).not.toHaveBeenCalled()
  })
})
