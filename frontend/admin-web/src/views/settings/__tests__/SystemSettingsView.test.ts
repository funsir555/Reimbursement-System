import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h, inject, provide } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import SystemSettingsView from '@/views/settings/SystemSettingsView.vue'

const mocks = vi.hoisted(() => ({
  route: {
    query: { tab: 'organization' }
  },
  router: {
    replace: vi.fn(),
    push: vi.fn()
  },
  systemSettingsApi: {
    getBootstrap: vi.fn(),
    updateSyncConnector: vi.fn(),
    runSync: vi.fn()
  },
  elMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  },
  elMessageBox: {
    confirm: vi.fn()
  }
}))

vi.mock('vue-router', () => ({
  useRoute: () => mocks.route,
  useRouter: () => mocks.router
}))

vi.mock('@/api', () => ({
  systemSettingsApi: mocks.systemSettingsApi
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage,
  ElMessageBox: mocks.elMessageBox
}))

vi.mock('@/utils/permissions', () => ({
  hasAnyPermission: () => true,
  hasPermission: () => true
}))

const SimpleContainer = defineComponent({
  template: '<div><slot name="header" /><slot name="reference" /><slot /><slot name="footer" /></div>'
})

const ButtonStub = defineComponent({
  emits: ['click'],
  template: '<button v-bind="$attrs" type="button" @click="$emit(\'click\')"><slot /></button>'
})

const InputStub = defineComponent({
  props: {
    modelValue: {
      type: [String, Number],
      default: ''
    }
  },
  emits: ['update:modelValue'],
  template:
    '<input v-bind="$attrs" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
})

const SwitchStub = defineComponent({
  props: {
    modelValue: {
      type: [Boolean, Number],
      default: false
    },
    activeValue: {
      type: [Boolean, Number],
      default: true
    },
    inactiveValue: {
      type: [Boolean, Number],
      default: false
    }
  },
  emits: ['update:modelValue'],
  template: `
    <input
      v-bind="$attrs"
      type="checkbox"
      :checked="modelValue === activeValue || modelValue === true"
      @change="$emit('update:modelValue', $event.target.checked ? activeValue : inactiveValue)"
    />
  `
})

const SelectStub = defineComponent({
  props: {
    modelValue: {
      type: [String, Number, Array],
      default: ''
    }
  },
  emits: ['update:modelValue'],
  template: '<div v-bind="$attrs"><slot /></div>'
})

const TabsStub = defineComponent({
  props: {
    modelValue: {
      type: String,
      default: ''
    }
  },
  emits: ['update:modelValue'],
  template: '<div v-bind="$attrs"><slot /></div>'
})

const TabPaneStub = defineComponent({
  template: '<div><slot /></div>'
})

