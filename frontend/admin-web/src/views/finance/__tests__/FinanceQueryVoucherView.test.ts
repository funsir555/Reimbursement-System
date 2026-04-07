import { defineComponent, nextTick, reactive } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import FinanceQueryVoucherView from '@/views/finance/FinanceQueryVoucherView.vue'

const routerPush = vi.fn()
const financeCompanyStore = reactive({
  currentCompanyId: 'COMPANY_A',
  currentCompanyName: '广州远智教育科技有限公司'
})

const mocks = vi.hoisted(() => ({
  financeApi: {
    getVoucherMeta: vi.fn(),
    listVouchers: vi.fn(),
    exportVouchers: vi.fn()
  },
  elMessage: {
    success: vi.fn(),
    error: vi.fn()
  }
}))

vi.mock('@/api', () => ({
  financeApi: mocks.financeApi
}))

vi.mock('@/stores/financeCompany', () => ({
  useFinanceCompanyStore: () => financeCompanyStore
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush })
}))

vi.mock('@/utils/permissions', () => ({
  hasPermission: vi.fn(() => true),
  readStoredUser: vi.fn(() => ({ permissionCodes: ['finance:general_ledger:query_voucher:export'] }))
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage
}))

const InputStub = defineComponent({
  props: { modelValue: { type: [String, Number], default: '' }, placeholder: { type: String, default: '' }, disabled: { type: Boolean, default: false } },
  emits: ['update:modelValue'],
  template: '<input :value="modelValue" :placeholder="placeholder" :disabled="disabled" @input="$emit(\'update:modelValue\', $event.target.value)" />'
})

const SelectStub = defineComponent({
  props: { modelValue: { type: [String, Number], default: '' }, disabled: { type: Boolean, default: false } },
  emits: ['update:modelValue'],
  template: '<select :value="modelValue" :disabled="disabled" @change="$emit(\'update:modelValue\', $event.target.value)"><slot /></select>'
})

const OptionStub = defineComponent({
  props: { label: { type: String, default: '' }, value: { type: [String, Number], default: '' } },
  template: '<option :value="value">{{ label }}</option>'
})

const ButtonStub = defineComponent({ emits: ['click'], template: '<button type="button" @click="$emit(\'click\', $event)"><slot /></button>' })
const CardStub = defineComponent({ template: '<div><slot /></div>' })
const TagStub = defineComponent({ template: '<span><slot /></span>' })
const TableColumnStub = defineComponent({ template: '<div><slot :row="{}" /></div>' })

const TableStub = defineComponent({
  props: { data: { type: Array, default: () => [] } },
  emits: ['row-dblclick'],
  template: '<div><button v-for="row in data" :key="row.voucherNo" class="row-trigger" @dblclick="$emit(\'row-dblclick\', row)">{{ row.displayVoucherNo }}</button><slot /></div>'
})

async function mountView() {
  const wrapper = mount(FinanceQueryVoucherView, {
    global: {
      directives: {
        loading: () => undefined
      },
      stubs: {
        'el-card': CardStub,
        'el-button': ButtonStub,
        'el-input': InputStub,
        'el-select': SelectStub,
        'el-option': OptionStub,
        'el-date-picker': InputStub,
        'el-table': TableStub,
        'el-table-column': TableColumnStub,
        'el-pagination': defineComponent({ template: '<div />' }),
        'el-tag': TagStub
      }
    }
  })
  await flushPromises()
  await nextTick()
  return wrapper
}

describe('FinanceQueryVoucherView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.financeApi.getVoucherMeta.mockResolvedValue({
      data: {
        voucherTypeOptions: [{ value: '记', label: '记账凭证' }]
      }
    })
    mocks.financeApi.listVouchers.mockResolvedValue({
      data: {
        total: 1,
        page: 1,
        pageSize: 20,
        items: [{
          voucherNo: 'COMPANY_A~4~记~12',
          displayVoucherNo: '记-0012',
          companyId: 'COMPANY_A',
          iperiod: 4,
          csign: '记',
          voucherTypeLabel: '记账凭证',
          dbillDate: '2026-04-05',
          summary: '办公用品',
          cbill: '财务制单员',
          idoc: 1,
          status: 'UNPOSTED',
          statusLabel: '未记账',
          editable: true,
          entryCount: 2,
          totalDebit: '100.00',
          totalCredit: '100.00'
        }]
      }
    })
  })

  it('loads vouchers with current finance company and opens detail on row dblclick', async () => {
    const wrapper = await mountView()

    expect(mocks.financeApi.listVouchers).toHaveBeenCalledWith(expect.objectContaining({ companyId: 'COMPANY_A', page: 1, pageSize: 20 }))
    expect(wrapper.text()).toContain('查询凭证')

    await wrapper.find('.row-trigger').trigger('dblclick')
    expect(routerPush).toHaveBeenCalledWith({ name: 'finance-query-voucher-detail', params: { voucherNo: 'COMPANY_A~4~记~12' } })
  })
})
