import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ExpenseRuntimeFormEditor from '@/views/expense/components/ExpenseRuntimeFormEditor.vue'

const mocks = vi.hoisted(() => ({
  expenseApi: {
    getDocumentPicker: vi.fn()
  },
  expenseCreateApi: {
    listVendorOptions: vi.fn(),
    listPayeeOptions: vi.fn(),
    listPayeeAccountOptions: vi.fn(),
    createVendor: vi.fn(),
    uploadAttachment: vi.fn()
  },
  elMessage: {
    error: vi.fn(),
    warning: vi.fn(),
    success: vi.fn()
  }
}))

vi.mock('@/api', async () => {
  const actual = await vi.importActual<typeof import('@/api')>('@/api')
  return {
    ...actual,
    expenseApi: mocks.expenseApi,
    expenseCreateApi: mocks.expenseCreateApi
  }
})

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessage: mocks.elMessage
  }
})

const SimpleContainer = defineComponent({
  template: '<div><slot /><slot name="tip" /><slot name="footer" /></div>'
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

const SelectStub = defineComponent({
  props: {
    modelValue: {
      type: [String, Number, Array, Object],
      default: ''
    }
  },
  emits: ['update:modelValue'],
  template: '<div data-testid="select"><slot /><slot name="footer" /></div>'
})

const InputStub = defineComponent({
  inheritAttrs: false,
  props: {
    modelValue: {
      type: [String, Number],
      default: ''
    }
  },
  emits: ['update:modelValue'],
  template: '<input v-bind="$attrs" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
})

const InputNumberStub = defineComponent({
  inheritAttrs: false,
  props: {
    modelValue: {
      type: Number,
      default: undefined
    }
  },
  emits: ['update:modelValue'],
  template: '<input v-bind="$attrs" type="number" :value="modelValue ?? \'\'" @input="$emit(\'update:modelValue\', Number($event.target.value))" />'
})

const MoneyInputStub = defineComponent({
  inheritAttrs: false,
  props: {
    modelValue: {
      type: [String, Number],
      default: ''
    }
  },
  emits: ['update:modelValue'],
  template: '<input v-bind="$attrs" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
})

const OptionStub = defineComponent({
  props: {
    label: {
      type: String,
      default: ''
    }
  },
  template: '<span class="option">{{ label }}</span>'
})

const UploadStub = defineComponent({
  props: {
    fileList: {
      type: Array,
      default: () => []
    }
  },
  emits: ['change', 'remove'],
  template: '<div data-testid="upload"><slot /><slot name="tip" /></div>'
})

const SupplierPaymentInfoFieldsStub = defineComponent({
  props: {
    formState: {
      type: Object,
      required: true
    }
  },
  methods: {
    applyDefaults() {
      Object.assign(this.formState as Record<string, unknown>, {
        receiptAccountName: '新增供应商开户名',
        cVenBankCode: 'ICBC',
        cVenBank: '中国工商银行',
        receiptBankProvince: '上海市',
        receiptBankCity: '上海市',
        receiptBranchCode: 'ICBC-SH-001',
        receiptBranchName: '中国工商银行上海分行',
        cVenAccount: '6222020000000001'
      })
    }
  },
  mounted() {
    this.applyDefaults()
  },
  updated() {
    this.applyDefaults()
  },
  template: '<div data-testid="supplier-payment-info-fields" />'
})

function createPermission() {
  return {
    fixedStages: {
      DRAFT_BEFORE_SUBMIT: 'EDITABLE',
      RESUBMIT_AFTER_RETURN: 'EDITABLE',
      IN_APPROVAL: 'READONLY',
      ARCHIVED: 'READONLY'
    },
    sceneOverrides: []
  } as const
}

function createBusinessBlock(fieldKey: string, label: string, componentCode: string, props: Record<string, unknown> = {}) {
  return {
    blockId: fieldKey,
    fieldKey,
    kind: 'BUSINESS_COMPONENT' as const,
    label,
    span: 1,
    required: false,
    helpText: '',
    props: {
      componentCode,
      ...props
    },
    permission: createPermission()
  }
}

function createAttachmentBlock(fieldKey: string) {
  return {
    blockId: fieldKey,
    fieldKey,
    kind: 'CONTROL' as const,
    label: '发票附件',
    span: 1,
    required: false,
    helpText: '',
    props: {
      controlType: 'ATTACHMENT',
      maxCount: 3
    },
    permission: createPermission()
  }
}

function mountEditor(
  modelValue: Record<string, unknown>,
  blocks: unknown[],
  extraProps: Record<string, unknown> = {}
) {
  const wrapper = mount(ExpenseRuntimeFormEditor, {
    props: {
      modelValue,
      schema: {
        layoutMode: 'TWO_COLUMN',
        blocks
      },
      companyOptions: [
        { label: '上海分公司', value: 'COMPANY-001' },
        { label: '北京分公司', value: 'COMPANY-002' }
      ],
      ...extraProps
    },
    global: {
      stubs: {
        'el-form-item': SimpleContainer,
        'el-input': InputStub,
        'el-input-number': InputNumberStub,
        MoneyInput: MoneyInputStub,
        'money-input': MoneyInputStub,
        'el-date-picker': SimpleContainer,
        'el-select': SelectStub,
        'el-option': OptionStub,
        'el-radio-group': SimpleContainer,
        'el-radio': SimpleContainer,
        'el-checkbox-group': SimpleContainer,
        'el-checkbox': SimpleContainer,
        'el-switch': SimpleContainer,
        'el-upload': UploadStub,
        'el-button': ButtonStub,
        'el-dialog': SimpleContainer,
        SupplierPaymentInfoFields: SupplierPaymentInfoFieldsStub
      }
    }
  })

  return {
    wrapper,
    modelValue
  }
}

describe('ExpenseRuntimeFormEditor', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.expenseApi.getDocumentPicker.mockResolvedValue({ data: { relationType: 'RELATED', groups: [] } })
    mocks.expenseCreateApi.listVendorOptions.mockResolvedValue({ data: [] })
    mocks.expenseCreateApi.listPayeeOptions.mockResolvedValue({ data: [] })
    mocks.expenseCreateApi.listPayeeAccountOptions.mockResolvedValue({ data: [] })
    mocks.expenseCreateApi.createVendor.mockResolvedValue({ data: {} })
    mocks.expenseCreateApi.uploadAttachment.mockResolvedValue({
      data: {
        attachmentId: 'ATT-001',
        fileName: 'invoice.pdf',
        contentType: 'application/pdf',
        previewUrl: '/api/auth/expenses/attachments/ATT-001/content'
      }
    })
  })

  it('renders payment company business component with company options', async () => {
    const { wrapper } = mountEditor({
      paymentCompany: ''
    }, [
      createBusinessBlock('paymentCompany', '付款公司', 'payment-company')
    ])

    await flushPromises()

    expect(wrapper.text()).toContain('上海分公司')
    expect(wrapper.text()).toContain('北京分公司')
    expect(mocks.expenseCreateApi.listVendorOptions).toHaveBeenCalled()
    expect(mocks.expenseCreateApi.listPayeeOptions).toHaveBeenCalled()
    expect(mocks.expenseCreateApi.listPayeeAccountOptions).toHaveBeenCalled()
  })

  it('loads payee options from personal-center names and employee-mode payee accounts', async () => {
    mountEditor({
      payee: {
        value: 'PERSONAL_PAYEE:张三',
        label: '张三',
        sourceType: 'PERSONAL_PRIVATE_PAYEE',
        sourceCode: '张三'
      },
      payeeAccount: ''
    }, [
      createBusinessBlock('payee', '收款人', 'payee'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
    ])

    await flushPromises()

    expect(mocks.expenseCreateApi.listPayeeOptions).toHaveBeenCalledWith({
      keyword: '',
      personalOnly: true
    })
    expect(mocks.expenseCreateApi.listPayeeAccountOptions).toHaveBeenLastCalledWith({
      keyword: '',
      linkageMode: 'EMPLOYEE',
      payeeName: '张三',
      counterpartyCode: undefined
    })
  })

  it('switches payee-account lookup to enterprise mode when a counterparty is selected', async () => {
    mountEditor({
      counterparty: 'VEN-001',
      payeeAccount: ''
    }, [
      createBusinessBlock('counterparty', '收款单位', 'counterparty'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
    ])

    await flushPromises()

    expect(mocks.expenseCreateApi.listPayeeAccountOptions).toHaveBeenLastCalledWith({
      keyword: '',
      linkageMode: 'ENTERPRISE',
      payeeName: undefined,
      counterpartyCode: 'VEN-001'
    })
  })

  it('shows add supplier entry for counterparty and backfills the created vendor code', async () => {
    mocks.expenseCreateApi.createVendor.mockResolvedValue({
      data: {
        cVenCode: 'VEN-NEW',
        cVenName: '新增供应商'
      }
    })
    mocks.expenseCreateApi.listVendorOptions.mockResolvedValue({
      data: [{
        value: 'VEN-NEW',
        label: '新增供应商',
        secondaryLabel: 'VEN-NEW / NEW',
        cVenCode: 'VEN-NEW',
        cVenName: '新增供应商'
      }]
    })

    const { wrapper, modelValue } = mountEditor({
      counterparty: ''
    }, [
      createBusinessBlock('counterparty', '收款单位', 'counterparty')
    ])

    await flushPromises()

    expect(wrapper.text()).toContain('增加供应商')

    const openButton = wrapper.findAll('button').find((button) => button.text() === '增加供应商')
    expect(openButton).toBeTruthy()
    await openButton!.trigger('click')
    await wrapper.get('input[placeholder="请输入供应商名称"]').setValue('新增供应商')
    Object.assign(
      wrapper.getComponent(SupplierPaymentInfoFieldsStub).props('formState') as Record<string, unknown>,
      {
        receiptAccountName: '新增供应商开户名',
        cVenBankCode: 'ICBC',
        cVenBank: '中国工商银行',
        receiptBankProvince: '上海市',
        receiptBankCity: '上海市',
        receiptBranchCode: 'ICBC-SH-001',
        receiptBranchName: '中国工商银行上海分行',
        cVenAccount: '6222020000000001'
      }
    )
    const saveButton = wrapper.findAll('button').find((button) => button.text() === '保存供应商')
    expect(saveButton).toBeTruthy()
    await saveButton!.trigger('click')
    await flushPromises()

    expect(mocks.expenseCreateApi.createVendor).toHaveBeenCalledWith(expect.objectContaining({
      cVenName: '新增供应商',
      receiptAccountName: '新增供应商开户名',
      cVenBank: '中国工商银行',
      receiptBranchName: '中国工商银行上海分行'
    }))
    expect(modelValue.counterparty).toBe('VEN-NEW')
  })

  it('uploads selected attachment and writes metadata back to the field', async () => {
    const { wrapper, modelValue } = mountEditor({
      invoiceAttachments: []
    }, [
      createAttachmentBlock('invoiceAttachments')
    ])

    await flushPromises()

    const upload = wrapper.getComponent(UploadStub)
    const rawFile = new File(['pdf-body'], 'invoice.pdf', { type: 'application/pdf' })
    upload.vm.$emit('change', {
      name: 'invoice.pdf',
      raw: rawFile
    })

    await flushPromises()

    expect(mocks.expenseCreateApi.uploadAttachment).toHaveBeenCalledWith(rawFile)
    expect(modelValue).toMatchObject({
      invoiceAttachments: [{
        attachmentId: 'ATT-001',
        fileName: 'invoice.pdf',
        contentType: 'application/pdf',
        previewUrl: '/api/auth/expenses/attachments/ATT-001/content'
      }]
    })
  })

  it('removes attachment metadata by attachment id when file list item is deleted', async () => {
    const { wrapper, modelValue } = mountEditor({
      invoiceAttachments: [
        {
          attachmentId: 'ATT-001',
          fileName: 'invoice.pdf',
          contentType: 'application/pdf',
          previewUrl: '/api/auth/expenses/attachments/ATT-001/content'
        },
        {
          attachmentId: 'ATT-002',
          fileName: 'taxi.png',
          contentType: 'image/png',
          previewUrl: '/api/auth/expenses/attachments/ATT-002/content'
        }
      ]
    }, [
      createAttachmentBlock('invoiceAttachments')
    ])

    await flushPromises()

    const upload = wrapper.getComponent(UploadStub)
    upload.vm.$emit('remove', {
      uid: 1,
      name: 'invoice.pdf'
    })

    await flushPromises()

    expect(modelValue).toMatchObject({
      invoiceAttachments: [{
        attachmentId: 'ATT-002',
        fileName: 'taxi.png'
      }]
    })
  })

  it('loads and confirms related document selections from the picker', async () => {
    mocks.expenseApi.getDocumentPicker.mockResolvedValue({
      data: {
        relationType: 'RELATED',
        groups: [
          {
            templateType: 'report',
            templateTypeLabel: '报销单',
            total: 1,
            page: 1,
            pageSize: 10,
            items: [{
              documentCode: 'DOC-REL-001',
              documentTitle: '差旅报销单',
              templateType: 'report',
              templateTypeLabel: '报销单',
              templateName: '标准报销模板',
              status: 'APPROVED',
              statusLabel: '已审批',
              totalAmount: 1200
            }]
          }
        ]
      }
    })

    const { wrapper, modelValue } = mountEditor({
      relatedDocs: []
    }, [
      createBusinessBlock('relatedDocs', '关联单据', 'related-document', {
        allowedTemplateTypes: ['report', 'application', 'contract', 'loan']
      })
    ])

    await flushPromises()
    await wrapper.get('[data-testid="open-document-picker-relatedDocs"]').trigger('click')
    await flushPromises()

    expect(mocks.expenseApi.getDocumentPicker).toHaveBeenCalledWith({
      relationType: 'RELATED',
      templateTypes: ['report', 'application', 'contract', 'loan'],
      keyword: undefined
    })

    await wrapper.get('[data-testid="toggle-document-picker-DOC-REL-001"]').trigger('click')
    await wrapper.get('[data-testid="confirm-document-picker"]').trigger('click')
    await flushPromises()

    expect(modelValue.relatedDocs).toEqual([{
      documentCode: 'DOC-REL-001',
      documentTitle: '差旅报销单',
      templateType: 'report',
      templateTypeLabel: '报销单',
      templateName: '标准报销模板',
      status: 'APPROVED',
      statusLabel: '已审批'
    }])
  })

  it('stores writeoff selections and supports editing writeoff amount after selection', async () => {
    mocks.expenseApi.getDocumentPicker.mockResolvedValue({
      data: {
        relationType: 'WRITEOFF',
        groups: [
          {
            templateType: 'loan',
            templateTypeLabel: '借款单',
            total: 1,
            page: 1,
            pageSize: 10,
            items: [{
              documentCode: 'DOC-WO-001',
              documentTitle: '项目借款单',
              templateType: 'loan',
              templateTypeLabel: '借款单',
              templateName: '借款模板',
              status: 'APPROVED',
              statusLabel: '已审批',
              totalAmount: 800,
              availableWriteOffAmount: 500,
              writeOffSourceKind: 'LOAN'
            }]
          }
        ]
      }
    })

    const { wrapper, modelValue } = mountEditor({
      writeoffDocs: []
    }, [
      createBusinessBlock('writeoffDocs', '核销单据', 'writeoff-document', {
        allowedTemplateTypes: ['report', 'loan']
      })
    ])

    await flushPromises()
    await wrapper.get('[data-testid="open-document-picker-writeoffDocs"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="toggle-document-picker-DOC-WO-001"]').trigger('click')
    await wrapper.get('[data-testid="confirm-document-picker"]').trigger('click')
    await flushPromises()

    expect(modelValue.writeoffDocs).toEqual([expect.objectContaining({
      documentCode: 'DOC-WO-001',
      writeOffSourceKind: 'LOAN',
      availableWriteOffAmount: 500
    })])

    await wrapper.get('[data-testid="writeoff-amount-writeoffDocs-DOC-WO-001"]').setValue('120')
    await flushPromises()

    expect(modelValue.writeoffDocs).toEqual([expect.objectContaining({
      documentCode: 'DOC-WO-001',
      writeOffAmount: '120.00',
      remainingAmount: '380.00'
    })])
  })
})
