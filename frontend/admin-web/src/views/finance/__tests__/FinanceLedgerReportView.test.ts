import { defineComponent, nextTick, reactive } from 'vue'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { LedgerReportKind } from '@/api'
import SubjectTreeSelect from '@/components/inputs/SubjectTreeSelect.vue'
import FinanceLedgerReportView from '../FinanceLedgerReportView.vue'

const company = reactive({ currentCompanyId: 'C1', currentCompanyName: '公司一' })
const period = reactive({ currentYear: 2022, currentPeriod: 12, currentMonthText: '2022-12' })
const route = reactive({ query: {} as Record<string, string>, fullPath: '/ledger/detail' })
const mocks = vi.hoisted(() => ({
  getMeta: vi.fn(), queryDetailLedger: vi.fn(), exportDetailLedger: vi.fn(),
  queryBalanceSheet: vi.fn(), queryGeneralLedger: vi.fn(), exportGeneralLedger: vi.fn(), querySequenceLedger: vi.fn(), push: vi.fn(),
  warning: vi.fn(), error: vi.fn(), success: vi.fn()
}))

vi.mock('@/api', () => ({ ledgerReportApi: {
  getMeta: mocks.getMeta, queryDetailLedger: mocks.queryDetailLedger, exportDetailLedger: mocks.exportDetailLedger,
  queryBalanceSheet: mocks.queryBalanceSheet, queryGeneralLedger: mocks.queryGeneralLedger,
  exportGeneralLedger: mocks.exportGeneralLedger,
  querySequenceLedger: mocks.querySequenceLedger
} }))
vi.mock('@/stores/financeCompany', () => ({ useFinanceCompanyStore: () => company }))
vi.mock('@/stores/financePeriod', () => ({ useFinancePeriodStore: () => period }))
vi.mock('vue-router', () => ({ useRoute: () => route, useRouter: () => ({ push: mocks.push }) }))
vi.mock('@/utils/permissions', () => ({ hasPermission: () => true, readStoredUser: () => ({}) }))
vi.mock('element-plus', async (importOriginal) => ({
  ...await importOriginal<typeof import('element-plus')>(),
  ElMessage: { warning: mocks.warning, error: mocks.error, success: mocks.success }
}))

const Button = defineComponent({ emits: ['click'], template: '<button @click="$emit(\'click\')"><slot /></button>' })
const Card = defineComponent({ template: '<div><slot /></div>' })

async function mountReport(reportKind: LedgerReportKind) {
  const wrapper = shallowMount(FinanceLedgerReportView, {
    props: { reportKind },
    global: { stubs: { 'el-card': Card, 'el-button': Button } }
  })
  await flushPromises()
  await nextTick()
  return wrapper
}

async function click(wrapper: Awaited<ReturnType<typeof mountReport>>, label: string) {
  const button = wrapper.findAll('button').find(item => item.text().trim() === label)
  expect(button).toBeDefined()
  await button!.trigger('click')
  await flushPromises()
}

