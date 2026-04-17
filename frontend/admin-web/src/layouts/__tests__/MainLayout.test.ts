import { defineComponent, reactive } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MainLayout from '@/layouts/MainLayout.vue'

const mocks = vi.hoisted(() => ({
  route: {
    path: '/dashboard',
    fullPath: '/dashboard'
  },
  router: {
    push: vi.fn()
  },
  authApi: {
    getCurrentUser: vi.fn()
  },
  downloadApi: {
    getCenter: vi.fn()
  },
  notificationApi: {
    getSummary: vi.fn()
  },
  financeWorkspace: {
    tabs: [] as Array<{ path: string }>,
    activePath: '/dashboard',
    isFinancePath: vi.fn(() => false),
    syncRoute: vi.fn(),
    activate: vi.fn(),
    close: vi.fn(),
    closeOthers: vi.fn(),
    closeToRight: vi.fn(),
    getNextPathAfterClose: vi.fn(() => '')
  },
  financeCompany: {
    companyOptions: [] as Array<{ companyId: string; label: string }>,
    currentCompanyId: '',
    loading: false,
    switching: false,
    ensureInitialized: vi.fn(),
    switchCompany: vi.fn(),
    reset: vi.fn()
  },
  elMessage: {
    success: vi.fn()
  },
  stopDownloadListener: vi.fn()
}))

const routeState = reactive(mocks.route)
const permissionState = reactive<{ codes: string[] }>({ codes: [] })

vi.mock('vue-router', () => ({
  useRoute: () => routeState,
  useRouter: () => mocks.router
}))

vi.mock('@/api', () => ({
  authApi: mocks.authApi,
  downloadApi: mocks.downloadApi,
  notificationApi: mocks.notificationApi
}))

vi.mock('@/stores/financeWorkspace', () => ({
  useFinanceWorkspaceStore: () => mocks.financeWorkspace
}))

vi.mock('@/stores/financeCompany', () => ({
  useFinanceCompanyStore: () => mocks.financeCompany
}))

vi.mock('@/utils/permissions', () => ({
  EXPENSE_CREATE_ENTRY_PERMISSION_CODES: ['expense:create:create', 'expense:create:submit'],
  hasAnyPermission: (requiredCodes: string[], owned: { permissionCodes?: string[] } | string[] | null) => {
    const permissionCodes = Array.isArray(owned)
      ? owned
      : owned?.permissionCodes || permissionState.codes
    return requiredCodes.some((code) => permissionCodes.includes(code))
  }
}))

vi.mock('@/utils/downloadCenter', () => ({
  onDownloadCenterOpen: vi.fn(() => mocks.stopDownloadListener)
}))

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage
}))

vi.mock('@element-plus/icons-vue', () => ({
  ArrowDown: { template: '<span />' },
  Bell: { template: '<span />' },
  Coin: { template: '<span />' },
  Download: { template: '<span />' },
  FolderOpened: { template: '<span />' },
  House: { template: '<span />' },
  Money: { template: '<span />' },
  Plus: { template: '<span />' },
  Search: { template: '<span />' },
  Setting: { template: '<span />' },
  SwitchButton: { template: '<span />' },
  User: { template: '<span />' },
  UserFilled: { template: '<span />' },
  Wallet: { template: '<span />' }
}))

const MenuStub = defineComponent({
  template: '<div><slot /></div>'
})

const SubMenuStub = defineComponent({
  props: {
    index: {
      type: String,
      default: ''
    }
  },
  template: '<section :data-submenu="index"><div><slot name="title" /></div><div><slot /></div></section>'
})

const MenuItemStub = defineComponent({
  props: {
    index: {
      type: String,
      default: ''
    }
  },
  template: '<div :data-index="index"><slot /></div>'
})

const ButtonStub = defineComponent({
  inheritAttrs: false,
  emits: ['click'],
  template: '<button type="button" v-bind="$attrs" @click="$emit(\'click\', $event)"><slot /></button>'
})

