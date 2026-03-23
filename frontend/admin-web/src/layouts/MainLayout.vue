<template>
  <div class="min-h-screen bg-gray-50">
    <!-- 顶部导航栏 -->
    <header class="bg-white shadow-sm sticky top-0 z-50">
      <div class="flex items-center justify-between h-16 px-6">
        <!-- Logo -->
        <div class="flex items-center gap-3">
          <div class="w-9 h-9 bg-blue-600 rounded-lg flex items-center justify-center">
            <el-icon :size="20" class="text-white"><Money /></el-icon>
          </div>
          <span class="text-xl font-bold text-gray-800">FinEx</span>
        </div>
        
        <!-- 搜索框 -->
        <div class="flex-1 max-w-xl mx-8">
          <el-input
            v-model="searchQuery"
            placeholder="搜索报销单、发票、审批..."
            class="w-full"
            :prefix-icon="Search"
          />
        </div>
        
        <!-- 右侧功能区 -->
        <div class="flex items-center gap-4">
          <el-button type="primary" :icon="Plus" @click="showNewExpense = true">
            新建报销
          </el-button>
          
          <el-badge :value="3" class="cursor-pointer">
            <el-icon :size="20" class="text-gray-600 hover:text-gray-800"><Bell /></el-icon>
          </el-badge>
          
          <el-dropdown trigger="click">
            <div class="flex items-center gap-2 cursor-pointer">
              <el-avatar :size="36" :icon="UserFilled" class="bg-blue-600" />
              <span class="text-sm text-gray-700">张经理</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item :icon="User">个人中心</el-dropdown-item>
                <el-dropdown-item :icon="Setting">系统设置</el-dropdown-item>
                <el-dropdown-item divided :icon="SwitchButton" @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>
    
    <div class="flex">
      <!-- 侧边栏 -->
      <aside class="w-64 bg-white shadow-sm min-h-[calc(100vh-64px)] sticky top-16">
        <el-menu
          :default-active="activeMenu"
          class="border-0 py-4"
          router
        >
          <el-menu-item index="/dashboard">
            <el-icon><HomeFilled /></el-icon>
            <template #title>首页</template>
          </el-menu-item>
          
          
          <el-sub-menu index="/expense">
            <template #title>
              <el-icon><Document /></el-icon>
              <span>报销管理</span>
            </template>
            <el-menu-item index="/expense/create">新建报销</el-menu-item>
            <el-menu-item index="/expense/list">我的报销</el-menu-item>
            <el-menu-item index="/expense/approval">待我审批</el-menu-item>
            <el-menu-item index="/expense/audit">财务审核</el-menu-item>
          </el-sub-menu>
          
          
          <el-sub-menu index="/invoice">
            <template #title>
              <el-icon><Ticket /></el-icon>
              <span>发票管理</span>
            </template>
            <el-menu-item index="/invoice/upload">发票录入</el-menu-item>
            <el-menu-item index="/invoice/list">发票库</el-menu-item>
            <el-menu-item index="/invoice/verify">发票验真</el-menu-item>
          </el-sub-menu>
          
          <el-menu-item index="/payment">
            <el-icon><CreditCard /></el-icon>
            <template #title>银企直连</template>
          </el-menu-item>
          
          <el-menu-item index="/voucher">
            <el-icon><Files /></el-icon>
            <template #title>凭证管理</template>
          </el-menu-item>
          
          <el-menu-item index="/report">
            <el-icon><TrendCharts /></el-icon>
            <template #title>报表分析</template>
          </el-menu-item>
          
          <el-sub-menu index="/setting">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统设置</span>
            </template>
            <el-menu-item index="/setting/flow">审批流程</el-menu-item>
            <el-menu-item index="/setting/org">组织架构</el-menu-item>
            <el-menu-item index="/setting/budget">预算设置</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </aside>
      
      <!-- 主内容区 -->
      <main class="flex-1 p-6">
        <router-view />
      </main>
    </div>
    
    <!-- 新建报销弹窗 -->
    <new-expense-dialog v-model="showNewExpense" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Money, Search, Bell, ArrowDown, Plus,
  UserFilled, User, Setting, SwitchButton,
  HomeFilled, Document, Ticket, CreditCard, Files, TrendCharts
} from '@element-plus/icons-vue'
import NewExpenseDialog from '@/components/NewExpenseDialog.vue'

const route = useRoute()
const router = useRouter()

const searchQuery = ref('')
const showNewExpense = ref(false)

const activeMenu = computed(() => route.path)

const logout = () => {
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>
