import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import FinanceSystemEnableView from '@/views/finance/FinanceSystemEnableView.vue'

const mocks = vi.hoisted(() => ({
  financeSystemManagementApi: {
    getModuleEnables: vi.fn(),
    toggleModuleEnable: vi.fn(),
    backupModuleData: vi.fn(),
    getModuleBackupRecords: vi.fn(),
    clearModuleData: vi.fn()
  },
  financeCompany: {
    currentCompanyId: 'COMPANY_A',
    currentCompanyName: '广州测试公司',
    currentCompanyLabel: 'A01 - 广州测试公司',
    currentCompanyHasActiveAccountSet: true,
    refreshContext: vi.fn()
  },
  elMessage: {
    success: vi.fn(),
    error: vi.fn()
  },
  elMessageBox: {
    confirm: vi.fn(() => Promise.resolve())
  }
}))

vi.mock('@/api', () => ({
  financeSystemManagementApi: mocks.financeSystemManagementApi
}))

vi.mock('@/stores/financeCompany', () => ({
  useFinanceCompanyStore: () => mocks.financeCompany
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessage: mocks.elMessage,
    ElMessageBox: mocks.elMessageBox
  }
})

const CardStub = defineComponent({
  template: '<div class="card-stub"><slot /></div>'
})

const AlertStub = defineComponent({
  props: {
    title: { type: String, default: '' },
    description: { type: String, default: '' }
  },
  template: '<div class="alert-stub">{{ title }} {{ description }}</div>'
})

const TagStub = defineComponent({
  template: '<span class="tag-stub"><slot /></span>'
})

const ButtonStub = defineComponent({
  props: {
    disabled: { type: Boolean, default: false },
    loading: { type: Boolean, default: false },
    type: { type: String, default: '' },
    plain: { type: Boolean, default: false }
  },
  emits: ['click'],
  template: `
    <button
      class="button-stub"
      :data-type="type"
      :data-plain="plain ? 'true' : 'false'"
      type="button"
      :disabled="disabled || loading"
      @click="$emit('click')"
    >
      <slot />
    </button>
  `
})

const SwitchStub = defineComponent({
  props: {
    modelValue: { type: Boolean, default: false },
    disabled: { type: Boolean, default: false }
  },
  emits: ['change'],
  template: `
    <button
      class="switch-stub"
      type="button"
      :disabled="disabled"
      @click="$emit('change', !modelValue)"
    >
      {{ modelValue ? '开' : '关' }}
    </button>
  `
})

const DialogStub = defineComponent({
  props: {
    modelValue: { type: Boolean, default: false },
    title: { type: String, default: '' }
  },
  emits: ['update:modelValue'],
  template: `
    <div v-if="modelValue" class="dialog-stub">
      <div class="dialog-title">{{ title }}</div>
      <slot />
    </div>
  `
})

const TableStub = defineComponent({
  props: {
    data: { type: Array, default: () => [] }
  },
  template: `
    <div class="table-stub">
      <slot />
      <div v-for="row in data" :key="row.id ?? row.moduleCode ?? row.backupFileName" class="table-row">
        {{ row.backupStartedAt }} {{ row.backupFilePath }} {{ row.backupFileName }} {{ row.backupUserName }} {{ row.backupStatus }}
      </div>
    </div>
  `
})

const TableColumnStub = defineComponent({
  template: '<div><slot :row="{}" /></div>'
})

async function mountView() {
  const wrapper = mount(FinanceSystemEnableView, {
    global: {
      directives: {
        loading: () => undefined
      },
      stubs: {
        'el-button': ButtonStub,
        'el-card': CardStub,
        'el-alert': AlertStub,
        'el-tag': TagStub,
        'el-switch': SwitchStub,
        'el-dialog': DialogStub,
        'el-table': TableStub,
        'el-table-column': TableColumnStub
      }
    }
  })
  await flushPromises()
  return wrapper
}

