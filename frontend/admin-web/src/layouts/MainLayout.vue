<template>
  <div class="min-h-screen bg-gray-50">
    <header class="bg-white shadow-sm sticky top-0 z-50">
      <div class="flex items-center justify-between h-16 px-6">
        <div class="flex items-center gap-3">
          <div class="w-9 h-9 bg-blue-600 rounded-lg flex items-center justify-center">
            <el-icon :size="20" class="text-white"><Money /></el-icon>
          </div>
          <span class="text-xl font-bold text-gray-800">FinEx</span>
        </div>

        <div class="flex-1 max-w-xl mx-8">
          <el-input
            v-model="searchQuery"
            placeholder="搜索报销单、发票、流程模板..."
            class="w-full"
            :prefix-icon="Search"
          />
        </div>

        <div class="flex items-center gap-4">
          <el-button type="primary" :icon="Plus" @click="showNewExpense = true">
            新建报销
          </el-button>

          <el-badge :value="downloadPendingCount" :hidden="downloadPendingCount === 0" class="cursor-pointer">
            <el-button circle text @click="downloadDrawerVisible = true">
              <el-icon :size="20" class="text-gray-600 hover:text-gray-800"><Download /></el-icon>
            </el-button>
          </el-badge>

          <el-badge :value="3" class="cursor-pointer">
            <el-button circle text>
              <el-icon :size="20" class="text-gray-600 hover:text-gray-800"><Bell /></el-icon>
            </el-button>
          </el-badge>

          <el-dropdown trigger="click" @command="handleUserCommand">
            <div class="flex items-center gap-2 cursor-pointer">
              <el-avatar :size="36" :icon="UserFilled" class="bg-blue-600" />
              <span class="text-sm text-gray-700">{{ userName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item :icon="User" command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item :icon="Setting" command="settings">系统设置</el-dropdown-item>
                <el-dropdown-item divided :icon="SwitchButton" command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <div class="flex">
      <aside class="w-64 bg-white shadow-sm min-h-[calc(100vh-64px)] sticky top-16">
        <el-menu :default-active="activeMenu" class="border-0 py-4" router>
          <el-menu-item index="/dashboard" class="menu-level-1">
            <el-icon><House /></el-icon>
            <template #title>首页</template>
          </el-menu-item>

          <el-sub-menu index="/expense" class="menu-level-1">
            <template #title>
              <el-icon><Wallet /></el-icon>
              <span>报销管理</span>
            </template>
            <el-menu-item index="/expense/create" class="menu-level-2">新建报销</el-menu-item>
            <el-menu-item index="/expense/list" class="menu-level-2">我的报销</el-menu-item>
            <el-menu-item index="/expense/approval" class="menu-level-2">待我审批</el-menu-item>
            <el-sub-menu index="/expense/payment" class="menu-level-2">
              <template #title>
                <span>支付</span>
              </template>
              <el-menu-item index="/expense/payment/bank-link" class="menu-level-3">银企直连</el-menu-item>
            </el-sub-menu>
            <el-menu-item index="/expense/documents" class="menu-level-2">单据查询</el-menu-item>
            <el-menu-item index="/expense/voucher-generation" class="menu-level-2">凭证生成</el-menu-item>
            <el-sub-menu index="/expense/workbench" class="menu-level-2">
              <template #title>
                <span>管理工作台</span>
              </template>
              <el-menu-item index="/expense/workbench/process-management" class="menu-level-3">流程管理</el-menu-item>
              <el-menu-item index="/expense/workbench/budget-management" class="menu-level-3">预算管理</el-menu-item>
            </el-sub-menu>
          </el-sub-menu>

          <el-sub-menu index="/finance" class="menu-level-1">
            <template #title>
              <el-icon><Coin /></el-icon>
              <span>财务管理</span>
            </template>
            <el-sub-menu index="/finance/general-ledger" class="menu-level-2">
              <template #title>
                <span>总账</span>
              </template>
              <el-menu-item index="/finance/general-ledger/new-voucher" class="menu-level-3">新建凭证</el-menu-item>
              <el-menu-item index="/finance/general-ledger/query-voucher" class="menu-level-3">查询凭证</el-menu-item>
              <el-menu-item index="/finance/general-ledger/review-voucher" class="menu-level-3">审核凭证</el-menu-item>
              <el-menu-item index="/finance/general-ledger/balance-sheet" class="menu-level-3">余额表</el-menu-item>
            </el-sub-menu>
            <el-menu-item index="/finance/fixed-assets" class="menu-level-2">固定资产</el-menu-item>
            <el-sub-menu index="/finance/reports" class="menu-level-2">
              <template #title>
                <span>财务报表</span>
              </template>
              <el-menu-item index="/finance/reports/balance-sheet" class="menu-level-3">资产负债表</el-menu-item>
              <el-menu-item index="/finance/reports/income-statement" class="menu-level-3">利润表</el-menu-item>
              <el-menu-item index="/finance/reports/cash-flow" class="menu-level-3">现金流量表</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="/finance/archives" class="menu-level-2">
              <template #title>
                <span>会计档案</span>
              </template>
              <el-menu-item index="/finance/archives/customers" class="menu-level-3">客户档案</el-menu-item>
              <el-menu-item index="/finance/archives/suppliers" class="menu-level-3">供应商档案</el-menu-item>
              <el-menu-item index="/finance/archives/employees" class="menu-level-3">员工档案</el-menu-item>
              <el-menu-item index="/finance/archives/departments" class="menu-level-3">部门档案</el-menu-item>
            </el-sub-menu>
          </el-sub-menu>

          <el-sub-menu index="/archives" class="menu-level-1">
            <template #title>
              <el-icon><FolderOpened /></el-icon>
              <span>电子档案</span>
            </template>
            <el-menu-item index="/archives/invoices" class="menu-level-2">发票管理</el-menu-item>
            <el-menu-item index="/archives/account-books" class="menu-level-2">账套管理</el-menu-item>
          </el-sub-menu>

          <el-menu-item index="/settings" class="menu-level-1">
            <el-icon><Setting /></el-icon>
            <template #title>系统设置</template>
          </el-menu-item>
        </el-menu>
      </aside>

      <main class="flex-1 p-6">
        <router-view />
      </main>
    </div>

    <new-expense-dialog v-model="showNewExpense" />
    <download-center-drawer
      v-model="downloadDrawerVisible"
      @loaded="handleDownloadLoaded"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi, downloadApi, type UserProfile } from '@/api'
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
import NewExpenseDialog from '@/components/NewExpenseDialog.vue'
import DownloadCenterDrawer from '@/components/DownloadCenterDrawer.vue'

const route = useRoute()
const router = useRouter()

const searchQuery = ref('')
const showNewExpense = ref(false)
const downloadDrawerVisible = ref(false)
const downloadPendingCount = ref(0)
const currentUser = ref<UserProfile | null>(null)

const activeMenu = computed(() => route.path)
const userName = computed(() => currentUser.value?.name || currentUser.value?.username || '未登录用户')

const loadCurrentUser = async () => {
  const cachedUser = localStorage.getItem('user')
  if (cachedUser) {
    currentUser.value = JSON.parse(cachedUser) as UserProfile
  }

  try {
    const res = await authApi.getCurrentUser()
    if (res.code === 200) {
      currentUser.value = res.data
      localStorage.setItem('user', JSON.stringify(res.data))
    }
  } catch {
    // 登录失效时由请求层统一处理
  }
}

const loadDownloadCount = async () => {
  try {
    const res = await downloadApi.getCenter()
    if (res.code === 200) {
      downloadPendingCount.value = res.data.inProgress.length
    }
  } catch {
    downloadPendingCount.value = 0
  }
}

onMounted(() => {
  loadCurrentUser()
  loadDownloadCount()
})

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  ElMessage.success('已退出登录')
  router.push('/login')
}

const handleUserCommand = (command: string) => {
  if (command === 'profile') {
    router.push('/profile')
    return
  }

  if (command === 'settings') {
    router.push('/settings')
    return
  }

  if (command === 'logout') {
    logout()
  }
}

const handleDownloadLoaded = (count: number) => {
  downloadPendingCount.value = count
}
</script>
