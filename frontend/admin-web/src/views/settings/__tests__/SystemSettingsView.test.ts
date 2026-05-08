import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h, inject, provide, ref } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
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
    updateDepartment: vi.fn(),
    createEmployee: vi.fn(),
    updateEmployee: vi.fn(),
    deleteEmployee: vi.fn(),
    assignRolePermissions: vi.fn(),
    assignUserRoles: vi.fn(),
    createCompanyBankAccount: vi.fn(),
    updateCompanyBankAccount: vi.fn(),
    deleteCompanyBankAccount: vi.fn(),
    updateOcrProvider: vi.fn(),
    testOcrProvider: vi.fn(),
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
  },
  permissionTreeState: {
    checkedKeys: [] as string[]
  }
}))

vi.mock('vue-router', () => ({
  useRoute: () => mocks.route,
  useRouter: () => mocks.router
}))

vi.mock('@/api', () => ({
  systemSettingsApi: mocks.systemSettingsApi
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessage: mocks.elMessage,
    ElMessageBox: mocks.elMessageBox
  }
})

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
  emits: ['node-click', 'node-expand', 'node-collapse'],
  setup(props, { slots, emit, expose }) {
    const checkedKeys = ref<string[]>([])

    function setCheckedKeys(keys: string[]) {
      checkedKeys.value = Array.isArray(keys) ? [...keys] : []
      mocks.permissionTreeState.checkedKeys = [...checkedKeys.value]
    }

    function getCheckedKeys() {
      return [...mocks.permissionTreeState.checkedKeys]
    }

    expose({
      setCheckedKeys,
      getCheckedKeys
    })

    function renderNodes(nodes: any[]) {
      return nodes.map((item) =>
        h('div', { key: item.nodeKey || item.id, class: 'tree-node-group' }, [
          h(
            'div',
            {
              class: 'tree-node-shell',
              'data-node-key': item.nodeKey || item.id,
              onClick: () => emit('node-click', item)
            },
            slots.default ? slots.default({ data: item }) : h('div', item?.deptName || item?.name || '')
          ),
          Array.isArray(item?.children) && item.children.length
            ? h('div', { class: 'tree-node-children' }, renderNodes(item.children))
            : null
        ])
      )
    }

    return () =>
      h('div', { 'data-testid': 'tree-stub' }, [
        h('div', { 'data-testid': 'tree-checked-keys' }, checkedKeys.value.join(',')),
        ...renderNodes(props.data as any[])
      ])
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
        'settings:company_accounts:view',
        'settings:api_interfaces:view',
        'settings:api_interfaces:ocr_edit',
        'settings:api_interfaces:ocr_test'
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
    jobs: [],
    ocrProviders: [
      {
        id: 1,
        providerCode: 'ALIYUN',
        providerName: '阿里云',
        enabled: true,
        accessKeyId: 'test-ak',
        hasSecret: true,
        maskedSecret: 'abc******xyz',
        endpoint: 'ocr-api.cn-hangzhou.aliyuncs.com',
        connectTimeoutMs: 5000,
        readTimeoutMs: 15000,
        lastTestAt: '2026-04-19 10:30:00',
        lastTestStatus: 'SUCCESS',
        lastTestMessage: '测试通过'
      },
      {
        id: 2,
        providerCode: 'TENCENT',
        providerName: '腾讯云',
        enabled: false,
        accessKeyId: '',
        hasSecret: false,
        maskedSecret: '',
        endpoint: '',
        connectTimeoutMs: 5000,
        readTimeoutMs: 15000,
        lastTestAt: '',
        lastTestStatus: 'IDLE',
        lastTestMessage: '待接入'
      },
      {
        id: 3,
        providerCode: 'BAIDU',
        providerName: '百度云',
        enabled: false,
        accessKeyId: '',
        hasSecret: false,
        maskedSecret: '',
        endpoint: '',
        connectTimeoutMs: 5000,
        readTimeoutMs: 15000,
        lastTestAt: '',
        lastTestStatus: 'IDLE',
        lastTestMessage: '待接入'
      }
    ]
  }
}

