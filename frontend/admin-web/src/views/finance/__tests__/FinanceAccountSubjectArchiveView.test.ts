import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, inject, provide, reactive } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import FinanceAccountSubjectArchiveView from '@/views/finance/FinanceAccountSubjectArchiveView.vue'

const mocks = vi.hoisted(() => ({
  financeArchiveApi: {
    getAccountSubjectMeta: vi.fn(),
    listAccountSubjects: vi.fn(),
    getAccountSubjectDetail: vi.fn(),
    createAccountSubject: vi.fn(),
    updateAccountSubject: vi.fn(),
    updateAccountSubjectStatus: vi.fn(),
    updateAccountSubjectClose: vi.fn()
  },
  financeCompany: {
    currentCompanyId: 'COMPANY_A',
    currentCompanyName: '??????',
    currentCompanyLabel: 'A01 - 广州测试公司',
    currentCompanyHasActiveAccountSet: true,
    registerSwitchGuard: vi.fn(),
    unregisterSwitchGuard: vi.fn()
  },
  elMessage: {
    success: vi.fn(),
    warning: vi.fn(),
    error: vi.fn()
  },
  elMessageBox: {
    confirm: vi.fn()
  }
}))

const financeCompanyStore = reactive(mocks.financeCompany)

vi.mock('@/api', () => ({
  financeArchiveApi: mocks.financeArchiveApi
}))

vi.mock('@/stores/financeCompany', () => ({
  useFinanceCompanyStore: () => financeCompanyStore
}))

vi.mock('@/utils/permissions', () => ({
  hasPermission: () => true,
  readStoredUser: () => ({
    permissionCodes: [
      'finance:archives:account_subjects:view',
      'finance:archives:account_subjects:create',
      'finance:archives:account_subjects:edit',
      'finance:archives:account_subjects:disable',
      'finance:archives:account_subjects:close'
    ]
  })
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage,
  ElMessageBox: mocks.elMessageBox
}))

const SimpleStub = defineComponent({
  template: '<div><slot /><slot name="append" /><slot name="footer" /><slot name="title" /></div>'
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
  template: '<div><slot /><slot name="append" /><slot name="footer" /><slot name="title" /></div>'
})

const TableColumnStub = defineComponent({
  props: {
    prop: {
      type: String,
      default: ''
    }
  },
  setup() {
    const rows = inject<any[]>('tableRows', [])
    return { rows }
  },
  template: `
    <div>
      <template v-for="row in rows" :key="String((row.subject_code || '') + prop)">
        <slot :row="row">
          <span>{{ prop ? row[prop] : '' }}</span>
        </slot>
      </template>
    </div>
  `
})

async function mountView() {
  const wrapper = mount(FinanceAccountSubjectArchiveView, {
    global: {
      stubs: {
        'el-card': SimpleStub,
        'el-button': SimpleStub,
        'el-input': SimpleStub,
        'el-select': SimpleStub,
        'el-option': true,
        'el-table': TableStub,
        'el-table-column': TableColumnStub,
        'el-tag': SimpleStub,
        'el-drawer': SimpleStub,
        'el-form': SimpleStub,
        'el-form-item': SimpleStub,
        'el-collapse': SimpleStub,
        'el-collapse-item': SimpleStub,
        'el-date-picker': SimpleStub,
        'el-input-number': SimpleStub,
        'el-switch': SimpleStub
      },
      directives: {
        loading: () => undefined
      }
    }
  })
  await flushPromises()
  return wrapper
}

