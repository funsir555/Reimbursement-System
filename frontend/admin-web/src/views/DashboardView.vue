<template>
  <div class="expense-wb-page dashboard-workbench space-y-6" v-loading="loading">
    <section class="expense-wb-hero dashboard-workbench__hero">
      <div class="expense-wb-hero__content dashboard-workbench__hero-content">
        <div class="dashboard-workbench__hero-head">
          <div>
            <p class="dashboard-workbench__eyebrow">Expense Center</p>
            <h1 class="expense-wb-hero__title">首页工作台</h1>
            <p class="dashboard-workbench__hero-subtitle">
              {{ dashboard?.user.name || dashboard?.user.username || '同事' }}，以下是你当前最需要处理的事项。
            </p>
          </div>
          <div class="dashboard-workbench__hero-date">{{ currentDate }}</div>
        </div>

        <div class="dashboard-workbench__hero-stats" data-testid="dashboard-stat-grid">
          <button
            v-for="stat in statCards"
            :key="stat.key"
            :data-testid="`dashboard-stat-card-${stat.key}`"
            type="button"
            class="expense-wb-stat-card expense-wb-stat-card--compact dashboard-workbench__hero-stat"
            :class="{ 'is-placeholder': stat.placeholder }"
            @click="handleStatClick(stat)"
          >
            <div class="expense-wb-stat-card__top">
              <div class="dashboard-workbench__hero-stat-content">
                <p class="expense-wb-stat-card__label">{{ stat.label }}</p>
                <p class="expense-wb-stat-card__value">{{ stat.value }}</p>
                <p class="dashboard-workbench__hero-stat-tip">{{ stat.tip }}</p>
              </div>
              <span class="expense-wb-stat-card__icon" :class="`expense-wb-stat-card__icon--${stat.tone}`">
                <el-icon :size="20">
                  <component :is="stat.icon" />
                </el-icon>
              </span>
            </div>
          </button>
        </div>
      </div>
    </section>

    <el-card class="expense-wb-panel dashboard-workbench__section">
      <template #header>
        <div class="expense-wb-table-shell__header !p-0 !border-0">
          <div>
            <p class="expense-wb-table-shell__title">最近常用模块</p>
            <p class="expense-wb-table-shell__desc">
              自动记录最近访问的业务模块，方便快速回到常用入口。
            </p>
          </div>
        </div>
      </template>

      <div class="dashboard-workbench__module-grid">
        <button
          v-for="module in recentModules"
          :key="module.path"
          type="button"
          class="expense-wb-choice-card dashboard-workbench__module-card"
          @click="openPath(module.path)"
        >
          <div class="expense-wb-choice-card__header">
            <div>
              <p class="expense-wb-choice-card__title">{{ module.label }}</p>
              <p class="expense-wb-choice-card__subtitle">{{ module.description }}</p>
            </div>
            <span class="expense-wb-stat-card__icon expense-wb-stat-card__icon--blue dashboard-workbench__module-icon">
              <el-icon :size="20">
                <component :is="moduleIconMap[module.iconKey] || Grid" />
              </el-icon>
            </span>
          </div>
          <div class="expense-wb-choice-card__footer">
            <span>点击进入模块</span>
            <span>&rarr;</span>
          </div>
        </button>
      </div>
    </el-card>

    <el-card class="expense-wb-panel expense-wb-table-shell dashboard-workbench__section">
      <template #header>
        <div class="expense-wb-table-shell__header !p-0 !border-0">
          <div>
            <p class="expense-wb-table-shell__title">最近报销单</p>
            <p class="expense-wb-table-shell__desc">
              优先展示你最近提交或更新的报销单，方便继续跟踪进度。
            </p>
          </div>
          <el-button text type="primary" @click="openPath('/expense/list')">
            查看全部 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </template>

      <el-table :data="dashboard?.recentExpenses || []" style="width: 100%">
        <el-table-column prop="documentCode" label="单据编号" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <button
              class="cursor-pointer font-medium text-blue-600 hover:underline"
              type="button"
              @click="openExpenseDetail(row.documentCode || row.no)"
            >
              {{ row.documentCode || row.no }}
            </button>
          </template>
        </el-table-column>
        <el-table-column prop="documentTitle" label="单据标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="templateName" label="模板名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="documentStatusLabel" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.documentStatusLabel || row.status)">
              {{ row.documentStatusLabel || row.status || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submittedAt" label="提交日期" width="168" show-overflow-tooltip />
        <el-table-column prop="amount" label="金额" width="140">
          <template #default="{ row }">
            <span class="font-semibold text-slate-800">&yen; {{ formatAmount(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openExpenseDetail(row.documentCode || row.no)">
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, type Component } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Coin,
  Connection,
  DocumentChecked,
  Grid,
  Plus,
  Search,
  Select,
  Tickets,
  Wallet
} from '@element-plus/icons-vue'
import { dashboardApi, type DashboardData } from '@/api'
import { formatMoney } from '@/utils/money'
import { hasAnyPermission } from '@/utils/permissions'
import {
  DASHBOARD_DATA_CHANGED_EVENT,
  resolveRecentModules,
  type DashboardRecentModuleItem
} from './dashboard/dashboardRecentModules'

const router = useRouter()
const loading = ref(false)
const dashboard = ref<DashboardData | null>(null)
const recentModules = ref<DashboardRecentModuleItem[]>([])

const moduleIconMap: Record<string, Component> = {
  Plus,
  Tickets,
  Select,
  Search,
  Wallet,
  Coin,
  DocumentChecked,
  Connection,
  Grid
}

const currentDate = computed(() => {
  const date = new Date()
  const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${weekDays[date.getDay()]}`
})

const statCards = computed(() => [
  {
    key: 'approval',
    label: '待我审批',
    value: dashboard.value?.pendingApprovalCount || 0,
    tip: '进入审批工作台处理待办任务',
    icon: Select,
    tone: 'blue',
    path: '/expense/approval'
  },
  {
    key: 'repayment',
    label: '待还款',
    value: dashboard.value?.pendingRepaymentCount || 0,
    tip: '查看借款单待核销余额并发起还款核销',
    icon: Wallet,
    tone: 'amber',
    path: '/dashboard/pending-repayments'
  },
  {
    key: 'prepay',
    label: '待核销',
    value: dashboard.value?.pendingPrepayWriteOffCount || 0,
    tip: '查看预付未到票报销单并核销预付款',
    icon: Coin,
    tone: 'green',
    path: '/dashboard/pending-prepay-writeoffs'
  },
  {
    key: 'application',
    label: '未使用申请',
    value: dashboard.value?.unusedApplicationCount || 0,
    tip: '入口先保留占位，后续接入真实业务',
    icon: Tickets,
    tone: 'rose',
    placeholder: true
  },
  {
    key: 'contract',
    label: '未付款合同单',
    value: dashboard.value?.unpaidContractCount || 0,
    tip: '入口先保留占位，后续接入真实业务',
    icon: DocumentChecked,
    tone: 'blue',
    placeholder: true
  }
])

onMounted(() => {
  void loadDashboard()
  window.addEventListener(DASHBOARD_DATA_CHANGED_EVENT, loadDashboard)
})

onUnmounted(() => {
  window.removeEventListener(DASHBOARD_DATA_CHANGED_EVENT, loadDashboard)
})

async function loadDashboard() {
  loading.value = true
  try {
    const res = await dashboardApi.getOverview()
    dashboard.value = res.data
    recentModules.value = resolveRecentModules(res.data.user)
  } catch (error: any) {
    ElMessage.error(error.message || '加载首页数据失败')
  } finally {
    loading.value = false
  }
}

function formatAmount(value?: string) {
  return formatMoney(value || '0.00')
}

function statusTagType(status?: string) {
  const map: Record<string, string> = {
    '审批中': 'warning',
    '待支付': 'warning',
    '支付中': 'warning',
    '已通过': 'success',
    '已支付': 'success',
    '已完成': 'success',
    '已驳回': 'danger',
    '支付异常': 'danger',
    '草稿': 'info',
    '流程异常': 'info'
  }
  return map[status || ''] || 'info'
}

function handleStatClick(stat: { path?: string; placeholder?: boolean }) {
  if (stat.placeholder) {
    ElMessage.info('该模块暂未接入真实业务')
    return
  }
  if (stat.path) {
    openPath(stat.path)
  }
}

function openPath(path: string) {
  const permissionMap: Record<string, string[]> = {
    '/expense/approval': ['expense:approval:view'],
    '/dashboard/pending-repayments': ['dashboard:view'],
    '/dashboard/pending-prepay-writeoffs': ['dashboard:view'],
    '/expense/list': ['expense:list:view']
  }
  const requiredPermissions = permissionMap[path]
  if (requiredPermissions && !hasAnyPermission(requiredPermissions, dashboard.value?.user)) {
    ElMessage.warning('当前没有该模块访问权限')
    return
  }
  void router.push(path)
}

function openExpenseDetail(documentCode?: string) {
  if (!documentCode) {
    return
  }
  void router.push(`/expense/documents/${encodeURIComponent(documentCode)}`)
}
</script>
