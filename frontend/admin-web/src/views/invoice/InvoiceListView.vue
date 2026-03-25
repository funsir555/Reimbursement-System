<template>
  <div class="space-y-6">
    <!-- 页面标题 -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-800">发票库</h1>
        <p class="text-gray-500 mt-1">管理您的所有发票</p>
      </div>
      <el-button type="primary" :icon="Upload">
        上传发票
      </el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <el-card>
        <div class="text-center">
          <p class="text-3xl font-bold text-blue-600">156</p>
          <p class="text-sm text-gray-500 mt-1">发票总数</p>
        </div>
      </el-card>
      <el-card>
        <div class="text-center">
          <p class="text-3xl font-bold text-green-600">142</p>
          <p class="text-sm text-gray-500 mt-1">已验真</p>
        </div>
      </el-card>
      <el-card>
        <div class="text-center">
          <p class="text-3xl font-bold text-orange-600">12</p>
          <p class="text-sm text-gray-500 mt-1">待验真</p>
        </div>
      </el-card>
      <el-card>
        <div class="text-center">
          <p class="text-3xl font-bold text-red-600">2</p>
          <p class="text-sm text-gray-500 mt-1">异常发票</p>
        </div>
      </el-card>
    </div>

    <!-- 搜索筛选 -->
    <el-card>
      <div class="flex flex-wrap gap-4">
        <el-input
          v-model="searchQuery"
          placeholder="搜索发票代码/号码"
          class="w-64"
          :prefix-icon="Search"
        />
        <el-select v-model="filterType" placeholder="发票类型" class="w-40">
          <el-option label="全部" value="" />
          <el-option label="增值税专用发票" value="special" />
          <el-option label="增值税普通发票" value="normal" />
          <el-option label="电子发票" value="electronic" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="验真状态" class="w-40">
          <el-option label="全部" value="" />
          <el-option label="已验真" value="verified" />
          <el-option label="待验真" value="pending" />
          <el-option label="验真失败" value="failed" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="currentPage = 1">查询</el-button>
        <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
      </div>
    </el-card>

    <!-- 发票列表 -->
    <el-card>
      <el-table :data="pagedInvoiceList" style="width: 100%" v-loading="loading">
        <el-table-column prop="code" label="发票代码" width="120" />
        <el-table-column prop="number" label="发票号码" width="120" />
        <el-table-column prop="type" label="发票类型" width="140">
          <template #default="{ row }">
            <el-tag size="small">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="seller" label="销售方" show-overflow-tooltip />
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">
            <span class="font-medium">¥{{ row.amount.toLocaleString() }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="date" label="开票日期" width="120" />
        <el-table-column prop="status" label="验真状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getVerifyType(row.status)" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small">查看</el-button>
            <el-button v-if="row.status !== '已验真'" link type="primary" size="small">验真</el-button>
            <el-button link type="danger" size="small">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="flex justify-end mt-6">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="filteredInvoiceList.length"
          layout="total, prev, pager, next"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { invoiceApi, type InvoiceSummary } from '@/api'
import { Upload, Search, Refresh } from '@element-plus/icons-vue'

const searchQuery = ref('')
const filterType = ref('')
const filterStatus = ref('')
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const invoiceList = ref<InvoiceSummary[]>([])

onMounted(async () => {
  loading.value = true
  try {
    const res = await invoiceApi.list()
    if (res.code === 200) {
      invoiceList.value = res.data
      return
    }
    ElMessage.error(res.message || '加载发票列表失败')
  } catch (error: any) {
    ElMessage.error(error.message || '加载发票列表失败')
  } finally {
    loading.value = false
  }
})

const filteredInvoiceList = computed(() => {
  return invoiceList.value.filter((item) => {
    const matchedKeyword =
      !searchQuery.value ||
      item.code.includes(searchQuery.value) ||
      item.number.includes(searchQuery.value)

    const matchedType =
      !filterType.value ||
      (filterType.value === 'special' && item.type === '增值税专用发票') ||
      (filterType.value === 'normal' && item.type === '增值税普通发票') ||
      (filterType.value === 'electronic' && item.type === '电子发票')

    const matchedStatus =
      !filterStatus.value ||
      (filterStatus.value === 'verified' && item.status === '已验真') ||
      (filterStatus.value === 'pending' && item.status === '待验真') ||
      (filterStatus.value === 'failed' && item.status === '验真失败')

    return matchedKeyword && matchedType && matchedStatus
  })
})

const pagedInvoiceList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredInvoiceList.value.slice(start, start + pageSize.value)
})

const getVerifyType = (status: string) => {
  const map: Record<string, string> = {
    '已验真': 'success',
    '待验真': 'warning',
    '验真失败': 'danger'
  }
  return map[status] || 'info'
}

const resetFilters = () => {
  searchQuery.value = ''
  filterType.value = ''
  filterStatus.value = ''
  currentPage.value = 1
}
</script>