const TreeStub = defineComponent({
  props: {
    data: {
      type: Array,
      default: () => []
    }
  },
  setup(props, { slots }) {
    return () =>
      h(
        'div',
        {},
        props.data.map((item: any) =>
          slots.default ? slots.default({ data: item }) : h('div', item?.deptName || '')
        )
      )
  }
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
      <template v-for="(row, index) in rows" :key="index + prop">
        <slot :row="row">
          <span>{{ prop ? row[prop] : '' }}</span>
        </slot>
      </template>
    </div>
  `
})

function createBootstrap() {
  return {
    currentUser: {
      userId: 1,
      username: 'admin',
      name: '\u7ba1\u7406\u5458',
      roles: ['SUPER_ADMIN'],
      permissionCodes: [
        'settings:menu',
        'settings:organization:view',
        'settings:organization:sync_config',
        'settings:organization:run_sync',
        'settings:employees:view',
        'settings:employees:create',
        'settings:employees:edit',
        'settings:employees:delete',
        'settings:roles:assign_users',
        'settings:companies:view',
        'settings:company_accounts:view'
      ]
    },
    departments: [],
    employees: [],
    roles: [],
    permissions: [],
    companies: [],
    companyBankAccounts: [],
    connectors: [
      {
        id: 1,
        platformCode: 'DINGTALK',
        platformName: '????',
        enabled: true,
        autoSyncEnabled: false,
        syncIntervalMinutes: 60,
        appKey: '',
        appSecret: '',
        appId: '',
        corpId: '',
        agentId: '',
        lastSyncAt: '',
        lastSyncStatus: 'IDLE',
        lastSyncMessage: '\u5c1a\u672a\u6267\u884c\u540c\u6b65'
      },
      {
        id: 2,
        platformCode: 'WECOM',
        platformName: '????',
        enabled: true,
        autoSyncEnabled: false,
        syncIntervalMinutes: 60,
        appKey: '',
        appSecret: '',
        appId: '',
        corpId: '',
        agentId: '',
        lastSyncAt: '',
        lastSyncStatus: 'IDLE',
        lastSyncMessage: '\u5c1a\u672a\u6267\u884c\u540c\u6b65'
      },
      {
        id: 3,
        platformCode: 'FEISHU',
        platformName: '????',
        enabled: true,
        autoSyncEnabled: false,
        syncIntervalMinutes: 60,
        appKey: '',
        appSecret: '',
        appId: '',
        corpId: '',
        agentId: '',
        lastSyncAt: '',
        lastSyncStatus: 'IDLE',
        lastSyncMessage: '\u5c1a\u672a\u6267\u884c\u540c\u6b65'
      }
    ],
    jobs: []
  }
}

async function mountView() {
  const wrapper = mount(SystemSettingsView, {
    global: {
      stubs: {
        'el-card': SimpleContainer,
        'el-tabs': TabsStub,
        'el-tab-pane': TabPaneStub,
        'el-button': ButtonStub,
        'el-input': InputStub,
        'el-switch': SwitchStub,
        'el-select': SelectStub,
        'el-tree-select': SelectStub,
        'el-option': true,
        'el-input-number': InputStub,
        'el-form': SimpleContainer,
        'el-form-item': SimpleContainer,
        'el-empty': SimpleContainer,
        'el-alert': SimpleContainer,
        'el-tree': TreeStub,
        'el-table': TableStub,
        'el-table-column': TableColumnStub,
        'el-tag': SimpleContainer,
        'el-dialog': SimpleContainer
      },
      directives: {
        loading: () => undefined
      }
    }
  })
  await flushPromises()
  return wrapper
}

describe('SystemSettingsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    mocks.router.replace.mockResolvedValue(undefined)
    mocks.router.push.mockResolvedValue(undefined)
    mocks.systemSettingsApi.getBootstrap.mockResolvedValue({ data: createBootstrap() })
    mocks.systemSettingsApi.updateSyncConnector.mockResolvedValue({})
    mocks.systemSettingsApi.runSync.mockResolvedValue({})
  })

  it('renders the compact settings hero classes', async () => {
    const wrapper = await mountView()

    const hero = wrapper.find('section.rounded-3xl')

    expect(hero.exists()).toBe(true)
    expect(hero.classes()).toContain('px-5')
    expect(hero.classes()).toContain('py-4')
    expect(wrapper.text()).toContain('\u7cfb\u7edf\u8bbe\u7f6e\u4e2d\u5fc3')
  })

  it('renders employee sync connector cards with local platform labels', async () => {
    const wrapper = await mountView()

    const titles = wrapper
      .findAll('[data-testid="employee-sync-connector-title"]')
      .map((item) => item.text())

    expect(mocks.systemSettingsApi.getBootstrap).toHaveBeenCalled()
    expect(titles).toEqual(['\u9489\u9489', '\u4f01\u5fae', '\u98de\u4e66'])
    expect(wrapper.text()).not.toContain('????')
  })

  it('keeps synced employee core fields readonly while stat fields stay editable', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as any

    vm.openEmployeeDialog({
      userId: 9,
      username: 'sync_user',
      name: '\u540c\u6b65\u5458\u5de5',
      phone: '13800000000',
      email: 'sync@example.com',
      companyId: 'COMPANY_A',
      companyName: '\u6d4b\u8bd5\u516c\u53f8',
      deptId: 12,
      deptName: '\u8d22\u52a1\u90e8',
      position: '\u4f1a\u8ba1',
      laborRelationBelong: '\u6b63\u5f0f',
      statDepartmentBelong: '\u534e\u4e1c\u8d22\u52a1',
      statRegionBelong: '\u534e\u4e1c',
      statAreaBelong: '\u4e0a\u6d77',
      status: 1,
      sourceType: 'WECOM',
      syncManaged: true,
      lastSyncAt: '',
      roleCodes: []
    })
    await flushPromises()

    const usernameInput = wrapper.find('[data-testid="employee-username-input"]')
    const statDepartmentInput = wrapper.find('[data-testid="employee-stat-department-input"]')

    expect(vm.employeeSyncLocked).toBe(true)
    expect(usernameInput.attributes('disabled')).toBeDefined()
    expect(statDepartmentInput.attributes('disabled')).toBeUndefined()
  })

  it('keeps synced department core fields readonly while stat fields stay editable', async () => {
    const bootstrap = createBootstrap()
    bootstrap.currentUser.permissionCodes.push('settings:organization:edit')
    bootstrap.departments = [
      {
        id: 12,
        companyId: 'COMPANY_A',
        deptCode: 'D_SYNC',
        deptName: '同步部门',
        parentId: undefined,
        leaderUserId: undefined,
        leaderUserName: '',
        syncSource: 'WECOM',
        syncEnabled: true,
        syncManaged: true,
        syncStatus: 'SYNCED',
        syncRemark: '来自企微',
        statDepartmentBelong: '华东共享',
        statRegionBelong: '华东',
        statAreaBelong: '上海',
        status: 1,
        sortOrder: 1,
        children: []
      }
    ]
    mocks.systemSettingsApi.getBootstrap.mockResolvedValueOnce({ data: bootstrap })

    const wrapper = await mountView()
    const vm = wrapper.vm as any

    const deptNameInput = wrapper.find('input[value="同步部门"]')
    const statDepartmentInput = wrapper.find('[data-testid="department-stat-department-input"]')

    expect(vm.departmentCoreFieldsReadonly).toBe(true)
    expect(vm.departmentStatEditable).toBe(true)
    expect(deptNameInput.attributes('disabled')).toBeDefined()
    expect(statDepartmentInput.attributes('disabled')).toBeUndefined()
  })
})
