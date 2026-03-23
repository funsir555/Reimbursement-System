<template>
  <div class="space-y-6">
    <!-- 页面标题 -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-800">首页</h1>
        <p class="text-gray-500 mt-1">欢迎回来，张经理！今日待处理事项概览</p>
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
            <p class="text-3xl font-bold text-blue-600">12</p>
            <p class="text-xs text-gray-400 mt-2">较昨日 +3</p>
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
            <p class="text-3xl font-bold text-green-600">¥8,520</p>
            <p class="text-xs text-gray-400 mt-2">共 6 笔</p>
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
            <p class="text-3xl font-bold text-purple-600">156</p>
            <p class="text-xs text-gray-400 mt-2">本月新增 23 张</p>
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
            <p class="text-3xl font-bold text-orange-600">¥45,200</p>
            <p class="text-xs text-gray-400 mt-2">本月预算使用率 32%</p>
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
        
        <el-table :data="recentExpenses" style="width: 100%">
          <el-table-column prop="no" label="单号" width="140">
            <template #default="{ row }">
              <span class="text-blue-600 cursor-pointer hover:underline">{{ row.no }}</span>
            </template>
          </el-table-column>
          
          <el-table-column prop="type" label="类型" width="100" />
          
          <el-table-column prop="amount" label="金额" width="120">
            <template #default="{ row }">
              <span class="font-medium">¥{{ row.amount.toLocaleString() }}</span>
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
            <el-tag type="danger" size="small">12 待处理</el-tag>
          </div>
        </template>
        
        <div class="space-y-4">
          <div 
            v-for="item in pendingApprovals" 
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
              <p class="font-bold text-gray-800">¥{{ item.amount.toLocaleString() }}</p>
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
            <el-tag type="warning" size="small">3 待处理</el-tag>
          </div>
        </template>
        
        
        <div class="space-y-4">
          <div 
            v-for="item in invoiceAlerts" 
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
import { ref, computed } from 'vue'
import {
  Money, Ticket, Document, Plus, ArrowRight,
  Timer, Wallet, Warning
} from '@element-plus/icons-vue'

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

const recentExpenses = ref([
  { no: 'BX20250323001', type: '差旅费', amount: 2850, date: '2025-03-23', status: '审批中' },
  { no: 'BX20250322002', type: '办公费', amount: 456, date: '2025-03-22', status: '已通过' },
  { no: 'BX20250321001', type: '招待费', amount: 1280, date: '2025-03-21', status: '已通过' },
  { no: 'BX20250320001', type: '交通费', amount: 68, date: '2025-03-20', status: '已驳回' },
])

const pendingApprovals = ref([
  {
    id: 1,
    title: '上海出差费用报销',
    submitter: '李明',
    time: '10分钟前',
    amount: 3245,
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=1'
  },
  {
    id: 2,
    title: '客户招待费用',
    submitter: '王芳',
    time: '30分钟前',
    amount: 1580,
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=2'
  },
  {
    id: 3,
    title: '季度办公用品采购',
    submitter: '张伟',
    time: '1小时前',
    amount: 8920,
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=3'
  }
])

const invoiceAlerts = ref([
  {
    id: 1,
    title: '发票重复报销',
    desc: '发票代码：011001900211，该发票已于 2025-03-15 报销',
    time: '30分钟前'
  },
  {
    id: 2,
    title: '发票验真失败',
    desc: '发票代码：031001900211，国家税务总局查验失败',
    time: '1小时前'
  },
  {
    id: 3,
    title: '发票即将过期',
    desc: '3张发票将在7天内超过报销期限',
    time: '2小时前'
  }
])

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
