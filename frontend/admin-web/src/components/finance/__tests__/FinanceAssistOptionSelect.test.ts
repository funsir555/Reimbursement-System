import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { describe, expect, it, vi } from 'vitest'
import FinanceAssistOptionSelect from '@/components/finance/FinanceAssistOptionSelect.vue'

const { message } = vi.hoisted(() => ({
  message: {
    warning: vi.fn()
  }
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: message
  }
})

const SelectStub = defineComponent({
  props: {
    modelValue: { type: [String, Number], default: '' }
  },
  emits: ['update:modelValue', 'change', 'focus'],
  template: '<div class="select-stub"><slot /><slot name="footer" /></div>'
})

const OptionStub = defineComponent({
  props: {
    label: { type: String, default: '' },
    value: { type: [String, Number], default: '' }
  },
  template: '<div class="option-stub">{{ label }}</div>'
})

const ButtonStub = defineComponent({
  props: {
    disabled: { type: Boolean, default: false }
  },
  emits: ['click'],
  template: '<button type="button" :disabled="disabled" @click="$emit(\'click\', $event)"><slot /></button>'
})

describe('FinanceAssistOptionSelect', () => {
  it('renders options as code plus name and exposes the add action', async () => {
    const wrapper = mount(FinanceAssistOptionSelect, {
      props: {
        modelValue: '',
        options: [{ value: 'C001', code: 'C001', name: '客户甲', label: '客户甲' }],
        addable: true,
        addText: '增加'
      },
      global: {
        stubs: {
          'el-select': SelectStub,
          'el-option': OptionStub,
          'el-button': ButtonStub
        }
      }
    })

    expect(wrapper.text()).toContain('C001  客户甲')

    await wrapper.get('button').trigger('click')
    expect(wrapper.emitted('request-add')).toHaveLength(1)
  })

  it('shows the disabled add warning instead of emitting when creation is blocked', async () => {
    const wrapper = mount(FinanceAssistOptionSelect, {
      props: {
        modelValue: '',
        options: [],
        addable: true,
        addDisabled: true,
        addDisabledMessage: '请先选择项目分类'
      },
      global: {
        stubs: {
          'el-select': SelectStub,
          'el-option': OptionStub,
          'el-button': ButtonStub
        }
      }
    })

    await wrapper.get('button').trigger('click')

    expect(wrapper.emitted('request-add')).toBeUndefined()
    expect(message.warning).toHaveBeenCalledWith('请先选择项目分类')
  })
})
