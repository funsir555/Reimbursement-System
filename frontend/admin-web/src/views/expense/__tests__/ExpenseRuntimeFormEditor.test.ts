import { flushPromises, mount } from '@vue/test-utils'
import { computed, defineComponent, h, inject, provide, ref } from 'vue'
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
  profileApi: {
    createBankAccount: vi.fn(),
    updateBankAccount: vi.fn()
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
    expenseCreateApi: mocks.expenseCreateApi,
    profileApi: mocks.profileApi
  }
})

vi.mock('element-plus', async () => ({
  ElMessage: mocks.elMessage,
  ElForm: {
    name: 'ElForm',
    template: '<div><slot /><slot name="footer" /></div>'
  },
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
    emits: ['update:modelValue', 'change', 'visible-change'],
    template: '<div v-bind="$attrs"><slot /><slot name="empty" /><slot name="footer" /></div>'
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
  },
  ElTabs: defineComponent({
    name: 'ElTabs',
    props: {
      modelValue: {
        type: [String, Number],
        default: ''
      }
    },
    emits: ['update:modelValue'],
    setup(props, { emit, slots }) {
      const activeName = computed(() => String(props.modelValue ?? ''))
      provide('codex-el-tabs-active-name', activeName)
      provide('codex-el-tabs-set-active-name', (name: string) => emit('update:modelValue', name))
      return () => h('div', { class: 'el-tabs', 'data-testid': 'document-picker-tabs' }, slots.default?.())
    }
  }),
  ElTabPane: defineComponent({
    name: 'ElTabPane',
    props: {
      label: {
        type: String,
        default: ''
      },
      name: {
        type: String,
        default: ''
      }
    },
    setup(props, { slots }) {
      const activeName = inject<{ value: string }>('codex-el-tabs-active-name', computed(() => ''))
      const setActiveName = inject<((name: string) => void) | undefined>('codex-el-tabs-set-active-name', undefined)
      return () => h('div', { class: 'el-tab-pane' }, [
        h('button', {
          type: 'button',
          'data-testid': `document-picker-tab-${props.name}`,
          'data-active': String(activeName.value === props.name),
          onClick: () => setActiveName?.(String(props.name))
        }, props.label),
        activeName.value === props.name
          ? h('div', { 'data-testid': `document-picker-pane-${props.name}` }, slots.default?.())
          : null
      ])
    }
  })
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

function createDocumentPickerGroup(
  templateType: string,
  templateTypeLabel: string,
  documentCode: string,
  documentTitle = `${templateTypeLabel}示例单据`
) {
  return {
    templateType,
    templateTypeLabel,
    total: 1,
    page: 1,
    pageSize: 10,
    items: [
      {
        documentCode,
        documentTitle,
        templateType,
        templateTypeLabel,
        status: 'COMPLETED',
        statusLabel: '已完成',
        totalAmount: '88.00',
        availableWriteOffAmount: '88.00',
        writeOffSourceKind: templateType === 'loan' ? 'LOAN' : 'REPORT'
      }
    ]
  }
}

function mountEditor(initialModelValue: Record<string, unknown>, blocks: unknown[], extraProps: Record<string, unknown> = {}) {
  const model = ref<Record<string, unknown>>({ ...initialModelValue })
  const hydratingForm = ref(false)
  const hydrationVersion = ref(0)
  const Host = defineComponent({
    setup() {
      return () => h(ExpenseRuntimeFormEditor, {
        modelValue: model.value,
        'onUpdate:modelValue': (next: Record<string, unknown>) => {
          model.value = next
        },
        hydratingForm: hydratingForm.value,
        hydrationVersion: hydrationVersion.value,
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
      hydratingForm.value = true
      model.value = { ...next }
      hydrationVersion.value += 1
      Promise.resolve().then(() => {
        hydratingForm.value = false
      })
    },
    hydrateInPlace(next: Record<string, unknown>) {
      hydratingForm.value = true
      Object.keys(model.value).forEach((key) => {
        delete model.value[key]
      })
      Object.assign(model.value, next)
      hydrationVersion.value += 1
      Promise.resolve().then(() => {
        hydratingForm.value = false
      })
    }
  }
}

async function triggerDocumentPickerOpen(wrapper: ReturnType<typeof mount>, fieldKey: string) {
  const trigger = wrapper.get(`[data-testid="open-document-picker-${fieldKey}"]`)
  await trigger.trigger('mousedown')
  await trigger.trigger('click')
}

function createManagedSelectDouble() {
  const input = document.createElement('input')
  const root = document.createElement('div')
  root.appendChild(input)
  return {
    handleClose: vi.fn(),
    handleQueryChange: vi.fn(),
    toggleMenu: vi.fn(),
    blur: vi.fn(),
    expanded: true,
    query: '供应商1',
    previousQuery: '供应商1',
    states: { inputValue: '供应商1' },
    inputRef: {
      blur: vi.fn(),
      input
    },
    $el: root
  }
}

function validPersonalBankForm() {
  return {
    accountName: '张三',
    accountNo: '6222020202020202',
    accountType: '对私账户',
    bankCode: 'BOC',
    bankName: '中国银行',
    province: '上海市',
    city: '上海市',
    branchCode: 'BOC-SH',
    branchName: '中国银行上海分行',
    defaultAccount: 0,
    status: 1
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
    mocks.profileApi.createBankAccount.mockResolvedValue({
      data: {
        id: 101,
        accountName: '张三',
        accountNo: '6222020202020202',
        accountNoMasked: '6222 **** 0202',
        accountType: '对私账户',
        bankName: '中国银行',
        bankCode: 'BOC',
        province: '上海市',
        city: '上海市',
        branchCode: 'BOC-SH',
        branchName: '中国银行上海分行',
        defaultAccount: false,
        status: 1,
        statusLabel: '启用'
      }
    })
    mocks.profileApi.updateBankAccount.mockResolvedValue({
      data: {
        id: 101
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
    const { model } = mountEditor({
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

    model.value.paymentCompany = 'COMPANY-002'
    await flushPromises()

    expect(model.value.counterparty).toBe('')
    expect(model.value.payeeAccount).toBe('')
    expect(mocks.expenseCreateApi.listVendorOptions).toHaveBeenLastCalledWith({
      keyword: undefined,
      paymentCompanyId: 'COMPANY-002'
    })
  })

  it('keeps counterparty and payee account when the parent restores the same draft selections', async () => {
    const initialPayeeAccount = { value: 'VENDOR_ACCOUNT:1', label: '默认账户', ownerName: '广州供应商' }
    const { model, hydrateInPlace } = mountEditor({
      paymentCompany: '',
      counterparty: '',
      payeeAccount: ''
    }, [
      createBusinessBlock('paymentCompany', '付款公司', 'payment-company'),
      createBusinessBlock('counterparty', '收款单位', 'counterparty'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
    ])

    await flushPromises()
    mocks.expenseCreateApi.listVendorOptions.mockClear()
    mocks.expenseCreateApi.listPayeeAccountOptions.mockClear()

    hydrateInPlace({
      paymentCompany: 'COMPANY-001',
      counterparty: 'VEN-001',
      payeeAccount: initialPayeeAccount
    })
    await flushPromises()

    expect(model.value.paymentCompany).toBe('COMPANY-001')
    expect(model.value.counterparty).toBe('VEN-001')
    expect(model.value.payeeAccount).toEqual(initialPayeeAccount)
    expect(mocks.expenseCreateApi.listVendorOptions).toHaveBeenCalledWith({
      keyword: undefined,
      paymentCompanyId: 'COMPANY-001'
    })
    expect(mocks.expenseCreateApi.listPayeeAccountOptions).toHaveBeenCalledWith({
      keyword: '',
      linkageMode: 'ENTERPRISE',
      payeeName: undefined,
      counterpartyCode: 'VEN-001',
      paymentCompanyId: 'COMPANY-001'
    })
  })

  it('does not clear counterparty and payee account when the parent hydrates the same object in place', async () => {
    const initialPayeeAccount = {
      value: 'VENDOR_ACCOUNT:9',
      label: '结算账户',
      ownerName: '上海供应商'
    }
    const { model, hydrateInPlace } = mountEditor({
      paymentCompany: 'COMPANY-001',
      counterparty: 'VEN-001',
      payeeAccount: initialPayeeAccount
    }, [
      createBusinessBlock('paymentCompany', '付款公司', 'payment-company'),
      createBusinessBlock('counterparty', '收款单位', 'counterparty'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
    ])

    await flushPromises()
    mocks.expenseCreateApi.listVendorOptions.mockClear()
    mocks.expenseCreateApi.listPayeeAccountOptions.mockClear()

    hydrateInPlace({
      paymentCompany: 'COMPANY-001',
      counterparty: 'VEN-001',
      payeeAccount: initialPayeeAccount
    })
    await flushPromises()

    expect(model.value.counterparty).toBe('VEN-001')
    expect(model.value.payeeAccount).toEqual(initialPayeeAccount)
    expect(mocks.expenseCreateApi.listVendorOptions).toHaveBeenCalledWith({
      keyword: undefined,
      paymentCompanyId: 'COMPANY-001'
    })
    expect(mocks.expenseCreateApi.listPayeeAccountOptions).toHaveBeenCalledWith({
      keyword: '',
      linkageMode: 'ENTERPRISE',
      payeeName: undefined,
      counterpartyCode: 'VEN-001',
      paymentCompanyId: 'COMPANY-001'
    })
  })

  it('keeps selected counterparty and payee account visible even before remote options reload them', async () => {
    mocks.expenseCreateApi.listVendorOptions.mockResolvedValue({ data: [] })
    mocks.expenseCreateApi.listPayeeAccountOptions.mockResolvedValue({ data: [] })

    const { wrapper } = mountEditor({
      paymentCompany: 'COMPANY-001',
      counterparty: 'VEN-001',
      payeeAccount: {
        value: 'VENDOR_ACCOUNT:1',
        label: '供应商默认账户',
        ownerName: '广州供应商',
        accountNoMasked: '****0001'
      }
    }, [
      createBusinessBlock('paymentCompany', '付款公司', 'payment-company'),
      createBusinessBlock('counterparty', '收款单位', 'counterparty'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
    ])

    await flushPromises()

    expect(wrapper.text()).toContain('VEN-001')
    expect(wrapper.text()).toContain('供应商默认账户')
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

  it('shows the add personal payee entry when no personal payee is maintained', async () => {
    const { wrapper } = mountEditor({ payee: '', payeeAccount: '' }, [
      createBusinessBlock('payee', '收款人', 'payee'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
    ])

    await flushPromises()

    const payeeSelect = wrapper.findAllComponents({ name: 'ElSelect' })
      .find((component) => component.attributes('data-testid') === 'payee-select-payee')
    expect(payeeSelect).toBeTruthy()

    payeeSelect!.vm.$emit('visible-change', true)
    await flushPromises()

    expect(wrapper.get('[data-testid="payee-create-personal-payee"]').text()).toContain('增加收款人')
  })

  it('creates a personal payee in place and refreshes the employee payee chain', async () => {
    mocks.expenseCreateApi.listPayeeOptions
      .mockResolvedValueOnce({ data: [] })
      .mockResolvedValueOnce({
        data: [{
          value: 'PERSONAL_PAYEE:张三',
          label: '张三',
          sourceType: 'PERSONAL_PRIVATE_PAYEE',
          sourceCode: '张三',
          secondaryLabel: '个人对私账户'
        }]
      })

    const { wrapper, model } = mountEditor({ payee: '', payeeAccount: '' }, [
      createBusinessBlock('payee', '收款人', 'payee'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
    ])

    await flushPromises()

    const payeeSelect = wrapper.findAllComponents({ name: 'ElSelect' })
      .find((component) => component.attributes('data-testid') === 'payee-select-payee')
    expect(payeeSelect).toBeTruthy()

    payeeSelect!.vm.$emit('visible-change', true)
    await flushPromises()
    await wrapper.get('[data-testid="payee-create-personal-payee"]').trigger('click')
    await flushPromises()

    const dialog = wrapper.findComponent({ name: 'PersonalBankAccountDialog' })
    expect(dialog.exists()).toBe(true)
    Object.assign((dialog.vm as any).bankForm, validPersonalBankForm())

    await (dialog.vm as any).submitBankAccount()
    await flushPromises()
    await flushPromises()

    expect(mocks.profileApi.createBankAccount).toHaveBeenCalledWith(expect.objectContaining({
      accountName: '张三',
      branchName: '中国银行上海分行'
    }))
    expect(model.value.payee).toEqual(expect.objectContaining({
      value: 'PERSONAL_PAYEE:张三',
      label: '张三'
    }))
    expect(model.value.payeeAccount).toBe('')
    expect(mocks.expenseCreateApi.listPayeeAccountOptions).toHaveBeenLastCalledWith({
      keyword: '',
      linkageMode: 'EMPLOYEE',
      payeeName: '张三',
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

  it('warns and shows the add bank account entry when the selected supplier has no bank info', async () => {
    const { wrapper } = mountEditor({ paymentCompany: 'COMPANY-001', counterparty: 'VEN-001', payeeAccount: '' }, [
      createBusinessBlock('paymentCompany', '付款公司', 'payment-company'),
      createBusinessBlock('counterparty', '收款单位', 'counterparty'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
    ])

    await flushPromises()

    const payeeAccountSelect = wrapper.findAllComponents({ name: 'ElSelect' })
      .find((component) => component.attributes('data-testid') === 'payee-account-select-payeeAccount')
    expect(payeeAccountSelect).toBeTruthy()

    payeeAccountSelect!.vm.$emit('visible-change', true)
    await flushPromises()

    expect(mocks.elMessage.warning).toHaveBeenCalledWith('未维护银行信息，请维护银行信息')
    expect(wrapper.get('[data-testid="payee-account-maintain-vendor"]').text()).toContain('新增银行账户')
  })

  it('warns after payee account loading finishes with no bank info while the dropdown stays open', async () => {
    const resolvePayeeAccountsList: Array<(value: { data: [] }) => void> = []
    mocks.expenseCreateApi.listPayeeAccountOptions.mockImplementation(() => (
      new Promise<{ data: [] }>((resolve) => {
        resolvePayeeAccountsList.push(resolve)
      })
    ))

    const { wrapper } = mountEditor({ paymentCompany: 'COMPANY-001', counterparty: 'VEN-001', payeeAccount: '' }, [
      createBusinessBlock('paymentCompany', '付款公司', 'payment-company'),
      createBusinessBlock('counterparty', '收款单位', 'counterparty'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
    ])

    const payeeAccountSelect = wrapper.findAllComponents({ name: 'ElSelect' })
      .find((component) => component.attributes('data-testid') === 'payee-account-select-payeeAccount')
    expect(payeeAccountSelect).toBeTruthy()

    payeeAccountSelect!.vm.$emit('visible-change', true)
    await flushPromises()

    expect(mocks.elMessage.warning).not.toHaveBeenCalledWith('未维护银行信息，请维护银行信息')

    resolvePayeeAccountsList.forEach((resolve) => resolve({ data: [] }))
    await flushPromises()
    await flushPromises()

    expect(mocks.elMessage.warning).toHaveBeenCalledTimes(1)
    expect(mocks.elMessage.warning).toHaveBeenCalledWith('未维护银行信息，请维护银行信息')
    expect(wrapper.get('[data-testid="payee-account-maintain-vendor"]').text()).toContain('新增银行账户')
  })

  it('does not warn or show the add bank account entry when enterprise supplier accounts already exist', async () => {
    mocks.expenseCreateApi.listPayeeAccountOptions.mockResolvedValue({
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

    const { wrapper } = mountEditor({ paymentCompany: 'COMPANY-001', counterparty: 'VEN-001', payeeAccount: '' }, [
      createBusinessBlock('paymentCompany', '付款公司', 'payment-company'),
      createBusinessBlock('counterparty', '收款单位', 'counterparty'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
    ])

    await flushPromises()

    const payeeAccountSelect = wrapper.findAllComponents({ name: 'ElSelect' })
      .find((component) => component.attributes('data-testid') === 'payee-account-select-payeeAccount')
    expect(payeeAccountSelect).toBeTruthy()

    payeeAccountSelect!.vm.$emit('visible-change', true)
    await flushPromises()

    expect(mocks.elMessage.warning).not.toHaveBeenCalledWith('未维护银行信息，请维护银行信息')
    expect(wrapper.find('[data-testid="payee-account-maintain-vendor"]').exists()).toBe(false)
  })

  it('does not warn or show the add bank account entry for personal payee accounts', async () => {
    const { wrapper } = mountEditor({ payee: '', payeeAccount: '' }, [
      createBusinessBlock('payee', '收款人', 'payee'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
    ])

    await flushPromises()

    const payeeAccountSelect = wrapper.findAllComponents({ name: 'ElSelect' })
      .find((component) => component.attributes('data-testid') === 'payee-account-select-payeeAccount')
    expect(payeeAccountSelect).toBeTruthy()

    payeeAccountSelect!.vm.$emit('visible-change', true)
    await flushPromises()

    expect(mocks.elMessage.warning).not.toHaveBeenCalledWith('未维护银行信息，请维护银行信息')
    expect(wrapper.find('[data-testid="payee-account-maintain-vendor"]').exists()).toBe(false)
  })

  it('shows the add bank account entry and updates the selected supplier account in place', async () => {
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

    const payeeAccountSelect = wrapper.findAllComponents({ name: 'ElSelect' })
      .find((component) => component.attributes('data-testid') === 'payee-account-select-payeeAccount')
    expect(payeeAccountSelect).toBeTruthy()
    payeeAccountSelect!.vm.$emit('visible-change', true)
    await flushPromises()

    expect(wrapper.get('[data-testid="payee-account-maintain-vendor"]').text()).toContain('新增银行账户')
    await wrapper.get('[data-testid="payee-account-maintain-vendor"]').trigger('click')
    await flushPromises()
    expect(mocks.expenseCreateApi.getVendorDetail).toHaveBeenCalledWith('COMPANY-001', 'VEN-001')

    const saveButton = wrapper.findAll('button').find((button) => button.text() === '保存银行账户')
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
    expect(mocks.elMessage.success).toHaveBeenCalledWith('供应商银行信息已更新')
    expect(model.value.payeeAccount).toBe('')
    expect(mocks.expenseCreateApi.listPayeeAccountOptions.mock.calls.length).toBeGreaterThanOrEqual(2)
  })

  it('uses the unified vendor bank info failure wording', async () => {
    mocks.expenseCreateApi.updateVendor.mockRejectedValueOnce(new Error(''))

    const { wrapper } = mountEditor({ paymentCompany: 'COMPANY-001', counterparty: 'VEN-001', payeeAccount: '' }, [
      createBusinessBlock('paymentCompany', '付款公司', 'payment-company'),
      createBusinessBlock('counterparty', '收款单位', 'counterparty'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
    ])

    await flushPromises()
    const payeeAccountSelect = wrapper.findAllComponents({ name: 'ElSelect' })
      .find((component) => component.attributes('data-testid') === 'payee-account-select-payeeAccount')
    expect(payeeAccountSelect).toBeTruthy()
    payeeAccountSelect!.vm.$emit('visible-change', true)
    await flushPromises()
    await wrapper.get('[data-testid="payee-account-maintain-vendor"]').trigger('click')
    await flushPromises()

    const saveButton = wrapper.findAll('button').find((button) => button.text() === '保存银行账户')
    expect(saveButton).toBeTruthy()
    await saveButton!.trigger('click')
    await flushPromises()

    expect(mocks.elMessage.error).toHaveBeenCalledWith('维护供应商银行信息失败')
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
    expect(wrapper.text()).toContain('支持点击页签切换报销单、申请单、合同单与借款单，并同时关联多张已审批通过的单据。')
    expect(wrapper.text()).toContain('支持点击页签切换报销单与借款单，选中后逐条填写本次核销金额。')
    expect(wrapper.text()).toContain('类型：报销单 / 状态：已审批')
    expect(wrapper.text()).toContain('核销来源')
    expect(wrapper.text()).toContain('可核销余额')
    expect(wrapper.text()).toContain('核销金额')
    expect(wrapper.text()).toContain('暂未选择单据')
    expect(wrapper.text()).toContain('暂无可选单据')
    expect(wrapper.html()).toContain('搜索单据编号、标题或模板名称')
    expect(wrapper.text()).toContain('确认选择')
    expect(wrapper.text()).toContain('删除')
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

  it('opens related and writeoff pickers by real button click after counterparty is selected', async () => {
    const relatedBlock = createBusinessBlock('relatedDocs', '关联单据', 'related-document')
    const writeoffBlock = createBusinessBlock('writeoffDocs', '核销单据', 'writeoff-document')
    const { wrapper } = mountEditor({
      paymentCompany: 'COMPANY-001',
      counterparty: 'VEN-001',
      relatedDocs: [],
      writeoffDocs: []
    }, [
      createBusinessBlock('paymentCompany', '付款公司', 'payment-company'),
      createBusinessBlock('counterparty', '收款单位', 'counterparty'),
      relatedBlock,
      writeoffBlock
    ])

    await flushPromises()

    const vm = wrapper.findComponent(ExpenseRuntimeFormEditor).vm as unknown as {
      documentPickerDialog: {
        visible: boolean
        fieldKey: string
        relationType: string
      }
    }

    await triggerDocumentPickerOpen(wrapper, 'relatedDocs')
    await flushPromises()

    expect(vm.documentPickerDialog.visible).toBe(true)
    expect(vm.documentPickerDialog.fieldKey).toBe('relatedDocs')
    expect(vm.documentPickerDialog.relationType).toBe('RELATED')
    expect(mocks.expenseApi.getDocumentPicker).toHaveBeenLastCalledWith(expect.objectContaining({
      relationType: 'RELATED'
    }))

    await triggerDocumentPickerOpen(wrapper, 'writeoffDocs')
    await flushPromises()

    expect(vm.documentPickerDialog.visible).toBe(true)
    expect(vm.documentPickerDialog.fieldKey).toBe('writeoffDocs')
    expect(vm.documentPickerDialog.relationType).toBe('WRITEOFF')
    expect(mocks.expenseApi.getDocumentPicker).toHaveBeenLastCalledWith(expect.objectContaining({
      relationType: 'WRITEOFF'
    }))
  })

  it('keeps document pickers clickable while vendor bank accounts are loading and after they load', async () => {
    const relatedBlock = createBusinessBlock('relatedDocs', '关联单据', 'related-document')
    const writeoffBlock = createBusinessBlock('writeoffDocs', '核销单据', 'writeoff-document')
    let resolvePayeeAccounts: ((value: {
      data: Array<{
        value: string
        label: string
        sourceType: string
        ownerCode: string
        ownerName: string
        accountName: string
        bankName: string
        accountNoMasked: string
        secondaryLabel: string
      }>
    }) => void) | null = null
    const payeeAccountPromise = new Promise<{
      data: Array<{
        value: string
        label: string
        sourceType: string
        ownerCode: string
        ownerName: string
        accountName: string
        bankName: string
        accountNoMasked: string
        secondaryLabel: string
      }>
    }>((resolve) => {
      resolvePayeeAccounts = resolve
    })
    mocks.expenseCreateApi.listPayeeAccountOptions.mockReturnValueOnce(payeeAccountPromise)

    const { wrapper } = mountEditor({
      paymentCompany: 'COMPANY-001',
      counterparty: 'VEN-001',
      payeeAccount: '',
      relatedDocs: [],
      writeoffDocs: []
    }, [
      createBusinessBlock('paymentCompany', '付款公司', 'payment-company'),
      createBusinessBlock('counterparty', '收款单位', 'counterparty'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account'),
      relatedBlock,
      writeoffBlock
    ])

    const vm = wrapper.findComponent(ExpenseRuntimeFormEditor).vm as unknown as {
      documentPickerDialog: {
        visible: boolean
        fieldKey: string
        relationType: string
      }
      closeDocumentPicker: () => void
    }

    await flushPromises()

    await triggerDocumentPickerOpen(wrapper, 'relatedDocs')
    await flushPromises()

    expect(vm.documentPickerDialog.visible).toBe(true)
    expect(vm.documentPickerDialog.fieldKey).toBe('relatedDocs')
    expect(vm.documentPickerDialog.relationType).toBe('RELATED')
    expect(mocks.expenseApi.getDocumentPicker).toHaveBeenLastCalledWith(expect.objectContaining({
      relationType: 'RELATED'
    }))

    vm.closeDocumentPicker()
    await flushPromises()

    resolvePayeeAccounts?.({
      data: [{
        value: 'VENDOR_ACCOUNT:1',
        label: '上海测试供应商',
        sourceType: 'ENTERPRISE_VENDOR',
        ownerCode: 'VEN-001',
        ownerName: '上海测试供应商',
        accountName: '上海测试供应商',
        bankName: '中国工商银行',
        accountNoMasked: '6222 **** 0001',
        secondaryLabel: '中国工商银行 / 6222 **** 0001'
      }]
    })
    await flushPromises()

    await triggerDocumentPickerOpen(wrapper, 'writeoffDocs')
    await flushPromises()

    expect(vm.documentPickerDialog.visible).toBe(true)
    expect(vm.documentPickerDialog.fieldKey).toBe('writeoffDocs')
    expect(vm.documentPickerDialog.relationType).toBe('WRITEOFF')
    expect(mocks.expenseApi.getDocumentPicker).toHaveBeenLastCalledWith(expect.objectContaining({
      relationType: 'WRITEOFF'
    }))
    expect(mocks.expenseCreateApi.listPayeeAccountOptions).toHaveBeenCalled()
  })

  it('still opens the related picker without counterparty instead of blocking on a prerequisite', async () => {
    const relatedBlock = createBusinessBlock('relatedDocs', '关联单据', 'related-document')
    const { wrapper } = mountEditor({
      paymentCompany: 'COMPANY-001',
      counterparty: '',
      relatedDocs: []
    }, [
      createBusinessBlock('paymentCompany', '付款公司', 'payment-company'),
      createBusinessBlock('counterparty', '收款单位', 'counterparty'),
      relatedBlock
    ])

    await flushPromises()

    const vm = wrapper.findComponent(ExpenseRuntimeFormEditor).vm as unknown as {
      documentPickerDialog: {
        visible: boolean
        fieldKey: string
      }
    }

    await triggerDocumentPickerOpen(wrapper, 'relatedDocs')
    await flushPromises()

    expect(vm.documentPickerDialog.visible).toBe(true)
    expect(vm.documentPickerDialog.fieldKey).toBe('relatedDocs')
    expect(mocks.elMessage.warning).not.toHaveBeenCalledWith('请先选择收款单位')
    expect(mocks.expenseApi.getDocumentPicker).toHaveBeenLastCalledWith(expect.objectContaining({
      relationType: 'RELATED'
    }))
  })

  it('still opens the picker after switching counterparty multiple times', async () => {
    const relatedBlock = createBusinessBlock('relatedDocs', '关联单据', 'related-document')
    const { wrapper, setModelValue } = mountEditor({
      paymentCompany: 'COMPANY-001',
      counterparty: 'VEN-001',
      relatedDocs: []
    }, [
      createBusinessBlock('paymentCompany', '付款公司', 'payment-company'),
      createBusinessBlock('counterparty', '收款单位', 'counterparty'),
      relatedBlock
    ])

    await flushPromises()

    setModelValue({
      paymentCompany: 'COMPANY-001',
      counterparty: 'VEN-002',
      relatedDocs: []
    })
    await flushPromises()

    setModelValue({
      paymentCompany: 'COMPANY-001',
      counterparty: 'VEN-003',
      relatedDocs: []
    })
    await flushPromises()

    const vm = wrapper.findComponent(ExpenseRuntimeFormEditor).vm as unknown as {
      documentPickerDialog: {
        visible: boolean
        fieldKey: string
        relationType: string
      }
    }

    await triggerDocumentPickerOpen(wrapper, 'relatedDocs')
    await flushPromises()

    expect(vm.documentPickerDialog.visible).toBe(true)
    expect(vm.documentPickerDialog.fieldKey).toBe('relatedDocs')
    expect(vm.documentPickerDialog.relationType).toBe('RELATED')
    expect(mocks.expenseApi.getDocumentPicker).toHaveBeenLastCalledWith(expect.objectContaining({
      relationType: 'RELATED'
    }))
  })

  it('settles counterparty and payee-account select state immediately after counterparty selection', async () => {
    const { wrapper } = mountEditor({
      paymentCompany: 'COMPANY-001',
      counterparty: '',
      payeeAccount: ''
    }, [
      createBusinessBlock('paymentCompany', '付款公司', 'payment-company'),
      createBusinessBlock('counterparty', '收款单位', 'counterparty'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
    ])

    await flushPromises()

    const vm = wrapper.findComponent(ExpenseRuntimeFormEditor).vm as unknown as {
      setCounterpartySelectRef: (fieldKey: string, instance: unknown) => void
      setPayeeAccountSelectRef: (fieldKey: string, instance: unknown) => void
      handleCounterpartySelection: (fieldKey: string, value: string) => void
    }

    const counterpartySelect = createManagedSelectDouble()
    const payeeAccountSelect = createManagedSelectDouble()

    vm.setCounterpartySelectRef('counterparty', counterpartySelect)
    vm.setPayeeAccountSelectRef('payeeAccount', payeeAccountSelect)
    vm.handleCounterpartySelection('counterparty', 'VEN-001')
    await flushPromises()

    expect(counterpartySelect.handleClose).toHaveBeenCalled()
    expect(counterpartySelect.handleQueryChange).toHaveBeenCalledWith('')
    expect(counterpartySelect.inputRef.blur).toHaveBeenCalled()
    expect(counterpartySelect.blur).toHaveBeenCalled()
    expect(counterpartySelect.query).toBe('')
    expect(counterpartySelect.previousQuery).toBe('')
    expect(counterpartySelect.states.inputValue).toBe('')
    expect(counterpartySelect.inputRef.input.value).toBe('')

    expect(payeeAccountSelect.handleClose).toHaveBeenCalled()
    expect(payeeAccountSelect.handleQueryChange).toHaveBeenCalledWith('')
    expect(payeeAccountSelect.states.inputValue).toBe('')
  })

  it('settles lingering linked selects again after auto-loading payee accounts for a newly selected supplier', async () => {
    const { wrapper, setModelValue } = mountEditor({
      paymentCompany: 'COMPANY-001',
      counterparty: '',
      payeeAccount: ''
    }, [
      createBusinessBlock('paymentCompany', '付款公司', 'payment-company'),
      createBusinessBlock('counterparty', '收款单位', 'counterparty'),
      createBusinessBlock('payeeAccount', '收款账户', 'payee-account')
    ])

    await flushPromises()
    vi.clearAllMocks()

    const vm = wrapper.findComponent(ExpenseRuntimeFormEditor).vm as unknown as {
      setCounterpartySelectRef: (fieldKey: string, instance: unknown) => void
      setPayeeAccountSelectRef: (fieldKey: string, instance: unknown) => void
      handleCounterpartySelection: (fieldKey: string, value: string) => void
    }

    const counterpartySelect = createManagedSelectDouble()
    const payeeAccountSelect = createManagedSelectDouble()

    vm.setCounterpartySelectRef('counterparty', counterpartySelect)
    vm.setPayeeAccountSelectRef('payeeAccount', payeeAccountSelect)

    vm.handleCounterpartySelection('counterparty', 'VEN-001')
    await flushPromises()

    expect(mocks.expenseCreateApi.listPayeeAccountOptions).toHaveBeenCalledWith(expect.objectContaining({
      counterpartyCode: 'VEN-001',
      paymentCompanyId: 'COMPANY-001'
    }))
    expect(counterpartySelect.handleClose).toHaveBeenCalled()
    expect(payeeAccountSelect.handleClose).toHaveBeenCalled()
    expect(counterpartySelect.states.inputValue).toBe('')
    expect(payeeAccountSelect.states.inputValue).toBe('')
  })

  it('renders document picker tabs by relation type and switches visible panels', async () => {
    const relatedBlock = createBusinessBlock('relatedDocs', '关联单据', 'related-document')
    const writeoffBlock = createBusinessBlock('writeoffDocs', '核销单据', 'writeoff-document')
    mocks.expenseApi.getDocumentPicker
      .mockResolvedValueOnce({
        data: {
          relationType: 'RELATED',
          groups: [
            createDocumentPickerGroup('report', '报销单', 'DOC-REL-REPORT-001'),
            createDocumentPickerGroup('application', '申请单', 'DOC-REL-APPLICATION-001'),
            createDocumentPickerGroup('contract', '合同单', 'DOC-REL-CONTRACT-001'),
            createDocumentPickerGroup('loan', '借款单', 'DOC-REL-LOAN-001')
          ]
        }
      })
      .mockResolvedValueOnce({
        data: {
          relationType: 'WRITEOFF',
          groups: [
            createDocumentPickerGroup('report', '报销单', 'DOC-WO-REPORT-001'),
            createDocumentPickerGroup('loan', '借款单', 'DOC-WO-LOAN-001')
          ]
        }
      })

    const { wrapper } = mountEditor({ relatedDocs: [], writeoffDocs: [] }, [
      relatedBlock,
      writeoffBlock
    ])

    await flushPromises()

    const vm = wrapper.findComponent(ExpenseRuntimeFormEditor).vm as unknown as {
      openDocumentPicker: (block: ReturnType<typeof createBusinessBlock>) => Promise<void> | void
    }

    await vm.openDocumentPicker(relatedBlock)
    await flushPromises()

    expect(wrapper.get('[data-testid="document-picker-tab-report"]').text()).toContain('报销单（1）')
    expect(wrapper.get('[data-testid="document-picker-tab-application"]').text()).toContain('申请单（1）')
    expect(wrapper.get('[data-testid="document-picker-tab-contract"]').text()).toContain('合同单（1）')
    expect(wrapper.get('[data-testid="document-picker-tab-loan"]').text()).toContain('借款单（1）')
    expect(wrapper.find('[data-testid="document-picker-panel-report"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="document-picker-panel-loan"]').exists()).toBe(false)

    await wrapper.get('[data-testid="document-picker-tab-loan"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="document-picker-panel-report"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="document-picker-panel-loan"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('DOC-REL-LOAN-001')

    await vm.openDocumentPicker(writeoffBlock)
    await flushPromises()

    expect(wrapper.find('[data-testid="document-picker-tab-report"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="document-picker-tab-loan"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="document-picker-tab-application"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="document-picker-tab-contract"]').exists()).toBe(false)
  })

  it('keeps the active document type when available and falls back to the first tab when it disappears', async () => {
    const relatedBlock = createBusinessBlock('relatedDocs', '关联单据', 'related-document')
    mocks.expenseApi.getDocumentPicker
      .mockResolvedValueOnce({
        data: {
          relationType: 'RELATED',
          groups: [
            createDocumentPickerGroup('report', '报销单', 'DOC-REL-REPORT-001'),
            createDocumentPickerGroup('loan', '借款单', 'DOC-REL-LOAN-001')
          ]
        }
      })
      .mockResolvedValueOnce({
        data: {
          relationType: 'RELATED',
          groups: [
            createDocumentPickerGroup('report', '报销单', 'DOC-REL-REPORT-001'),
            createDocumentPickerGroup('loan', '借款单', 'DOC-REL-LOAN-001')
          ]
        }
      })
      .mockResolvedValueOnce({
        data: {
          relationType: 'RELATED',
          groups: [
            createDocumentPickerGroup('report', '报销单', 'DOC-REL-REPORT-001')
          ]
        }
      })

    const { wrapper } = mountEditor({ relatedDocs: [] }, [relatedBlock])

    await flushPromises()

    const vm = wrapper.findComponent(ExpenseRuntimeFormEditor).vm as unknown as {
      openDocumentPicker: (block: ReturnType<typeof createBusinessBlock>) => Promise<void> | void
      loadDocumentPicker: () => Promise<void>
      documentPickerDialog: {
        activeTemplateType: string
        selectedCodes: string[]
      }
    }

    await vm.openDocumentPicker(relatedBlock)
    await flushPromises()
    await wrapper.get('[data-testid="document-picker-tab-loan"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="toggle-document-picker-DOC-REL-LOAN-001"]').trigger('click')
    await flushPromises()

    expect(vm.documentPickerDialog.activeTemplateType).toBe('loan')
    expect(vm.documentPickerDialog.selectedCodes).toContain('DOC-REL-LOAN-001')

    await vm.loadDocumentPicker()
    await flushPromises()

    expect(vm.documentPickerDialog.activeTemplateType).toBe('loan')
    expect(vm.documentPickerDialog.selectedCodes).toContain('DOC-REL-LOAN-001')

    await vm.loadDocumentPicker()
    await flushPromises()

    expect(vm.documentPickerDialog.activeTemplateType).toBe('report')
    expect(vm.documentPickerDialog.selectedCodes).toContain('DOC-REL-LOAN-001')
  })

  it('round-trips related and writeoff picker selections into form data', async () => {
    const relatedBlock = createBusinessBlock('relatedDocs', '关联单据', 'related-document')
    const writeoffBlock = createBusinessBlock('writeoffDocs', '核销单据', 'writeoff-document')
    mocks.expenseApi.getDocumentPicker
      .mockResolvedValueOnce({
        data: {
          relationType: 'RELATED',
          groups: [
            {
              templateType: 'report',
              templateTypeLabel: '报销单',
              total: 1,
              page: 1,
              pageSize: 10,
              items: [
                {
                  documentCode: 'DOC-REL-001',
                  documentTitle: '差旅报销单',
                  templateType: 'report',
                  templateTypeLabel: '报销单',
                  status: 'COMPLETED',
                  statusLabel: '已完成',
                  totalAmount: '88.00'
                }
              ]
            }
          ]
        }
      })
      .mockResolvedValueOnce({
        data: {
          relationType: 'WRITEOFF',
          groups: [
            {
              templateType: 'loan',
              templateTypeLabel: '借款单',
              total: 1,
              page: 1,
              pageSize: 10,
              items: [
                {
                  documentCode: 'DOC-WO-001',
                  documentTitle: '项目借款单',
                  templateType: 'loan',
                  templateTypeLabel: '借款单',
                  status: 'COMPLETED',
                  statusLabel: '已完成',
                  totalAmount: '500.00',
                  availableWriteOffAmount: '500.00',
                  writeOffSourceKind: 'LOAN'
                }
              ]
            }
          ]
        }
      })

    const { wrapper, model } = mountEditor({ relatedDocs: [], writeoffDocs: [] }, [
      relatedBlock,
      writeoffBlock
    ])

    await flushPromises()

    const vm = wrapper.findComponent(ExpenseRuntimeFormEditor).vm as unknown as {
      openDocumentPicker: (block: ReturnType<typeof createBusinessBlock>) => Promise<void> | void
      updateWriteOffAmount: (block: ReturnType<typeof createBusinessBlock>, documentCode: string, value: string | number) => void
    }

    await vm.openDocumentPicker(relatedBlock)
    await flushPromises()
    await wrapper.get('[data-testid="toggle-document-picker-DOC-REL-001"]').trigger('click')
    await wrapper.get('[data-testid="confirm-document-picker"]').trigger('click')
    await flushPromises()

    expect(model.value.relatedDocs).toEqual([
      expect.objectContaining({
        documentCode: 'DOC-REL-001',
        documentTitle: '差旅报销单',
        templateTypeLabel: '报销单'
      })
    ])

    await vm.openDocumentPicker(writeoffBlock)
    await flushPromises()
    await wrapper.get('[data-testid="toggle-document-picker-DOC-WO-001"]').trigger('click')
    await wrapper.get('[data-testid="confirm-document-picker"]').trigger('click')
    vm.updateWriteOffAmount(writeoffBlock, 'DOC-WO-001', '120')
    await flushPromises()

    const writeoffDocs = model.value.writeoffDocs as Array<Record<string, unknown>>
    expect(writeoffDocs).toHaveLength(1)
    expect(writeoffDocs[0]?.documentCode).toBe('DOC-WO-001')
    expect(writeoffDocs[0]?.writeOffSourceKind).toBe('LOAN')
    expect(writeoffDocs[0]?.writeOffAmount).toBe('120.00')
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

  it('syncs invoiceAmount from OCR total but keeps manual overrides on later attachment changes', async () => {
    mocks.expenseCreateApi.recognizeAttachmentOcr
      .mockResolvedValueOnce({
        data: {
          status: 'SUCCESS',
          totalAmount: '88.80'
        }
      })
      .mockResolvedValueOnce({
        data: {
          status: 'SUCCESS',
          totalAmount: '12.00'
        }
      })

    const { wrapper, model, setModelValue } = mountEditor({ invoiceAttachments: [], invoiceAmount: '' }, [
      createControlBlock('invoiceAmount', '发票金额', 'AMOUNT', {
        systemFieldCode: 'INVOICE_AMOUNT'
      }),
      createControlBlock('invoiceAttachments', '发票附件', 'ATTACHMENT', {
        maxCount: 30,
        maxSizeMb: 10,
        accept: '.pdf,.png,.jpg,.jpeg'
      })
    ])

    await flushPromises()

    wrapper.findComponent('[data-testid="upload"]').vm.$emit('change', {
      raw: new File(['ok'], 'invoice-a.jpeg', { type: 'image/jpeg' })
    })
    await flushPromises()

    expect(model.value.invoiceAmount).toBe('88.80')
    expect(model.value.actualPaymentAmount).toBe('88.80')

    setModelValue({
      ...model.value,
      invoiceAmount: '66.00',
      actualPaymentAmount: '55.00'
    })
    await flushPromises()

    wrapper.findComponent('[data-testid="upload"]').vm.$emit('change', {
      raw: new File(['ok'], 'invoice-b.jpeg', { type: 'image/jpeg' })
    })
    await flushPromises()

    expect(model.value.invoiceAmount).toBe('66.00')
    expect(model.value.actualPaymentAmount).toBe('55.00')
  })

  it('syncs actualPaymentAmount when invoiceAmount is edited in full-payment mode', async () => {
    const { wrapper, model } = mountEditor({
      businessScenario: 'INVOICE_FULL_PAYMENT',
      invoiceAmount: '',
      actualPaymentAmount: ''
    }, [
      createControlBlock('invoiceAmount', '发票金额', 'AMOUNT', {
        systemFieldCode: 'INVOICE_AMOUNT'
      }),
      createControlBlock('actualPaymentAmount', '实际支付金额', 'AMOUNT', {
        systemFieldCode: 'ACTUAL_PAYMENT_AMOUNT'
      })
    ], {
      detailType: 'ENTERPRISE_TRANSACTION',
      defaultBusinessScenario: 'INVOICE_FULL_PAYMENT'
    })

    await flushPromises()

    await wrapper.findAll('input')[0]!.setValue('120.50')
    await flushPromises()

    expect(model.value.invoiceAmount).toBe('120.50')
    expect(model.value.actualPaymentAmount).toBe('120.50')
  })

  it('validateBeforeSubmit blocks prepay details when amount and actualPaymentAmount differ', async () => {
    const { wrapper } = mountEditor({
      businessScenario: 'PREPAY_UNBILLED',
      amount: '88.00',
      actualPaymentAmount: '66.00'
    }, [
      createControlBlock('amount', '金额', 'AMOUNT', {
        systemFieldCode: 'DETAIL_AMOUNT',
        visibleSceneModes: ['PREPAY_UNBILLED']
      }),
      createControlBlock('actualPaymentAmount', '实际支付金额', 'AMOUNT', {
        systemFieldCode: 'ACTUAL_PAYMENT_AMOUNT'
      })
    ], {
      detailType: 'ENTERPRISE_TRANSACTION',
      defaultBusinessScenario: 'PREPAY_UNBILLED'
    })

    await flushPromises()

    const editor = wrapper.findComponent(ExpenseRuntimeFormEditor).vm as unknown as {
      validateBeforeSubmit: () => boolean
    }

    expect(editor.validateBeforeSubmit()).toBe(false)
    expect(mocks.elMessage.warning).toHaveBeenCalledWith('预付未到票场景下，【金额】必须等于【实际支付金额】')
  })

  it('validateBeforeSubmit blocks empty required text fields', async () => {
    const requiredTextBlock = {
      ...createControlBlock('counterpartyName', '收款单位', 'TEXT'),
      required: true
    }
    const { wrapper } = mountEditor({}, [requiredTextBlock])

    await flushPromises()

    const editor = wrapper.findComponent(ExpenseRuntimeFormEditor).vm as unknown as {
      validateBeforeSubmit: () => boolean
    }

    expect(editor.validateBeforeSubmit()).toBe(false)
    expect(mocks.elMessage.warning).toHaveBeenCalledWith('请先填写【收款单位】')
  })

  it('validateBeforeSubmit treats zero amount and false switch as valid required values', async () => {
    const { wrapper } = mountEditor({
      amount: 0,
      confirmed: false
    }, [
      {
        ...createControlBlock('amount', '金额', 'AMOUNT'),
        required: true
      },
      {
        ...createControlBlock('confirmed', '是否确认', 'SWITCH'),
        required: true
      }
    ])

    await flushPromises()

    const editor = wrapper.findComponent(ExpenseRuntimeFormEditor).vm as unknown as {
      validateBeforeSubmit: () => boolean
    }

    expect(editor.validateBeforeSubmit()).toBe(true)
    expect(mocks.elMessage.warning).not.toHaveBeenCalled()
  })

  it('validateBeforeSubmit blocks empty required related document selections', async () => {
    const { wrapper } = mountEditor({}, [
      {
        ...createBusinessBlock('relatedDocs', '关联单据', 'related-document'),
        required: true
      }
    ])

    await flushPromises()

    const editor = wrapper.findComponent(ExpenseRuntimeFormEditor).vm as unknown as {
      validateBeforeSubmit: () => boolean
    }

    expect(editor.validateBeforeSubmit()).toBe(false)
    expect(mocks.elMessage.warning).toHaveBeenCalledWith('请先填写【关联单据】')
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
