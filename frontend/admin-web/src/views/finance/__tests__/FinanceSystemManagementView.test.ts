import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, inject, provide } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import FinanceSystemManagementView from '@/views/finance/FinanceSystemManagementView.vue'

const mocks = vi.hoisted(() => ({
  financeSystemManagementApi: {
    getMeta: vi.fn(),
    listAccountSets: vi.fn(),
    createAccountSet: vi.fn(),
    getTaskStatus: vi.fn()
  },
  financeCompany: {
    currentCompanyId: 'COMPANY_A',
    currentCompanyName: '广州测试公司',
    currentCompanyLabel: 'A01 - 广州测试公司'
  },
  elMessage: {
    success: vi.fn(),
    warning: vi.fn(),
    error: vi.fn()
  }
}))

vi.mock('@/api', () => ({
  financeSystemManagementApi: mocks.financeSystemManagementApi
}))

vi.mock('@/stores/financeCompany', () => ({
  useFinanceCompanyStore: () => mocks.financeCompany
}))

vi.mock('@/utils/permissions', () => ({
  hasPermission: () => true,
  readStoredUser: () => ({
    permissionCodes: ['finance:system_management:view', 'finance:system_management:create', 'finance:system_management:task:view']
  })
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage
}))

const SimpleStub = defineComponent({
  template: '<div><slot /><slot name="footer" /></div>'
})

const TableStub = defineComponent({
  props: {
    data: {
      type: Array,
      default: () => []
    }
  },
  setup(props) {
    provide('tableRows', props.data)
    return {}
  },
  template: '<div><slot /></div>'
})

const TableColumnStub = defineComponent({
  setup() {
    const rows = inject<any[]>('tableRows', [])
    return { rows }
  },
  template: `
    <div>
      <template v-for="(row, index) in rows" :key="index">
        <slot :row="row" />
      </template>
    </div>
  `
})

async function mountView() {
  const wrapper = mount(FinanceSystemManagementView, {
    global: {
      stubs: {
        'el-card': SimpleStub,
        'el-button': SimpleStub,
        'el-table': TableStub,
        'el-table-column': TableColumnStub,
        'el-drawer': SimpleStub,
        'el-steps': SimpleStub,
        'el-step': SimpleStub,
        'el-radio-group': SimpleStub,
        'el-radio': SimpleStub,
        'el-form': SimpleStub,
        'el-form-item': SimpleStub,
        'el-select': SimpleStub,
        'el-option': true,
        'el-date-picker': SimpleStub,
        'el-input': SimpleStub,
        'el-tag': SimpleStub,
        'el-progress': SimpleStub
      },
      directives: {
        loading: () => undefined
      }
    }
  })
  await flushPromises()
  return wrapper
}

