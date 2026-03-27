<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-800">我的报销</h1>
        <p class="mt-1 text-gray-500">管理您提交的报销单据</p>
      </div>
      <el-button
        v-if="canAny(['expense:create:view', 'expense:create:create'])"
        type="primary"
        :icon="Plus"
        @click="$emit('new-expense')"
      >
        新建报销
      </el-button>
    </div>

    <el-card>
      <div class="flex flex-wrap gap-4">
        <el-input
          v-model="searchQuery"
          placeholder="搜索单号或事由"
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
        <el-button type="primary" :icon="Search" @click="currentPage = 1">查询</el-button>
        <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
      </div>
    </el-card>

    <el-card>
      <el-table :data="pagedExpenseList" style="width: 100%" v-loading="loading">
        <el-table-column prop="no" label="报销单号" width="150">
          <template #default="{ row }">
            <span class="cursor-pointer font-medium text-blue-600 hover:underline">
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

        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small">查看</el-button>
            <el-button v-if="row.status === '草稿' && can('expense:list:edit')" link type="primary" size="small">编辑</el-button>
            <el-button v-if="row.status === '草稿' && can('expense:list:delete')" link type="danger" size="small">删除</el-button>
            <el-button v-if="row.status === '已驳回' && can('expense:list:submit')" link type="warning" size="small">重新提交</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-6 flex justify-end">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="filteredExpenseList.length"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { expenseApi, type ExpenseSummary } from '@/api'
import { hasAnyPermission, hasPermission, readStoredUser } from '@/utils/permissions'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'

const searchQuery = ref('')
const filterStatus = ref('')
const dateRange = ref<string[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const expenseList = ref<ExpenseSummary[]>([])
const permissionCodes = ref(readStoredUser()?.permissionCodes || [])

const can = (code: string) => hasPermission(code, permissionCodes.value)
const canAny = (codes: string[]) => hasAnyPermission(codes, permissionCodes.value)

onMounted(async () => {
  loading.value = true
  try {
    const res = await expenseApi.list()
    expenseList.value = res.data
  } catch (error: any) {
    ElMessage.error(error.message || '加载报销列表失败')
  } finally {
    loading.value = false
  }
})

const filteredExpenseList = computed(() =>
  expenseList.value.filter((item) => {
    const matchedKeyword =
      !searchQuery.value ||
      item.no.includes(searchQuery.value) ||
      item.reason.includes(searchQuery.value)

    const matchedStatus =
      !filterStatus.value ||
      (filterStatus.value === 'pending' && item.status === '审批中') ||
      (filterStatus.value === 'approved' && item.status === '已通过') ||
      (filterStatus.value === 'rejected' && item.status === '已驳回') ||
      (filterStatus.value === 'draft' && item.status === '草稿')

    const [startDate, endDate] = dateRange.value
    const matchedDate = !startDate || !endDate || (item.date >= startDate && item.date <= endDate)

    return matchedKeyword && matchedStatus && matchedDate
  })
)

const pagedExpenseList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredExpenseList.value.slice(start, start + pageSize.value)
})

const getTypeTagType = (type: string) => {
  const map: Record<string, string> = {
    差旅费: 'primary',
    交通费: 'success',
    招待费: 'warning',
    办公费: 'info'
  }
  return map[type] || ''
}

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    审批中: 'warning',
    已通过: 'success',
    已驳回: 'danger',
    草稿: 'info'
  }
  return map[status] || 'info'
}

const resetFilters = () => {
  searchQuery.value = ''
  filterStatus.value = ''
  dateRange.value = []
  currentPage.value = 1
}

defineEmits(['new-expense'])
</script>
