<template>
  <div class="min-h-screen bg-gray-50">
    <header class="sticky top-0 z-50 bg-white shadow-sm">
      <div class="flex h-16 items-center justify-between px-6">
        <div class="flex items-center gap-3">
          <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-600">
            <el-icon :size="20" class="text-white"><Money /></el-icon>
          </div>
          <span class="text-xl font-bold text-gray-800">FinEx</span>
        </div>

        <div class="mx-8 max-w-xl flex-1">
          <el-input
            v-model="searchQuery"
            placeholder="搜索报销单、发票、流程模板、Agent"
            class="w-full"
            :prefix-icon="Search"
          />
        </div>

        <div class="flex items-center gap-4">
          <el-button
            v-if="canAny([...EXPENSE_CREATE_ENTRY_PERMISSION_CODES])"
            type="primary"
            :icon="Plus"
            @click="goCreateExpense"
          >
            新建报销
          </el-button>

          <el-badge
            v-if="canAny(['profile:view', 'profile:downloads:view'])"
            :value="downloadPendingCount"
            :hidden="downloadPendingCount === 0"
            class="cursor-pointer"
            data-testid="download-badge"
          >
            <el-button circle text data-testid="download-trigger" @click="downloadDrawerVisible = true">
              <el-icon :size="20" class="text-gray-600 hover:text-gray-800"><Download /></el-icon>
            </el-button>
          </el-badge>

          <el-badge
            :value="notificationUnreadCount"
            :hidden="notificationUnreadCount === 0"
            class="cursor-pointer"
            data-testid="notification-badge"
          >
            <el-button circle text data-testid="notification-trigger" @click="notificationDrawerVisible = true">
              <el-icon :size="20" class="text-gray-600 hover:text-gray-800"><Bell /></el-icon>
            </el-button>
          </el-badge>

          <el-dropdown trigger="click" @command="handleUserCommand">
            <div class="flex cursor-pointer items-center gap-2">
              <el-avatar :size="36" :icon="UserFilled" class="bg-blue-600" />
              <span class="text-sm text-gray-700">{{ userName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="canViewProfile" :icon="User" command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item v-if="canOpenSettings" :icon="Setting" command="settings">系统设置</el-dropdown-item>
                <el-dropdown-item divided :icon="SwitchButton" command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <div class="flex">
      <aside class="sticky top-16 min-h-[calc(100vh-64px)] w-64 bg-white shadow-sm">
        <el-menu :default-active="activeMenu" class="border-0 py-4" router>
          <el-menu-item v-if="canShowDashboard" index="/dashboard" class="menu-level-1">
            <el-icon><House /></el-icon>
            <template #title>首页</template>
          </el-menu-item>

          <el-sub-menu v-if="canShowExpense" index="/expense" class="menu-level-1">
            <template #title>
              <el-icon><Wallet /></el-icon>
              <span>报销管理</span>
            </template>
            <el-menu-item
              v-if="canAny([...EXPENSE_CREATE_ENTRY_PERMISSION_CODES])"
              index="/expense/create"
              class="menu-level-2"
            >
              新建报销
            </el-menu-item>
            <el-menu-item v-if="canAny(['expense:list:view'])" index="/expense/list" class="menu-level-2">
              我的报销
            </el-menu-item>
            <el-menu-item v-if="canAny(['expense:approval:view'])" index="/expense/approval" class="menu-level-2">
              待我审批
            </el-menu-item>
            <el-sub-menu
              v-if="canAny(['expense:payment:payment_order:view', 'expense:payment:bank_link:view'])"
              index="/expense/payment"
              class="menu-level-2"
            >
              <template #title>
                <span>支付</span>
              </template>
              <el-menu-item
                v-if="canAny(['expense:payment:payment_order:view'])"
                index="/expense/payment/orders"
                class="menu-level-3"
              >
                付款单
              </el-menu-item>
              <el-menu-item
                v-if="canAny(['expense:payment:bank_link:view'])"
                index="/expense/payment/bank-link"
                class="menu-level-3"
              >
                银企直连
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-if="canAny(['expense:documents:view'])" index="/expense/documents" class="menu-level-2">
              单据查询
            </el-menu-item>
            <el-menu-item
              v-if="canAny(['expense:voucher_generation:view'])"
              index="/expense/voucher-generation"
              class="menu-level-2"
            >
              凭证生成
            </el-menu-item>
            <el-sub-menu v-if="canShowWorkbench" index="/expense/workbench" class="menu-level-2">
              <template #title>
                <span>管理工作台</span>
              </template>
              <el-menu-item
                v-if="canAny(['expense:process_management:view'])"
                index="/expense/workbench/process-management"
                class="menu-level-3"
              >
                流程管理
              </el-menu-item>
              <el-menu-item
                v-if="canAny(['expense:budget_management:view'])"
                index="/expense/workbench/budget-management"
                class="menu-level-3"
              >
                预算管理
              </el-menu-item>
            </el-sub-menu>
          </el-sub-menu>

          <el-sub-menu v-if="canShowFinance" index="/finance" class="menu-level-1">
            <template #title>
              <el-icon><Coin /></el-icon>
              <span>财务管理</span>
            </template>
            <el-sub-menu
              v-if="canAny([
                'finance:general_ledger:new_voucher:view',
                'finance:general_ledger:query_voucher:view',
                'finance:general_ledger:review_voucher:view',
                'finance:general_ledger:balance_sheet:view',
                'finance:general_ledger:detail_ledger:view',
                'finance:general_ledger:general_ledger:view',
                'finance:general_ledger:project_detail_ledger:view',
                'finance:general_ledger:supplier_detail_ledger:view',
                'finance:general_ledger:customer_detail_ledger:view',
                'finance:general_ledger:personal_detail_ledger:view',
                'finance:general_ledger:quantity_amount_detail_ledger:view'
              ])"
              index="/finance/general-ledger"
              class="menu-level-2"
            >
              <template #title>
                <span>总账</span>
              </template>
              <el-menu-item
                v-if="canAny(['finance:general_ledger:new_voucher:view'])"
                index="/finance/general-ledger/new-voucher"
                class="menu-level-3"
              >
                新建凭证
              </el-menu-item>
              <el-menu-item
                v-if="canAny(['finance:general_ledger:query_voucher:view'])"
                index="/finance/general-ledger/query-voucher"
                class="menu-level-3"
              >
                查询凭证
              </el-menu-item>
              <el-menu-item
                v-if="canAny(['finance:general_ledger:review_voucher:view'])"
                index="/finance/general-ledger/review-voucher"
                class="menu-level-3"
              >
                审核凭证
              </el-menu-item>
              <el-menu-item
                v-if="canAny(['finance:general_ledger:balance_sheet:view'])"
                index="/finance/general-ledger/balance-sheet"
                class="menu-level-3"
              >
                总账余额表
              </el-menu-item>
              <el-menu-item
                v-if="canAny(['finance:general_ledger:detail_ledger:view'])"
                index="/finance/general-ledger/detail-ledger"
                class="menu-level-3"
              >
                明细账
              </el-menu-item>
              <el-menu-item
                v-if="canAny(['finance:general_ledger:general_ledger:view'])"
                index="/finance/general-ledger/general-ledger"
                class="menu-level-3"
              >
                总分类账
              </el-menu-item>
              <el-menu-item
                v-if="canAny(['finance:general_ledger:project_detail_ledger:view'])"
                index="/finance/general-ledger/project-detail-ledger"
                class="menu-level-3"
              >
                项目明细账
              </el-menu-item>
              <el-menu-item
                v-if="canAny(['finance:general_ledger:supplier_detail_ledger:view'])"
                index="/finance/general-ledger/supplier-detail-ledger"
                class="menu-level-3"
              >
                供应商明细账
              </el-menu-item>
              <el-menu-item
                v-if="canAny(['finance:general_ledger:customer_detail_ledger:view'])"
                index="/finance/general-ledger/customer-detail-ledger"
                class="menu-level-3"
              >
                客户明细账
              </el-menu-item>
              <el-menu-item
                v-if="canAny(['finance:general_ledger:personal_detail_ledger:view'])"
                index="/finance/general-ledger/personal-detail-ledger"
                class="menu-level-3"
              >
                个人明细账
              </el-menu-item>
              <el-menu-item
                v-if="canAny(['finance:general_ledger:quantity_amount_detail_ledger:view'])"
                index="/finance/general-ledger/quantity-amount-detail-ledger"
                class="menu-level-3"
              >
                数量金额明细账
              </el-menu-item>
            </el-sub-menu>

            <el-menu-item v-if="canAny(['finance:fixed_assets:view'])" index="/finance/fixed-assets" class="menu-level-2">
              固定资产
            </el-menu-item>

            <el-sub-menu
              v-if="canAny([
                'finance:reports:balance_sheet:view',
                'finance:reports:income_statement:view',
                'finance:reports:cash_flow:view'
              ])"
              index="/finance/reports"
              class="menu-level-2"
            >
              <template #title>
                <span>财务报表</span>
              </template>
              <el-menu-item
                v-if="canAny(['finance:reports:balance_sheet:view'])"
                index="/finance/reports/balance-sheet"
                class="menu-level-3"
              >
                资产负债表
              </el-menu-item>
              <el-menu-item
                v-if="canAny(['finance:reports:income_statement:view'])"
                index="/finance/reports/income-statement"
                class="menu-level-3"
              >
                利润表
              </el-menu-item>
              <el-menu-item
                v-if="canAny(['finance:reports:cash_flow:view'])"
                index="/finance/reports/cash-flow"
                class="menu-level-3"
              >
                现金流量表
              </el-menu-item>
            </el-sub-menu>

            <el-sub-menu
              v-if="canAny([
                'finance:archives:customers:view',
                'finance:archives:suppliers:view',
                'finance:archives:employees:view',
                'finance:archives:departments:view',
                'finance:archives:account_subjects:view'
              ])"
              index="/finance/archives"
              class="menu-level-2"
            >
              <template #title>
                <span>会计档案</span>
              </template>
              <el-menu-item
                v-if="canAny(['finance:archives:customers:view'])"
                index="/finance/archives/customers"
                class="menu-level-3"
              >
                客户档案
              </el-menu-item>
              <el-menu-item
                v-if="canAny(['finance:archives:suppliers:view'])"
                index="/finance/archives/suppliers"
                class="menu-level-3"
              >
                供应商档案
              </el-menu-item>
              <el-menu-item
                v-if="canAny(['finance:archives:employees:view'])"
                index="/finance/archives/employees"
                class="menu-level-3"
              >
                员工档案
              </el-menu-item>
              <el-menu-item
                v-if="canAny(['finance:archives:departments:view'])"
                index="/finance/archives/departments"
                class="menu-level-3"
              >
                部门档案
              </el-menu-item>
              <el-menu-item
                v-if="canAny(['finance:archives:account_subjects:view'])"
                index="/finance/archives/account-subjects"
                class="menu-level-3"
              >
                会计科目
              </el-menu-item>
            </el-sub-menu>
          </el-sub-menu>

          <el-sub-menu v-if="canShowArchives" index="/archives" class="menu-level-1">
            <template #title>
              <el-icon><FolderOpened /></el-icon>
              <span>电子档案</span>
            </template>
            <el-menu-item v-if="canAny(['archives:invoices:view'])" index="/archives/invoices" class="menu-level-2">
              发票管理
            </el-menu-item>
            <el-menu-item
              v-if="canAny(['archives:account_books:view'])"
              index="/archives/account-books"
              class="menu-level-2"
            >
              账套管理
            </el-menu-item>
          </el-sub-menu>

          <el-menu-item v-if="canShowAgent" index="/archives/agents" class="menu-level-1">
            <span class="flex items-center gap-2">
              <pixel-duck-bot-icon class="h-[18px] w-[18px] text-amber-600" />
              <span>Agent</span>
            </span>
          </el-menu-item>

          <el-menu-item v-if="canOpenSettings" index="/settings" class="menu-level-1">
            <el-icon><Setting /></el-icon>
            <template #title>系统设置</template>
          </el-menu-item>
        </el-menu>
      </aside>

      <main class="flex-1 overflow-hidden p-6">
        <div class="flex h-[calc(100vh-88px)] flex-col overflow-hidden rounded-[28px] border border-slate-200/70 bg-white/70 shadow-sm">
          <finance-workspace-tabs
            v-if="showFinanceTabs"
            :tabs="financeWorkspace.tabs"
            :active-path="financeWorkspace.activePath"
            :company-options="financeCompany.companyOptions"
            :current-company-id="financeCompany.currentCompanyId"
            :company-loading="financeCompany.loading"
            :company-switching="financeCompany.switching"
            @select="handleFinanceTabSelect"
            @close="handleFinanceTabClose"
            @close-others="handleFinanceCloseOthers"
            @close-right="handleFinanceCloseRight"
            @change-company="handleFinanceCompanyChange"
          />

          <div class="min-h-0 flex-1 overflow-auto p-6">
            <router-view v-slot="{ Component, route: viewRoute }">
              <keep-alive>
                <component
                  :is="Component"
                  v-if="isFinancePath(viewRoute.path)"
                  :key="viewRoute.fullPath"
                />
              </keep-alive>
              <component
                :is="Component"
                v-if="!isFinancePath(viewRoute.path)"
                :key="viewRoute.fullPath"
              />
            </router-view>
          </div>
        </div>
      </main>
    </div>

    <download-center-drawer
      v-model="downloadDrawerVisible"
      :refresh-key="downloadDrawerRefreshKey"
      @loaded="handleDownloadLoaded"
    />
    <notification-center-drawer
      v-model="notificationDrawerVisible"
      @changed="handleNotificationChanged"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi, downloadApi, notificationApi, type UserProfile } from '@/api'