function createDepartmentTree() {
  return [
    {
      id: 1,
      companyId: 'COMPANY_A',
      deptCode: 'ROOT_A',
      deptName: '\u4e00\u7ea7A',
      parentId: undefined,
      leaderUserId: undefined,
      leaderName: '',
      syncSource: 'MANUAL',
      syncManaged: false,
      syncEnabled: true,
      syncStatus: 'MANUAL',
      syncRemark: '',
      status: 1,
      sortOrder: 1,
      children: [
        {
          id: 2,
          companyId: 'COMPANY_A',
          deptCode: 'A_CHILD',
          deptName: '\u4e8c\u7ea7A',
          parentId: 1,
          leaderUserId: undefined,
          leaderName: '',
          syncSource: 'MANUAL',
          syncManaged: false,
          syncEnabled: true,
          syncStatus: 'MANUAL',
          syncRemark: '',
          status: 1,
          sortOrder: 1,
          children: [
            {
              id: 3,
              companyId: 'COMPANY_A',
              deptCode: 'A_GRAND',
              deptName: '\u4e09\u7ea7A',
              parentId: 2,
              leaderUserId: undefined,
              leaderName: '',
              syncSource: 'MANUAL',
              syncManaged: false,
              syncEnabled: true,
              syncStatus: 'MANUAL',
              syncRemark: '',
              status: 1,
              sortOrder: 1,
              children: []
            }
          ]
        }
      ]
    },
    {
      id: 4,
      companyId: 'COMPANY_B',
      deptCode: 'ROOT_B',
      deptName: '\u4e00\u7ea7B',
      parentId: undefined,
      leaderUserId: undefined,
      leaderName: '',
      syncSource: 'MANUAL',
      syncManaged: false,
      syncEnabled: true,
      syncStatus: 'MANUAL',
      syncRemark: '',
      status: 1,
      sortOrder: 2,
      children: [
        {
          id: 5,
          companyId: 'COMPANY_B',
          deptCode: 'B_CHILD',
          deptName: '\u4e8c\u7ea7B',
          parentId: 4,
          leaderUserId: undefined,
          leaderName: '',
          syncSource: 'MANUAL',
          syncManaged: false,
          syncEnabled: true,
          syncStatus: 'MANUAL',
          syncRemark: '',
          status: 1,
          sortOrder: 1,
          children: []
        }
      ]
    }
  ]
}

function createOrganizationEnhancementBootstrap() {
  const bootstrap = createBootstrap()
  bootstrap.currentUser.permissionCodes.push(
    'settings:organization:create',
    'settings:organization:delete',
    'settings:organization:edit'
  )
  bootstrap.departments = [
    {
      id: 11,
      companyId: 'COMPANY_A',
      deptCode: 'ROOT_FIN',
      deptName: '财务中心',
      parentId: undefined,
      leaderUserId: undefined,
      leaderName: '',
      syncSource: 'MANUAL',
      syncManaged: false,
      syncEnabled: true,
      syncStatus: 'MANUAL',
      syncRemark: '',
      status: 1,
      sortOrder: 1,
      children: [
        {
          id: 12,
          companyId: 'COMPANY_A',
          deptCode: 'CHILD_AP',
          deptName: '应付组',
          parentId: 11,
          leaderUserId: undefined,
          leaderName: '',
          syncSource: 'MANUAL',
          syncManaged: false,
          syncEnabled: true,
          syncStatus: 'MANUAL',
          syncRemark: '',
          status: 1,
          sortOrder: 1,
          children: []
        }
      ]
    }
  ]
  bootstrap.employees = [
    {
      userId: 101,
      username: 'awang',
      name: '阿王',
      phone: '13800000001',
      email: 'awang@finex.com',
      companyId: 'COMPANY_A',
      companyName: '测试公司A',
      deptId: 11,
      deptName: '财务中心',
      departments: [{ deptId: 11, deptName: '财务中心' }],
      position: '会计',
      laborRelationBelong: '总部',
      statDepartmentBelong: '财务共享',
      statRegionBelong: '华东',
      statAreaBelong: '上海',
      status: 1,
      sourceType: 'MANUAL',
      syncManaged: false,
      lastSyncAt: '',
      roleCodes: ['FINANCE']
    },
    {
      userId: 102,
      username: 'zhaoer',
      name: '赵二',
      phone: '13800000002',
      email: 'zhaoer@finex.com',
      companyId: 'COMPANY_A',
      companyName: '测试公司A',
      deptId: 11,
      deptName: '财务中心',
      departments: [{ deptId: 11, deptName: '财务中心' }],
      position: '出纳',
      laborRelationBelong: '总部',
      statDepartmentBelong: '财务共享',
      statRegionBelong: '华东',
      statAreaBelong: '上海',
      status: 1,
      sourceType: 'WECOM',
      syncManaged: true,
      lastSyncAt: '',
      roleCodes: ['FINANCE']
    },
    {
      userId: 103,
      username: 'childuser',
      name: '李三',
      phone: '13800000003',
      email: 'lisi3@finex.com',
      companyId: 'COMPANY_A',
      companyName: '测试公司A',
      deptId: 12,
      deptName: '应付组',
      departments: [{ deptId: 12, deptName: '应付组' }],
      position: '助理',
      laborRelationBelong: '总部',
      statDepartmentBelong: '财务共享',
      statRegionBelong: '华东',
      statAreaBelong: '上海',
      status: 0,
      sourceType: 'MANUAL',
      syncManaged: false,
      lastSyncAt: '',
      roleCodes: []
    }
  ]
  bootstrap.roles = [
    {
      id: 51,
      roleCode: 'FINANCE',
      roleName: '财务角色',
      roleDescription: '财务权限',
      status: 1,
      permissionCodes: [],
      userIds: [101, 102],
      userNames: ['阿王', '赵二']
    }
  ]
  bootstrap.companies = [
    {
      companyId: 'COMPANY_A',
      companyCode: 'COMPANY_A',
      companyName: '测试公司A',
      invoiceTitle: '测试公司A',
      taxNo: '91310000TEST',
      status: 1,
      children: []
    }
  ]
  return bootstrap
}