const BadgeStub = defineComponent({
  props: {
    value: {
      type: [String, Number],
      default: ''
    },
    hidden: {
      type: Boolean,
      default: false
    }
  },
  template: '<div :data-badge-value="value" :data-badge-hidden="hidden"><slot /></div>'
})

const FinanceWorkspaceTabsStub = defineComponent({
  props: {
    currentCompanyId: {
      type: String,
      default: ''
    }
  },
  emits: ['change-company'],
  template: `
    <div data-testid="finance-workspace-tabs" :data-company-id="currentCompanyId">
      <button type="button" data-testid="company-switch" @click="$emit('change-company', 'COMPANY_B')">切换公司</button>
    </div>
  `
})

const RouterViewStub = defineComponent({
  setup(_, { slots }) {
    const component = defineComponent({
      template: '<div data-testid="route-view" />'
    })
    return () => slots.default?.({
      Component: component,
      route: routeState
    })
  }
})

async function mountView(permissionCodes: string[]) {
  permissionState.codes = permissionCodes
  const currentUser = {
    userId: 1,
    username: 'admin',
    name: '管理员',
    roles: [],
    permissionCodes
  }
  localStorage.setItem('user', JSON.stringify(currentUser))
  mocks.authApi.getCurrentUser.mockResolvedValue({ data: currentUser })

  const wrapper = mount(MainLayout, {
    global: {
      stubs: {
        'download-center-drawer': defineComponent({ template: '<div data-testid="download-drawer" />' }),
        'el-avatar': true,
        'el-badge': BadgeStub,
        'el-button': ButtonStub,
        'el-dropdown': MenuStub,
        'el-dropdown-item': MenuItemStub,
        'el-dropdown-menu': MenuStub,
        'el-icon': true,
        'el-input': true,
        'el-menu': MenuStub,
        'el-menu-item': MenuItemStub,
        'el-sub-menu': SubMenuStub,
        'finance-workspace-tabs': FinanceWorkspaceTabsStub,
        'keep-alive': defineComponent({
          setup(_, { slots }) {
            return () => slots.default?.()
          }
        }),
        'notification-center-drawer': defineComponent({ template: '<div data-testid="notification-drawer" />' }),
        'router-view': RouterViewStub
      }
    }
  })

  await flushPromises()
  return wrapper
}