function buildModules() {
  return [
    {
      companyId: 'COMPANY_A',
      moduleCode: 'GENERAL_LEDGER',
      moduleName: '总账',
      enabled: true,
      implemented: true,
      toggleAllowed: true,
      disableAllowed: false,
      backupAllowed: true,
      clearAllowed: false,
      backupRecordAllowed: true,
      blockedMessage: '请备份数据后清除所有内容再关闭',
      clearBlockedMessage: '总账不支持清除数据',
      placeholderActions: ['BACKUP_DATA', 'CLEAR_DATA']
    },
    {
      companyId: 'COMPANY_A',
      moduleCode: 'FIXED_ASSETS',
      moduleName: '固定资产',
      enabled: true,
      implemented: true,
      toggleAllowed: true,
      disableAllowed: false,
      backupAllowed: true,
      clearAllowed: true,
      backupRecordAllowed: true,
      blockedMessage: '请备份数据后清除所有内容再关闭',
      clearBlockedMessage: '请先备份再清理',
      placeholderActions: ['BACKUP_DATA', 'CLEAR_DATA']
    },
    {
      companyId: 'COMPANY_A',
      moduleCode: 'CASH_MANAGEMENT',
      moduleName: '出纳管理',
      enabled: false,
      implemented: false,
      toggleAllowed: false,
      disableAllowed: false,
      backupAllowed: false,
      clearAllowed: false,
      backupRecordAllowed: false,
      blockedMessage: '建设中',
      clearBlockedMessage: '当前模块尚未开放数据清理',
      placeholderActions: ['BACKUP_DATA', 'CLEAR_DATA']
    }
  ]
}

