import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h, ref } from 'vue'
import { describe, expect, it } from 'vitest'
import MoneyInput from '@/components/inputs/MoneyInput.vue'

const ElInputStub = defineComponent({
  name: 'ElInput',
  inheritAttrs: false,
  props: {
    modelValue: {
      type: String,
      default: ''
    },
    placeholder: {
      type: String,
      default: ''
    },
    disabled: {
      type: Boolean,
      default: false
    },
    readonly: {
      type: Boolean,
      default: false
    }
  },
  emits: ['focus', 'input', 'blur'],
  setup(props, { attrs, emit }) {
    return () => h('input', {
      ...attrs,
      value: props.modelValue,
      placeholder: props.placeholder,
      disabled: props.disabled,
      readOnly: props.readonly,
      onFocus: (event: FocusEvent) => emit('focus', event),
      onInput: (event: Event) => emit('input', (event.target as HTMLInputElement).value),
      onBlur: (event: FocusEvent) => emit('blur', event)
    })
  }
})

function mountWithModel(initialValue = '') {
  const model = ref(initialValue)
  const Host = defineComponent({
    setup() {
      return () => h(MoneyInput, {
        modelValue: model.value,
        'onUpdate:modelValue': (nextValue: string) => {
          model.value = nextValue
        }
      })
    }
  })

  const wrapper = mount(Host, {
    global: {
      stubs: {
        ElInput: ElInputStub
      }
    }
  })

  return {
    wrapper,
    model,
    setModel(nextValue: string) {
      model.value = nextValue
    }
  }
}

describe('MoneyInput', () => {
  it('keeps valid intermediate drafts while typing and normalizes on blur', async () => {
    const { wrapper, model } = mountWithModel()
    const input = wrapper.get('input')

    await input.trigger('focus')
    await input.setValue('12.')
    expect(model.value).toBe('12.')
    expect((input.element as HTMLInputElement).value).toBe('12.')

    await input.setValue('12.3')
    expect(model.value).toBe('12.3')
    expect((input.element as HTMLInputElement).value).toBe('12.3')

    await input.setValue('')
    expect(model.value).toBe('')
    expect((input.element as HTMLInputElement).value).toBe('')

    await input.setValue('12.3')
    await input.trigger('blur')
    await flushPromises()

    expect(model.value).toBe('12.30')
    expect((input.element as HTMLInputElement).value).toBe('12.30')
  })

  it('does not overwrite the focused draft when the parent model changes', async () => {
    const { wrapper, model, setModel } = mountWithModel('8.00')
    const input = wrapper.get('input')

    await input.trigger('focus')
    await input.setValue('12.')
    expect(model.value).toBe('12.')

    setModel('99.99')
    await flushPromises()

    expect((input.element as HTMLInputElement).value).toBe('12.')

    await input.trigger('blur')
    await flushPromises()

    expect(model.value).toBe('12.00')
    expect((input.element as HTMLInputElement).value).toBe('12.00')
  })
})