function createRolesBootstrap() {
  const bootstrap = createBootstrap()
  bootstrap.currentUser.roles = ['ADMIN']
  bootstrap.currentUser.permissionCodes = [
    'settings:menu',
    'settings:roles:view',
    'settings:roles:assign_permissions',
    'settings:roles:assign_users'
  ]
  bootstrap.permissions = [
    {
      permissionCode: 'settings:menu',
      permissionName: '系统设置',
      children: [
        {
          permissionCode: 'settings:roles:view',
          permissionName: '查看权限管理',
          children: []
        },
        {
          permissionCode: 'settings:roles:assign_permissions',
          permissionName: '分配权限',
          children: []
        },
        {
          permissionCode: 'settings:roles:assign_users',
          permissionName: '分配用户',
          children: []
        },
        {
          permissionCode: 'settings:companies:view',
          permissionName: '查看公司管理',
          children: []
        }
      ]
    }
  ]
  bootstrap.employees = [
    {
      userId: 201,
      username: 'zhangsan',
      name: '张三',
      phone: '13800000011',
      email: 'zhangsan@finex.com',
      companyId: 'COMPANY_A',
      companyName: '测试公司A',
      deptId: 11,
      deptName: '财务中心',
      departments: [{ deptId: 11, deptName: '财务中心' }],
      position: '会计',
      laborRelationBelong: '总部',
      statDepartmentBelong: '财务共享',
      statRegionBelong: '华东',
      statAreaBelong: '上海',
      status: 1,
      sourceType: 'MANUAL',
      syncManaged: false,
      lastSyncAt: '',
      roleCodes: ['ACCOUNTANT']
    },
    {
      userId: 202,
      username: 'lisi',
      name: '李四',
      phone: '13800000012',
      email: 'lisi@finex.com',
      companyId: 'COMPANY_A',
      companyName: '测试公司A',
      deptId: 11,
      deptName: '财务中心',
      departments: [{ deptId: 11, deptName: '财务中心' }],
      position: '出纳',
      laborRelationBelong: '总部',
      statDepartmentBelong: '财务共享',
      statRegionBelong: '华东',
      statAreaBelong: '上海',
      status: 1,
      sourceType: 'MANUAL',
      syncManaged: false,
      lastSyncAt: '',
      roleCodes: ['CASHIER']
    }
  ]
  bootstrap.roles = [
    {
      id: 71,
      roleCode: 'ACCOUNTANT',
      roleName: '会计',
      roleDescription: '会计权限',
      status: 1,
      permissionCodes: ['settings:roles:view', 'settings:roles:assign_permissions'],
      userIds: [201],
      userNames: ['张三']
    },
    {
      id: 72,
      roleCode: 'CASHIER',
      roleName: '出纳',
      roleDescription: '出纳权限',
      status: 1,
      permissionCodes: ['settings:roles:view', 'settings:roles:assign_users'],
      userIds: [202],
      userNames: ['李四']
    }
  ]
  return bootstrap
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
        'el-dialog': SimpleContainer,
        SupplierPaymentInfoFields: true
      },
      directives: {
        loading: () => undefined
      }
    }
  })
  await flushPromises()
  return wrapper
}

