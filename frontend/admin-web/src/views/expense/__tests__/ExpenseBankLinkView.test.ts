import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ExpenseBankLinkView from '@/views/expense/ExpenseBankLinkView.vue'

const mocks = vi.hoisted(() => ({
  expensePaymentApi: {
    listBankLinks: vi.fn(),
    getBankLink: vi.fn(),
    updateBankLink: vi.fn()
  },
  elMessage: {
    success: vi.fn(),
    error: vi.fn()
  }
}))

vi.mock('@/api', () => ({
  expensePaymentApi: mocks.expensePaymentApi
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage
}))

vi.mock('@/utils/permissions', () => ({
  hasPermission: () => true,
  readStoredUser: () => ({
    permissionCodes: ['expense:payment:bank_link:view', 'expense:payment:bank_link:edit']
  })
}))

const SimpleContainer = defineComponent({
  template: '<div><slot name="header" /><slot /><slot name="footer" /></div>'
})

const ButtonStub = defineComponent({
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
  template: '<button type="button" :disabled="disabled || loading" @click="$emit(\'click\', $event)"><slot /></button>'
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

const SwitchStub = defineComponent({
  props: {
    modelValue: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue'],
  template: '<input type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)" />'
})

function buildSummary() {
  return {
    companyBankAccountId: 1,
    companyId: 'COMP-001',
    companyName: '华南公司',
    accountName: '华南公司基本户',
    accountNo: '****3344',
    bankName: '招商银行',
    directConnectEnabled: true,
    directConnectProvider: 'CMB',
    directConnectChannel: 'CMB_CLOUD',
    directConnectStatusLabel: '已启用',
    lastDirectConnectStatus: '最近推送成功',
    lastReceiptStatus: '已获取回单'
  }
}

function buildConfig() {
  return {
    companyBankAccountId: 1,
    companyId: 'COMP-001',
    companyName: '华南公司',
    accountName: '华南公司基本户',
    accountNo: '6225880011223344',
    bankName: '招商银行',
    directConnectEnabled: true,
    directConnectProvider: 'CMB',
    directConnectChannel: 'CMB_CLOUD',
    directConnectProtocol: 'CMB_CLOUD_V1',
    directConnectCustomerNo: 'CUST-001',
    directConnectAppId: 'APP-001',
    directConnectAccountAlias: '主付款账户',
    directConnectAuthMode: 'RSA2',
    directConnectApiBaseUrl: 'https://cmb.example.com',
    directConnectCertRef: 'CERT-001',
    directConnectSecretRef: 'SECRET-REF-001',
    directConnectSignType: 'RSA2',
    directConnectEncryptType: 'AES256',
    operatorKey: 'OP-001',
    callbackSecret: 'CALLBACK-001',
    publicKeyRef: 'PUB-001',
    receiptQueryEnabled: true,
    lastDirectConnectStatus: '最近推送成功',
    lastDirectConnectError: ''
  }
}

async function mountView() {
  const wrapper = mount(ExpenseBankLinkView, {
    global: {
      stubs: {
        'el-card': SimpleContainer,
        'el-tag': SimpleContainer,
        'el-empty': SimpleContainer,
        'el-button': ButtonStub,
        'el-form': SimpleContainer,
        'el-form-item': SimpleContainer,
        'el-input': InputStub,
        'el-switch': SwitchStub,
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

describe('ExpenseBankLinkView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.expensePaymentApi.listBankLinks.mockResolvedValue({ data: [buildSummary()] })
    mocks.expensePaymentApi.getBankLink.mockResolvedValue({ data: buildConfig() })
    mocks.expensePaymentApi.updateBankLink.mockResolvedValue({ data: buildConfig() })
  })

  it('renders the bank-link list and the Chinese CMB cloud config form', async () => {
    const wrapper = await mountView()

    expect(mocks.expensePaymentApi.listBankLinks).toHaveBeenCalled()
    expect(mocks.expensePaymentApi.getBankLink).toHaveBeenCalledWith(1)
    expect(wrapper.text()).toContain('直连列表')
    expect(wrapper.text()).toContain('招商银行云直连配置')
    expect(wrapper.text()).toContain('华南公司')
    expect(wrapper.text()).toContain('华南公司基本户')
    expect(wrapper.text()).toContain('保存配置')
  })

  it('submits the selected account configuration back to the bank-link API', async () => {
    const wrapper = await mountView()
    const saveButton = wrapper.findAll('button').find((item) => item.text() === '保存配置')

    expect(saveButton).toBeTruthy()

    await saveButton!.trigger('click')
    await flushPromises()

    expect(mocks.expensePaymentApi.updateBankLink).toHaveBeenCalledWith(1, expect.objectContaining({
      enabled: true,
      directConnectProvider: 'CMB',
      directConnectChannel: 'CMB_CLOUD',
      operatorKey: 'OP-001',
      callbackSecret: 'CALLBACK-001',
      receiptQueryEnabled: true
    }))
    expect(mocks.elMessage.success).toHaveBeenCalledWith('银企直连配置已保存')
  })
})