describe('FinanceSystemManagementView', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.clearAllMocks()
    mocks.financeSystemManagementApi.getMeta.mockResolvedValue({
      data: {
        companyOptions: [
          { companyId: 'COMPANY_A', companyCode: 'A01', companyName: '广州测试公司', label: 'A01 - 广州测试公司', value: 'COMPANY_A' },
          { companyId: 'COMPANY_B', companyCode: 'B01', companyName: '深圳测试公司', label: 'B01 - 深圳测试公司', value: 'COMPANY_B' }
        ],
        supervisorOptions: [
          { value: '2', label: '李会计' }
        ],
        templateOptions: [
          {
            templateCode: 'AS_2007_ENTERPRISE',
            templateName: '2007 企业会计制度',
            accountingStandard: '2007 企业会计制度',
            level1SubjectCount: 60,
            commonSubjectCount: 12
          }
        ],
        referenceOptions: [
          {
            companyId: 'COMP-REF',
            companyName: '参考账套公司',
            templateCode: 'AS_2007_ENTERPRISE',
            templateName: '2007 企业会计制度',
            enabledYearMonth: '2026-01',
            subjectCodeScheme: '4-2-2-2',
            label: '参考账套公司 / 2007 企业会计制度 / 2026-01'
          }
        ],
        defaultSubjectCodeScheme: '4-2-2-2'
      }
    })
    mocks.financeSystemManagementApi.listAccountSets.mockResolvedValue({
      data: [
        {
          companyId: 'COMPANY_A',
          companyCode: 'COMP202604050001',
          companyName: '广州测试公司',
          status: 'ACTIVE',
          statusLabel: '已启用',
          enabledYearMonth: '2026-01',
          templateCode: 'AS_2007_ENTERPRISE',
          templateName: '2007 企业会计制度',
          supervisorName: '李会计',
          createMode: 'BLANK',
          subjectCodeScheme: '4-2-2-2',
          subjectCount: 88,
          lastTaskNo: 'FAS202604080001',
          lastTaskMessage: '账套创建完成',
          updatedAt: '2026-04-08 10:00:00'
        }
      ]
    })
    mocks.financeSystemManagementApi.createAccountSet.mockResolvedValue({
      data: {
        taskNo: 'FAS202604080002',
        status: 'PENDING',
        progress: 0,
        resultMessage: '账套创建任务已提交',
        finished: false
      }
    })
    mocks.financeSystemManagementApi.getTaskStatus.mockResolvedValue({
      data: {
        taskNo: 'FAS202604080002',
        status: 'SUCCESS',
        progress: 100,
        resultMessage: '账套创建完成',
        accountSetStatus: 'ACTIVE',
        finished: true
      }
    })
  })

  it('loads meta and account set list on mount', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      accountSets: Array<{ companyName: string; companyCode?: string }>
      wizardForm: { subjectCodeScheme: string }
      formatCompanyDisplay: (row: { companyId: string; companyCode?: string; companyName?: string }) => string
    }

    expect(mocks.financeSystemManagementApi.getMeta).toHaveBeenCalledTimes(1)
    expect(mocks.financeSystemManagementApi.listAccountSets).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('财务系统管理')
    expect(vm.accountSets[0]?.companyName).toBe('广州测试公司')
    expect(vm.accountSets[0]?.companyCode).toBe('COMP202604050001')
    expect(vm.formatCompanyDisplay(vm.accountSets[0] as { companyId: string; companyCode?: string; companyName?: string })).toBe('COMP202604050001 - 广州测试公司')
    expect(vm.wizardForm.subjectCodeScheme).toBe('4-2-2-2')

    wrapper.unmount()
  })

  it('locks template and scheme from reference account set when submitting', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      wizardForm: {
        createMode: 'BLANK' | 'REFERENCE'
        referenceCompanyId?: string
        targetCompanyId: string
        enabledYearMonth: string
        templateCode?: string
        supervisorUserId: number
        subjectCodeScheme?: string
      }
      syncReferenceFields: () => void
      submitCreateTask: () => Promise<void>
    }

    vm.wizardForm.createMode = 'REFERENCE'
    vm.wizardForm.referenceCompanyId = 'COMP-REF'
    vm.wizardForm.targetCompanyId = 'COMPANY_B'
    vm.wizardForm.enabledYearMonth = '2026-04'
    vm.wizardForm.supervisorUserId = 2
    vm.syncReferenceFields()
    await vm.submitCreateTask()
    await flushPromises()

    expect(vm.wizardForm.templateCode).toBe('AS_2007_ENTERPRISE')
    expect(vm.wizardForm.subjectCodeScheme).toBe('4-2-2-2')
    expect(mocks.financeSystemManagementApi.createAccountSet).toHaveBeenCalledWith({
      createMode: 'REFERENCE',
      referenceCompanyId: 'COMP-REF',
      targetCompanyId: 'COMPANY_B',
      enabledYearMonth: '2026-04',
      templateCode: undefined,
      supervisorUserId: 2,
      subjectCodeScheme: undefined
    })

    vi.runOnlyPendingTimers()
    await flushPromises()
    expect(mocks.financeSystemManagementApi.getTaskStatus).toHaveBeenCalledWith('FAS202604080002')

    wrapper.unmount()
  })

  it('normalizes enabledYearMonth before submitting create task', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      wizardForm: {
        createMode: 'BLANK' | 'REFERENCE'
        targetCompanyId: string
        enabledYearMonth: string
        templateCode?: string
        supervisorUserId: number
        subjectCodeScheme?: string
      }
      buildPayload: () => {
        enabledYearMonth: string
      }
      submitCreateTask: () => Promise<void>
    }

    vm.wizardForm.createMode = 'BLANK'
    vm.wizardForm.targetCompanyId = 'COMPANY_B'
    vm.wizardForm.enabledYearMonth = ' 2022-11 '
    vm.wizardForm.templateCode = 'AS_2007_ENTERPRISE'
    vm.wizardForm.supervisorUserId = 2
    vm.wizardForm.subjectCodeScheme = '4-2-2-2'

    expect(vm.buildPayload().enabledYearMonth).toBe('2022-11')

    await vm.submitCreateTask()
    await flushPromises()

    expect(mocks.financeSystemManagementApi.createAccountSet).toHaveBeenCalledWith({
      createMode: 'BLANK',
      referenceCompanyId: undefined,
      targetCompanyId: 'COMPANY_B',
      enabledYearMonth: '2022-11',
      templateCode: 'AS_2007_ENTERPRISE',
      supervisorUserId: 2,
      subjectCodeScheme: '4-2-2-2'
    })

    wrapper.unmount()
  })

  it('keeps the account set list visible when meta loading fails', async () => {
    mocks.financeSystemManagementApi.getMeta.mockRejectedValue(new Error('账套元数据加载失败'))

    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      accountSets: Array<{ companyName: string; companyCode?: string }>
      canOpenCreateWizard: boolean
      metaErrorMessage: string
      formatCompanyDisplay: (row: { companyId: string; companyCode?: string; companyName?: string }) => string
    }

    expect(mocks.financeSystemManagementApi.listAccountSets).toHaveBeenCalledTimes(1)
    expect(vm.accountSets).toHaveLength(1)
    expect(vm.canOpenCreateWizard).toBe(false)
    expect(vm.metaErrorMessage).toBe('账套元数据加载失败')
    expect(vm.formatCompanyDisplay(vm.accountSets[0] as { companyId: string; companyCode?: string; companyName?: string })).toBe('COMP202604050001 - 广州测试公司')
    expect(wrapper.text()).toContain('账套元数据加载失败')

    wrapper.unmount()
  })

  it('blocks blank mode when templateCode is not in current options', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      wizardForm: {
        createMode: 'BLANK' | 'REFERENCE'
        templateCode?: string
        supervisorUserId: number
      }
      validateStep: (step: number) => boolean
    }

    vm.wizardForm.createMode = 'BLANK'
    vm.wizardForm.templateCode = 'AS_INVALID'
    vm.wizardForm.supervisorUserId = 2

    expect(vm.validateStep(2)).toBe(false)
    expect(mocks.elMessage.warning).toHaveBeenCalledWith('当前所选账套模板已失效，请重新选择')

    wrapper.unmount()
  })

  it('clears invalid reference option when template context is incomplete', async () => {
    mocks.financeSystemManagementApi.getMeta.mockResolvedValueOnce({
      data: {
        companyOptions: [
          { companyId: 'COMPANY_A', companyCode: 'A01', companyName: '广州测试公司', label: 'A01 - 广州测试公司', value: 'COMPANY_A' }
        ],
        supervisorOptions: [
          { value: '2', label: '李会计' }
        ],
        templateOptions: [
          {
            templateCode: 'AS_2007_ENTERPRISE',
            templateName: '2007 企业会计制度',
            accountingStandard: '2007 企业会计制度',
            level1SubjectCount: 60,
            commonSubjectCount: 12
          }
        ],
        referenceOptions: [
          {
            companyId: 'COMP-REF',
            companyName: '参考账套公司',
            templateCode: '',
            templateName: '2007 企业会计制度',
            enabledYearMonth: '2026-01',
            subjectCodeScheme: '4-2-2-2',
            label: '参考账套公司 / 2007 企业会计制度 / 2026-01'
          }
        ],
        defaultSubjectCodeScheme: '4-2-2-2'
      }
    })

    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      wizardForm: {
        createMode: 'BLANK' | 'REFERENCE'
        referenceCompanyId?: string
      }
      validateStep: (step: number) => boolean
    }

    vm.wizardForm.createMode = 'REFERENCE'
    vm.wizardForm.referenceCompanyId = 'COMP-REF'
    await flushPromises()

    expect(vm.wizardForm.referenceCompanyId).toBe('')
    expect(vm.validateStep(0)).toBe(false)
    expect(mocks.elMessage.warning).toHaveBeenCalledWith('所选参照账套缺少账套模板，请重新选择')

    wrapper.unmount()
  })

  it('clears stale template selection after meta reload', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      wizardForm: {
        createMode: 'BLANK' | 'REFERENCE'
        templateCode?: string
      }
      loadMeta: () => Promise<void>
    }

    vm.wizardForm.createMode = 'BLANK'
    vm.wizardForm.templateCode = 'AS_2007_ENTERPRISE'
    mocks.financeSystemManagementApi.getMeta.mockResolvedValueOnce({
      data: {
        companyOptions: [
          { companyId: 'COMPANY_A', companyCode: 'A01', companyName: '广州测试公司', label: 'A01 - 广州测试公司', value: 'COMPANY_A' }
        ],
        supervisorOptions: [
          { value: '2', label: '李会计' }
        ],
        templateOptions: [],
        referenceOptions: [],
        defaultSubjectCodeScheme: '4-2-2-2'
      }
    })

    await vm.loadMeta()
    await flushPromises()

    expect(vm.wizardForm.templateCode).toBe('')

    wrapper.unmount()
  })

  it('shows backend chinese validation message when create task fails', async () => {
    mocks.financeSystemManagementApi.createAccountSet.mockRejectedValueOnce(new Error('账套模板已停用'))

    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      wizardForm: {
        createMode: 'BLANK' | 'REFERENCE'
        targetCompanyId: string
        enabledYearMonth: string
        templateCode?: string
        supervisorUserId: number
        subjectCodeScheme?: string
      }
      submitCreateTask: () => Promise<void>
    }

    vm.wizardForm.createMode = 'BLANK'
    vm.wizardForm.targetCompanyId = 'COMPANY_B'
    vm.wizardForm.enabledYearMonth = '2026-04'
    vm.wizardForm.templateCode = 'AS_2007_ENTERPRISE'
    vm.wizardForm.supervisorUserId = 2
    vm.wizardForm.subjectCodeScheme = '4-2-2-2'

    await vm.submitCreateTask()
    await flushPromises()

    expect(mocks.elMessage.error).toHaveBeenCalledWith('账套模板已停用')

    wrapper.unmount()
  })

  it('shows a readable error when the account set list fails', async () => {
    mocks.financeSystemManagementApi.listAccountSets.mockRejectedValue(new Error('账套列表加载失败'))

    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      accountSets: Array<unknown>
      listErrorMessage: string
    }

    expect(vm.accountSets).toHaveLength(0)
    expect(vm.listErrorMessage).toBe('账套列表加载失败')
    expect(wrapper.text()).toContain('账套列表加载失败')

    wrapper.unmount()
  })
})
