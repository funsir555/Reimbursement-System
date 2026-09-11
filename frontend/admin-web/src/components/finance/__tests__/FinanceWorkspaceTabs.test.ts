import { defineComponent, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import FinanceWorkspaceTabs from '@/components/finance/FinanceWorkspaceTabs.vue'

const SelectStub = defineComponent({
  inheritAttrs: false,
  props: {
    modelValue: { type: [String, Number], default: '' },
    filterMethod: { type: Function, default: undefined },
    loading: { type: Boolean, default: false },
    disabled: { type: Boolean, default: false }
  },
  emits: ['update:modelValue', 'visible-change'],
  template: '<div v-bind="$attrs"><slot /></div>'
})

const OptionStub = defineComponent({
  props: {
    label: { type: String, default: '' },
    value: { type: [String, Number], default: '' }
  },
  template: '<div class="company-option" :data-value="value">{{ label }}</div>'
})

const TooltipStub = defineComponent({
  props: {
    content: { type: String, default: '' }
  },
  template: '<div class="tooltip-stub" :data-content="content"><slot /></div>'
})

function mountView(extraProps: Record<string, unknown> = {}) {
  return mount(FinanceWorkspaceTabs, {
    props: {
      tabs: [
        { path: '/finance/home', title: '财务管理', closable: false, pinned: true, kind: 'home' },
        { path: '/finance/general-ledger/new-voucher', title: '新建凭证', closable: true, pinned: false, kind: 'page' },
        { path: '/finance/general-ledger/opening-balance', title: '期初余额', closable: true, pinned: false, kind: 'page' }
      ],
      activePath: '/finance/general-ledger/new-voucher',
      companyOptions: [
        {
          companyId: 'COMPANY_A',
          companyCode: 'COMP202604050001',
          companyName: '广州财务公司',
          label: 'COMP202604050001 - 广州财务公司',
          value: 'COMPANY_A'
        },
        {
          companyId: 'COMPANY_B',
          companyCode: 'COMP202603260001',
          companyName: '远志教育公司',
          label: 'COMP202603260001 - 远志教育公司',
          value: 'COMPANY_B'
        }
      ],
      currentCompanyId: 'COMPANY_A',
      periodYear: 2026,
      periodMonth: 4,
      periodYearOptions: [2026],
      periodMonthOptions: [4],
      periodStartYear: 2026,
      periodStartMonth: 4,
      periodEndYear: 2026,
      periodEndMonth: 4,
      ...extraProps
    },
    global: {
      stubs: {
        'el-select': SelectStub,
        'el-option': OptionStub,
        'el-tooltip': TooltipStub
      }
    }
  })
}

describe('FinanceWorkspaceTabs', () => {
  it('renders flat tabs, keeps tools on one row, and narrows the period selectors', () => {
    const wrapper = mountView({ periodHint: '当前公司未创建账套' })

    expect(wrapper.find('.finance-tabs-wrap').exists()).toBe(true)
    expect(wrapper.find('.finance-tab').exists()).toBe(true)
    expect(wrapper.find('.finance-tab-active').exists()).toBe(true)
    expect(wrapper.find('[data-tab-path="/finance/home"] .finance-tab-close').exists()).toBe(false)
    expect(wrapper.find('[data-tab-path="/finance/general-ledger/new-voucher"] .finance-tab-close').exists()).toBe(true)
    expect(wrapper.find('.finance-tool-inline-group.finance-period-group').exists()).toBe(true)
    expect(wrapper.find('.finance-tool-inline-group.finance-company-group').exists()).toBe(true)
    expect(wrapper.text()).toContain('会计期间')
    expect(wrapper.text()).toContain('当前公司')
    expect(wrapper.text()).not.toContain('页签')
    expect(wrapper.find('[data-testid="period-year-select"]').attributes('style')).toContain('--finance-period-select-width: 88px')
    expect(wrapper.find('[data-testid="period-month-select"]').attributes('style')).toContain('--finance-period-select-width: 60px')
    expect(wrapper.get('[data-testid="company-select"]').attributes('style')).toContain('--finance-company-select-width: 292px')
    expect(wrapper.find('[data-testid="period-tooltip"]').exists()).toBe(true)
    expect(wrapper.find('.tooltip-stub').attributes('data-content')).toBe('当前公司未创建账套')
    expect(wrapper.text()).not.toContain('当前公司未创建账套')
  })

  it('shows company names only and keeps company filtering usable for name and code', async () => {
    const wrapper = mountView()
    const companySelect = wrapper.get('[data-testid="company-select"]')
    const filterMethod = wrapper.findAllComponents(SelectStub)[2]?.props('filterMethod') as ((query: string) => void) | undefined

    expect(companySelect.findAll('.company-option').map((item) => item.text())).toEqual(['广州财务公司', '远志教育公司'])
    expect(wrapper.text()).not.toContain('COMP202604050001 - 广州财务公司')

    filterMethod?.('20260405')
    await nextTick()
    expect(companySelect.findAll('.company-option').map((item) => item.text())).toEqual(['广州财务公司'])

    filterMethod?.('远志')
    await nextTick()
    expect(companySelect.findAll('.company-option').map((item) => item.text())).toEqual(['远志教育公司'])

    await wrapper.findAllComponents(SelectStub)[2]?.vm.$emit('visible-change', false)
    await nextTick()
    expect(companySelect.findAll('.company-option').map((item) => item.text())).toEqual(['广州财务公司', '远志教育公司'])
  })

  it('still emits company switch and period switch events', async () => {
    const wrapper = mountView()
    const selects = wrapper.findAllComponents(SelectStub)

    await selects[2]?.vm.$emit('update:modelValue', 'COMPANY_B')
    await selects[0]?.vm.$emit('update:modelValue', 2026)
    await selects[1]?.vm.$emit('update:modelValue', 4)

    expect(wrapper.emitted('changeCompany')).toEqual([['COMPANY_B']])
    expect(wrapper.emitted('changePeriod')).toEqual([
      [{ year: 2026, month: 4 }],
      [{ year: 2026, month: 4 }]
    ])
  })

  it('uses the last available month when switching to a year where the current month is unavailable', async () => {
    const wrapper = mountView({
      periodYear: 2022,
      periodMonth: 11,
      periodYearOptions: [2022, 2023],
      periodMonthOptions: [11, 12],
      periodStartYear: 2022,
      periodStartMonth: 11,
      periodEndYear: 2023,
      periodEndMonth: 1
    })

    const yearSelect = wrapper.findAllComponents(SelectStub)[0]
    await yearSelect.vm.$emit('update:modelValue', 2023)

    expect(wrapper.emitted('changePeriod')).toEqual([
      [{ year: 2023, month: 1 }]
    ])
  })

  it('does not emit a period switch when the target year has no available months', async () => {
    const wrapper = mountView({
      periodYear: 2022,
      periodMonth: 11,
      periodYearOptions: [2022, 2023],
      periodMonthOptions: [11, 12],
      periodStartYear: 2022,
      periodStartMonth: 11,
      periodEndYear: 2022,
      periodEndMonth: 12
    })

    const yearSelect = wrapper.findAllComponents(SelectStub)[0]
    await yearSelect.vm.$emit('update:modelValue', 2023)

    expect(wrapper.emitted('changePeriod')).toBeUndefined()
  })

  it('keeps the close button visible for a single closable functional tab', () => {
    const wrapper = mountView({
      tabs: [
        { path: '/finance/home', title: '财务管理', closable: false, pinned: true, kind: 'home' },
        { path: '/finance/general-ledger/new-voucher', title: '新建凭证', closable: true, pinned: false, kind: 'page' }
      ]
    })

    expect(wrapper.find('[data-tab-path="/finance/home"] .finance-tab-close').exists()).toBe(false)
    expect(wrapper.find('[data-tab-path="/finance/general-ledger/new-voucher"] .finance-tab-close').exists()).toBe(true)
  })
})
