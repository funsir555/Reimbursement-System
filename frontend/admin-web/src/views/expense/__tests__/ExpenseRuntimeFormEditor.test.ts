import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h, ref } from 'vue'
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
    getVendorDetail: vi.fn(),
    createVendor: vi.fn(),
    updateVendor: vi.fn(),
    uploadAttachment: vi.fn(),
    recognizeAttachmentOcr: vi.fn()
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

vi.mock('element-plus', async () => ({
  ElMessage: mocks.elMessage,
  ElFormItem: {
    name: 'ElFormItem',
    template: '<div><slot /><slot name="tip" /><slot name="footer" /></div>'
  },
  ElInput: {
    name: 'ElInput',
    inheritAttrs: false,
    props: {
      modelValue: {
        type: [String, Number],
        default: ''
      }
    },
    emits: ['update:modelValue'],
    template: '<input v-bind="$attrs" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
  },
  ElInputNumber: {
    name: 'ElInputNumber',
    inheritAttrs: false,
    props: {
      modelValue: {
        type: Number,
        default: undefined
      }
    },
    emits: ['update:modelValue'],
    template: '<input v-bind="$attrs" type="number" :value="modelValue ?? null" @input="$emit(\'update:modelValue\', Number($event.target.value))" />'
  },
  ElDatePicker: {
    name: 'ElDatePicker',
    inheritAttrs: false,
    render() {
      return h('div', {
        ...this.$attrs,
        'data-testid': 'date-picker',
        'data-placeholder': this.$attrs.placeholder || '',
        'data-start-placeholder': this.$attrs['start-placeholder'] || '',
        'data-end-placeholder': this.$attrs['end-placeholder'] || '',
        'data-range-separator': this.$attrs['range-separator'] || ''
      })
    }
  },
  ElSelect: {
    name: 'ElSelect',
    inheritAttrs: false,
    props: {
      modelValue: {
        type: [String, Number, Array, Object],
        default: ''
      }
    },
    emits: ['update:modelValue', 'change'],
    template: '<div v-bind="$attrs"><slot /><slot name="footer" /></div>'
  },
  ElOption: {
    name: 'ElOption',
    props: {
      label: {
        type: String,
        default: ''
      }
    },
    template: '<span class="option">{{ label }}</span>'
  },
  ElRadioGroup: { name: 'ElRadioGroup', template: '<div><slot /></div>' },
  ElRadio: { name: 'ElRadio', template: '<div><slot /></div>' },
  ElCheckboxGroup: { name: 'ElCheckboxGroup', template: '<div><slot /></div>' },
  ElCheckbox: { name: 'ElCheckbox', template: '<div><slot /></div>' },
  ElSwitch: { name: 'ElSwitch', template: '<div><slot /></div>' },
  ElUpload: {
    name: 'ElUpload',
    inheritAttrs: false,
    props: {
      fileList: {
        type: Array,
        default: () => []
      }
    },
    emits: ['change', 'remove'],
    template: '<div v-bind="$attrs" data-testid="upload"><slot /><slot name="tip" /></div>'
  },
  ElButton: {
    name: 'ElButton',
    props: {
      disabled: {
        type: Boolean,
        default: false
      },
      loading: {
        type: Boolean,
        default: false
      }
    },
    emits: ['click'],
    template: '<button type="button" :disabled="disabled" :data-loading="String(loading)" @click="$emit(\'click\', $event)"><slot /></button>'
  },
  ElDialog: {
    name: 'ElDialog',
    template: '<div><slot /><slot name="footer" /></div>'
  }
}))

