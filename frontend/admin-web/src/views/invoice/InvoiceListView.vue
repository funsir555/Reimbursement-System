<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-gray-800">发票库</h1>
        <p class="mt-1 text-gray-500">发起导出、验真和 OCR 后，任务会在后台异步执行。</p>
      </div>
      <div class="flex items-center gap-3">
        <el-button v-if="can('archives:invoices:export')" :icon="Download" @click="handleExport" :loading="exporting">导出列表</el-button>
        <el-button v-if="can('archives:invoices:upload')" type="primary" :icon="Upload">上传发票</el-button>
      </div>
    </div>

    <div class="grid grid-cols-1 gap-4 md:grid-cols-4">
      <el-card>
        <div class="text-center">
          <p class="text-3xl font-bold text-blue-600">{{ invoiceList.length }}</p>
          <p class="mt-1 text-sm text-gray-500">发票总数</p>
        </div>
      </el-card>
      <el-card>
        <div class="text-center">
          <p class="text-3xl font-bold text-green-600">{{ verifiedCount }}</p>
          <p class="mt-1 text-sm text-gray-500">已验真</p>
        </div>
      </el-card>
      <el-card>
        <div class="text-center">
          <p class="text-3xl font-bold text-orange-600">{{ pendingVerifyCount }}</p>
          <p class="mt-1 text-sm text-gray-500">处理中</p>
        </div>
      </el-card>
      <el-card>
        <div class="text-center">
          <p class="text-3xl font-bold text-cyan-600">{{ recognizedCount }}</p>
          <p class="mt-1 text-sm text-gray-500">已识别</p>
        </div>
      </el-card>
    </div>

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
          <el-option label="验真中" value="running" />
          <el-option label="待验真" value="pending" />
          <el-option label="验真失败" value="failed" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="currentPage = 1">查询</el-button>
        <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
      </div>
    </el-card>

    <el-card>
      <el-table :data="pagedInvoiceList" style="width: 100%" v-loading="loading">
        <el-table-column prop="code" label="发票代码" width="130" />
        <el-table-column prop="number" label="发票号码" width="120" />
        <el-table-column prop="type" label="发票类型" width="150">
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
        <el-table-column prop="status" label="验真状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getVerifyType(row.status)" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ocrStatus" label="OCR 状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getOcrType(row.ocrStatus)" size="small" effect="plain">
              {{ row.ocrStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small">查看</el-button>
            <el-button
              v-if="can('archives:invoices:verify')"
              link
              type="primary"
              size="small"
              :disabled="row.status === '已验真' || row.status === '验真中'"
              @click="handleVerify(row)"
            >
              验真
            </el-button>
            <el-button
              v-if="can('archives:invoices:ocr')"
              link
              type="primary"
              size="small"
              :disabled="row.ocrStatus === '已识别' || row.ocrStatus === '识别中'"
              @click="handleOcr(row)"
            >
              OCR
            </el-button>
            <el-button v-if="can('archives:invoices:delete')" link type="danger" size="small" @click="removeInvoice(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-6 flex justify-end">
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
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { asyncTaskApi, invoiceApi, type InvoiceSummary } from '@/api'
import { hasPermission, readStoredUser } from '@/utils/permissions'
import { Download, Refresh, Search, Upload } from '@element-plus/icons-vue'

const searchQuery = ref('')
const filterType = ref('')
const filterStatus = ref('')
const loading = ref(false)
const exporting = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const invoiceList = ref<InvoiceSummary[]>([])
const permissionCodes = ref(readStoredUser()?.permissionCodes || [])

let pollingTimer: number | null = null

const can = (code: string) => hasPermission(code, permissionCodes.value)

const loadInvoices = async (silent = false) => {
  if (!silent) {
    loading.value = true
  }

  try {
    const res = await invoiceApi.list()
    invoiceList.value = res.data
    syncPolling()
  } catch (error: any) {
    ElMessage.error(error.message || '加载发票列表失败')
  } finally {
    if (!silent) {
      loading.value = false
    }
  }
}

const filteredInvoiceList = computed(() =>
  invoiceList.value.filter((item) => {
    const matchedKeyword =
      !searchQuery.value || item.code.includes(searchQuery.value) || item.number.includes(searchQuery.value)

    const matchedType =
      !filterType.value ||
      (filterType.value === 'special' && item.type === '增值税专用发票') ||
      (filterType.value === 'normal' && item.type === '增值税普通发票') ||
      (filterType.value === 'electronic' && item.type === '电子发票')

    const matchedStatus =
      !filterStatus.value ||
      (filterStatus.value === 'verified' && item.status === '已验真') ||
      (filterStatus.value === 'running' && item.status === '验真中') ||
      (filterStatus.value === 'pending' && item.status === '待验真') ||
      (filterStatus.value === 'failed' && item.status === '验真失败')

    return matchedKeyword && matchedType && matchedStatus
  })
)

const pagedInvoiceList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredInvoiceList.value.slice(start, start + pageSize.value)
})