describe('detail ledger query timing', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    company.currentCompanyId = 'C1'
    route.query = {}
    route.fullPath = '/ledger/detail'
    mocks.getMeta.mockResolvedValue({ data: {
      companyOptions: ['C1', 'C2'].map(companyId => ({ companyId, label: companyId,
        periodStartYear: 2022, periodStartMonth: 1, periodEndYear: 2023, periodEndMonth: 12 })),
      accountOptions: [
        { value: '1002', code: '1002', label: '1002 一级科目', subjectLevel: 1, subjectCategory: 'ASSET' },
        { value: '100204', code: '100204', label: '100204 下级科目', parentValue: '1002', subjectLevel: 2, subjectCategory: 'ASSET' },
        { value: '1012', code: '1012', label: '1012 一级科目', subjectLevel: 1, subjectCategory: 'ASSET' },
        { value: '101202', code: '101202', label: '101202 下级科目', parentValue: '1012', subjectLevel: 2, subjectCategory: 'ASSET' }
      ], projectClassOptions: [{ value: 'P1', label: '分类一' }],
      projectOptions: [{ value: 'I1', parentValue: 'P1', label: '项目一' }]
    } })
    mocks.queryDetailLedger.mockResolvedValue({ data: { total: 0, page: 1, pageSize: 20, items: [] } })
    mocks.queryBalanceSheet.mockResolvedValue({ data: { total: 0, page: 1, pageSize: 20, items: [] } })
    mocks.queryGeneralLedger.mockResolvedValue({ data: { total: 0, page: 1, pageSize: 20, items: [] } })
    mocks.querySequenceLedger.mockResolvedValue({ data: { total: 0, page: 1, pageSize: 20, items: [] } })
  })

  it('waits for query and clears ordinary detail results on reset', async () => {
    const wrapper = await mountReport('DETAIL_LEDGER')
    expect(mocks.queryDetailLedger).not.toHaveBeenCalled()
    await click(wrapper, '刷新')
    expect(mocks.queryDetailLedger).not.toHaveBeenCalled()
    await click(wrapper, '查询')
    expect(mocks.queryDetailLedger).toHaveBeenCalledWith(expect.objectContaining({
      ledgerKind: 'DETAIL', iyearFrom: 2022, iperiodFrom: 12, iyearTo: 2022, iperiodTo: 12
    }))
    expect(wrapper.findComponent({ name: 'FinanceDetailLedgerRows' }).exists()).toBe(true)
    await click(wrapper, '重置')
    expect(wrapper.findComponent({ name: 'FinanceDetailLedgerRows' }).exists()).toBe(false)
    expect(mocks.queryDetailLedger).toHaveBeenCalledTimes(1)
    await click(wrapper, '查询')
    company.currentCompanyId = 'C2'
    await flushPromises()
    expect(wrapper.findComponent({ name: 'FinanceDetailLedgerRows' }).exists()).toBe(false)
    expect(mocks.queryDetailLedger).toHaveBeenCalledTimes(2)
  })

  it('does not validate the project ledger before query', async () => {
    await mountReport('PROJECT_DETAIL_LEDGER')
    expect(mocks.queryDetailLedger).not.toHaveBeenCalled()
    expect(mocks.warning).not.toHaveBeenCalled()
    expect(mocks.error).not.toHaveBeenCalled()
  })

  it.each<LedgerReportKind>([
    'BALANCE_SHEET', 'GENERAL_LEDGER', 'DETAIL_LEDGER', 'PROJECT_DETAIL_LEDGER',
    'SUPPLIER_DETAIL_LEDGER', 'CUSTOMER_DETAIL_LEDGER', 'PERSONAL_DETAIL_LEDGER',
    'QUANTITY_AMOUNT_DETAIL_LEDGER'
  ])('uses the voucher subject tree for %s', async reportKind => {
    const wrapper = await mountReport(reportKind)
    const selectors = wrapper.findAllComponents(SubjectTreeSelect)
    expect(selectors).toHaveLength(2)
    expect(selectors[0]?.props('options')).toEqual(expect.arrayContaining([
      expect.objectContaining({ value: '100204', parentValue: '1002' })
    ]))
    wrapper.unmount()
  })

  it('keeps selected child codes in ordinary detail query and export', async () => {
    const wrapper = await mountReport('DETAIL_LEDGER')
    const selectors = wrapper.findAllComponents(SubjectTreeSelect)
    selectors[0]!.vm.$emit('update:modelValue', '100204')
    selectors[1]!.vm.$emit('update:modelValue', '101202')
    await nextTick()
    await click(wrapper, '查询')
    expect(mocks.queryDetailLedger).toHaveBeenCalledWith(expect.objectContaining({
      accountCodeFrom: '100204', accountCodeTo: '101202'
    }))
    await click(wrapper, '导出 Excel')
    expect(mocks.exportDetailLedger).toHaveBeenCalledWith(expect.objectContaining({
      accountCodeFrom: '100204', accountCodeTo: '101202'
    }))
    wrapper.unmount()
  })

  it('retains a child code when opening detail from general ledger', async () => {
    route.query = { periodFrom: '2022-11', periodTo: '2023-01' }
    mocks.queryGeneralLedger.mockResolvedValueOnce({ data: { total: 1, page: 1, pageSize: 20, items: [{
      subjectCode: '100204', subjectName: '下级科目', beginDebit: 0, beginCredit: 0,
      totalDebit: 0, totalCredit: 0, endDebit: 0, endCredit: 0, rowCount: 0, rows: []
    }] } })
    const wrapper = await mountReport('GENERAL_LEDGER')
    const link = wrapper.findAll('button').find(item => item.text().includes('期末'))
    expect(link).toBeDefined()
    await link!.trigger('click')
    expect(mocks.push).toHaveBeenCalledWith(expect.objectContaining({
      name: 'finance-detail-ledger',
      query: expect.objectContaining({ accountCodeFrom: '100204', accountCodeTo: '100204',
        periodFrom: '2022-11', periodTo: '2023-01' })
    }))
    wrapper.unmount()
  })

  it.each<[LedgerReportKind, string[], string[]]>([
    ['GENERAL_LEDGER', [], []],
    ['PROJECT_DETAIL_LEDGER', ['全部项目分类', '全部项目'], ['citemClass', 'citemId']],
    ['SUPPLIER_DETAIL_LEDGER', ['全部供应商'], ['csupId']],
    ['CUSTOMER_DETAIL_LEDGER', ['全部客户'], ['ccusId']],
    ['PERSONAL_DETAIL_LEDGER', ['全部个人'], ['cpersonId']],
    ['QUANTITY_AMOUNT_DETAIL_LEDGER', ['辅助项'], []]
  ])('queries and exports %s with only visible auxiliary conditions', async (reportKind, placeholders, allowedFields) => {
    route.query = {
      periodFrom: '2022-11', periodTo: '2023-01', cdeptId: 'D1', cpersonId: 'U1',
      ccusId: 'C1', csupId: 'S1', citemClass: 'P1', citemId: 'I1', balanceAssistDisplay: 'CODE_NAME'
    }
    const wrapper = await mountReport(reportKind)
    expect(wrapper.findAll('el-date-picker-stub')).toHaveLength(2)
    const assistPlaceholders = ['部门', '个人', '客户', '供应商', '项目分类', '项目', '辅助项',
      '全部个人', '全部客户', '全部供应商', '全部项目分类', '全部项目']
    expect(wrapper.findAll('el-select-stub').map(item => item.attributes('placeholder'))
      .filter(item => assistPlaceholders.includes(item || ''))).toEqual(placeholders)
    await click(wrapper, '查询')
    const queryMock = reportKind === 'GENERAL_LEDGER' ? mocks.queryGeneralLedger : mocks.queryDetailLedger
    const assertPayload = (payload: Record<string, unknown>) => {
      expect(payload).toEqual(expect.objectContaining({
        iyearFrom: 2022, iperiodFrom: 11, iyearTo: 2023, iperiodTo: 1,
        iyear: undefined, iperiod: undefined,
        balanceAssistDisplay: reportKind === 'QUANTITY_AMOUNT_DETAIL_LEDGER' ? 'CODE_NAME' : undefined
      }))
      for (const field of ['cdeptId', 'cpersonId', 'ccusId', 'csupId', 'citemClass', 'citemId']) {
        expect(payload[field]).toBe(allowedFields.includes(field) ? route.query[field] : undefined)
      }
    }
    assertPayload(queryMock.mock.lastCall?.[0])
    await click(wrapper, '导出 Excel')
    const exportMock = reportKind === 'GENERAL_LEDGER' ? mocks.exportGeneralLedger : mocks.exportDetailLedger
    assertPayload(exportMock.mock.lastCall?.[0])
    wrapper.unmount()
  })

  it.each<[LedgerReportKind, string[]]>([
    ['PROJECT_DETAIL_LEDGER', ['citemClass', 'citemId']],
    ['SUPPLIER_DETAIL_LEDGER', ['csupId']],
    ['CUSTOMER_DETAIL_LEDGER', ['ccusId']],
    ['PERSONAL_DETAIL_LEDGER', ['cpersonId']]
  ])('uses empty %s auxiliary filters for query and export', async (reportKind, fields) => {
    const wrapper = await mountReport(reportKind)
    await click(wrapper, '查询')
    const queryPayload = mocks.queryDetailLedger.mock.lastCall?.[0]
    for (const field of fields) expect(queryPayload[field]).toBeUndefined()
    expect(mocks.warning).not.toHaveBeenCalled()
    await click(wrapper, '导出 Excel')
    const exportPayload = mocks.exportDetailLedger.mock.lastCall?.[0]
    for (const field of fields) expect(exportPayload[field]).toBeUndefined()
    await click(wrapper, '重置')
    await click(wrapper, '查询')
    for (const field of fields) expect(mocks.queryDetailLedger.mock.lastCall?.[0][field]).toBeUndefined()
    wrapper.unmount()
  })

  it('does not carry project conditions into supplier ledger after switching report kinds', async () => {
    route.query = { citemClass: 'P1', citemId: 'I1', csupId: 'S1' }
    const wrapper = await mountReport('PROJECT_DETAIL_LEDGER')
    await click(wrapper, '查询')
    expect(mocks.queryDetailLedger).toHaveBeenLastCalledWith(expect.objectContaining({ citemClass: 'P1', citemId: 'I1', csupId: undefined }))
    await wrapper.setProps({ reportKind: 'SUPPLIER_DETAIL_LEDGER' })
    await flushPromises()
    await click(wrapper, '查询')
    expect(mocks.queryDetailLedger).toHaveBeenLastCalledWith(expect.objectContaining({ citemClass: undefined, citemId: undefined, csupId: 'S1' }))
    wrapper.unmount()
  })

  it('keeps sequence ledger on a single year and month', async () => {
    route.query = { cdeptId: 'D1', cpersonId: 'U1', ccusId: 'C1', csupId: 'S1', citemClass: 'P1', citemId: 'I1' }
    const wrapper = await mountReport('SEQUENCE_LEDGER')
    expect(wrapper.findAll('el-date-picker-stub')).toHaveLength(0)
    expect(wrapper.findAll('el-select-stub').map(item => item.attributes('placeholder')))
      .toEqual(expect.arrayContaining(['部门', '个人', '客户', '供应商', '项目分类', '项目']))
    expect(mocks.querySequenceLedger).toHaveBeenCalledWith(expect.objectContaining({
      iyear: 2022, iperiod: 12, cdeptId: 'D1', cpersonId: 'U1', ccusId: 'C1',
      csupId: 'S1', citemClass: 'P1', citemId: 'I1'
    }))
    wrapper.unmount()
  })
})
