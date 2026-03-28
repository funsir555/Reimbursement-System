import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { flushPromises, shallowMount } from '@vue/test-utils'
import LoginView from '@/views/LoginView.vue'
import { authApi } from '@/api'
import { ElMessage } from 'element-plus'

const pushMock = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: pushMock
  })
}))

vi.mock('@/api', () => ({
  authApi: {
    loginByPassword: vi.fn()
  }
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    warning: vi.fn(),
    success: vi.fn(),
    error: vi.fn(),
    info: vi.fn()
  }
}))

const ElFormStub = defineComponent({
  template: '<form><slot /></form>'
})

const ElFormItemStub = defineComponent({
  template: '<div><slot /></div>'
})

const ElInputStub = defineComponent({
  props: {
    modelValue: {
      type: String,
      default: ''
    },
    type: {
      type: String,
      default: 'text'
    },
    size: {
      type: String,
      default: ''
    },
    placeholder: {
      type: String,
      default: ''
    }
  },
  emits: ['update:modelValue', 'keyup.enter'],
  template:
    '<input :type="type" :placeholder="placeholder" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" @keyup.enter="$emit(\'keyup.enter\')" />'
})

const ElButtonStub = defineComponent({
  props: {
    disabled: Boolean,
    loading: Boolean
  },
  emits: ['click'],
  template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>'
})

const ElCheckboxStub = defineComponent({
  props: {
    modelValue: Boolean
  },
  emits: ['update:modelValue'],
  template:
    '<input type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)" />'
})

const ElIconStub = defineComponent({
  template: '<i><slot /></i>'
})

function createWrapper() {
  return shallowMount(LoginView, {
    global: {
      stubs: {
        'el-form': ElFormStub,
        'el-form-item': ElFormItemStub,
        'el-input': ElInputStub,
        'el-button': ElButtonStub,
        'el-checkbox': ElCheckboxStub,
        'el-icon': ElIconStub
      }
    }
  })
}

describe('LoginView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('blocks password login when username is missing', async () => {
    const wrapper = createWrapper()

    await wrapper.findComponent(ElButtonStub).trigger('click')

    expect(ElMessage.warning).toHaveBeenCalledTimes(1)
    expect(authApi.loginByPassword).not.toHaveBeenCalled()
    expect(pushMock).not.toHaveBeenCalled()
  })

  it('stores login state and redirects after a successful login', async () => {
    vi.mocked(authApi.loginByPassword).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: {
        userId: 1,
        username: 'alice',
        name: 'Alice',
        token: 'token-123',
        expireIn: 604800,
        roles: ['admin'],
        permissionCodes: ['dashboard:view']
      }
    })

    const wrapper = createWrapper()
    const inputs = wrapper.findAll('input')

    await inputs[0].setValue('alice')
    await inputs[1].setValue('secret')
    await wrapper.findComponent(ElButtonStub).trigger('click')
    await flushPromises()

    expect(authApi.loginByPassword).toHaveBeenCalledWith('alice', 'secret')
    expect(localStorage.getItem('token')).toBe('token-123')
    expect(JSON.parse(localStorage.getItem('user') || '{}')).toMatchObject({
      username: 'alice',
      permissionCodes: ['dashboard:view']
    })
    expect(ElMessage.success).toHaveBeenCalledTimes(1)
    expect(pushMock).toHaveBeenCalledWith('/dashboard')
  })
})