const verifiedCount = computed(() => invoiceList.value.filter((item) => item.status === '已验真').length)
const pendingVerifyCount = computed(() =>
  invoiceList.value.filter((item) => item.status === '待验真' || item.status === '验真中').length
)
const recognizedCount = computed(() => invoiceList.value.filter((item) => item.ocrStatus === '已识别').length)
const hasRunningTask = computed(() =>
  invoiceList.value.some((item) => item.status === '验真中' || item.ocrStatus === '识别中')
)

const getVerifyType = (status: string) => {
  const map: Record<string, string> = {
    已验真: 'success',
    验真中: 'warning',
    待验真: 'info',
    验真失败: 'danger'
  }
  return map[status] || 'info'
}

const getOcrType = (status: string) => {
  const map: Record<string, string> = {
    已识别: 'success',
    识别中: 'warning',
    待识别: 'info',
    识别失败: 'danger'
  }
  return map[status] || 'info'
}

const handleExport = async () => {
  exporting.value = true
  try {
    const res = await asyncTaskApi.exportInvoices()
    ElMessage.success(res.message || '导出任务已提交，请到下载中心查看进度')
  } catch (error: any) {
    ElMessage.error(error.message || '导出任务提交失败')
  } finally {
    exporting.value = false
  }
}

const handleVerify = async (row: InvoiceSummary) => {
  try {
    const res = await asyncTaskApi.verifyInvoice({ code: row.code, number: row.number })
    ElMessage.success(res.message || '发票验真任务已提交')
    await loadInvoices(true)
  } catch (error: any) {
    ElMessage.error(error.message || '发票验真任务提交失败')
  }
}

const handleOcr = async (row: InvoiceSummary) => {
  try {
    const res = await asyncTaskApi.ocrInvoice({ code: row.code, number: row.number })
    ElMessage.success(res.message || 'OCR 任务已提交')
    await loadInvoices(true)
  } catch (error: any) {
    ElMessage.error(error.message || 'OCR 任务提交失败')
  }
}

const removeInvoice = (row: InvoiceSummary) => {
  invoiceList.value = invoiceList.value.filter((item) => !(item.code === row.code && item.number === row.number))
  ElMessage.success('已从当前列表移除')
  syncPolling()
}

const resetFilters = () => {
  searchQuery.value = ''
  filterType.value = ''
  filterStatus.value = ''
  currentPage.value = 1
}

const stopPolling = () => {
  if (pollingTimer !== null) {
    window.clearInterval(pollingTimer)
    pollingTimer = null
  }
}

const startPolling = () => {
  if (pollingTimer !== null) {
    return
  }
  pollingTimer = window.setInterval(() => {
    loadInvoices(true)
  }, 4000)
}

const syncPolling = () => {
  if (hasRunningTask.value) {
    startPolling()
    return
  }
  stopPolling()
}

onMounted(() => {
  loadInvoices()
})

onUnmounted(() => {
  stopPolling()
})
</script>
