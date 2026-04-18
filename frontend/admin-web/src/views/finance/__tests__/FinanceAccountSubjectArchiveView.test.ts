import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, inject, provide, reactive } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import FinanceAccountSubjectArchiveView from '@/views/finance/FinanceAccountSubjectArchiveView.vue'

const mocks = vi.hoisted(() => ({
  financeArchiveApi: {
    getAccountSubjectMeta: vi.fn(),
    getProjectArchiveMeta: vi.fn(),
    listAccountSubjects: vi.fn(),
    getAccountSubjectDetail: vi.fn(),
    getAccountSubjectDerivedDefaults: vi.fn(),
    createAccountSubject: vi.fn(),
    updateAccountSubject: vi.fn(),
    updateAccountSubjectStatus: vi.fn(),
    updateAccountSubjectClose: vi.fn()
  },
  financeCompany: {
    currentCompanyId: 'COMPANY_A',
    currentCompanyName: '测试公司',
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

const FormItemStub = defineComponent({
  props: {
    label: {
      type: String,
      default: ''
    }
  },
  template: '<div><span v-if="label">{{ label }}</span><slot /></div>'
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

const PaginationStub = defineComponent({
  props: {
    currentPage: {
      type: Number,
      default: 1
    },
    pageSize: {
      type: Number,
      default: 10
    },
    pageSizes: {
      type: Array,
      default: () => []
    },
    total: {
      type: Number,
      default: 0
    }
  },
  emits: ['update:currentPage', 'update:pageSize'],
  template: `
    <div data-testid="pagination">
      <span data-testid="pagination-total">{{ total }}</span>
      <span data-testid="pagination-current">{{ currentPage }}</span>
      <span data-testid="pagination-size">{{ pageSize }}</span>
      <button data-testid="pagination-next" type="button" @click="$emit('update:currentPage', currentPage + 1)">next</button>
      <button
        v-for="size in pageSizes"
        :key="size"
        type="button"
        :data-testid="\`pagination-size-\${size}\`"
        @click="$emit('update:pageSize', size)"
      >
        {{ size }}
      </button>
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
        'el-form-item': FormItemStub,
        'el-collapse': SimpleStub,
        'el-collapse-item': SimpleStub,
        'el-date-picker': SimpleStub,
        'el-input-number': SimpleStub,
        'el-switch': SimpleStub,
        'el-pagination': PaginationStub
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
    financeCompanyStore.currentCompanyName = '测试公司'
    financeCompanyStore.currentCompanyLabel = 'A01 - 广州测试公司'
    financeCompanyStore.currentCompanyHasActiveAccountSet = true
    mocks.elMessageBox.confirm.mockResolvedValue(true)
    mocks.financeArchiveApi.getAccountSubjectMeta.mockResolvedValue({
      data: {
        subjectCategoryOptions: [
          { value: 'ASSET', label: '资产' },
          { value: 'LIABILITY', label: '负债' },
          { value: 'PROFIT', label: '损益' }
        ],
        statusOptions: [
          { value: '1', label: '启用' },
          { value: '0', label: '停用' }
        ],
        closeStatusOptions: [
          { value: '0', label: '未封存' },
          { value: '1', label: '已封存' }
        ],
        yesNoOptions: [
          { value: '1', label: '是' },
          { value: '0', label: '否' }
        ]
      }
    })
    mocks.financeArchiveApi.getProjectArchiveMeta.mockResolvedValue({
      data: {
        statusOptions: [],
        closeStatusOptions: [],
        projectClassOptions: [
          { value: '01', label: '01 / 市场项目' },
          { value: '97', label: '97 / 研发项目' }
        ]
      }
    })
    mocks.financeArchiveApi.listAccountSubjects.mockResolvedValue({
      data: [
        {
          subject_code: '1001',
          subject_name: '库存现金',
          subject_level: 1,
          subject_category: 'ASSET',
          balance_direction: 'DEBIT',
          status: 1,
          bclose: 0,
          leaf_flag: 0,
          auxiliary_summary: '未设置',
          cash_bank_summary: '现金科目 / 日记账',
          children: [
            {
              subject_code: '100101',
              subject_name: '库存现金-子科目',
              parent_subject_code: '1001',
              subject_level: 2,
              subject_category: 'ASSET',
              balance_direction: 'DEBIT',
              status: 1,
              bclose: 0,
              leaf_flag: 1,
              auxiliary_summary: '人员',
              cash_bank_summary: '现金科目',
              children: []
            }
          ]
        },
        {
          subject_code: '2202',
          subject_name: '应付账款',
          subject_level: 1,
          subject_category: 'LIABILITY',
          balance_direction: 'CREDIT',
          status: 1,
          bclose: 0,
          leaf_flag: 1,
          auxiliary_summary: '未设置',
          cash_bank_summary: '未设置',
          children: []
        },
        {
          subject_code: '6602',
          subject_name: '管理费用',
          subject_level: 1,
          subject_category: 'PROFIT',
          balance_direction: 'DEBIT',
          status: 1,
          bclose: 0,
          leaf_flag: 1,
          auxiliary_summary: '未设置',
          cash_bank_summary: '未设置',
          children: []
        },
        ...Array.from({ length: 9 }, (_, index) => ({
          subject_code: `50${String(index + 1).padStart(2, '0')}`,
          subject_name: `附加科目${index + 1}`,
          subject_level: 1,
          subject_category: 'ASSET',
          balance_direction: 'DEBIT',
          status: 1,
          bclose: 0,
          leaf_flag: 1,
          auxiliary_summary: '未设置',
          cash_bank_summary: '未设置',
          children: []
        }))
      ]
    })
    mocks.financeArchiveApi.getAccountSubjectDerivedDefaults.mockImplementation(async (_companyId: string, subjectCode: string) => {
      if (subjectCode === '5401') {
        return {
          data: {
            subject_level: 1,
            subject_category: 'PROFIT',
            balance_direction: 'DEBIT',
            leaf_flag: 1,
            matched_by: 'TEMPLATE_EXACT'
          }
        }
      }
      if (subjectCode === '6602') {
        return {
          data: {
            subject_level: 1,
            subject_category: 'PROFIT',
            balance_direction: 'DEBIT',
            leaf_flag: 1,
            matched_by: 'TEMPLATE_EXACT'
          }
        }
      }
      if (subjectCode === '220201') {
        return {
          data: {
            parent_subject_code: '2202',
            subject_level: 2,
            subject_category: 'LIABILITY',
            balance_direction: 'CREDIT',
            leaf_flag: 1,
            matched_by: 'EXISTING_PARENT'
          }
        }
      }
      if (subjectCode === '112203') {
        return {
          data: {
            parent_subject_code: '1122',
            subject_level: 2,
            subject_category: 'ASSET',
            balance_direction: 'DEBIT',
            leaf_flag: 1,
            matched_by: 'EXISTING_PARENT'
          }
        }
      }
      if (subjectCode === '100101') {
        return {
          data: {
            parent_subject_code: '1001',
            subject_level: 2,
            subject_category: 'ASSET',
            balance_direction: 'DEBIT',
            leaf_flag: 1,
            matched_by: 'EXISTING_PARENT'
          }
        }
      }
      return {
        data: {
          balance_direction: 'DEBIT',
          leaf_flag: 1,
          matched_by: 'UNMATCHED'
        }
      }
    })
    mocks.financeArchiveApi.createAccountSubject.mockResolvedValue({ data: {} })
    mocks.financeArchiveApi.getAccountSubjectDetail.mockResolvedValue({
      data: {
        subject_code: '100101',
        subject_name: '库存现金-子科目',
        parent_subject_code: '1001',
        subject_level: 2,
        subject_category: 'ASSET',
        balance_direction: 'DEBIT',
        bperson: 1,
        bitem: 1,
        cass_item: '77',
        itrans: 8,
        bReport: 1,
        bGCJS: 1,
        iViewItem: 3,
        cother: 'ERP',
        iotherused: 5,
        bCashItem: 1
      }
    })
    mocks.financeArchiveApi.updateAccountSubjectStatus.mockResolvedValue({ data: true })
    mocks.financeArchiveApi.updateAccountSubjectClose.mockResolvedValue({ data: true })
  })
  it('loads meta and subject tree on mount', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      subjectTree: Array<{ subject_code: string }>
      paginatedSubjectTree: Array<{ subject_code: string }>
      meta: { subjectCategoryOptions: Array<{ value: string }> }
    }

    expect(mocks.financeArchiveApi.getAccountSubjectMeta).toHaveBeenCalledTimes(1)
    expect(mocks.financeArchiveApi.getProjectArchiveMeta).toHaveBeenCalledWith('COMPANY_A')
    expect(mocks.financeArchiveApi.listAccountSubjects).toHaveBeenCalledWith({
      companyId: 'COMPANY_A',
      keyword: undefined,
      subjectCategory: undefined,
      status: undefined,
      bclose: undefined
    })
    expect(vm.subjectTree[0]?.subject_code).toBe('1001')
    expect(vm.subjectTree).toHaveLength(12)
    expect(vm.paginatedSubjectTree).toHaveLength(10)
    expect(vm.meta.subjectCategoryOptions[0]?.value).toBe('ASSET')
    expect(wrapper.text()).not.toContain('详情')
    expect(wrapper.text()).not.toContain('新增下级')

    wrapper.unmount()
  })

  it('paginates account subjects by root nodes and keeps child trees together', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      paginatedSubjectTree: Array<{ subject_code: string; children?: Array<{ subject_code: string }> }>
    }

    expect(wrapper.get('[data-testid="pagination-total"]').text()).toBe('12')
    expect(vm.paginatedSubjectTree[0]?.subject_code).toBe('1001')
    expect(vm.paginatedSubjectTree[0]?.children?.[0]?.subject_code).toBe('100101')

    await wrapper.get('[data-testid="pagination-next"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="pagination-current"]').text()).toBe('2')
    expect(vm.paginatedSubjectTree).toHaveLength(2)
    expect(vm.paginatedSubjectTree.some((item) => item.subject_code === '100101')).toBe(false)
  })

  it('updates root-level pagination when the page size changes', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      paginatedSubjectTree: Array<{ subject_code: string }>
    }

    await wrapper.get('[data-testid="pagination-size-20"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="pagination-size"]').text()).toBe('20')
    expect(vm.paginatedSubjectTree).toHaveLength(12)
  })

  it('treats 4-digit codes as root subjects without parent warning', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      openCreateDrawer: (parentSubjectCode?: string) => void
      loadDerivedDefaults: () => Promise<void>
      form: Record<string, unknown>
      missingAutoParent: boolean
      parentDisplayText: string
      subjectLevelPreview?: number
      categoryPreviewValue?: string
      balanceDirectionPreview: string
      leafFlagPreviewText: string
      derivedDefaults: { matched_by?: string } | null
    }

    vm.openCreateDrawer('2202')
    vm.form.subject_code = '5401'
    await vm.loadDerivedDefaults()
    await flushPromises()

    expect(vm.missingAutoParent).toBe(false)
    expect(vm.parentDisplayText).toBe('无上级科目')
    expect(vm.subjectLevelPreview).toBe(1)
    expect(vm.categoryPreviewValue).toBe('PROFIT')
    expect(vm.balanceDirectionPreview).toBe('DEBIT')
    expect(vm.leafFlagPreviewText).toBe('是')
    expect(vm.derivedDefaults?.matched_by).toBe('TEMPLATE_EXACT')

    wrapper.unmount()
  })

  it('shows matched parent details for child subjects', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      openCreateDrawer: (parentSubjectCode?: string) => void
      loadDerivedDefaults: () => Promise<void>
      form: Record<string, unknown>
      parentDisplayText: string
      subjectLevelPreview?: number
      categoryPreviewValue?: string
      balanceDirectionPreview: string
      missingAutoParent: boolean
    }

    vm.openCreateDrawer()
    vm.form.subject_code = '220201'
    await vm.loadDerivedDefaults()
    await flushPromises()

    expect(vm.parentDisplayText).toContain('2202')
    expect(vm.subjectLevelPreview).toBe(2)
    expect(vm.categoryPreviewValue).toBe('LIABILITY')
    expect(vm.balanceDirectionPreview).toBe('CREDIT')
    expect(vm.missingAutoParent).toBe(false)

    wrapper.unmount()
  })

  it('blocks saving when a non-root subject still cannot match a parent', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      openCreateDrawer: (parentSubjectCode?: string) => void
      loadDerivedDefaults: () => Promise<void>
      form: Record<string, unknown>
      saveSubject: () => Promise<void>
      missingAutoParent: boolean
    }

    vm.openCreateDrawer()
    vm.form.subject_code = '990001'
    vm.form.subject_name = '未匹配科目'
    await vm.loadDerivedDefaults()
    await flushPromises()

    expect(vm.missingAutoParent).toBe(true)

    await vm.saveSubject()
    await flushPromises()

    expect(mocks.elMessage.warning).toHaveBeenCalledWith('请先创建上级科目，再新增当前科目')
    expect(mocks.financeArchiveApi.createAccountSubject).not.toHaveBeenCalled()

    wrapper.unmount()
  })

  it('shows derived parent details for 112203 without parent warning', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      openCreateDrawer: (parentSubjectCode?: string) => void
      loadDerivedDefaults: () => Promise<void>
      form: Record<string, unknown>
      parentDisplayText: string
      subjectLevelPreview?: number
      categoryPreviewValue?: string
      balanceDirectionPreview: string
      leafFlagPreviewText: string
      missingAutoParent: boolean
    }

    vm.openCreateDrawer()
    vm.form.subject_code = '112203'
    await vm.loadDerivedDefaults()
    await flushPromises()

    expect(vm.parentDisplayText).toContain('1122')
    expect(vm.subjectLevelPreview).toBe(2)
    expect(vm.categoryPreviewValue).toBe('ASSET')
    expect(vm.balanceDirectionPreview).toBe('DEBIT')
    expect(vm.leafFlagPreviewText).toBe('是')
    expect(vm.missingAutoParent).toBe(false)
    expect(mocks.elMessage.error).not.toHaveBeenCalledWith('系统异常，请稍后重试')

    wrapper.unmount()
  })

  it('uses direct 0/1 auxiliary switches and clears cass_item when project accounting is disabled', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      openCreateDrawer: (parentSubjectCode?: string) => void
      loadDerivedDefaults: () => Promise<void>
      form: Record<string, unknown>
      projectClassOptionsForDisplay: Array<{ value: string; label: string }>
      updateAuxiliaryFlag: (field: string, value: number) => void
      buildPayload: () => Record<string, unknown>
    }

    vm.openCreateDrawer('1001')
    vm.form.subject_code = '100101'
    vm.form.subject_name = '库存现金-子科目'
    await vm.loadDerivedDefaults()
    await flushPromises()

    vm.form.bperson = 1
    vm.form.bcus = 0
    vm.form.bsup = 0
    vm.form.bdept = 0
    vm.form.bitem = 1
    vm.form.cass_item = '01'

    expect(vm.projectClassOptionsForDisplay.map((item) => item.value)).toEqual(['01', '97'])
    expect(vm.buildPayload()).toMatchObject({
      parent_subject_code: '1001',
      bperson: 1,
      bcus: 0,
      bsup: 0,
      bdept: 0,
      bitem: 1,
      cass_item: '01'
    })

    vm.updateAuxiliaryFlag('bitem', 0)
    await flushPromises()

    expect(vm.form.cass_item).toBe('')
    expect(vm.buildPayload()).toMatchObject({
      bperson: 1,
      bitem: 0
    })
    expect(vm.buildPayload().cass_item).toBeUndefined()

    wrapper.unmount()
  })

  it('keeps hidden control values in payload and preserves legacy cass_item display in edit mode', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as unknown as {
      form: Record<string, unknown>
      projectClassOptionsForDisplay: Array<{ value: string; label: string }>
      buildPayload: () => Record<string, unknown>
    }

    Object.assign(vm.form, {
      bitem: 1,
      cass_item: '77',
      itrans: 8,
      bReport: 1,
      bGCJS: 1,
      iViewItem: 3,
      cother: 'ERP',
      iotherused: 5,
      bCashItem: 1
    })
    await flushPromises()

    expect(wrapper.text()).not.toContain('特殊标记')
    expect(wrapper.text()).not.toContain('视图项目类型')
    expect(wrapper.text()).not.toContain('工程结算')
    expect(wrapper.text()).not.toContain('转账通知')
    expect(wrapper.text()).toContain('受控系统')
    expect(wrapper.text()).toContain('其他系统已使用')
    expect(wrapper.text()).toContain('现金流量科目')
    expect(vm.projectClassOptionsForDisplay[0]).toEqual({ value: '77', label: '77' })
    expect(vm.buildPayload()).toMatchObject({
      cass_item: '77',
      itrans: 8,
      bReport: 1,
      bGCJS: 1,
      iViewItem: 3,
      cother: 'ERP',
      iotherused: 5,
      bCashItem: 1
    })

    wrapper.unmount()
  })
})