describe('FinanceSystemEnableView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.financeCompany.currentCompanyId = 'COMPANY_A'
    mocks.financeCompany.currentCompanyName = '广州测试公司'
    mocks.financeCompany.currentCompanyLabel = 'A01 - 广州测试公司'
    mocks.financeCompany.currentCompanyHasActiveAccountSet = true
    mocks.financeCompany.refreshContext.mockResolvedValue(undefined)
    mocks.financeSystemManagementApi.getModuleEnables.mockResolvedValue({
      data: {
        companyId: 'COMPANY_A',
        companyName: '广州测试公司',
        modules: buildModules()
      }
    })
    mocks.financeSystemManagementApi.toggleModuleEnable.mockResolvedValue({
      data: {
        companyId: 'COMPANY_A',
        companyName: '广州测试公司',
        modules: buildModules().map((item) =>
          item.moduleCode === 'GENERAL_LEDGER'
            ? { ...item, enabled: false, disableAllowed: true, blockedMessage: undefined }
            : item
        )
      }
    })
    mocks.financeSystemManagementApi.backupModuleData.mockResolvedValue({
      data: {
        id: 1,
        companyId: 'COMPANY_A',
        moduleCode: 'GENERAL_LEDGER',
        backupFileName: 'COMPANY_A-GENERAL_LEDGER-20260518_120000.sql',
        backupFilePath: 'C:/backup/COMPANY_A-GENERAL_LEDGER-20260518_120000.sql',
        backupStatus: 'SUCCESS',
        backupStartedAt: '2026-05-18 12:00:00',
        backupUserName: '张会计'
      }
    })
    mocks.financeSystemManagementApi.getModuleBackupRecords.mockResolvedValue({
      data: [
        {
          id: 1,
          companyId: 'COMPANY_A',
          moduleCode: 'GENERAL_LEDGER',
          backupFileName: 'COMPANY_A-GENERAL_LEDGER-20260518_120000.sql',
          backupFilePath: 'C:/backup/COMPANY_A-GENERAL_LEDGER-20260518_120000.sql',
          backupStatus: 'SUCCESS',
          backupStartedAt: '2026-05-18 12:00:00',
          backupUserName: '张会计'
        }
      ]
    })
    mocks.financeSystemManagementApi.clearModuleData.mockResolvedValue({
      data: {
        companyId: 'COMPANY_A',
        companyName: '广州测试公司',
        modules: buildModules().map((item) =>
          item.moduleCode === 'FIXED_ASSETS'
            ? { ...item, enabled: true, disableAllowed: true, blockedMessage: undefined }
            : item
        )
      }
    })
  })

  it('shows the no-account-set hint and skips loading when the current company has no active account set', async () => {
    mocks.financeCompany.currentCompanyHasActiveAccountSet = false

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('当前公司未创建账套')
    expect(mocks.financeSystemManagementApi.getModuleEnables).not.toHaveBeenCalled()
  })

  it('loads module cards and renders implemented and placeholder states', async () => {
    const wrapper = await mountView()

    expect(mocks.financeSystemManagementApi.getModuleEnables).toHaveBeenCalledWith('COMPANY_A')
    expect(wrapper.text()).toContain('系统启用')
    expect(wrapper.text()).toContain('总账')
    expect(wrapper.text()).toContain('固定资产')
    expect(wrapper.text()).toContain('出纳管理')
    expect(wrapper.text()).toContain('建设中')
  })

  it('toggles implemented modules and refreshes the finance context', async () => {
    const wrapper = await mountView()

    await wrapper.find('.switch-stub').trigger('click')
    await flushPromises()

    expect(mocks.financeSystemManagementApi.toggleModuleEnable).toHaveBeenCalledWith({
      companyId: 'COMPANY_A',
      moduleCode: 'GENERAL_LEDGER',
      enabled: false
    })
    expect(mocks.financeCompany.refreshContext).toHaveBeenCalledWith('COMPANY_A')
    expect(mocks.elMessage.success).toHaveBeenCalledWith('总账已关闭')
  })

  it('hides clear button for general ledger and keeps full actions for fixed assets', async () => {
    const wrapper = await mountView()
    const buttonTexts = wrapper.findAll('.button-stub').map((item) => item.text())

    expect(buttonTexts.filter((item) => item === '清除数据')).toHaveLength(2)
    expect(wrapper.text()).toContain('备份数据')
    expect(wrapper.text()).toContain('备份记录')
  })

  it('backs up module data through the backup action', async () => {
    const wrapper = await mountView()
    const backupButtons = wrapper.findAll('.button-stub').filter((item) => item.text() === '备份数据')

    await backupButtons[0].trigger('click')
    await flushPromises()

    expect(mocks.financeSystemManagementApi.backupModuleData).toHaveBeenCalledWith({
      companyId: 'COMPANY_A',
      moduleCode: 'GENERAL_LEDGER'
    })
    expect(mocks.elMessage.success).toHaveBeenCalledWith('总账备份完成')
  })

  it('opens backup record dialog and renders backup details', async () => {
    const wrapper = await mountView()
    const recordButtons = wrapper.findAll('.button-stub').filter((item) => item.text() === '备份记录')

    await recordButtons[0].trigger('click')
    await flushPromises()

    expect(mocks.financeSystemManagementApi.getModuleBackupRecords).toHaveBeenCalledWith('COMPANY_A', 'GENERAL_LEDGER')
    expect(wrapper.text()).toContain('备份记录')
    expect(wrapper.text()).toContain('C:/backup/COMPANY_A-GENERAL_LEDGER-20260518_120000.sql')
    expect(wrapper.text()).toContain('张会计')
  })

  it('confirms and clears fixed asset data', async () => {
    const wrapper = await mountView()
    const clearButtons = wrapper.findAll('.button-stub').filter((item) => item.text() === '清除数据')

    await clearButtons[0].trigger('click')
    await flushPromises()

    expect(mocks.elMessageBox.confirm).toHaveBeenCalled()
    expect(mocks.financeSystemManagementApi.clearModuleData).toHaveBeenCalledWith({
      companyId: 'COMPANY_A',
      moduleCode: 'FIXED_ASSETS'
    })
    expect(mocks.financeCompany.refreshContext).toHaveBeenCalledWith('COMPANY_A')
    expect(mocks.elMessage.success).toHaveBeenCalledWith('固定资产数据已清除')
  })
})
