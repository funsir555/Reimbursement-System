import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { archiveAgentApi } from '@/api'
import AgentToolConfigView from '../AgentToolConfigView.vue'

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '9' } }),
  useRouter: () => ({ push: vi.fn() })
}))
vi.mock('@/utils/permissions', () => ({
  hasAnyPermission: () => true,
  readStoredUser: () => ({ permissionCodes: ['agent:workbench:run'] })
}))
vi.mock('@/api', () => ({
  archiveAgentApi: {
    getMeta: vi.fn(), getDetail: vi.fn(), searchToolSuppliers: vi.fn(), testTool: vi.fn()
  }
}))

const toolCode = 'finance.query_supplier_detail_ledger'

async function mountTool() {
  vi.mocked(archiveAgentApi.getMeta).mockResolvedValue({ data: { tools: [{
    toolCode, label: '供应商明细账', description: '', domain: 'Finance', entry: '',
    permissionCode: '', queryTemplateCode: 'finance.supplier_detail_ledger.v1',
    testFields: [
      { key: 'companyId', label: '账套', type: 'company', required: true },
      { key: 'supplierCode', label: '供应商', type: 'supplier', required: true },
      { key: 'supplierName', label: '供应商名称', type: 'text', required: false }
    ]
  }] } } as never)
  vi.mocked(archiveAgentApi.getDetail).mockResolvedValue({ data: {
    id: 9, agentName: '测试', tools: [{ toolCode, enabled: true }], latestVersionNo: 1
  } } as never)
  vi.mocked(archiveAgentApi.searchToolSuppliers).mockResolvedValue({ data: [
    { code: '40036', name: '供应商6' }
  ] } as never)
  vi.mocked(archiveAgentApi.testTool).mockResolvedValue({ data: {
    summary: '查询完成', records: [], totalCount: 0
  } } as never)
  const wrapper = shallowMount(AgentToolConfigView, {
    global: { directives: { loading: () => {} } }
  })
  await flushPromises()
  const vm = wrapper.vm as unknown as {
    selectedCode: string
    testValues: Record<string, string | number | undefined>
    visibleTestFields: Array<{ key: string }>
    supplierOptions: Array<{ code: string; name: string }>
    searchSuppliers: (keyword: string) => Promise<void>
    testTool: () => Promise<void>
  }
  vm.selectedCode = toolCode
  await nextTick()
  return { wrapper, vm }
}

describe('supplier ledger single-tool selection', () => {
  beforeEach(() => vi.clearAllMocks())

  it('searches by name, displays one supplier field and submits its code', async () => {
    const { wrapper, vm } = await mountTool()
    vm.testValues.companyId = 'C1'
    await nextTick()
    await vm.searchSuppliers('供应商6')
    expect(archiveAgentApi.searchToolSuppliers).toHaveBeenCalledWith(9, 'C1', '供应商6')
    expect(vm.supplierOptions).toEqual([{ code: '40036', name: '供应商6' }])
    expect(vm.visibleTestFields.map(field => field.key)).toEqual(['companyId', 'supplierCode'])

    vm.testValues.supplierCode = '40036'
    await vm.testTool()
    expect(archiveAgentApi.testTool).toHaveBeenCalledWith(9, toolCode, {
      queryTemplateCode: 'finance.supplier_detail_ledger.v1',
      parameters: { companyId: 'C1', supplierCode: '40036' }
    })
    wrapper.unmount()
  })

  it('clears the supplier when the account set changes', async () => {
    const { wrapper, vm } = await mountTool()
    vm.testValues.companyId = 'C1'
    await nextTick()
    await vm.searchSuppliers('供应商6')
    vm.testValues.supplierCode = '40036'
    vm.testValues.companyId = 'C2'
    await nextTick()
    expect(vm.testValues.supplierCode).toBeUndefined()
    expect(vm.supplierOptions).toEqual([])
    wrapper.unmount()
  })
})
