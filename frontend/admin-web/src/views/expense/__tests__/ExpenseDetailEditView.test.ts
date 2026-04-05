import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ExpenseDetailEditView from '@/views/expense/ExpenseDetailEditView.vue'

const mocks = vi.hoisted(() => ({
  route: {
    params: { detailNo: 'D001' },
    query: { draftKey: 'draft-001' },
    fullPath: '/expense/create/details/D001?draftKey=draft-001'
  },
  router: {
    push: vi.fn(),
    replace: vi.fn()
  },
  elMessage: {
    warning: vi.fn(),
    error: vi.fn(),
    success: vi.fn()
  }
}))

vi.mock('vue-router', () => ({
  useRoute: () => mocks.route,
  useRouter: () => mocks.router
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage
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

const FormItemStub = defineComponent({
  template: '<label><slot /></label>'
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

const globalStubs = {
  'el-card': SimpleContainer,
  'el-tag': TagStub,
  'el-button': ButtonStub,
  'el-input': InputStub,
  'el-empty': EmptyStub,
  'el-form-item': FormItemStub,
  'el-icon': SimpleContainer,
  'expense-runtime-form-editor': {
    template: '<div data-testid="detail-form-editor" />'
  }
}

function buildDraft(detailFormData: Record<string, unknown>) {
  return {
    templateCode: 'TPL-001',
    formValues: {},
    templateDetail: {
      templateCode: 'TPL-001',
      templateName: '差旅报销模板',
      templateType: 'report',
      templateTypeLabel: '报销单',
      categoryCode: 'travel',
      schema: {
        layoutMode: 'TWO_COLUMN',
        blocks: []
      },
      sharedArchives: [],
      expenseDetailDesignCode: 'EDD-001',
      expenseDetailDesignName: '差旅明细',
      expenseDetailType: 'COMMON',
      expenseDetailTypeLabel: '普通报销',
      expenseDetailModeDefault: '',
      expenseDetailSchema: {
        layoutMode: 'TWO_COLUMN',
        blocks: []
      },
      expenseDetailSharedArchives: [],
      departmentOptions: []
    },
    expenseDetails: [
      {
        detailNo: 'D001',
        detailTitle: '住宿费',
        sortOrder: 1,
        formData: detailFormData
      }
    ]
  }
}

async function mountView(detailFormData: Record<string, unknown>) {
  window.sessionStorage.setItem('expense-create-draft:draft-001', JSON.stringify(buildDraft(detailFormData)))

  const wrapper = mount(ExpenseDetailEditView, {
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

describe('ExpenseDetailEditView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.sessionStorage.clear()
    mocks.route.params = { detailNo: 'D001' }
    mocks.route.query = { draftKey: 'draft-001' }
    mocks.route.fullPath = '/expense/create/details/D001?draftKey=draft-001'
    mocks.router.push.mockResolvedValue(undefined)
    mocks.router.replace.mockResolvedValue(undefined)
    vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(() => ({
      x: 60,
      y: 0,
      top: 0,
      left: 60,
      bottom: 200,
      right: 1260,
      width: 1200,
      height: 200,
      toJSON: () => ({})
    } as DOMRect))
  })

  it('renders uploaded invoices, uses balanced layout, and switches preview after clicking another invoice', async () => {
    const wrapper = await mountView({
      invoiceAttachments: [
        {
          attachmentId: 'ATT-001',
          fileName: 'invoice-a.pdf',
          contentType: 'application/pdf',
          previewUrl: '/api/auth/expenses/attachments/ATT-001/content'
        },
        {
          attachmentId: 'ATT-002',
          fileName: 'invoice-b.png',
          contentType: 'image/png',
          previewUrl: '/api/auth/expenses/attachments/ATT-002/content'
        }
      ]
    })

    const invoiceItems = wrapper.findAll('[data-testid="expense-invoice-item"]')
    expect(invoiceItems).toHaveLength(2)
    expect(wrapper.get('[data-testid="expense-invoice-preview-file"]').text()).toContain('invoice-a.pdf')
    expect(wrapper.find('[data-testid="expense-invoice-preview-pdf"]').exists()).toBe(true)

    await invoiceItems[1]!.trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="expense-invoice-preview-file"]').text()).toContain('invoice-b.png')
    expect(wrapper.find('[data-testid="expense-invoice-preview-image"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="expense-invoice-workbench"]').classes()).toContain('expense-invoice-workbench--balanced')
    expect(wrapper.find('.expense-invoice-panel--compact-verify').exists()).toBe(true)
  })

  it('shows an empty state when no invoice files are present', async () => {
    const wrapper = await mountView({})

    expect(wrapper.text()).toContain('暂未上传发票文件')
  })

  it('renders a full-width floating footer bar with payment amount summary', async () => {
    const wrapper = await mountView({
      actualPaymentAmount: '120.5',
      invoiceAttachments: []
    })

    const floatingBar = wrapper.get('[data-testid="expense-detail-edit-floating-bar"]')
    expect(floatingBar.classes()).toContain('expense-detail-edit-floating-bar')
    expect(floatingBar.attributes('style')).toContain('width: 1200px')
    expect(wrapper.find('.expense-detail-edit-floating-bar__inner').exists()).toBe(true)
    expect(wrapper.get('[data-testid="expense-detail-edit-floating-amount"]').text()).toContain('金额：¥ 120.50')
    expect(floatingBar.text()).toContain('取消')
    expect(floatingBar.text()).toContain('保存并返回')
  })

  it('shows zero amount when actualPaymentAmount is empty or invalid', async () => {
    const emptyWrapper = await mountView({
      actualPaymentAmount: '',
      invoiceAttachments: []
    })
    expect(emptyWrapper.get('[data-testid="expense-detail-edit-floating-amount"]').text()).toContain('金额：¥ 0.00')

    window.sessionStorage.clear()
    const invalidWrapper = await mountView({
      actualPaymentAmount: 'abc',
      invoiceAttachments: []
    })
    expect(invalidWrapper.get('[data-testid="expense-detail-edit-floating-amount"]').text()).toContain('金额：¥ 0.00')
  })

  it('keeps save behavior unchanged and normalizes actualPaymentAmount before saving', async () => {
    const wrapper = await mountView({
      actualPaymentAmount: '120.5',
      invoiceAttachments: []
    })

    const saveButtons = wrapper.findAll('button').filter((item) => item.text().includes('保存并返回'))
    expect(saveButtons).toHaveLength(1)
    expect(saveButtons[0]!.classes()).toContain('expense-detail-edit-floating-bar__button')
    expect(saveButtons[0]!.classes()).toContain('expense-detail-edit-floating-bar__button--primary')

    await saveButtons[0]!.trigger('click')
    await flushPromises()

    const savedDraft = JSON.parse(window.sessionStorage.getItem('expense-create-draft:draft-001') || '{}')
    expect(savedDraft.expenseDetails[0].formData.actualPaymentAmount).toBe('120.50')
    expect(mocks.elMessage.success).toHaveBeenCalledWith('费用明细已保存')
    expect(mocks.router.push).toHaveBeenCalled()
  })
})
