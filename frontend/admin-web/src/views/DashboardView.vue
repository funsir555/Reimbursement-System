<template>
  <div class="space-y-6" v-loading="loading">
    <!-- 页面标题 -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-800">首页</h1>
        <p class="text-gray-500 mt-1">欢迎回来，{{ dashboard?.user.name || dashboard?.user.username || '同事' }}！今日待处理事项概览</p>
      </div>
      <div class="text-right">
        <p class="text-sm text-gray-500">{{ currentDate }}</p>
      </div>
    </div>
    
    <!-- 统计卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      <!-- 待我审批 -->
      <el-card class="hover:shadow-lg transition-shadow cursor-pointer">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-500 mb-1">待我审批</p>
            <p class="text-3xl font-bold text-blue-600">{{ dashboard?.pendingApprovalCount || 0 }}</p>
            <p class="text-xs text-gray-400 mt-2">较昨日 +{{ dashboard?.pendingApprovalDelta || 0 }}</p>
          </div>
          <div class="w-12 h-12 bg-blue-50 rounded-xl flex items-center justify-center">
            <el-icon :size="24" class="text-blue-600"><Timer /></el-icon>
          </div>
        </div>
      </el-card>
      
      <!-- 本月报销 -->
      <el-card class="hover:shadow-lg transition-shadow cursor-pointer">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-500 mb-1">本月报销</p>
            <p class="text-3xl font-bold text-green-600">¥{{ formatCurrency(dashboard?.monthlyExpenseAmount) }}</p>
            <p class="text-xs text-gray-400 mt-2">共 {{ dashboard?.monthlyExpenseCount || 0 }} 笔</p>
          </div>
          <div class="w-12 h-12 bg-green-50 rounded-xl flex items-center justify-center">
            <el-icon :size="24" class="text-green-600"><Money /></el-icon>
          </div>
        </div>
      </el-card>
      
      <!-- 发票数量 -->
      <el-card class="hover:shadow-lg transition-shadow cursor-pointer">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-500 mb-1">发票数量</p>
            <p class="text-3xl font-bold text-purple-600">{{ dashboard?.invoiceCount || 0 }}</p>
            <p class="text-xs text-gray-400 mt-2">本月新增 {{ dashboard?.monthlyInvoiceCount || 0 }} 张</p>
          </div>
          <div class="w-12 h-12 bg-purple-50 rounded-xl flex items-center justify-center">
            <el-icon :size="24" class="text-purple-600"><Ticket /></el-icon>
          </div>
        </div>
      </el-card>
      
      <!-- 预算剩余 -->
      <el-card class="hover:shadow-lg transition-shadow cursor-pointer">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-500 mb-1">预算剩余</p>
            <p class="text-3xl font-bold text-orange-600">¥{{ formatCurrency(dashboard?.budgetRemaining) }}</p>
            <p class="text-xs text-gray-400 mt-2">本月预算使用率 {{ dashboard?.budgetUsageRate || 0 }}%</p>
          </div>
          <div class="w-12 h-12 bg-orange-50 rounded-xl flex items-center justify-center">
            <el-icon :size="24" class="text-orange-600"><Wallet /></el-icon>
          </div>
        </div>
      </el-card>
    </div>
    
    <!-- 快捷操作 & 最近报销 -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- 快捷操作 -->
      <el-card class="lg:col-span-1">
        <template #header>
          <div class="flex items-center justify-between">
            <span class="font-semibold">快捷操作</span>
          </div>
        </template>
        
        <div class="grid grid-cols-2 gap-4">
          <div 
            v-for="action in quickActions" 
            :key="action.name"
            class="flex flex-col items-center justify-center p-4 rounded-xl bg-gray-50 hover:bg-blue-50 cursor-pointer transition-colors group"
            @click="action.handler"
          >
            <div class="w-12 h-12 rounded-xl flex items-center justify-center mb-2 transition-colors"
                 :class="action.bgClass">
              <component :is="action.icon" :size="24" :class="action.iconClass" />
            </div>
            <span class="text-sm text-gray-700 group-hover:text-blue-600">{{ action.name }}</span>
          </div>
        </div>
      </el-card>
      
      <!-- 最近报销单 -->
      <el-card class="lg:col-span-2">
        <template #header>
          <div class="flex items-center justify-between">
            <span class="font-semibold">最近报销单</span>
            <el-button text type="primary" @click="$router.push('/expense/list')">
              查看全部 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </template>
        
        <el-table :data="dashboard?.recentExpenses || []" style="width: 100%">
          <el-table-column prop="no" label="单号" width="140">
            <template #default="{ row }">
              <span class="text-blue-600 cursor-pointer hover:underline">{{ row.no }}</span>
            </template>
          </el-table-column>
          
          <el-table-column prop="type" label="类型" width="100" />
          
          <el-table-column prop="amount" label="金额" width="120">
            <template #default="{ row }">
              <span class="font-medium">¥{{ formatCurrency(row.amount) }}</span>
            </template>
          </el-table-column>
          
          <el-table-column prop="date" label="日期" width="120" />
          
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button link type="primary" size="small">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
    
    <!-- 待审批 & 发票提醒 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- 待我审批 -->
      <el-card>
        <template #header>
          <div class="flex items-center justify-between">
            <span class="font-semibold">待我审批</span>
            <el-tag type="danger" size="small">{{ dashboard?.pendingApprovalCount || 0 }} 待处理</el-tag>
          </div>
        </template>
        
        <div class="space-y-4">
          <div 
            v-for="item in dashboard?.pendingApprovals || []" 
            :key="item.id"
            class="flex items-center justify-between p-4 rounded-lg bg-gray-50 hover:bg-blue-50 transition-colors cursor-pointer"
          >
            <div class="flex items-center gap-4">
              <el-avatar :size="40" :src="item.avatar" />
              <div>
                <p class="font-medium text-gray-800">{{ item.title }}</p>
                <p class="text-sm text-gray-500">{{ item.submitter }} · {{ item.time }}</p>
              </div>
            </div>
            <div class="text-right">
              <p class="font-bold text-gray-800">¥{{ formatCurrency(item.amount) }}</p>
              <el-button size="small" type="primary">审批</el-button>
            </div>
          </div>
        </div>
      </el-card>
      
      <!-- 发票异常提醒 -->
      <el-card>
        <template #header>
          <div class="flex items-center justify-between">
            <span class="font-semibold">发票异常提醒</span>
            <el-tag type="warning" size="small">{{ (dashboard?.invoiceAlerts || []).length }} 待处理</el-tag>
          </div>
        </template>
        
        
        <div class="space-y-4">
          <div 
            v-for="item in dashboard?.invoiceAlerts || []" 
            :key="item.id"
            class="flex items-start gap-4 p-4 rounded-lg bg-gray-50 hover:bg-orange-50 transition-colors"
          >
            <div class="w-10 h-10 rounded-lg bg-orange-100 flex items-center justify-center flex-shrink-0">
              <el-icon :size="20" class="text-orange-600"><Warning /></el-icon>
            </div>
            
            <div class="flex-1">
              <p class="font-medium text-gray-800">{{ item.title }}</p>
              <p class="text-sm text-gray-500 mt-1">{{ item.desc }}</p>
              <p class="text-xs text-gray-400 mt-2">{{ item.time }}</p>
            </div>
            
            <el-button size="small" type="primary" text>处理</el-button>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { dashboardApi, type DashboardData } from '@/api'