import DownloadCenterDrawer from '@/components/DownloadCenterDrawer.vue'
import NotificationCenterDrawer from '@/components/NotificationCenterDrawer.vue'
import PixelDuckBotIcon from '@/components/icons/PixelDuckBotIcon.vue'
import FinanceWorkspaceTabs from '@/components/finance/FinanceWorkspaceTabs.vue'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { useFinanceWorkspaceStore } from '@/stores/financeWorkspace'
import { onDownloadCenterOpen } from '@/utils/downloadCenter'
import { EXPENSE_CREATE_ENTRY_PERMISSION_CODES, hasAnyPermission } from '@/utils/permissions'
import {
  Money,
  Search,
  Bell,
  Download,
  ArrowDown,
  Plus,
  UserFilled,
  User,
  Setting,
  SwitchButton,
  House,
  Wallet,
  Coin,
  FolderOpened
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const financeCompany = useFinanceCompanyStore()
const financeWorkspace = useFinanceWorkspaceStore()

const searchQuery = ref('')
const downloadDrawerVisible = ref(false)
const downloadDrawerRefreshKey = ref(0)
const downloadPendingCount = ref(0)
const notificationDrawerVisible = ref(false)
const notificationUnreadCount = ref(0)
const currentUser = ref<UserProfile | null>(null)

const activeMenu = computed(() => route.path)
const userName = computed(() => currentUser.value?.name || currentUser.value?.username || '未登录用户')
const permissionCodes = computed(() => currentUser.value?.permissionCodes || [])

const canAny = (codes: string[]) => hasAnyPermission(codes, permissionCodes.value)

const canViewProfile = computed(() => canAny(['profile:menu', 'profile:view']))
const canOpenSettings = computed(() =>
  canAny([
    'settings:menu',
    'settings:organization:view',
    'settings:employees:view',
    'settings:roles:view',
    'settings:companies:view',
    'settings:company_accounts:view'
  ])
)
const canShowDashboard = computed(() => canAny(['dashboard:menu', 'dashboard:view']))
const canShowExpense = computed(() =>
  canAny([
    'expense:menu',
    ...EXPENSE_CREATE_ENTRY_PERMISSION_CODES,
    'expense:list:view',
    'expense:approval:view',
    'expense:payment:payment_order:view',
    'expense:payment:bank_link:view',
    'expense:documents:view',
    'expense:voucher_generation:view',
    'expense:process_management:view',
    'expense:budget_management:view'
  ])
)
const canShowWorkbench = computed(() =>
  canAny([
    'expense:process_management:view',
    'expense:budget_management:view'
  ])
)
const canShowFinance = computed(() =>
  canAny([
    'finance:menu',
    'finance:general_ledger:new_voucher:view',
    'finance:general_ledger:query_voucher:view',
    'finance:general_ledger:review_voucher:view',
    'finance:general_ledger:balance_sheet:view',
    'finance:general_ledger:detail_ledger:view',
    'finance:general_ledger:general_ledger:view',
    'finance:general_ledger:project_detail_ledger:view',
    'finance:general_ledger:supplier_detail_ledger:view',
    'finance:general_ledger:customer_detail_ledger:view',
    'finance:general_ledger:personal_detail_ledger:view',
    'finance:general_ledger:quantity_amount_detail_ledger:view',
    'finance:fixed_assets:view',
    'finance:reports:balance_sheet:view',
    'finance:reports:income_statement:view',
    'finance:reports:cash_flow:view',
    'finance:archives:customers:view',
    'finance:archives:suppliers:view',
    'finance:archives:employees:view',
    'finance:archives:departments:view',
    'finance:archives:account_subjects:view'
  ])
)
const canShowArchives = computed(() => canAny(['archives:menu', 'archives:invoices:view', 'archives:account_books:view']))
const canShowAgent = computed(() => canAny(['agents:menu', 'agents:view']))
const showFinanceTabs = computed(() => isFinancePath(route.path) && financeWorkspace.tabs.length > 0)

const loadCurrentUser = async () => {
  const cachedUser = localStorage.getItem('user')
  if (cachedUser) {
    currentUser.value = JSON.parse(cachedUser) as UserProfile
  }

  try {
    const res = await authApi.getCurrentUser()
    currentUser.value = res.data
    localStorage.setItem('user', JSON.stringify(res.data))
  } catch {
    // Keep cached user to avoid blocking shell rendering.
  }
}

const loadDownloadCount = async () => {
  if (!canAny(['profile:view', 'profile:downloads:view'])) {
    downloadPendingCount.value = 0
    return
  }

  try {
    const res = await downloadApi.getCenter()
    downloadPendingCount.value = res.data.inProgress.length
  } catch {
    downloadPendingCount.value = 0
  }
}

const loadNotificationSummary = async () => {
  try {
    const res = await notificationApi.getSummary()
    notificationUnreadCount.value = res.data.unreadCount || 0
  } catch {
    notificationUnreadCount.value = 0
  }
}

onMounted(async () => {
  await loadCurrentUser()
  await loadDownloadCount()
  await loadNotificationSummary()
})

const stopListeningDownloadCenter = onDownloadCenterOpen(() => {
  downloadDrawerVisible.value = true
  downloadDrawerRefreshKey.value += 1
  void loadDownloadCount()
})

onBeforeUnmount(() => {
  stopListeningDownloadCenter()
})

watch(
  () => route.fullPath,
  () => {
    financeWorkspace.syncRoute(route)
  },
  { immediate: true }
)

watch(notificationDrawerVisible, (visible) => {
  if (visible) {
    void loadNotificationSummary()
  }
})

watch(
  () => [route.path, currentUser.value?.companyId] as const,
  ([path, companyId]) => {
    if (!isFinancePath(path)) {
      return
    }
    void financeCompany.ensureInitialized(companyId)
  },
  { immediate: true }
)

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  financeCompany.reset()
  ElMessage.success('已退出登录')
  void router.push('/login')
}

