import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, reactive } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ExpenseDetailReadonlyView from '@/views/expense/ExpenseDetailReadonlyView.vue'

const mocks = vi.hoisted(() => ({
  route: {
    params: { documentCode: 'DOC-001', detailNo: 'D001' },
    query: { returnTo: '/expense/documents/DOC-001' }
  },
  router: {
    push: vi.fn(),
    replace: vi.fn()
  },
  expenseApi: {
    getExpenseDetail: vi.fn(),
    getDetail: vi.fn()
  },
  elMessage: {
    warning: vi.fn(),
    error: vi.fn()
  },
  syncReadonlyPayeeLookups: vi.fn()
}))

mocks.route = reactive({
  params: { documentCode: 'DOC-001', detailNo: 'D001' },
  query: { returnTo: '/expense/documents/DOC-001' as string | undefined }
})

vi.mock('vue-router', () => ({
  useRoute: () => mocks.route,
  useRouter: () => mocks.router
}))

vi.mock('@/api', () => ({
  expenseApi: mocks.expenseApi
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessage: mocks.elMessage
  }
})

vi.mock('@/views/expense/useReadonlyPayeeLookups', () => ({
  useReadonlyPayeeLookups: () => ({
    vendorOptionMap: {},
    payeeOptionMap: {},
    payeeAccountOptionMap: {},
    syncReadonlyPayeeLookups: mocks.syncReadonlyPayeeLookups
  })
}))

const SimpleContainer = defineComponent({
  template: '<div><slot name="header" /><slot /></div>'
})

const InvoiceWorkbenchStub = defineComponent({
  template: '<div><slot name="main-before-list" /></div>'
})

const ReadonlyRendererStub = defineComponent({
  template: '<div data-testid="readonly-renderer" />'
})

async function mountView() {
  const wrapper = mount(ExpenseDetailReadonlyView, {
    global: {
      stubs: {
        ExpenseInvoiceWorkbench: InvoiceWorkbenchStub,
        ExpenseFormReadonlyRenderer: ReadonlyRendererStub,
        'el-card': SimpleContainer,
        'el-tag': SimpleContainer,
        'el-empty': SimpleContainer,
        'el-icon': SimpleContainer
      },
      directives: {
        loading: () => undefined
      }
    }
  })
  await flushPromises()
  return wrapper
}

describe('ExpenseDetailReadonlyView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.route.params.documentCode = 'DOC-001'
    mocks.route.params.detailNo = 'D001'
    mocks.route.query.returnTo = '/expense/documents/DOC-001'
    mocks.router.push.mockResolvedValue(undefined)
    mocks.router.replace.mockResolvedValue(undefined)
    mocks.expenseApi.getExpenseDetail.mockResolvedValue({
      data: {
        detailNo: 'D001',
        detailTitle: '住宿费',
        detailType: 'NORMAL_REIMBURSEMENT',
        detailTypeLabel: '普通报销',
        formData: {},
        schemaSnapshot: { layoutMode: 'TWO_COLUMN', blocks: [] }
      }
    })
    mocks.expenseApi.getDetail.mockResolvedValue({
      data: {
        companyOptions: [],
        departmentOptions: []
      }
    })
    mocks.syncReadonlyPayeeLookups.mockResolvedValue(undefined)
  })

  it('prefers returnTo when leaving the readonly expense detail page', async () => {
    const wrapper = await mountView()

    await wrapper.get('button').trigger('click')

    expect(mocks.router.push).toHaveBeenCalledWith('/expense/documents/DOC-001')
  })

  it('falls back to the parent document detail when no returnTo is provided', async () => {
    mocks.route.query.returnTo = undefined
    const wrapper = await mountView()

    await wrapper.get('button').trigger('click')

    expect(mocks.router.push).toHaveBeenCalledWith({
      name: 'expense-document-detail',
      params: {
        documentCode: 'DOC-001'
      }
    })
  })
})