describe('FinanceAccountSubjectArchiveView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    financeCompanyStore.currentCompanyId = 'COMPANY_A'
    financeCompanyStore.currentCompanyName = '??????'
    financeCompanyStore.currentCompanyLabel = 'A01 - 骞垮窞娴嬭瘯鍏徃'
    financeCompanyStore.currentCompanyHasActiveAccountSet = true
    mocks.elMessageBox.confirm.mockResolvedValue(true)
    mocks.financeArchiveApi.getAccountSubjectMeta.mockResolvedValue({
      data: {
        subjectCategoryOptions: [
          { value: 'ASSET', label: '??' },
          { value: 'LIABILITY', label: '??' }
        ],
        statusOptions: [
          { value: '1', label: '??' },
          { value: '0', label: '??' }
        ],
        closeStatusOptions: [
          { value: '0', label: '???' },
          { value: '1', label: '???' }
        ],
        yesNoOptions: [
          { value: '1', label: '?' },
          { value: '0', label: '?' }
        ]
      }
    })
    mocks.financeArchiveApi.listAccountSubjects.mockResolvedValue({
      data: [
        {
          subject_code: '1001',
          subject_name: '????',
          subject_level: 1,
          subject_category: 'ASSET',
          status: 1,
          bclose: 0,
          leaf_flag: 1,
          auxiliary_summary: '?',
          cash_bank_summary: '???? / ???',
          children: []
        }
      ]
    })
    mocks.financeArchiveApi.createAccountSubject.mockResolvedValue({ data: {} })
    mocks.financeArchiveApi.updateAccountSubjectStatus.mockResolvedValue({ data: true })
    mocks.financeArchiveApi.updateAccountSubjectClose.mockResolvedValue({ data: true })
  })

  it('loads meta and subject tree on mount', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      subjectTree: Array<{ subject_code: string }>
      meta: { subjectCategoryOptions: Array<{ value: string }> }
    }

    expect(mocks.financeArchiveApi.getAccountSubjectMeta).toHaveBeenCalledTimes(1)
    expect(mocks.financeArchiveApi.listAccountSubjects).toHaveBeenCalledWith({
      companyId: 'COMPANY_A',
      keyword: undefined,
      subjectCategory: undefined,
      status: undefined,
      bclose: undefined
    })
    expect(vm.subjectTree[0]?.subject_code).toBe('1001')
    expect(vm.meta.subjectCategoryOptions[0]?.value).toBe('ASSET')

    wrapper.unmount()
  })

  it('builds create payload with subject specific fields', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      openCreateDrawer: (parentSubjectCode?: string) => void
      form: Record<string, unknown>
      buildPayload: () => Record<string, unknown>
    }

    vm.openCreateDrawer('1001')
    vm.form.subject_code = '100101'
    vm.form.subject_name = '???'
    vm.form.subject_category = 'ASSET'
    vm.form.cclassany = 'ASSET'
    vm.form.bcash = 1
    vm.form.br = 1
    vm.form.cgather = '1'

    expect(vm.buildPayload()).toMatchObject({
      subject_code: '100101',
      subject_name: '???',
      parent_subject_code: '1001',
      subject_category: 'ASSET',
      bcash: 1,
      br: 1,
      cgather: '1'
    })

    wrapper.unmount()
  })

  it('submits status and close actions through dedicated endpoints', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      toggleStatus: (row: Record<string, unknown>) => Promise<void>
      toggleClose: (row: Record<string, unknown>) => Promise<void>
    }

    await vm.toggleStatus({ subject_code: '1001', subject_name: '????', status: 1, bclose: 0 } as Record<string, unknown>)
    await vm.toggleClose({ subject_code: '1001', subject_name: '????', status: 1, bclose: 0 } as Record<string, unknown>)
    await flushPromises()

    expect(mocks.financeArchiveApi.updateAccountSubjectStatus).toHaveBeenCalledWith('COMPANY_A', '1001', 0)
    expect(mocks.financeArchiveApi.updateAccountSubjectClose).toHaveBeenCalledWith('COMPANY_A', '1001', 1)

    wrapper.unmount()
  })

  it('shows a readable notice and skips querying subjects when the company has no account set', async () => {
    financeCompanyStore.currentCompanyHasActiveAccountSet = false

    const wrapper = await mountView()

    expect(mocks.financeArchiveApi.listAccountSubjects).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('当前公司未创建账套，请切换公司或先建账。')

    wrapper.unmount()
  })

  it('reloads the subject list when the finance company context changes', async () => {
    const wrapper = await mountView()

    financeCompanyStore.currentCompanyId = 'COMPANY_B'
    financeCompanyStore.currentCompanyName = '深圳测试公司'
    financeCompanyStore.currentCompanyLabel = 'B01 - 深圳测试公司'
    await flushPromises()

    expect(mocks.financeArchiveApi.listAccountSubjects).toHaveBeenCalledWith({
      companyId: 'COMPANY_A',
      keyword: undefined,
      subjectCategory: undefined,
      status: undefined,
      bclose: undefined
    })
    expect(mocks.financeArchiveApi.listAccountSubjects).toHaveBeenCalledWith({
      companyId: 'COMPANY_B',
      keyword: undefined,
      subjectCategory: undefined,
      status: undefined,
      bclose: undefined
    })

    wrapper.unmount()
  })
})
