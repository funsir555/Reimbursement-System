import { defineComponent, nextTick, reactive } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import FinanceReviewVoucherView from '@/views/finance/FinanceReviewVoucherView.vue'

const routerPush = vi.fn()

const financeCompanyStore = reactive({
  currentCompanyId: 'COMPANY_A',
  currentCompanyName: '广州远智教育科技有限公司'
})

const mocks = vi.hoisted(() => ({
  financeApi: {
    getVoucherMeta: vi.fn(),
    listVouchers: vi.fn(),
    reviewVoucher: vi.fn(),
    unreviewVoucher: vi.fn(),
    markVoucherError: vi.fn(),
    clearVoucherError: vi.fn(),
    batchUpdateVoucherState: vi.fn()
  },
  elMessage: {
    success: vi.fn(),
    warning: vi.fn(),
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
  readStoredUser: vi.fn(() => ({
    permissionCodes: [
      'finance:general_ledger:review_voucher:review',
      'finance:general_ledger:review_voucher:unreview',
      'finance:general_ledger:review_voucher:mark_error'
    ]
  }))
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage
}))

const InputStub = defineComponent({
  props: {
    modelValue: { type: [String, Number], default: '' },
    placeholder: { type: String, default: '' },
    disabled: { type: Boolean, default: false }
  },
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

const ButtonStub = defineComponent({
  props: { disabled: { type: Boolean, default: false } },
  emits: ['click'],
  template: '<button type="button" :disabled="disabled" @click="$emit(\'click\', $event)"><slot /></button>'
})

const CardStub = defineComponent({ template: '<div><slot /></div>' })
const TagStub = defineComponent({ template: '<span><slot /></span>' })

const TableStub = defineComponent({
  props: { data: { type: Array, default: () => [] } },
  emits: ['selection-change', 'current-change', 'row-click', 'row-dblclick'],
  template: `
    <div>
      <div v-for="row in data" :key="row.voucherNo" class="table-row">
        <button class="row-current" @click="$emit('current-change', row); $emit('row-click', row)">{{ row.displayVoucherNo }}</button>
        <button class="row-dblclick" @dblclick="$emit('row-dblclick', row)">{{ row.displayVoucherNo }}</button>
      </div>
      <slot />
    </div>
  `
})

const TableColumnStub = defineComponent({ template: '<div><slot :row="{}" /></div>' })

async function mountView() {
  const wrapper = mount(FinanceReviewVoucherView, {
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

function buildRows() {
  return [
    {
      voucherNo: 'COMPANY_A~4~记~12',
      displayVoucherNo: '记-0012',
      companyId: 'COMPANY_A',
      iperiod: 4,
      csign: '记',
      voucherTypeLabel: '记账凭证',
      dbillDate: '2026-04-05',
      summary: '办公用品',
      cbill: '财务制单员',
      checkerName: '',
      idoc: 1,
      status: 'UNPOSTED',
      statusLabel: '未记账',
      editable: true,
      entryCount: 2,
      totalDebit: '100.00',
      totalCredit: '100.00'
    },
    {
      voucherNo: 'COMPANY_A~4~记~13',
      displayVoucherNo: '记-0013',
      companyId: 'COMPANY_A',
      iperiod: 4,
      csign: '记',
      voucherTypeLabel: '记账凭证',
      dbillDate: '2026-04-06',
      summary: '市场费用',
      cbill: '财务制单员',
      checkerName: '审核人甲',
      idoc: 2,
      status: 'REVIEWED',
      statusLabel: '已审核',
      editable: false,
      entryCount: 2,
      totalDebit: '200.00',
      totalCredit: '200.00'
    }
  ]
}

describe('FinanceReviewVoucherView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.financeApi.getVoucherMeta.mockResolvedValue({
      data: {
        voucherTypeOptions: [{ value: '记', label: '记账凭证' }]
      }
    })
    mocks.financeApi.listVouchers.mockResolvedValue({
      data: {
        total: 2,
        page: 1,
        pageSize: 20,
        items: buildRows()
      }
    })
    mocks.financeApi.reviewVoucher.mockResolvedValue({ data: { voucherNo: 'COMPANY_A~4~记~12' } })
    mocks.financeApi.unreviewVoucher.mockResolvedValue({ data: { voucherNo: 'COMPANY_A~4~记~13' } })
    mocks.financeApi.markVoucherError.mockResolvedValue({ data: { voucherNo: 'COMPANY_A~4~记~12' } })
    mocks.financeApi.clearVoucherError.mockResolvedValue({ data: { voucherNo: 'COMPANY_A~4~记~12' } })
    mocks.financeApi.batchUpdateVoucherState.mockResolvedValue({
      data: {
        action: 'REVIEW',
        successCount: 2,
        voucherNos: ['COMPANY_A~4~记~12', 'COMPANY_A~4~记~14']
      }
    })
  })

  it('loads the current month review workbench with the fixed status filter', async () => {
    const wrapper = await mountView()

    expect(wrapper.text()).toContain('审核凭证')
    expect(mocks.financeApi.listVouchers).toHaveBeenCalledWith(
      expect.objectContaining({
        companyId: 'COMPANY_A',
        status: 'UNPOSTED,REVIEWED,ERROR',
        billMonth: expect.stringMatching(/^\d{4}-\d{2}$/)
      })
    )
  })

  it('opens review detail on row double click and on explicit detail open', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      pager: { items: Array<{ voucherNo: string }> }
      openDetail: (row: { voucherNo: string }) => void
    }

    await wrapper.find('.row-dblclick').trigger('dblclick')
    expect(routerPush).toHaveBeenCalledWith({
      name: 'finance-review-voucher-detail',
      params: { voucherNo: 'COMPANY_A~4~记~12' }
    })

    vm.openDetail(vm.pager.items[1] as { voucherNo: string })
    expect(routerPush).toHaveBeenLastCalledWith({
      name: 'finance-review-voucher-detail',
      params: { voucherNo: 'COMPANY_A~4~记~13' }
    })
  })

  it('supports current-row action and multi-select batch action', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      pager: { items: Array<any> }
      currentRow: any
      selectedRows: any[]
      handleVoucherStateAction: (action: 'REVIEW' | 'UNREVIEW' | 'TOGGLE_ERROR') => Promise<void>
    }

    vm.currentRow = vm.pager.items[0]
    await vm.handleVoucherStateAction('REVIEW')
    expect(mocks.financeApi.reviewVoucher).toHaveBeenCalledWith('COMPANY_A', 'COMPANY_A~4~记~12')

    vm.selectedRows = [
      vm.pager.items[0],
      {
        ...vm.pager.items[0],
        voucherNo: 'COMPANY_A~4~记~14',
        displayVoucherNo: '记-0014'
      }
    ]
    await vm.handleVoucherStateAction('REVIEW')
    expect(mocks.financeApi.batchUpdateVoucherState).toHaveBeenCalledWith({
      companyId: 'COMPANY_A',
      action: 'REVIEW',
      voucherNos: ['COMPANY_A~4~记~12', 'COMPANY_A~4~记~14']
    })
  })

  it('rejects mixed-status selections with a clear chinese warning', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      pager: { items: Array<any> }
      selectedRows: any[]
      handleVoucherStateAction: (action: 'REVIEW' | 'UNREVIEW' | 'TOGGLE_ERROR') => Promise<void>
    }

    vm.selectedRows = [vm.pager.items[0], vm.pager.items[1]]
    await vm.handleVoucherStateAction('TOGGLE_ERROR')

    expect(mocks.elMessage.warning).toHaveBeenCalledWith('勾选凭证的状态不一致，请按同一状态分批处理')
    expect(mocks.financeApi.batchUpdateVoucherState).not.toHaveBeenCalled()
  })

  it('switches the error action label to clear-error for error vouchers', async () => {
    mocks.financeApi.listVouchers.mockResolvedValueOnce({
      data: {
        total: 1,
        page: 1,
        pageSize: 20,
        items: [
          {
            ...buildRows()[0],
            status: 'ERROR',
            statusLabel: '已标记错误'
          }
        ]
      }
    })

    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      currentRow: any
      effectiveErrorActionLabel: string
      handleVoucherStateAction: (action: 'REVIEW' | 'UNREVIEW' | 'TOGGLE_ERROR') => Promise<void>
    }

    vm.currentRow = {
      ...buildRows()[0],
      status: 'ERROR',
      statusLabel: '已标记错误'
    }
    await nextTick()
    expect(vm.effectiveErrorActionLabel).toBe('取消错误')

    await vm.handleVoucherStateAction('TOGGLE_ERROR')
    expect(mocks.financeApi.clearVoucherError).toHaveBeenCalledWith('COMPANY_A', 'COMPANY_A~4~记~12')
  })
})