import {
  Money, Ticket, Document, Plus, ArrowRight,
  Timer, Wallet, Warning
} from '@element-plus/icons-vue'
import { formatMoney } from '@/utils/money'

const loading = ref(false)
const dashboard = ref<DashboardData | null>(null)

const currentDate = computed(() => {
  const date = new Date()
  const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${weekDays[date.getDay()]}`
})

const quickActions = [
  {
    name: '新建报销',
    icon: 'Plus',
    bgClass: 'bg-blue-100',
    iconClass: 'text-blue-600',
    handler: () => {}
  },
  {
    name: '发票录入',
    icon: 'Ticket',
    bgClass: 'bg-purple-100',
    iconClass: 'text-purple-600',
    handler: () => {}
  },
  {
    name: '审批中心',
    icon: 'Document',
    bgClass: 'bg-green-100',
    iconClass: 'text-green-600',
    handler: () => {}
  },
  {
    name: '我的报销',
    icon: 'Money',
    bgClass: 'bg-orange-100',
    iconClass: 'text-orange-600',
    handler: () => {}
  }
]

onMounted(async () => {
  loading.value = true
  try {
    const res = await dashboardApi.getOverview()
    if (res.code === 200) {
      dashboard.value = res.data
      return
    }
    ElMessage.error(res.message || '加载首页数据失败')
  } catch (error: any) {
    ElMessage.error(error.message || '加载首页数据失败')
  } finally {
    loading.value = false
  }
})

const formatCurrency = (amount?: string) => formatMoney(amount || '0.00')

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    '审批中': 'warning',
    '已通过': 'success',
    '已驳回': 'danger',
    '草稿': 'info'
  }
  return map[status] || 'info'
}
</script>
