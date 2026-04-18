import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import PersonalCenterView from '@/views/profile/PersonalCenterView.vue'

const mocks = vi.hoisted(() => ({
  route: {
    query: { tab: 'bank' }
  },
  router: {
    replace: vi.fn(),
    push: vi.fn()
  },
  profileApi: {
    getOverview: vi.fn(),
    listBankAccounts: vi.fn(),
    createBankAccount: vi.fn(),
    updateBankAccount: vi.fn(),
    updateBankAccountStatus: vi.fn(),
    setDefaultBankAccount: vi.fn(),
    changePassword: vi.fn()
  },
  elMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  }
}))

vi.mock('vue-router', () => ({
  useRoute: () => mocks.route,
  useRouter: () => mocks.router
}))

vi.mock('@/api', async () => {
  const actual = await vi.importActual<typeof import('@/api')>('@/api')
  return {
    ...actual,
    profileApi: mocks.profileApi
  }
})

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessage: mocks.elMessage
  }
})

vi.mock('@/utils/permissions', () => ({
  hasPermission: () => true,
  readStoredUser: () => ({ permissionCodes: ['profile:view', 'profile:password:update'] })
}))

const SimpleContainer = defineComponent({
  template: '<div><slot name="header" /><slot /><slot name="footer" /></div>'
})

const ButtonStub = defineComponent({
  emits: ['click'],
  template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
})

const InputStub = defineComponent({
  props: {
    modelValue: {
      type: [String, Number],
      default: ''
    }
  },
  emits: ['update:modelValue'],
  template:
    '<input v-bind="$attrs" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
})

const SwitchStub = defineComponent({
  props: {
    modelValue: {
      type: [Boolean, Number],
      default: false
    },
    activeValue: {
      type: [Boolean, Number],
      default: true
    },
    inactiveValue: {
      type: [Boolean, Number],
      default: false
    }
  },
  emits: ['update:modelValue'],
  template: `
    <input
      type="checkbox"
      :checked="modelValue === activeValue || modelValue === true"
      @change="$emit('update:modelValue', $event.target.checked ? activeValue : inactiveValue)"
    />
  `
})

const overview = {
  user: {
    userId: 1,
    username: 'zhangsan',
    name: '张三',
    position: '财务',
    laborRelationBelong: '总部',
    phone: '13800138000',
    email: 'zhangsan@example.com'
  },
  bankAccounts: []
}

function validBankForm() {
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

async function mountView() {
  const wrapper = mount(PersonalCenterView, {
    global: {
      stubs: {
        'el-card': SimpleContainer,
        'el-tag': SimpleContainer,
        'el-empty': SimpleContainer,
        'el-dialog': SimpleContainer,
        'el-form': SimpleContainer,
        'el-form-item': SimpleContainer,
        'el-input': InputStub,
        'el-button': ButtonStub,
        'el-switch': SwitchStub,
        SupplierPaymentInfoFields: true
      }
    }
  })
  await flushPromises()
  return wrapper
}

describe('PersonalCenterView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.router.replace.mockResolvedValue(undefined)
    mocks.router.push.mockResolvedValue(undefined)
    mocks.profileApi.getOverview.mockResolvedValue({ data: overview })
    mocks.profileApi.listBankAccounts.mockResolvedValue({ data: [] })
    mocks.profileApi.createBankAccount.mockResolvedValue({ data: { id: 1 } })
    mocks.profileApi.updateBankAccount.mockResolvedValue({ data: { id: 2 } })
    mocks.profileApi.updateBankAccountStatus.mockResolvedValue({ data: true })
    mocks.profileApi.setDefaultBankAccount.mockResolvedValue({ data: true })
    mocks.profileApi.changePassword.mockResolvedValue({ data: true })
  })

  it('shows the unified personal bank account success message when creating', async () => {
    let capturedPayload: Record<string, unknown> | null = null
    mocks.profileApi.createBankAccount.mockImplementationOnce(async (payload: Record<string, unknown>) => {
      capturedPayload = { ...payload }
      return { data: { id: 1 } }
    })
    const wrapper = await mountView()
    const vm = wrapper.vm as any

    Object.assign(vm.bankForm, validBankForm())

    await vm.submitBankAccount()
    await flushPromises()

    expect(capturedPayload).toEqual(expect.objectContaining({
      accountName: '张三',
      branchName: '中国银行上海分行'
    }))
    expect(mocks.elMessage.success).toHaveBeenCalledWith('个人银行账户已新增')
  })

  it('shows the unified personal bank account success message when updating', async () => {
    let capturedPayload: Record<string, unknown> | null = null
    mocks.profileApi.updateBankAccount.mockImplementationOnce(async (_id: number, payload: Record<string, unknown>) => {
      capturedPayload = { ...payload }
      return { data: { id: 2 } }
    })
    const wrapper = await mountView()
    const vm = wrapper.vm as any

    vm.bankDialogMode = 'edit'
    vm.editingBankAccountId = 12
    Object.assign(vm.bankForm, validBankForm())

    await vm.submitBankAccount()
    await flushPromises()

    expect(mocks.profileApi.updateBankAccount).toHaveBeenCalledWith(12, expect.any(Object))
    expect(capturedPayload).toEqual(expect.objectContaining({
      bankName: '中国银行'
    }))
    expect(mocks.elMessage.success).toHaveBeenCalledWith('个人银行账户已更新')
  })

  it('shows the unified personal bank account status wording', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as any

    await vm.toggleBankAccountStatus({ id: 9, status: 1 })
    await flushPromises()

    expect(mocks.profileApi.updateBankAccountStatus).toHaveBeenCalledWith(9, 0)
    expect(mocks.elMessage.success).toHaveBeenCalledWith('个人银行账户已停用')
  })

  it('shows the unified personal bank account default wording', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as any

    await vm.setDefaultBankAccount({ id: 9 })
    await flushPromises()

    expect(mocks.profileApi.setDefaultBankAccount).toHaveBeenCalledWith(9)
    expect(mocks.elMessage.success).toHaveBeenCalledWith('个人银行账户已设为默认')
  })
})
