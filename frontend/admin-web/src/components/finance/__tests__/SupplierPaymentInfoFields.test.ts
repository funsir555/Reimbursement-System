import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, reactive } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import SupplierPaymentInfoFields from '@/components/finance/SupplierPaymentInfoFields.vue'

const mocks = vi.hoisted(() => ({
  financeBankApi: {
    listBanks: vi.fn(),
    listBankProvinces: vi.fn(),
    listBankCities: vi.fn(),
    listBankBranches: vi.fn(),
    lookupBranchByCnaps: vi.fn()
  },
  elMessage: {
    error: vi.fn()
  }
}))

vi.mock('@/api', () => ({
  financeBankApi: mocks.financeBankApi
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage
}))

const SimpleContainer = defineComponent({
  template: '<div><slot /></div>'
})

const InputStub = defineComponent({
  props: {
    modelValue: {
      type: String,
      default: ''
    }
  },
  emits: ['update:modelValue'],
  template:
    '<input v-bind="$attrs" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
})

const SelectStub = defineComponent({
  props: {
    modelValue: {
      type: String,
      default: ''
    }
  },
  emits: ['update:modelValue', 'change'],
  template: '<div><slot /></div>'
})

function buildBranch() {
  return {
    id: 1,
    bankCode: 'CMB',
    bankName: '招商银行',
    province: '广东省',
    city: '深圳市',
    branchCode: 'CMB-SZ-FH',
    branchName: '招商银行深圳福华支行',
    cnapsCode: '308584000013',
    value: 'CMB-SZ-FH',
    label: '招商银行深圳福华支行 (广东省 / 深圳市)'
  }
}

describe('SupplierPaymentInfoFields', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.financeBankApi.listBanks.mockResolvedValue({
      data: [
        { bankCode: 'CMB', bankName: '招商银行', businessScope: 'PUBLIC', value: 'CMB', label: '招商银行' }
      ]
    })
    mocks.financeBankApi.listBankProvinces.mockResolvedValue({ data: ['广东省'] })
    mocks.financeBankApi.listBankCities.mockResolvedValue({ data: ['深圳市'] })
    mocks.financeBankApi.listBankBranches.mockResolvedValue({ data: [buildBranch()] })
    mocks.financeBankApi.lookupBranchByCnaps.mockResolvedValue({ data: buildBranch() })
  })

  it('loads banks with the configured business scope and keeps cnaps read-only', async () => {
    const formState = reactive<Record<string, unknown>>({})
    const wrapper = mount(SupplierPaymentInfoFields, {
      props: {
        formState,
        businessScope: 'PUBLIC'
      },
      global: {
        stubs: {
          'el-form-item': SimpleContainer,
          'el-input': InputStub,
          'el-select': SelectStub,
          'el-option': true,
          'el-alert': SimpleContainer
        }
      }
    })
    await flushPromises()

    expect(mocks.financeBankApi.listBanks).toHaveBeenCalledWith({
      keyword: undefined,
      businessScope: 'PUBLIC'
    })

    const inputs = wrapper.findAll('input')
    expect(inputs[1]?.attributes('disabled')).toBeDefined()
    expect(inputs[1]?.attributes('readonly')).toBeDefined()
  })

  it('writes selected branch hierarchy back into the mapped form state', async () => {
    const formState = reactive<Record<string, unknown>>({})
    const wrapper = mount(SupplierPaymentInfoFields, {
      props: {
        formState,
        businessScope: 'PUBLIC'
      },
      global: {
        stubs: {
          'el-form-item': SimpleContainer,
          'el-input': InputStub,
          'el-select': SelectStub,
          'el-option': true,
          'el-alert': SimpleContainer
        }
      }
    })
    await flushPromises()

    const selects = wrapper.findAllComponents(SelectStub)
    await selects[0]!.vm.$emit('update:modelValue', 'CMB')
    await selects[0]!.vm.$emit('change', 'CMB')
    await flushPromises()
    await selects[1]!.vm.$emit('update:modelValue', '广东省')
    await selects[1]!.vm.$emit('change', '广东省')
    await flushPromises()
    await selects[2]!.vm.$emit('update:modelValue', '深圳市')
    await selects[2]!.vm.$emit('change', '深圳市')
    await flushPromises()
    await selects[3]!.vm.$emit('update:modelValue', 'CMB-SZ-FH')
    await selects[3]!.vm.$emit('change', 'CMB-SZ-FH')
    await flushPromises()

    expect(mocks.financeBankApi.listBankProvinces).toHaveBeenCalledWith({
      bankCode: 'CMB',
      businessScope: 'PUBLIC'
    })
    expect(formState.cVenBankCode).toBe('CMB')
    expect(formState.cVenBank).toBe('招商银行')
    expect(formState.receiptBankProvince).toBe('广东省')
    expect(formState.receiptBankCity).toBe('深圳市')
    expect(formState.receiptBranchCode).toBe('CMB-SZ-FH')
    expect(formState.receiptBranchName).toBe('招商银行深圳福华支行')
    expect(formState.cVenBankNub).toBe('308584000013')
  })
})