function indexOfText(content: string, needle: string) {
  const index = content.indexOf(needle)
  expect(index).toBeGreaterThan(-1)
  return index
}

describe('SystemSettingsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    mocks.permissionTreeState.checkedKeys = []
    mocks.router.replace.mockResolvedValue(undefined)
    mocks.router.push.mockResolvedValue(undefined)
    mocks.systemSettingsApi.getBootstrap.mockResolvedValue({ data: createBootstrap() })
    mocks.systemSettingsApi.updateDepartment.mockResolvedValue({})
    mocks.systemSettingsApi.createEmployee.mockResolvedValue({ data: { userId: 88 } })
    mocks.systemSettingsApi.updateEmployee.mockResolvedValue({ data: { userId: 88 } })
    mocks.systemSettingsApi.deleteEmployee.mockResolvedValue({})
    mocks.systemSettingsApi.assignRolePermissions.mockResolvedValue({})
    mocks.systemSettingsApi.assignUserRoles.mockResolvedValue({})
    mocks.systemSettingsApi.createCompanyBankAccount.mockResolvedValue({})
    mocks.systemSettingsApi.updateCompanyBankAccount.mockResolvedValue({})
    mocks.systemSettingsApi.deleteCompanyBankAccount.mockResolvedValue({})
    mocks.systemSettingsApi.updateOcrProvider.mockResolvedValue({ data: createBootstrap().ocrProviders[0] })
    mocks.systemSettingsApi.testOcrProvider.mockResolvedValue({
      data: {
        ...createBootstrap().ocrProviders[0],
        lastTestStatus: 'SUCCESS',
        lastTestMessage: '测试通过'
      }
    })
    mocks.systemSettingsApi.updateSyncConnector.mockResolvedValue({})
    mocks.systemSettingsApi.runSync.mockResolvedValue({})
    mocks.elMessageBox.confirm.mockResolvedValue(undefined)
  })

  it('renders the compact settings hero classes', async () => {
    const wrapper = await mountView()

    const hero = wrapper.find('section.rounded-3xl')

    expect(hero.exists()).toBe(true)
    expect(hero.classes()).toContain('px-5')
    expect(hero.classes()).toContain('py-4')
    expect(wrapper.text()).toContain('\u7cfb\u7edf\u8bbe\u7f6e\u4e2d\u5fc3')
  })

  it('syncs checked permissions and selected users when switching roles', async () => {
    mocks.route.query = { tab: 'roles' }
    mocks.systemSettingsApi.getBootstrap.mockResolvedValue({ data: createRolesBootstrap() })

    const wrapper = await mountView()
    const vm = wrapper.vm as any

    vm.handleRoleSelect(vm.roles[0])
    await flushPromises()

    expect(mocks.permissionTreeState.checkedKeys).toEqual([
      'settings:roles:view',
      'settings:roles:assign_permissions'
    ])
    expect(vm.selectedRoleUserIds).toEqual([201])

    vm.handleRoleSelect(vm.roles[1])
    await flushPromises()

    expect(mocks.permissionTreeState.checkedKeys).toEqual([
      'settings:roles:view',
      'settings:roles:assign_users'
    ])
    expect(vm.selectedRoleUserIds).toEqual([202])
  })

  it('saves the current checked permission keys from the role permission tree', async () => {
    mocks.route.query = { tab: 'roles' }
    mocks.systemSettingsApi.getBootstrap.mockResolvedValue({ data: createRolesBootstrap() })

    const wrapper = await mountView()
    const vm = wrapper.vm as any

    vm.handleRoleSelect(vm.roles[0])
    await flushPromises()

    mocks.permissionTreeState.checkedKeys = [
      'settings:roles:view',
      'settings:companies:view'
    ]

    const savePermissionsButton = wrapper
      .findAll('button')
      .find((button) => button.text().trim() === '保存权限')

    expect(savePermissionsButton).toBeTruthy()

    await savePermissionsButton!.trigger('click')
    await flushPromises()

    expect(mocks.systemSettingsApi.assignRolePermissions).toHaveBeenCalledWith(71, [
      'settings:roles:view',
      'settings:companies:view'
    ])
    expect(mocks.elMessage.success).toHaveBeenCalledWith('角色权限已更新')
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

  it('selects the api interfaces tab from route query and defaults to OCR', async () => {
    mocks.route.query = { tab: 'apiInterfaces' }

    const wrapper = await mountView()
    const vm = wrapper.vm as any

    expect(vm.activeTab).toBe('apiInterfaces')
    expect(vm.activeApiInterface).toBe('ocr')
    expect(wrapper.text()).toContain('OCR')
    expect(wrapper.text()).toContain('发票验真')
    expect(wrapper.text()).toContain('接口文档')
    expect(wrapper.find('[data-testid="api-interface-title"]').text()).toBe('OCR 云端接入配置')
    expect(wrapper.text()).toContain('阿里云')
    expect(wrapper.text()).toContain('腾讯云')
    expect(wrapper.text()).toContain('百度云')
    expect(wrapper.find('[data-testid="ocr-provider-panel"]').exists()).toBe(true)
  })

  it('renders aliyun ram least-privilege guidance in the ocr settings panel', async () => {
    mocks.route.query = { tab: 'apiInterfaces' }

    const wrapper = await mountView()

    expect(wrapper.find('[data-testid="ocr-ram-guidance"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('finex-ocr-runtime')
    expect(wrapper.text()).toContain('FinexInvoiceOcrRuntimePolicy')
    expect(wrapper.find('[data-testid="ocr-ram-policy-snippet"]').text()).toContain('"ocr:RecognizeInvoice"')
  })

  it('switches api interface placeholder panels without firing extra requests', async () => {
    mocks.route.query = { tab: 'apiInterfaces' }

    const wrapper = await mountView()
    const vm = wrapper.vm as any

    expect(mocks.systemSettingsApi.getBootstrap).toHaveBeenCalledTimes(1)
    await wrapper.find('[data-testid="api-interface-nav-apiDocs"]').trigger('click')
    await flushPromises()

    expect(vm.activeApiInterface).toBe('apiDocs')
    expect(wrapper.find('[data-testid="api-interface-title"]').text()).toBe('本项目接口文档')
    expect(wrapper.find('[data-testid="api-interface-note"]').text()).toContain('认证、报销、发票')
    expect(mocks.systemSettingsApi.getBootstrap).toHaveBeenCalledTimes(1)
    expect(mocks.systemSettingsApi.updateDepartment).not.toHaveBeenCalled()
    expect(mocks.systemSettingsApi.updateSyncConnector).not.toHaveBeenCalled()
    expect(mocks.systemSettingsApi.runSync).not.toHaveBeenCalled()
  })

  it('blocks save when access key id changes but secret is blank', async () => {
    mocks.route.query = { tab: 'apiInterfaces' }
    const wrapper = await mountView()

    const vm = wrapper.vm as any
    vm.ocrForm.enabled = true
    vm.ocrForm.accessKeyId = 'new-ak'
    vm.ocrForm.accessKeySecret = ''
    vm.ocrForm.endpoint = 'ocr-api.cn-hangzhou.aliyuncs.com'
    vm.ocrForm.connectTimeoutMs = 6000
    vm.ocrForm.readTimeoutMs = 20000

    await vm.saveOcrProvider()
    await flushPromises()

    expect(mocks.systemSettingsApi.updateOcrProvider).not.toHaveBeenCalled()
    expect(mocks.elMessage.warning).toHaveBeenCalledWith(
      '更换 AccessKey ID 时，必须同时重新填写 AccessKey Secret'
    )
  })

  it('saves aliyun ocr config when access key id is unchanged and secret stays blank', async () => {
    mocks.route.query = { tab: 'apiInterfaces' }
    const wrapper = await mountView()

    const vm = wrapper.vm as any
    vm.ocrForm.enabled = true
    vm.ocrForm.accessKeyId = 'test-ak'
    vm.ocrForm.accessKeySecret = ''
    vm.ocrForm.endpoint = 'ocr-api.cn-hangzhou.aliyuncs.com'
    vm.ocrForm.connectTimeoutMs = 6000
    vm.ocrForm.readTimeoutMs = 20000

    await vm.saveOcrProvider()
    await flushPromises()

    expect(mocks.systemSettingsApi.updateOcrProvider).toHaveBeenCalledWith('ALIYUN', {
      enabled: 1,
      accessKeyId: 'test-ak',
      accessKeySecret: '',
      endpoint: 'ocr-api.cn-hangzhou.aliyuncs.com',
      connectTimeoutMs: 6000,
      readTimeoutMs: 20000
    })
    expect(mocks.elMessage.success).toHaveBeenCalledWith('OCR 配置已保存')
  })

  it('shows placeholder status for tencent and does not call real ocr save', async () => {
    mocks.route.query = { tab: 'apiInterfaces' }
    const wrapper = await mountView()
    const vm = wrapper.vm as any

    vm.activeOcrProviderCode = 'TENCENT'
    await flushPromises()

    expect(wrapper.text()).toContain('待接入')
    expect(wrapper.text()).toContain('首期仅开放阿里云 OCR 真实接入')

    await vm.saveOcrProvider()
    await flushPromises()

    expect(mocks.systemSettingsApi.updateOcrProvider).not.toHaveBeenCalled()
    expect(mocks.elMessage.warning).toHaveBeenCalledWith('当前厂商待接入，暂不支持保存真实配置')
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
      departments: [{ deptId: 12, deptName: '\u8d22\u52a1\u90e8' }],
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

  it('renders mixed organization nodes with departments before employees under the same parent', async () => {
    const bootstrap = createOrganizationEnhancementBootstrap()
    mocks.systemSettingsApi.getBootstrap.mockResolvedValue({ data: bootstrap })

    const wrapper = await mountView()
    const html = wrapper.html()

    expect(wrapper.find('[data-testid="organization-node-dept-12"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="organization-node-emp-101-dept-11"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="organization-node-emp-102-dept-11"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="organization-node-emp-103-dept-12"]').exists()).toBe(true)
    expect(indexOfText(html, 'organization-node-dept-12')).toBeLessThan(
      indexOfText(html, 'organization-node-emp-103-dept-12')
    )
    expect(indexOfText(html, 'organization-node-emp-101-dept-11')).toBeLessThan(
      indexOfText(html, 'organization-node-emp-102-dept-11')
    )
  })

  it('shows the employee info card instead of department config when an organization employee node is selected', async () => {
    const bootstrap = createOrganizationEnhancementBootstrap()
    mocks.systemSettingsApi.getBootstrap.mockResolvedValue({ data: bootstrap })

    const wrapper = await mountView()
    const vm = wrapper.vm as any

    await wrapper.get('[data-testid="organization-node-emp-101-dept-11"]').trigger('click')
    await flushPromises()

    expect(vm.selectedOrganizationNodeKey).toBe('emp-101-dept-11')
    expect(vm.selectedOrganizationNodeIsEmployee).toBe(true)
    expect(vm.selectedOrganizationEmployee?.userId).toBe(101)
    expect(wrapper.find('[data-testid="organization-employee-info"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="organization-department-config"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('阿王')
    expect(wrapper.text()).toContain('财务中心')
  })

  it('opens the shared employee dialog with employee data when double-clicking an organization employee node', async () => {
    const bootstrap = createOrganizationEnhancementBootstrap()
    mocks.systemSettingsApi.getBootstrap.mockResolvedValue({ data: bootstrap })

    const wrapper = await mountView()
    const vm = wrapper.vm as any

    await wrapper.get('[data-testid="organization-node-emp-102-dept-11"]').trigger('dblclick')
    await flushPromises()

    expect(vm.employeeDialogVisible).toBe(true)
    expect(vm.employeeForm.userId).toBe(102)
    expect(vm.employeeForm.username).toBe('zhaoer')
    expect(vm.employeeForm.name).toBe('赵二')
    expect(vm.employeeForm.deptIds).toEqual([11])
    expect((wrapper.get('[data-testid="employee-username-input"]').element as HTMLInputElement).value).toBe(
      'zhaoer'
    )
  })

  it('reuses the existing employee save flow when editing from the organization tree', async () => {
    const bootstrap = createOrganizationEnhancementBootstrap()
    mocks.systemSettingsApi.getBootstrap.mockResolvedValue({ data: bootstrap })
    mocks.systemSettingsApi.updateEmployee.mockResolvedValue({ data: { userId: 102 } })

    const wrapper = await mountView()
    const vm = wrapper.vm as any

    await wrapper.get('[data-testid="organization-node-emp-102-dept-11"]').trigger('dblclick')
    await flushPromises()

    vm.employeeForm.name = '赵二（更新）'
    vm.employeeForm.statDepartmentBelong = '财务核算'
    vm.employeeForm.roleIds = [51]

    await vm.saveEmployee(true)
    await flushPromises()

    expect(mocks.systemSettingsApi.updateEmployee).toHaveBeenCalledWith(
      102,
      expect.objectContaining({
        username: 'zhaoer',
        name: '赵二（更新）',
        deptIds: [11],
        statDepartmentBelong: '财务核算',
        status: 1
      })
    )
    expect(mocks.systemSettingsApi.assignUserRoles).toHaveBeenCalledWith(102, [51])
    expect(vm.employeeDialogVisible).toBe(false)
    expect(mocks.elMessage.success).toHaveBeenCalledWith('员工与角色已保存')
  })

  it('disables department deletion for employee nodes and restores it for department nodes', async () => {
    const bootstrap = createOrganizationEnhancementBootstrap()
    mocks.systemSettingsApi.getBootstrap.mockResolvedValue({ data: bootstrap })

    const wrapper = await mountView()
    const deleteButton = wrapper.get('[data-testid="organization-delete-button"]')

    expect(deleteButton.attributes('disabled')).toBeUndefined()

    await wrapper.get('[data-testid="organization-node-emp-101-dept-11"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="organization-delete-button"]').attributes('disabled')).toBeDefined()

    await wrapper.get('[data-testid="organization-node-dept-11"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="organization-delete-button"]').attributes('disabled')).toBeUndefined()
  })

  it('keeps the employee management edit entry working with the shared employee dialog', async () => {
    const bootstrap = createOrganizationEnhancementBootstrap()
    mocks.systemSettingsApi.getBootstrap.mockResolvedValue({ data: bootstrap })

    const wrapper = await mountView()
    const vm = wrapper.vm as any

    vm.selectedEmployee = bootstrap.employees[0]
    await flushPromises()

    const editButton = wrapper
      .findAll('button')
      .find((button) => button.text().trim() === '编辑')

    expect(editButton).toBeTruthy()

    await editButton!.trigger('click')
    await flushPromises()

    expect(vm.employeeDialogVisible).toBe(true)
    expect(vm.employeeForm.userId).toBe(101)
    expect(vm.employeeForm.username).toBe('awang')
  })

  it('keeps department tree collapsed to level-1 on initial load', async () => {
    const bootstrap = createBootstrap()
    bootstrap.currentUser.permissionCodes.push('settings:organization:edit')
    bootstrap.departments = createDepartmentTree()
    mocks.systemSettingsApi.getBootstrap.mockResolvedValueOnce({ data: bootstrap })

    const wrapper = await mountView()
    const vm = wrapper.vm as any

    expect(vm.departmentExpandedKeys).toEqual([])
  })

  it('preserves expanded departments and keeps the edited non-top-level department visible after saving config', async () => {
    const bootstrap = createBootstrap()
    bootstrap.currentUser.permissionCodes.push('settings:organization:edit')
    bootstrap.departments = createDepartmentTree()
    mocks.systemSettingsApi.getBootstrap.mockResolvedValue({ data: bootstrap })

    const wrapper = await mountView()
    const vm = wrapper.vm as any
    vm.handleDepartmentNodeExpand(vm.organizationTreeNodes[0])
    vm.handleDepartmentNodeExpand(vm.organizationTreeNodes[1])
    await wrapper.get('[data-testid="organization-node-dept-3"]').trigger('click')
    await flushPromises()

    await vm.saveDepartmentConfig()
    await flushPromises()

    expect(mocks.systemSettingsApi.updateDepartment).toHaveBeenCalledWith(3, expect.any(Object))
    expect(vm.departmentExpandedKeys).toEqual(['dept-1', 'dept-4', 'dept-2', 'dept-3'])
  })

  it('collapses a department and all expanded child departments at once', async () => {
    const bootstrap = createBootstrap()
    bootstrap.currentUser.permissionCodes.push('settings:organization:edit')
    bootstrap.departments = createDepartmentTree()
    mocks.systemSettingsApi.getBootstrap.mockResolvedValue({ data: bootstrap })

    const wrapper = await mountView()
    const vm = wrapper.vm as any
    vm.handleDepartmentNodeExpand(vm.organizationTreeNodes[0])
    vm.handleDepartmentNodeExpand(vm.organizationTreeNodes[0].children[0])
    vm.handleDepartmentNodeExpand(vm.organizationTreeNodes[0].children[0].children[0])
    vm.handleDepartmentNodeExpand(vm.organizationTreeNodes[1])

    vm.handleDepartmentNodeCollapse(vm.organizationTreeNodes[0])

    expect(vm.departmentExpandedKeys).toEqual(['dept-4'])
  })

  it('keeps level-1 collapsed after saving a top-level department config when nothing was expanded', async () => {
    const bootstrap = createBootstrap()
    bootstrap.currentUser.permissionCodes.push('settings:organization:edit')
    bootstrap.departments = createDepartmentTree()
    mocks.systemSettingsApi.getBootstrap.mockResolvedValue({ data: bootstrap })

    const wrapper = await mountView()
    const vm = wrapper.vm as any
    await wrapper.get('[data-testid="organization-node-dept-1"]').trigger('click')
    await flushPromises()

    await vm.saveDepartmentConfig()
    await flushPromises()

    expect(mocks.systemSettingsApi.updateDepartment).toHaveBeenCalledWith(1, expect.any(Object))
    expect(vm.departmentExpandedKeys).toEqual([])
  })

  it('preserves the organization tree expansion after saving employee information', async () => {
    const bootstrap = createOrganizationEnhancementBootstrap()
    mocks.systemSettingsApi.getBootstrap.mockResolvedValue({ data: bootstrap })

    const wrapper = await mountView()
    const vm = wrapper.vm as any
    vm.handleDepartmentNodeExpand(vm.organizationTreeNodes[0])
    await wrapper.get('[data-testid="organization-node-emp-101-dept-11"]').trigger('dblclick')
    await flushPromises()

    vm.employeeForm.name = '王一（更新）'
    await vm.saveEmployee(true)
    await flushPromises()

    expect(mocks.systemSettingsApi.updateEmployee).toHaveBeenCalledWith(101, expect.any(Object))
    expect(vm.departmentExpandedKeys).toEqual(['dept-11'])
  })

  it('uses the unified outlet wording when validating company bank accounts', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as any

    Object.assign(vm.companyBankAccountForm, {
      companyId: 'COMPANY-001',
      bankCode: 'ICBC',
      bankName: '中国工商银行',
      province: '上海市',
      city: '上海市',
      branchCode: '',
      branchName: '',
      accountName: '测试账户',
      accountNo: '622200001'
    })

    expect(vm.validateCompanyBankAccountForm()).toBe('\u8bf7\u9009\u62e9\u5f00\u6237\u94f6\u884c\u3001\u5f00\u6237\u7701\u3001\u5f00\u6237\u5e02\u4e0e\u5f00\u6237\u7f51\u70b9\u540e\u518d\u4fdd\u5b58')
  })

  it('shows the unified company bank account success wording when saving', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as any

    Object.assign(vm.companyBankAccountForm, {
      companyId: 'COMPANY-001',
      bankCode: 'ICBC',
      bankName: '中国工商银行',
      province: '上海市',
      city: '上海市',
      branchCode: 'ICBC-SH-001',
      branchName: '中国工商银行上海分行',
      accountName: '测试账户',
      accountNo: '622200001',
      status: 1,
      defaultAccount: 0
    })

    await vm.saveCompanyBankAccount()
    await flushPromises()

    expect(mocks.systemSettingsApi.createCompanyBankAccount).toHaveBeenCalled()
    expect(mocks.elMessage.success).toHaveBeenCalledWith('公司银行账户已保存')
  })

  it('shows the unified company bank account confirm and delete wording', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as any
    const row = {
      id: 7,
      accountName: '测试账户',
      accountNo: '6222000012345678'
    }

    await vm.handleDeleteCompanyBankAccount(row)
    await flushPromises()

    expect(mocks.elMessageBox.confirm).toHaveBeenCalledWith(
      '确认删除公司银行账户“测试账户 / 6222 **** **** 5678”吗？',
      '提示',
      { type: 'warning' }
    )
    expect(mocks.systemSettingsApi.deleteCompanyBankAccount).toHaveBeenCalledWith(7)
    expect(mocks.elMessage.success).toHaveBeenCalledWith('公司银行账户已删除')
  })

  it('shows the unified company bank account status wording', async () => {
    const wrapper = await mountView()
    const vm = wrapper.vm as any

    await vm.toggleCompanyBankAccountStatus({
      id: 9,
      companyId: 'COMPANY-001',
      bankCode: 'ICBC',
      bankName: '中国工商银行',
      province: '上海市',
      city: '上海市',
      branchCode: 'ICBC-SH-001',
      branchName: '中国工商银行上海分行',
      accountName: '测试账户',
      accountNo: '622200001',
      status: 0,
      defaultAccount: 0
    }, 1)
    await flushPromises()

    expect(mocks.systemSettingsApi.updateCompanyBankAccount).toHaveBeenCalled()
    expect(mocks.elMessage.success).toHaveBeenCalledWith('公司银行账户已启用')
  })

  afterEach(() => {
    mocks.route.query = { tab: 'organization' }
  })
})
