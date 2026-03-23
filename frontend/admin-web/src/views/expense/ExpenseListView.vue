<template>
  <div class="space-y-6">
    <!-- 页面标题 -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-800">我的报销</h1>
        <p class="text-gray-500 mt-1">管理您的所有报销单据</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="$emit('new-expense')">
        新建报销
      </el-button>
    </div>

    <!-- 搜索筛选区 -->
    <el-card>
      <div class="flex flex-wrap gap-4">
        <el-input
          v-model="searchQuery"
          placeholder="搜索单号/事由"
          class="w-64"
          :prefix-icon="Search"
        />
        <el-select v-model="filterStatus" placeholder="报销状态" class="w-40">
          <el-option label="全部" value="" />
          <el-option label="审批中" value="pending" />
          <el-option label="已通过" value="approved" />
          <el-option label="已驳回" value="rejected" />
          <el-option label="草稿" value="draft" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
        <el-button type="primary" :icon="Search">查询</el-button>
        <el-button :icon="Refresh">重置</el-button>
      </div>
    </el-card>

    <!-- 数据表格 -->
    <el-card>
      <el-table :data="expenseList" style="width: 100%" v-loading="loading">
        <el-table-column prop="no" label="报销单号" width="150">
          <template #default="{ row }">
            <span class="text-blue-600 cursor-pointer hover:underline font-medium">
              {{ row.no }}
            </span>
          </template>
        </el-table-column>
        
        <el-table-column prop="type" label="报销类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="getTypeTagType(row.type)">
              {{ row.type }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="reason" label="报销事由" show-overflow-tooltip />
        
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">
            <span class="font-bold text-gray-800">¥{{ row.amount.toLocaleString() }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="date" label="提交日期" width="120" />
        
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small">查看</el-button>
            <el-button v-if="row.status === '草稿'" link type="primary" size="small">编辑</el-button>
            <el-button v-if="row.status === '草稿'" link type="danger" size="small">删除</el-button>
            <el-button v-if="row.status === '已驳回'" link type="warning" size="small">重新提交</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="flex justify-end mt-6">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'

const searchQuery = ref('')
const filterStatus = ref('')
const dateRange = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(56)

const expenseList = ref([
  { no: 'BX20250323001', type: '差旅费', reason: '上海出差费用', amount: 2850, date: '2025-03-23', status: '审批中' },
  { no: 'BX20250322002', type: '办公费', reason: '办公用品采购', amount: 456, date: '2025-03-22', status: '已通过' },
  { no: 'BX20250321001', type: '招待费', reason: '客户接待费用', amount: 1280, date: '2025-03-21', status: '已通过' },
  { no: 'BX20250320001', type: '交通费', reason: '市内交通', amount: 68, date: '2025-03-20', status: '已驳回' },
  { no: 'BX20250319001', type: '差旅费', reason: '北京出差费用', amount: 5200, date: '2025-03-19', status: '已通过' },
])

const getTypeTagType = (type: string) => {
  const map: Record<string, string> = {
    '差旅费': 'primary',
    '交通费': 'success',
    '招待费': 'warning',
    '办公费': 'info'
  }
  return map[type] || ''
}

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    '审批中': 'warning',
    '已通过': 'success',
    '已驳回': 'danger',
    '草稿': 'info'
  }
  return map[status] || 'info'
}

defineEmits(['new-expense'])
</script>
