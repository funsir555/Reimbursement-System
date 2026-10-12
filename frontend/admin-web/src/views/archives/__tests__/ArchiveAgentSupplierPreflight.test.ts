import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { archiveAgentApi } from '@/api'
import ArchiveAgentView from '../ArchiveAgentView.vue'

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ push: vi.fn() })
}))
vi.mock('@/utils/permissions', () => ({
  hasAnyPermission: () => true,
  readStoredUser: () => ({ userId: 1, permissionCodes: ['agent:workbench:run'] })
}))
vi.mock('@/api', () => ({
  archiveAgentApi: {
    getMeta: vi.fn(), list: vi.fn(), prepareRun: vi.fn(), run: vi.fn(),
    listRuns: vi.fn(), getRunDetail: vi.fn()
  }
}))

describe('supplier clarification in workbench run', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    sessionStorage.clear()
  })

  it('sends the selected supplier code for its node and continues to the run', async () => {
    vi.mocked(archiveAgentApi.getMeta).mockResolvedValue({ data: { tools: [] } } as never)
    vi.mocked(archiveAgentApi.list).mockResolvedValue({ data: [] } as never)
    vi.mocked(archiveAgentApi.listRuns).mockResolvedValue({ data: [] } as never)
    vi.mocked(archiveAgentApi.getRunDetail).mockResolvedValue({ data: { status: 'SUCCESS' } } as never)
    vi.mocked(archiveAgentApi.prepareRun)
      .mockResolvedValueOnce({ data: {
        mode: 'CLARIFY', message: '请选择供应商', agentId: 9, versionId: 31,
        companyOptions: [], workflowOptions: [], supplierNodeKey: 'supplier',
        supplierOptions: [{ code: 'V1', name: '甲公司' }, { code: 'V2', name: '甲公司' }]
      } } as never)
      .mockResolvedValueOnce({ data: {
        mode: 'READY', message: '', agentId: 9, versionId: 31,
        agentName: '供应商明细账', usePublishedVersion: false,
        parameters: { supplier: { companyId: 'C1', supplierCode: 'V2' } },
        companyOptions: [], workflowOptions: [], supplierOptions: []
      } } as never)
    vi.mocked(archiveAgentApi.run).mockResolvedValue({ data: { id: 100, runNo: 'RUN-100' } } as never)

    const wrapper = shallowMount(ArchiveAgentView, {
      global: { directives: { loading: () => {} } }
    })
    await flushPromises()
    const vm = wrapper.vm as unknown as {
      editableAgent: { id?: number }
      runForm: { inputPayloadText: string }
      selectedRunCompanyId: string
      selectedRunSupplierCode: string
      selectedRunVersionId?: number
      runAgentNow: () => Promise<void>
    }
    vm.editableAgent.id = 9
    vm.runForm.inputPayloadText = JSON.stringify({ query: '查询甲公司供应商明细' })
    await nextTick()
    vm.selectedRunCompanyId = 'C1'
    await nextTick()

    await vm.runAgentNow()
    expect(vm.selectedRunVersionId).toBe(31)
    vm.selectedRunSupplierCode = 'V2'
    await vm.runAgentNow()

    expect(archiveAgentApi.prepareRun).toHaveBeenNthCalledWith(2, 9, {
      inputPayload: {
        query: '查询甲公司供应商明细', selectedCompanyId: 'C1',
        selectedVersionId: 31, selectedSupplierCodes: { supplier: 'V2' }
      }
    })
    expect(archiveAgentApi.run).toHaveBeenCalledWith(9, expect.objectContaining({
      expectedVersionId: 31,
      inputPayload: expect.objectContaining({ parameters: {
        supplier: { companyId: 'C1', supplierCode: 'V2' }
      } })
    }))
    wrapper.unmount()
  })
})
