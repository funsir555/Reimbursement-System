import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, reactive } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ExpenseDocumentBatchPrintView from '@/views/expense/ExpenseDocumentBatchPrintView.vue'

const mocks = vi.hoisted(() => ({
  route: {
    params: {},
    query: {
      documentCodes: 'DOC-001,DOC-002'
    }
  },
  router: {
    replace: vi.fn()
  },
  expenseApi: {
    getPrintPdf: vi.fn(),
    getBatchPrintPdf: vi.fn()
  },
  elMessage: {
    error: vi.fn()
  },
  createObjectURL: vi.fn(),
  revokeObjectURL: vi.fn()
}))

mocks.route = reactive({
  params: {},
  query: {
    documentCodes: 'DOC-001,DOC-002'
  }
})

vi.mock('vue-router', () => ({
  useRoute: () => mocks.route,
  useRouter: () => mocks.router
}))

vi.mock('@/api', () => ({
  expenseApi: mocks.expenseApi
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage
}))

const ButtonStub = defineComponent({
  props: {
    disabled: {
      type: Boolean,
      default: false
    },
    type: {
      type: String,
      default: ''
    },
    plain: {
      type: Boolean,
      default: false
    }
  },
  emits: ['click'],
  template: '<button type="button" :disabled="disabled" @click="$emit(\'click\')"><slot /></button>'
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

async function mountView() {
  const wrapper = mount(ExpenseDocumentBatchPrintView, {
    global: {
      stubs: {
        'el-button': ButtonStub,
        'el-empty': EmptyStub
      },
      directives: {
        loading: () => undefined
      }
    }
  })
  await flushPromises()
  return wrapper
}

const mountedWrappers: Array<Awaited<ReturnType<typeof mountView>>> = []

describe('ExpenseDocumentBatchPrintView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.route.params = {}
    mocks.route.query = { documentCodes: 'DOC-001,DOC-002' }
    mocks.router.replace.mockResolvedValue(undefined)

    let objectUrlIndex = 0
    mocks.createObjectURL.mockImplementation(() => `blob:preview-${++objectUrlIndex}`)
    Object.defineProperty(window.URL, 'createObjectURL', {
      configurable: true,
      writable: true,
      value: mocks.createObjectURL
    })
    Object.defineProperty(window.URL, 'revokeObjectURL', {
      configurable: true,
      writable: true,
      value: mocks.revokeObjectURL
    })

    mocks.expenseApi.getBatchPrintPdf.mockResolvedValue({
      blob: new Blob(['batch-pdf'], { type: 'application/pdf' }),
      fileName: 'expense-documents-batch-2.pdf',
      contentType: 'application/pdf'
    })
    mocks.expenseApi.getPrintPdf.mockResolvedValue({
      blob: new Blob(['single-pdf'], { type: 'application/pdf' }),
      fileName: 'expense-document-DOC-001.pdf',
      contentType: 'application/pdf'
    })
  })

  afterEach(() => {
    mountedWrappers.splice(0).forEach((wrapper) => wrapper.unmount())
  })

  it('loads batch PDF preview and renders browser PDF viewer iframe', async () => {
    const wrapper = await mountView()
    mountedWrappers.push(wrapper)

    expect(mocks.expenseApi.getBatchPrintPdf).toHaveBeenCalledWith(['DOC-001', 'DOC-002'], 'PORTRAIT')
    expect(wrapper.get('[data-testid="expense-pdf-preview-frame"]').attributes('src')).toContain('blob:preview-1')
    expect(wrapper.text()).toContain('批量打印预览')
    expect(wrapper.text()).toContain('2 张单据')
  })

  it('switches orientation and reloads the PDF with the new backend parameter', async () => {
    const wrapper = await mountView()
    mountedWrappers.push(wrapper)

    await wrapper.get('[data-testid="expense-pdf-preview-landscape"]').trigger('click')
    await flushPromises()

    expect(mocks.router.replace).toHaveBeenCalledWith({
      query: {
        documentCodes: 'DOC-001,DOC-002',
        orientation: 'LANDSCAPE'
      }
    })
    expect(mocks.expenseApi.getBatchPrintPdf).toHaveBeenLastCalledWith(['DOC-001', 'DOC-002'], 'LANDSCAPE')
    expect(mocks.revokeObjectURL).toHaveBeenCalledWith('blob:preview-1')
  })

  it('loads single-document preview when routed from detail print entry', async () => {
    mocks.route.params = { documentCode: 'DOC-001' }
    mocks.route.query = {}

    const wrapper = await mountView()
    mountedWrappers.push(wrapper)

    expect(mocks.expenseApi.getPrintPdf).toHaveBeenCalledWith('DOC-001', 'PORTRAIT')
    expect(wrapper.text()).toContain('单据打印预览')
  })

  it('shows a stable empty state when batch document codes are missing', async () => {
    mocks.route.params = {}
    mocks.route.query = {}

    const wrapper = await mountView()
    mountedWrappers.push(wrapper)

    expect(mocks.expenseApi.getBatchPrintPdf).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('缺少可打印的单据编号')
    expect(wrapper.find('[data-testid="expense-pdf-preview-frame"]').exists()).toBe(false)
  })
})