const MoneyInputStub = defineComponent({
  name: 'MoneyInputStub',
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

const SupplierPaymentInfoFieldsStub = defineComponent({
  name: 'SupplierPaymentInfoFieldsStub',
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

function createControlBlock(fieldKey: string, label: string, controlType: string, props: Record<string, unknown> = {}) {
  return {
    blockId: fieldKey,
    fieldKey,
    kind: 'CONTROL' as const,
    label,
    span: 1,
    required: false,
    helpText: '',
    props: {
      controlType,
      ...props
    },
    permission: createPermission()
  }
}

function mountEditor(initialModelValue: Record<string, unknown>, blocks: unknown[], extraProps: Record<string, unknown> = {}) {
  const model = ref<Record<string, unknown>>({ ...initialModelValue })
  const Host = defineComponent({
    setup() {
      return () => h(ExpenseRuntimeFormEditor, {
        modelValue: model.value,
        'onUpdate:modelValue': (next: Record<string, unknown>) => {
          model.value = next
        },
        schema: {
          layoutMode: 'TWO_COLUMN',
          blocks
        },
        companyOptions: [
          { label: '上海分公司', value: 'COMPANY-001' },
          { label: '北京分公司', value: 'COMPANY-002' }
        ],
        departmentOptions: [
          { label: '市场部', value: 'DEPT-001' }
        ],
        ...extraProps
      })
    }
  })

  const wrapper = mount(Host, {
    global: {
      stubs: {
        MoneyInput: MoneyInputStub,
        'money-input': MoneyInputStub,
        SupplierPaymentInfoFields: SupplierPaymentInfoFieldsStub
      }
    }
  })

  return {
    wrapper,
    model,
    setModelValue(next: Record<string, unknown>) {
      model.value = { ...next }
    }
  }
}

describe('ExpenseRuntimeFormEditor', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.expenseApi.getDocumentPicker.mockResolvedValue({ data: { relationType: 'RELATED', groups: [] } })
    mocks.expenseCreateApi.listVendorOptions.mockResolvedValue({ data: [] })
    mocks.expenseCreateApi.listPayeeOptions.mockResolvedValue({ data: [] })
    mocks.expenseCreateApi.listPayeeAccountOptions.mockResolvedValue({ data: [] })
    mocks.expenseCreateApi.getVendorDetail.mockResolvedValue({
      data: {
        cVenCode: 'VEN-001',
        cVenName: '上海测试供应商',
        receiptAccountName: '上海测试供应商',
        cVenBankCode: 'ICBC',
        cVenBank: '中国工商银行',
        receiptBankProvince: '上海市',
        receiptBankCity: '上海市',
        receiptBranchCode: 'ICBC-SH-001',
        receiptBranchName: '中国工商银行上海分行',
        cVenAccount: '6222020000000001'
      }
    })
    mocks.expenseCreateApi.createVendor.mockResolvedValue({
      data: {
        cVenCode: 'VEN-NEW',
        cVenName: '新增供应商'
      }
    })
    mocks.expenseCreateApi.updateVendor.mockResolvedValue({ data: {} })
    mocks.expenseCreateApi.uploadAttachment.mockResolvedValue({
      data: {
        attachmentId: 'ATT-001',
        fileName: 'invoice.pdf',
        contentType: 'application/pdf',
        previewUrl: '/api/auth/expenses/attachments/ATT-001/content'
      }
    })
    mocks.expenseCreateApi.recognizeAttachmentOcr.mockResolvedValue({
      data: {
        status: 'SUCCESS',
        providerCode: 'ALIYUN',
        providerName: '阿里云',
        invoiceCode: '1234567890',
        invoiceNumber: '87654321',
        invoiceDate: '2026-04-19',
        invoiceType: '增值税电子普通发票',
        sellerName: '上海测试商户',
        totalAmount: 188.5,
        taxAmount: 10.68,
        message: '识别成功'
      }
    })
  })

  it('keeps counterparty disabled before payment company is selected and does not load vendor options', async () => {
    const { wrapper } = mountEditor({ paymentCompany: '', counterparty: '' }, [
      createBusinessBlock('paymentCompany', '付款公司', 'payment-company'),
      createBusinessBlock('counterparty', '收款单位', 'counterparty')
    ])

    await flushPromises()

    expect(mocks.expenseCreateApi.listVendorOptions).not.toHaveBeenCalled()
    const counterpartySelect = wrapper.get('[data-testid="counterparty-select-counterparty"]')
    expect(counterpartySelect.attributes('disabled')).toBeDefined()
    expect(counterpartySelect.classes()).toContain('expense-runtime-counterparty-select')
    expect(counterpartySelect.attributes('placeholder')).toBe('请先选择付款公司')
  })

  it('uses current user company as the fallback company for counterparty options', async () => {
    mountEditor({ paymentCompany: '', counterparty: '', payeeAccount: '' }, [
      createBusinessBlock('paymentCompany', '付款公司', 'payment-company'),
      createBusinessBlock('counterparty', '收款单位', 'counterparty'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
    ], {
      currentUserCompanyId: 'COMPANY-001'
    })

    await flushPromises()

    expect(mocks.expenseCreateApi.listVendorOptions).toHaveBeenCalledWith({
      keyword: undefined,
      paymentCompanyId: 'COMPANY-001'
    })
  })

  it('loads vendor options with payment company and clears counterparty plus payee account when company changes', async () => {
    const initialPayeeAccount = { value: 'VENDOR_ACCOUNT:1', label: '默认账户' }
    const { model, setModelValue } = mountEditor({
      paymentCompany: 'COMPANY-001',
      counterparty: 'VEN-001',
      payeeAccount: initialPayeeAccount
    }, [
      createBusinessBlock('paymentCompany', '付款公司', 'payment-company'),
      createBusinessBlock('counterparty', '收款单位', 'counterparty'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
    ])

    await flushPromises()
    expect(mocks.expenseCreateApi.listVendorOptions).toHaveBeenCalledWith({
      keyword: undefined,
      paymentCompanyId: 'COMPANY-001'
    })

    setModelValue({
      paymentCompany: 'COMPANY-002',
      counterparty: 'VEN-001',
      payeeAccount: initialPayeeAccount
    })
    await flushPromises()

    expect(model.value.counterparty).toBe('')
    expect(model.value.payeeAccount).toBe('')
    expect(mocks.expenseCreateApi.listVendorOptions).toHaveBeenLastCalledWith({
      keyword: undefined,
      paymentCompanyId: 'COMPANY-002'
    })
  })

  it('keeps personal payee account lookups on the original employee-only chain', async () => {
    mountEditor({ payee: '', payeeAccount: '' }, [
      createBusinessBlock('payee', '收款人', 'payee'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
    ])

    await flushPromises()

    expect(mocks.expenseCreateApi.listPayeeAccountOptions).toHaveBeenCalledWith({
      keyword: '',
      linkageMode: 'EMPLOYEE',
      payeeName: undefined,
      counterpartyCode: undefined,
      paymentCompanyId: undefined
    })
  })

  it('creates a vendor under the selected payment company and backfills counterparty', async () => {
    mocks.expenseCreateApi.listVendorOptions.mockResolvedValue({
      data: [{
        value: 'VEN-NEW',
        label: '新增供应商',
        secondaryLabel: 'VEN-NEW / 新增供应商',
        cVenCode: 'VEN-NEW',
        cVenName: '新增供应商'
      }]
    })

    const { wrapper, model } = mountEditor({
      paymentCompany: 'COMPANY-001',
      counterparty: '',
      payeeAccount: ''
    }, [
      createBusinessBlock('paymentCompany', '付款公司', 'payment-company'),
      createBusinessBlock('counterparty', '收款单位', 'counterparty'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
    ])

    await flushPromises()
    await wrapper.get('[data-testid="counterparty-create-vendor-counterparty"]').trigger('click')
    await flushPromises()
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

    expect(mocks.expenseCreateApi.createVendor).toHaveBeenCalledWith(
      'COMPANY-001',
      expect.objectContaining({
        cVenName: '新增供应商',
        receiptAccountName: '新增供应商开户名',
        cVenBank: '中国工商银行'
      })
    )
    expect(mocks.elMessage.success).toHaveBeenCalledWith('供应商及收款信息已保存')
    expect(model.value.counterparty).toBe('VEN-NEW')
  })

  it('allows opening vendor creation with the current user company when no payment company is selected', async () => {
    const { wrapper } = mountEditor({ paymentCompany: '', counterparty: '', payeeAccount: '' }, [
      createBusinessBlock('paymentCompany', '付款公司', 'payment-company'),
      createBusinessBlock('counterparty', '收款单位', 'counterparty'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
    ], {
      currentUserCompanyId: 'COMPANY-001'
    })

    await flushPromises()
    await wrapper.get('[data-testid="counterparty-create-vendor-counterparty"]').trigger('click')
    await flushPromises()

    expect(mocks.elMessage.warning).not.toHaveBeenCalledWith('请先选择付款公司')
  })

  it('shows vendor account maintenance entry and updates the selected supplier account in place', async () => {
    mocks.expenseCreateApi.listPayeeAccountOptions
      .mockResolvedValueOnce({ data: [] })
      .mockResolvedValueOnce({ data: [] })
      .mockResolvedValueOnce({
        data: [{
          value: 'VENDOR_ACCOUNT:8',
          label: '上海测试供应商',
          sourceType: 'ENTERPRISE_VENDOR',
          ownerCode: 'VEN-001',
          ownerName: '上海测试供应商',
          accountName: '上海测试供应商',
          accountNoMasked: '6222 **** 8888',
          bankName: '中国工商银行',
          secondaryLabel: '中国工商银行 / 6222 **** 8888'
        }]
      })

    const { wrapper, model } = mountEditor({ paymentCompany: 'COMPANY-001', counterparty: 'VEN-001', payeeAccount: '' }, [
      createBusinessBlock('paymentCompany', '付款公司', 'payment-company'),
      createBusinessBlock('counterparty', '收款单位', 'counterparty'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
    ])

    await flushPromises()
    expect(mocks.expenseCreateApi.listPayeeAccountOptions).toHaveBeenCalledWith({
      keyword: '',
      linkageMode: 'ENTERPRISE',
      payeeName: undefined,
      counterpartyCode: 'VEN-001',
      paymentCompanyId: 'COMPANY-001'
    })

    const vm = wrapper.findComponent(ExpenseRuntimeFormEditor).vm as unknown as {
      openVendorAccountDialog: () => Promise<void> | void
    }
    await vm.openVendorAccountDialog()
    await flushPromises()
    expect(mocks.expenseCreateApi.getVendorDetail).toHaveBeenCalledWith('COMPANY-001', 'VEN-001')

    const saveButton = wrapper.findAll('button').find((button) => button.text() === '保存收款账户')
    expect(saveButton).toBeTruthy()
    await saveButton!.trigger('click')
    await flushPromises()

    expect(mocks.expenseCreateApi.updateVendor).toHaveBeenCalledWith(
      'COMPANY-001',
      'VEN-001',
      expect.objectContaining({
        cVenName: '上海测试供应商',
        cVenAccount: '6222020000000001'
      })
    )
    expect(mocks.elMessage.success).toHaveBeenCalledWith('供应商收款信息已更新')
    expect(model.value.payeeAccount).toBe('')
    expect(mocks.expenseCreateApi.listPayeeAccountOptions).toHaveBeenCalledTimes(2)
  })

  it('uses the unified vendor payment info failure wording', async () => {
    mocks.expenseCreateApi.updateVendor.mockRejectedValueOnce(new Error(''))

    const { wrapper } = mountEditor({ paymentCompany: 'COMPANY-001', counterparty: 'VEN-001', payeeAccount: '' }, [
      createBusinessBlock('paymentCompany', '付款公司', 'payment-company'),
      createBusinessBlock('counterparty', '收款单位', 'counterparty'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
    ])

    await flushPromises()
    const vm = wrapper.findComponent(ExpenseRuntimeFormEditor).vm as unknown as {
      openVendorAccountDialog: () => Promise<void> | void
    }
    await vm.openVendorAccountDialog()
    await flushPromises()

    const saveButton = wrapper.findAll('button').find((button) => button.text() === '保存收款账户')
    expect(saveButton).toBeTruthy()
    await saveButton!.trigger('click')
    await flushPromises()

    expect(mocks.elMessage.error).toHaveBeenCalledWith('维护供应商收款信息失败')
  })

  it('renders repaired chinese copy for related and writeoff document blocks', async () => {
    const relatedBlock = createBusinessBlock('relatedDocs', '关联单据', 'related-document')
    const writeoffBlock = createBusinessBlock('writeoffDocs', '核销单据', 'writeoff-document')
    const emptyRelatedBlock = createBusinessBlock('emptyRelatedDocs', '关联单据', 'related-document')

    const { wrapper } = mountEditor({
      relatedDocs: [
        {
          documentCode: 'DOC-REL-001',
          documentTitle: '差旅报销单',
          templateType: 'report',
          templateTypeLabel: '报销单',
          statusLabel: '已审批'
        }
      ],
      writeoffDocs: [
        {
          documentCode: 'DOC-WO-001',
          documentTitle: '项目借款单',
          templateType: 'loan',
          templateTypeLabel: '借款单',
          writeOffSourceKind: 'LOAN',
          availableWriteOffAmount: 500,
          writeOffAmount: 120,
          remainingAmount: 380
        }
      ],
      emptyRelatedDocs: []
    }, [
      relatedBlock,
      writeoffBlock,
      emptyRelatedBlock
    ])

    await flushPromises()

    expect(wrapper.text()).toContain('选择单据')
    expect(wrapper.text()).toContain('支持按单据类型分组选择多张已审批通过的业务单据。')
    expect(wrapper.text()).toContain('支持选择借款单或可核销报销单，选中后逐条填写本次核销金额。')
    expect(wrapper.text()).toContain('类型：报销单 / 状态：已审批')
    expect(wrapper.text()).toContain('核销来源')
    expect(wrapper.text()).toContain('可核销余额')
    expect(wrapper.text()).toContain('核销金额')
    expect(wrapper.text()).toContain('暂未选择单据')
    expect(wrapper.text()).toContain('暂无可选单据')
    expect(wrapper.html()).toContain('搜索单据编号、标题或模板名称')
    expect(wrapper.text()).toContain('确认选择')
    expect(wrapper.html()).not.toContain('???')
    expect(wrapper.html()).not.toContain('閫')
    expect(wrapper.html()).not.toContain('鎼滅储')
  })

  it('uses repaired chinese document picker titles for related and writeoff flows', async () => {
    const relatedBlock = createBusinessBlock('relatedDocs', '关联单据', 'related-document')
    const writeoffBlock = createBusinessBlock('writeoffDocs', '核销单据', 'writeoff-document')
    const { wrapper } = mountEditor({ relatedDocs: [], writeoffDocs: [] }, [
      relatedBlock,
      writeoffBlock
    ])

    await flushPromises()

    const vm = wrapper.findComponent(ExpenseRuntimeFormEditor).vm as unknown as {
      openDocumentPicker: (block: ReturnType<typeof createBusinessBlock>) => Promise<void> | void
      documentPickerTitle: string
    }

    await vm.openDocumentPicker(relatedBlock)
    await flushPromises()
    expect(vm.documentPickerTitle).toBe('选择关联单据')

    await vm.openDocumentPicker(writeoffBlock)
    await flushPromises()
    expect(vm.documentPickerTitle).toBe('选择核销单据')
    expect(mocks.expenseApi.getDocumentPicker).toHaveBeenLastCalledWith(expect.objectContaining({
      relationType: 'WRITEOFF'
    }))
  })

  it('applies the unified runtime control class to representative fill controls', async () => {
    const { wrapper } = mountEditor({
      summary: '',
      count: undefined,
      amount: '',
      happenedAt: '',
      paymentCompany: '',
      counterparty: '',
      payeeAccount: ''
    }, [
      createControlBlock('summary', '摘要', 'TEXT', { placeholder: '请输入摘要' }),
      createControlBlock('count', '数量', 'NUMBER'),
      createControlBlock('amount', '金额', 'AMOUNT'),
      createControlBlock('happenedAt', '发生日期', 'DATE'),
      createBusinessBlock('paymentCompany', '付款公司', 'payment-company'),
      createBusinessBlock('counterparty', '收款单位', 'counterparty'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
    ])

    await flushPromises()

    expect(wrapper.get('input[placeholder="请输入摘要"]').classes()).toContain('expense-runtime-control')
    expect(wrapper.get('input[type="number"]').classes()).toContain('expense-runtime-control')
    expect(wrapper.get('[data-testid="date-picker"]').classes()).toContain('expense-runtime-control')
    expect(wrapper.get('[data-testid="counterparty-select-counterparty"]').classes()).toContain('expense-runtime-control')
    expect(wrapper.get('[data-testid="payee-account-select-payeeAccount"]').classes()).toContain('expense-runtime-control')
    expect(wrapper.findAll('.expense-runtime-control').length).toBeGreaterThanOrEqual(7)
  })

  it('renders repaired chinese placeholders for common controls', async () => {
    const { wrapper } = mountEditor({ happenedAt: '', dateRange: [], enabledFlag: false, invoiceAttachments: [] }, [
      createControlBlock('happenedAt', '发生日期', 'DATE'),
      createControlBlock('dateRange', '期间', 'DATE_RANGE'),
      createControlBlock('enabledFlag', '是否开启', 'SWITCH'),
      createControlBlock('invoiceAttachments', '发票附件', 'ATTACHMENT', { maxCount: 3, maxSizeMb: 1 })
    ])

    await flushPromises()

    const datePickers = wrapper.findAll('[data-testid="date-picker"]')
    expect(datePickers[0].attributes('data-placeholder')).toBe('请选择日期')
    expect(datePickers[1].attributes('data-range-separator')).toBe('至')
    expect(datePickers[1].attributes('data-start-placeholder')).toBe('开始日期')
    expect(datePickers[1].attributes('data-end-placeholder')).toBe('结束日期')
    expect(wrapper.text()).toContain('选择文件')
    expect(wrapper.text()).toContain('最多 3 个文件，单个不超过 1 MB')
  })

  it('uses all-files selection for generic attachments and defaults the limit to 30', async () => {
    const { wrapper } = mountEditor({ attachments: [] }, [
      createControlBlock('attachments', '附件', 'ATTACHMENT', { maxCount: 30, maxSizeMb: 10, accept: '' })
    ])

    await flushPromises()

    const upload = wrapper.get('[data-testid="upload"]')
    expect(upload.attributes('accept')).toBeUndefined()
    expect(wrapper.text()).toContain('30')
  })

  it('limits invoice attachments to pdf png jpg jpeg and defaults the limit to 30', async () => {
    const { wrapper } = mountEditor({ invoiceAttachments: [] }, [
      createControlBlock('invoiceAttachments', '发票附件', 'ATTACHMENT', {
        maxCount: 30,
        maxSizeMb: 10,
        accept: '.pdf,.png,.jpg,.jpeg'
      })
    ])

    await flushPromises()

    const upload = wrapper.get('[data-testid="upload"]')
    expect(upload.attributes('accept')).toBe('.pdf,.png,.jpg,.jpeg')
    expect(wrapper.text()).toContain('30')
  })

  it('rejects unsupported invoice attachment files before uploading', async () => {
    const { wrapper } = mountEditor({ invoiceAttachments: [] }, [
      createControlBlock('invoiceAttachments', '发票附件', 'ATTACHMENT', {
        maxCount: 30,
        maxSizeMb: 10,
        accept: '.pdf,.png,.jpg,.jpeg'
      })
    ])

    await flushPromises()

    wrapper.findComponent('[data-testid="upload"]').vm.$emit('change', {
      raw: new File(['bad'], 'invoice.docx', {
        type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
      })
    })
    await flushPromises()

    expect(mocks.expenseCreateApi.uploadAttachment).not.toHaveBeenCalled()
    expect(mocks.elMessage.warning).toHaveBeenCalledWith('发票附件仅支持 PDF、PNG、JPG、JPEG 文件')
  })

  it('accepts jpeg invoice attachments, uploads them, and merges OCR snapshot', async () => {
    const { wrapper, model } = mountEditor({ invoiceAttachments: [] }, [
      createControlBlock('invoiceAttachments', '发票附件', 'ATTACHMENT', {
        maxCount: 30,
        maxSizeMb: 10,
        accept: '.pdf,.png,.jpg,.jpeg'
      })
    ])

    await flushPromises()

    wrapper.findComponent('[data-testid="upload"]').vm.$emit('change', {
      raw: new File(['ok'], 'invoice.jpeg', { type: 'image/jpeg' })
    })
    await flushPromises()

    expect(mocks.expenseCreateApi.uploadAttachment).toHaveBeenCalledTimes(1)
    expect(mocks.expenseCreateApi.recognizeAttachmentOcr).toHaveBeenCalledWith('ATT-001')
    expect((model.value.invoiceAttachments as Array<Record<string, unknown>>)[0]?.ocr).toMatchObject({
      status: 'SUCCESS',
      providerCode: 'ALIYUN',
      invoiceCode: '1234567890'
    })
  })

  it('uses the unified account and outlet wording when validating vendor drafts', async () => {
    const { wrapper } = mountEditor({ paymentCompany: 'COMPANY-001', counterparty: '', payeeAccount: '' }, [
      createBusinessBlock('paymentCompany', '付款公司', 'payment-company'),
      createBusinessBlock('counterparty', '收款单位', 'counterparty'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
    ])

    await flushPromises()

    const vm = wrapper.findComponent(ExpenseRuntimeFormEditor).vm as unknown as {
      vendorDraft: Record<string, unknown>
      validateVendorDraft: () => string
    }

    Object.assign(vm.vendorDraft, {
      cVenName: '测试供应商',
      receiptAccountName: 'A'.repeat(129),
      cVenBankCode: 'ICBC',
      cVenAccount: '622200001',
      cVenBank: '中国工商银行',
      receiptBankProvince: '上海市',
      receiptBankCity: '上海市',
      receiptBranchCode: 'ICBC-SH-001',
      receiptBranchName: '上海营业部'
    })
    expect(vm.validateVendorDraft()).toBe('账户名最多 128 个字符')

    Object.assign(vm.vendorDraft, {
      receiptAccountName: '测试账户',
      receiptBranchName: ''
    })
    expect(vm.validateVendorDraft()).toBe('请选择开户银行、开户省、开户市与开户网点后再保存')
  })
})