const goCreateExpense = () => {
  void router.push('/expense/create')
}

const handleUserCommand = (command: string) => {
  if (command === 'profile') {
    if (!canViewProfile.value) return
    void router.push('/profile')
    return
  }

  if (command === 'settings') {
    if (!canOpenSettings.value) return
    void router.push('/settings')
    return
  }

  if (command === 'logout') {
    logout()
  }
}

const handleDownloadLoaded = (count: number) => {
  downloadPendingCount.value = count
}

const handleNotificationChanged = () => {
  void loadNotificationSummary()
}

function isFinancePath(path: string) {
  return financeWorkspace.isFinancePath(path)
}

function handleFinanceTabSelect(path: string) {
  financeWorkspace.activate(path)
  void router.push(path)
}

function handleFinanceTabClose(path: string) {
  const nextPath = financeWorkspace.getNextPathAfterClose(path)
  const closingCurrent = route.fullPath === path
  financeWorkspace.close(path)
  if (closingCurrent) {
    void router.push(nextPath || '/dashboard')
  }
}

function handleFinanceCloseOthers(path: string) {
  financeWorkspace.closeOthers(path)
  if (route.fullPath !== path) {
    void router.push(path)
  }
}

function handleFinanceCloseRight(path: string) {
  financeWorkspace.closeToRight(path)
  if (!financeWorkspace.tabs.some((item) => item.path === route.fullPath)) {
    void router.push(path)
  }
}

async function handleFinanceCompanyChange(companyId: string) {
  await financeCompany.switchCompany(companyId)
}
</script>
