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
          <template v-for="item in visibleNavigationMenu" :key="item.key">
            <el-menu-item v-if="!item.children?.length" :index="item.index" class="menu-level-1">
              <span v-if="item.iconKey === 'Agent'" class="flex items-center gap-2">
                <pixel-duck-bot-icon class="h-[18px] w-[18px] text-amber-600" />
                <span>{{ item.title }}</span>
              </span>
              <template v-else>
                <el-icon v-if="item.iconKey"><component :is="resolveMenuIcon(item.iconKey)" /></el-icon>
                <span>{{ item.title }}</span>
              </template>
            </el-menu-item>

            <el-sub-menu v-else :index="item.index" class="menu-level-1">
              <template #title>
                <el-icon v-if="item.iconKey"><component :is="resolveMenuIcon(item.iconKey)" /></el-icon>
                <span>{{ item.title }}</span>
              </template>

              <template v-for="child in item.children" :key="child.key">
                <el-menu-item v-if="!child.children?.length" :index="child.index" class="menu-level-2">
                  {{ child.title }}
                </el-menu-item>

                <el-sub-menu v-else :index="child.index" class="menu-level-2">
                  <template #title>
                    <span>{{ child.title }}</span>
                  </template>
                  <el-menu-item
                    v-for="grandchild in child.children"
                    :key="grandchild.key"
                    :index="grandchild.index"
                    class="menu-level-3"
                  >
                    {{ grandchild.title }}
                  </el-menu-item>
                </el-sub-menu>
              </template>
            </el-sub-menu>
          </template>
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
import { MAIN_NAVIGATION_MENU, filterVisibleNavigationMenu, type NavigationIconKey } from '@/router/navigation-config'
import { getRouteMenuPermissionCodes, resolveRouteMeta } from '@/router/route-meta'
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

const PROFILE_MENU_PERMISSION_CODES = getRouteMenuPermissionCodes(resolveRouteMeta('profile'))
const SETTINGS_MENU_PERMISSION_CODES = getRouteMenuPermissionCodes(resolveRouteMeta('settings'))

const activeMenu = computed(() => route.path)
const userName = computed(() => currentUser.value?.name || currentUser.value?.username || '未登录用户')
const permissionCodes = computed(() => currentUser.value?.permissionCodes || [])
const visibleNavigationMenu = computed(() => filterVisibleNavigationMenu(MAIN_NAVIGATION_MENU, permissionCodes.value))

const MENU_ICON_MAP = {
  House,
  Wallet,
  Coin,
  FolderOpened,
  Setting
} satisfies Record<Exclude<NavigationIconKey, 'Agent'>, unknown>

const canAny = (codes: string[]) => hasAnyPermission(codes, permissionCodes.value)

const canViewProfile = computed(() => canAny(PROFILE_MENU_PERMISSION_CODES))
const canOpenSettings = computed(() => canAny(SETTINGS_MENU_PERMISSION_CODES))
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

function resolveMenuIcon(iconKey?: NavigationIconKey) {
  if (!iconKey || iconKey === 'Agent') {
    return null
  }
  return MENU_ICON_MAP[iconKey]
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
