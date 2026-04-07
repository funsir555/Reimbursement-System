<template>
  <div class="expense-wb-page expense-wb-page--list finance-query-voucher-page">
    <el-card class="expense-wb-toolbar expense-wb-toolbar--compact finance-query-voucher-toolbar">
      <div class="expense-wb-toolbar__row expense-wb-toolbar__row--compact finance-query-voucher-toolbar__top">
        <div class="expense-wb-toolbar__heading expense-wb-toolbar__heading--compact expense-wb-toolbar__heading--inline">
          <div>
            <p class="expense-wb-toolbar__title">查询凭证</p>
            <div class="expense-wb-toolbar__meta">
              <span class="expense-wb-soft-badge">当前公司 {{ financeCompany.currentCompanyName || '未设置' }}</span>
              <span class="expense-wb-soft-badge expense-wb-soft-badge--success">当前结果 {{ pager.total }}</span>
            </div>
          </div>
        </div>

        <div class="expense-wb-toolbar__actions">
          <el-button :loading="loading" @click="loadVouchers">刷新</el-button>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button :loading="exporting" :disabled="!canExport" @click="handleExport">下载</el-button>
        </div>
      </div>

      <div class="expense-wb-advanced-panel expense-wb-advanced-panel--embedded">
        <div class="expense-wb-advanced-grid finance-query-voucher-grid">
          <el-input v-model="filters.voucherNo" clearable placeholder="凭证号" />
          <el-select v-model="filters.csign" clearable placeholder="凭证类型">
            <el-option v-for="item in voucherTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-date-picker
            v-model="filters.billMonth"
            type="month"
            value-format="YYYY-MM"
            format="YYYY-MM"
            clearable
            placeholder="制单月份"
          />
          <el-date-picker
            v-model="filters.billMonthFrom"
            type="month"
            value-format="YYYY-MM"
            format="YYYY-MM"
            clearable
            placeholder="制单月份起"
            :disabled="Boolean(filters.billMonth)"
          />
          <el-date-picker
            v-model="filters.billMonthTo"
            type="month"
            value-format="YYYY-MM"
            format="YYYY-MM"
            clearable
            placeholder="制单月份止"
            :disabled="Boolean(filters.billMonth)"
          />
          <el-input v-model="filters.summary" clearable placeholder="摘要" />
        </div>
      </div>
    </el-card>

    <el-card class="expense-wb-panel expense-wb-table-shell expense-wb-table-shell--compact finance-query-voucher-table">
      <el-table :data="pager.items" v-loading="loading" stripe style="width: 100%" @row-dblclick="openDetail">
        <el-table-column prop="displayVoucherNo" label="凭证号" min-width="130" />
        <el-table-column prop="voucherTypeLabel" label="凭证类型" min-width="120" />
        <el-table-column prop="dbillDate" label="制单日期" min-width="120" />
        <el-table-column prop="iperiod" label="会计期间" min-width="100" />
        <el-table-column prop="summary" label="摘要" min-width="260" show-overflow-tooltip />
        <el-table-column prop="cbill" label="制单人" min-width="120" />
        <el-table-column prop="idoc" label="附件张数" min-width="100" />
        <el-table-column prop="totalDebit" label="借方合计" min-width="130" align="right" />
        <el-table-column prop="totalCredit" label="贷方合计" min-width="130" align="right" />
        <el-table-column label="状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="resolveStatusTagType(row.status)">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="expense-wb-pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pager.total"
          layout="total, sizes, prev, pager, next"
          @current-change="loadVouchers"
          @size-change="handlePageSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { financeApi, type FinanceVoucherMeta, type FinanceVoucherQueryParams, type FinanceVoucherSummary } from '@/api'
import { useFinanceCompanyStore } from '@/stores/financeCompany'
import { hasPermission, readStoredUser } from '@/utils/permissions'

const router = useRouter()
const financeCompany = useFinanceCompanyStore()

const loading = ref(false)
const exporting = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const voucherMeta = ref<FinanceVoucherMeta | null>(null)
const pager = reactive({
  total: 0,
  items: [] as FinanceVoucherSummary[]
})

const filters = reactive({
  voucherNo: '',
  csign: '',
  billMonth: '',
  billMonthFrom: '',
  billMonthTo: '',
  summary: ''
})

const canExport = computed(() => hasPermission('finance:general_ledger:query_voucher:export', readStoredUser()))
const voucherTypeOptions = computed(() => voucherMeta.value?.voucherTypeOptions || [])

watch(
  () => financeCompany.currentCompanyId,
  async (companyId, previousCompanyId) => {
    if (!companyId) return
    if (companyId !== previousCompanyId) {
      currentPage.value = 1
    }
    await loadVoucherMeta(companyId)
    await loadVouchers()
  },
  { immediate: true }
)

onMounted(async () => {
  if (!financeCompany.currentCompanyId) {
    await loadVouchers()
  }
})

function buildQueryParams(): FinanceVoucherQueryParams | null {
  const companyId = financeCompany.currentCompanyId
  if (!companyId) {
    return null
  }
  return {
    companyId,
    voucherNo: filters.voucherNo.trim() || undefined,
    csign: filters.csign || undefined,
    billMonth: filters.billMonth || undefined,
    billMonthFrom: filters.billMonth ? undefined : filters.billMonthFrom || undefined,
    billMonthTo: filters.billMonth ? undefined : filters.billMonthTo || undefined,
    summary: filters.summary.trim() || undefined,
    page: currentPage.value,
    pageSize: pageSize.value
  }
}

async function loadVoucherMeta(companyId = financeCompany.currentCompanyId) {
  if (!companyId) return
  try {
    const res = await financeApi.getVoucherMeta({ companyId })
    voucherMeta.value = res.data
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载凭证筛选项失败')
  }
}

async function loadVouchers() {
  const params = buildQueryParams()
  if (!params) return

  loading.value = true
  try {
    const res = await financeApi.listVouchers(params)
    pager.total = res.data.total
    pager.items = res.data.items || []
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载凭证列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadVouchers()
}

function handleReset() {
  filters.voucherNo = ''
  filters.csign = ''
  filters.billMonth = ''
  filters.billMonthFrom = ''
  filters.billMonthTo = ''
  filters.summary = ''
  currentPage.value = 1
  loadVouchers()
}

function handlePageSizeChange() {
  currentPage.value = 1
  loadVouchers()
}

async function handleExport() {
  const params = buildQueryParams()
  if (!params) return

  exporting.value = true
  try {
    await financeApi.exportVouchers({ ...params, page: undefined, pageSize: undefined })
    ElMessage.success('凭证导出已开始下载')
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '导出凭证失败')
  } finally {
    exporting.value = false
  }
}

function openDetail(row: FinanceVoucherSummary) {
  if (!row?.voucherNo) return
  router.push({
    name: 'finance-query-voucher-detail',
    params: { voucherNo: row.voucherNo }
  })
}

function resolveStatusTagType(status: string) {
  switch (status) {
    case 'POSTED':
      return 'success'
    case 'REVIEWED':
      return 'warning'
    default:
      return 'info'
  }
}
</script>

<style scoped>
.finance-query-voucher-page {
  gap: 14px;
}

.finance-query-voucher-toolbar {
  border-radius: 24px;
}

.finance-query-voucher-toolbar__top {
  align-items: flex-start;
}

.finance-query-voucher-grid {
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.finance-query-voucher-table {
  border-radius: 24px;
}

@media (max-width: 1400px) {
  .finance-query-voucher-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .finance-query-voucher-grid {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }
}
</style>
