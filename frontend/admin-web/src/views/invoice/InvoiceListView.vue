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
        <el-button type="primary" :icon="Search">查询</el-button>
        <el-button :icon="Refresh">重置</el-button>
      </div>
    </el-card>

    <!-- 发票列表 -->
    <el-card>
      <el-table :data="invoiceList" style="width: 100%">
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
          :total="total"
          layout="total, prev, pager, next"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Upload, Search, Refresh } from '@element-plus/icons-vue'

const searchQuery = ref('')
const filterType = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(156)

const invoiceList = ref([
  { code: '011001900211', number: '12345678', type: '增值税普通发票', seller: '北京科技有限公司', amount: 2850, date: '2025-03-20', status: '已验真' },
  { code: '031001900211', number: '87654321', type: '增值税专用发票', seller: '上海贸易有限公司', amount: 5200, date: '2025-03-18', status: '已验真' },
  { code: '011001900211', number: '11112222', type: '电子发票', seller: '广州服务公司', amount: 1280, date: '2025-03-15', status: '待验真' },
  { code: '031001900211', number: '33334444', type: '增值税普通发票', seller: '深圳电子公司', amount: 680, date: '2025-03-10', status: '验真失败' },
])

const getVerifyType = (status: string) => {
  const map: Record<string, string> = {
    '已验真': 'success',
    '待验真': 'warning',
    '验真失败': 'danger'
  }
  return map[status] || 'info'
}
</script>