describe('MainLayout', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    routeState.path = '/dashboard'
    routeState.fullPath = '/dashboard'
    mocks.financeWorkspace.tabs = []
    mocks.financeWorkspace.activePath = '/dashboard'
    mocks.financeWorkspace.isFinancePath = vi.fn(() => false)
    mocks.financeCompany.companyOptions = []
    mocks.financeCompany.currentCompanyId = ''
    mocks.downloadApi.getCenter.mockResolvedValue({ data: { inProgress: [{ id: 1 }] } })
    mocks.notificationApi.getSummary.mockResolvedValue({ data: { unreadCount: 2 } })
  })

  it('shows process management when the user only has process permission', async () => {
    const wrapper = await mountView(['expense:process_management:view'])

    expect(wrapper.find('[data-submenu="/expense/workbench"]').exists()).toBe(true)
    expect(wrapper.find('[data-index="/expense/workbench/process-management"]').exists()).toBe(true)
    expect(wrapper.find('[data-index="/expense/workbench/budget-management"]').exists()).toBe(false)
  })

  it('shows payment entries when the user has payment permissions', async () => {
    const wrapper = await mountView(['expense:payment:payment_order:view', 'expense:payment:bank_link:view'])

    expect(wrapper.find('[data-submenu="/expense/payment"]').exists()).toBe(true)
    expect(wrapper.find('[data-index="/expense/payment/orders"]').exists()).toBe(true)
    expect(wrapper.find('[data-index="/expense/payment/bank-link"]').exists()).toBe(true)
  })

  it('shows finance system management when the user has the finance system permission', async () => {
    const wrapper = await mountView(['finance:system_management:view'])

    expect(wrapper.find('[data-submenu="/finance"]').exists()).toBe(true)
    expect(wrapper.find('[data-index="/finance/system-management"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('财务系统管理')
  })

  it('shows project archive when the user has the project archive permission', async () => {
    const wrapper = await mountView(['finance:archives:projects:view'])

    expect(wrapper.find('[data-submenu="/finance"]').exists()).toBe(true)
    expect(wrapper.find('[data-submenu="/finance/archives"]').exists()).toBe(true)
    expect(wrapper.find('[data-index="/finance/archives/projects"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('项目档案')
  })

  it('shows the agent entry when the user has agent permission', async () => {
    const wrapper = await mountView(['agents:view'])

    expect(wrapper.find('[data-index="/archives/agents"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Agent')
  })

  it('shows notification and download triggers without breaking layout initialization', async () => {
    const wrapper = await mountView(['profile:view', 'profile:downloads:view'])

    expect(wrapper.find('[data-testid="download-trigger"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="notification-trigger"]').exists()).toBe(true)
    expect(mocks.downloadApi.getCenter).toHaveBeenCalledTimes(1)
    expect(mocks.notificationApi.getSummary).toHaveBeenCalledTimes(1)
  })

  it('keeps the sidebar fixed and gives the navigation area its own scroll container', async () => {
    const wrapper = await mountView([
      'expense:process_management:view',
      'expense:payment:payment_order:view',
      'expense:payment:bank_link:view',
      'finance:system_management:view'
    ])

    const sidebar = wrapper.get('[data-testid="main-layout-sidebar"]')
    const sidebarScroll = wrapper.get('[data-testid="main-layout-sidebar-scroll"]')

    expect(sidebar.classes()).toContain('sticky')
    expect(sidebar.classes()).toContain('top-16')
    expect(sidebar.classes()).toContain('overflow-hidden')
    expect(sidebar.classes()).toContain('h-[calc(100vh-64px)]')
    expect(sidebarScroll.classes()).toContain('overflow-y-auto')
    expect(sidebarScroll.classes()).toContain('overflow-x-hidden')
    expect(sidebar.find('[data-submenu="/expense/workbench"]').exists()).toBe(true)
    expect(sidebar.find('[data-submenu="/expense/payment"]').exists()).toBe(true)
  })

  it('turns the top-left brand area into a dashboard shortcut', async () => {
    const wrapper = await mountView(['profile:view'])

    const brand = wrapper.get('[data-testid="main-layout-brand"]')

    expect(brand.attributes('aria-label')).toBe('Go to dashboard')
    expect(brand.classes()).toContain('main-layout-brand')

    await brand.trigger('click')

    expect(mocks.router.push).toHaveBeenCalledWith('/dashboard')
  })

  it('passes finance company state to workspace tabs and forwards switch events', async () => {
    routeState.path = '/finance/general-ledger/new-voucher'
    routeState.fullPath = '/finance/general-ledger/new-voucher'
    mocks.financeWorkspace.tabs = [{ path: '/finance/general-ledger/new-voucher' }]
    mocks.financeWorkspace.activePath = '/finance/general-ledger/new-voucher'
    mocks.financeWorkspace.isFinancePath = vi.fn(() => true)
    mocks.financeCompany.companyOptions = [{ companyId: 'COMPANY_A', label: '公司 A' }]
    mocks.financeCompany.currentCompanyId = 'COMPANY_A'
    mocks.financeCompany.switchCompany.mockResolvedValue(true)

    const wrapper = await mountView(['finance:general_ledger:new_voucher:view'])

    expect(wrapper.find('[data-testid="finance-workspace-tabs"]').attributes('data-company-id')).toBe('COMPANY_A')
    await wrapper.find('[data-testid="company-switch"]').trigger('click')
    expect(mocks.financeCompany.switchCompany).toHaveBeenCalledWith('COMPANY_B')
  })
})
