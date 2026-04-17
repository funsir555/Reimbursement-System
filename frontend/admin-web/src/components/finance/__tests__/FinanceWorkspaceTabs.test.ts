import { defineComponent, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import FinanceWorkspaceTabs from '@/components/finance/FinanceWorkspaceTabs.vue'

const ButtonStub = defineComponent({
  emits: ['click'],
  template: '<button type="button" @click="$emit(\'click\', $event)"><slot /></button>'
})

const SelectStub = defineComponent({
  inheritAttrs: false,
  props: {
    modelValue: { type: String, default: '' },
    filterMethod: { type: Function, default: undefined },
    loading: { type: Boolean, default: false },
    disabled: { type: Boolean, default: false }
  },
  emits: ['update:modelValue', 'visible-change'],
  template: '<div data-testid="company-select" v-bind="$attrs"><slot /></div>'
})

const OptionStub = defineComponent({
  props: {
    label: { type: String, default: '' },
    value: { type: String, default: '' }
  },
  template: '<div class="company-option" :data-value="value">{{ label }}</div>'
})

const ContainerStub = defineComponent({
  template: '<div><slot /></div>'
})

function mountView() {
  return mount(FinanceWorkspaceTabs, {
    props: {
      tabs: [{ path: '/finance/general-ledger/new-voucher', title: 'New Voucher' }],
      activePath: '/finance/general-ledger/new-voucher',
      companyOptions: [
        {
          companyId: 'COMPANY_A',
          companyCode: 'COMP202604050001',
          companyName: 'Guangzhou Finance Company',
          label: 'COMP202604050001 - Guangzhou Finance Company',
          value: 'COMPANY_A'
        },
        {
          companyId: 'COMPANY_B',
          companyCode: 'COMP202603260001',
          companyName: 'Yuanzhi Education Company',
          label: 'COMP202603260001 - Yuanzhi Education Company',
          value: 'COMPANY_B'
        }
      ],
      currentCompanyId: 'COMPANY_A'
    },
    global: {
      stubs: {
        'el-button': ButtonStub,
        'el-select': SelectStub,
        'el-option': OptionStub,
        'el-dropdown': ContainerStub,
        'el-dropdown-menu': ContainerStub,
        'el-dropdown-item': ContainerStub,
        'el-icon': true
      }
    }
  })
}

describe('FinanceWorkspaceTabs', () => {
  it('renders company names only and applies the widened selector width', () => {
    const wrapper = mountView()
    const optionTexts = wrapper.findAll('.company-option').map((item) => item.text())
    const select = wrapper.get('[data-testid="company-select"]')

    expect(optionTexts).toEqual(['Guangzhou Finance Company', 'Yuanzhi Education Company'])
    expect(wrapper.text()).not.toContain('COMP202604050001 - Guangzhou Finance Company')
    expect(select.attributes('style')).toContain('--finance-company-select-width: 390px')
  })

  it('keeps company filtering usable for both company name and company code', async () => {
    const wrapper = mountView()
    const select = wrapper.getComponent(SelectStub)
    const filterMethod = select.props('filterMethod') as ((query: string) => void) | undefined

    filterMethod?.('20260405')
    await nextTick()
    expect(wrapper.findAll('.company-option').map((item) => item.text())).toEqual(['Guangzhou Finance Company'])

    filterMethod?.('Yuanzhi')
    await nextTick()
    expect(wrapper.findAll('.company-option').map((item) => item.text())).toEqual(['Yuanzhi Education Company'])

    await select.vm.$emit('visible-change', false)
    await nextTick()
    expect(wrapper.findAll('.company-option').map((item) => item.text())).toEqual([
      'Guangzhou Finance Company',
      'Yuanzhi Education Company'
    ])
  })

  it('still emits company switch events after the display changes', async () => {
    const wrapper = mountView()
    const select = wrapper.getComponent(SelectStub)

    await select.vm.$emit('update:modelValue', 'COMPANY_B')

    expect(wrapper.emitted('changeCompany')).toEqual([['COMPANY_B']])
  })
})
